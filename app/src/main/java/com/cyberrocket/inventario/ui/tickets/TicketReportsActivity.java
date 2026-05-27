package com.cyberrocket.inventario.ui.tickets;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.Chamado;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TicketReportsActivity extends AppCompatActivity {

    private TextView tvStartDateText;
    private TextView tvEndDateText;
    private TextView tvUserDashboardLabel;
    
    private TextView tvMetricTotal;
    private TextView tvMetricAvgTime;
    private TextView tvMetricCompletionRate;
    
    private RecyclerView rvReportsDaily;
    private TextView tvReportsEmpty;
    private ProgressBar pbReportsLoading;

    private Calendar calendarStart;
    private Calendar calendarEnd;
    
    private String loggedUser = "";
    private ArrayList<Chamado> allTickets = new ArrayList<>();
    private ArrayList<DailyStat> dailyStatsList = new ArrayList<>();
    private DailyStatsAdapter adapter;

    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat keyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_reports);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Relatórios de Chamados");
        }

        // Initialize UI Elements
        tvStartDateText = findViewById(R.id.tvStartDateText);
        tvEndDateText = findViewById(R.id.tvEndDateText);
        tvUserDashboardLabel = findViewById(R.id.tvUserDashboardLabel);
        
        tvMetricTotal = findViewById(R.id.tvMetricTotal);
        tvMetricAvgTime = findViewById(R.id.tvMetricAvgTime);
        tvMetricCompletionRate = findViewById(R.id.tvMetricCompletionRate);
        
        rvReportsDaily = findViewById(R.id.rvReportsDaily);
        tvReportsEmpty = findViewById(R.id.tvReportsEmpty);
        pbReportsLoading = findViewById(R.id.pbReportsLoading);

        LinearLayout btnStartDate = findViewById(R.id.btnStartDate);
        LinearLayout btnEndDate = findViewById(R.id.btnEndDate);

        // RecyclerView setup
        rvReportsDaily.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DailyStatsAdapter(dailyStatsList);
        rvReportsDaily.setAdapter(adapter);

        // Setup dates (Default: last 30 days)
        calendarEnd = Calendar.getInstance();
        calendarStart = Calendar.getInstance();
        calendarStart.add(Calendar.DAY_OF_MONTH, -30);

        updateDateLabels();

        // Get Logged User details
        Crud crud = new Crud();
        loggedUser = crud.SelectItem(this, "CONFIG", 1, 3);
        if (loggedUser == null || loggedUser.isEmpty()) {
            loggedUser = "Usuário";
        }
        tvUserDashboardLabel.setText("Relatório pessoal de: " + loggedUser);

        // Pickers
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));

        // Load data
        loadTicketsFromGLPI();
    }

    private void updateDateLabels() {
        tvStartDateText.setText(displayDateFormat.format(calendarStart.getTime()));
        tvEndDateText.setText(displayDateFormat.format(calendarEnd.getTime()));
    }

    private void showDatePicker(boolean isStart) {
        Calendar current = isStart ? calendarStart : calendarEnd;
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            
            if (isStart) {
                if (selected.after(calendarEnd)) {
                    Toast.makeText(this, "A data de início não pode ser após a data de fim.", Toast.LENGTH_SHORT).show();
                    return;
                }
                calendarStart = selected;
            } else {
                if (selected.before(calendarStart)) {
                    Toast.makeText(this, "A data de fim não pode ser antes da data de início.", Toast.LENGTH_SHORT).show();
                    return;
                }
                calendarEnd = selected;
            }
            
            updateDateLabels();
            calculateAndDisplayStatistics();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadTicketsFromGLPI() {
        pbReportsLoading.setVisibility(View.VISIBLE);
        GLPIConnect glpi = new GLPIConnect(this);
        
        // Fetch tickets with expand_dropdowns to get real requester usernames
        glpi.GetArray("/apirest.php/Ticket?expand_dropdowns=true&sort=id&order=DESC&range=0-1000", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                pbReportsLoading.setVisibility(View.GONE);
                allTickets.clear();

                try {
                    JSONArray array = new JSONArray(serverResponse);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        
                        String requester = obj.optString("users_id_recipient", "");
                        
                        // Filter by current user (case insensitive match)
                        boolean match = false;
                        if (!requester.isEmpty() && !loggedUser.isEmpty()) {
                            if (requester.toLowerCase().contains(loggedUser.toLowerCase()) 
                                || loggedUser.toLowerCase().contains(requester.toLowerCase())) {
                                match = true;
                            }
                        }

                        // If it matches, we add to reports list
                        if (match || loggedUser.equalsIgnoreCase("admin") || loggedUser.equalsIgnoreCase("glpi")) {
                            Chamado chamado = new Chamado();
                            chamado.setId(obj.optString("id"));
                            chamado.setTitulo(obj.optString("name"));
                            chamado.setDescricao(obj.optString("content"));
                            chamado.setDataCriacao(obj.optString("date"));
                            chamado.setDataFechamento(obj.optString("closedate"));
                            chamado.setTipo(String.valueOf(obj.optInt("type", 1)));
                            chamado.setStatusInfo(String.valueOf(obj.optInt("status", 1)));
                            chamado.setUsuarioRequerente(requester);

                            allTickets.add(chamado);
                        }
                    }

                    calculateAndDisplayStatistics();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(TicketReportsActivity.this, "Erro ao decodificar dados de chamados.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                pbReportsLoading.setVisibility(View.GONE);
                Toast.makeText(TicketReportsActivity.this, "Erro de conexão com o GLPI. Verifique sua rede.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void calculateAndDisplayStatistics() {
        if (allTickets.isEmpty()) {
            tvMetricTotal.setText("0");
            tvMetricAvgTime.setText("--");
            tvMetricCompletionRate.setText("0% (0 fechados)");
            tvReportsEmpty.setVisibility(View.VISIBLE);
            rvReportsDaily.setVisibility(View.GONE);
            return;
        }

        // Set date range parameters
        Date rangeStart = getZeroTimeDate(calendarStart.getTime());
        // Set end date to 23:59:59 to include full day
        Calendar endCal = (Calendar) calendarEnd.clone();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        Date rangeEnd = endCal.getTime();

        int totalCreated = 0;
        int totalClosed = 0;
        long totalExecutionTimeMs = 0;
        int closedWithTimeCount = 0;

        // Key: Date string ("yyyy-MM-dd"), Value: DailyStat object
        Map<String, DailyStat> dailyStatsMap = new HashMap<>();

        for (Chamado ticket : allTickets) {
            String creationStr = ticket.getDataCriacao();
            if (creationStr == null || creationStr.isEmpty()) continue;

            try {
                Date creationDate = apiDateFormat.parse(creationStr);
                if (creationDate == null) continue;

                // Check if inside the selected date range
                if (creationDate.after(rangeStart) && creationDate.before(rangeEnd)) {
                    totalCreated++;

                    // Check if closed
                    boolean isClosed = false;
                    String status = ticket.getStatusInfo();
                    String closeDateStr = ticket.getDataFechamento();

                    // status 5 = Resolvido, 6 = Fechado
                    if ("5".equals(status) || "6".equals(status) || (closeDateStr != null && !closeDateStr.isEmpty() && !closeDateStr.equals("null"))) {
                        isClosed = true;
                        totalClosed++;
                    }

                    // Calculation of execution time
                    long executionTime = -1;
                    if (isClosed && closeDateStr != null && !closeDateStr.isEmpty() && !closeDateStr.equals("null")) {
                        Date closeDate = apiDateFormat.parse(closeDateStr);
                        if (closeDate != null) {
                            executionTime = closeDate.getTime() - creationDate.getTime();
                            if (executionTime >= 0) {
                                totalExecutionTimeMs += executionTime;
                                closedWithTimeCount++;
                            }
                        }
                    }

                    // Group by date
                    String dayKey = keyDateFormat.format(creationDate);
                    DailyStat stat = dailyStatsMap.get(dayKey);
                    if (stat == null) {
                        stat = new DailyStat(dayKey, creationDate);
                        dailyStatsMap.put(dayKey, stat);
                    }

                    stat.ticketsCreated++;
                    if (isClosed) {
                        stat.ticketsClosed++;
                        if (executionTime >= 0) {
                            stat.totalExecutionTime += executionTime;
                            stat.closedTicketsCount++;
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Update top-level dashboard metrics
        tvMetricTotal.setText(String.valueOf(totalCreated));
        
        if (closedWithTimeCount > 0) {
            long avgTimeMs = totalExecutionTimeMs / closedWithTimeCount;
            tvMetricAvgTime.setText(formatDuration(avgTimeMs));
        } else {
            tvMetricAvgTime.setText("--");
        }

        if (totalCreated > 0) {
            int rate = (totalClosed * 100) / totalCreated;
            tvMetricCompletionRate.setText(rate + "% (" + totalClosed + " resolvidos)");
        } else {
            tvMetricCompletionRate.setText("0% (0 resolvidos)");
        }

        // Populate daily reports list
        dailyStatsList.clear();
        dailyStatsList.addAll(dailyStatsMap.values());

        // Sort dates decrescendo (most recent first)
        Collections.sort(dailyStatsList, (s1, s2) -> s2.dateRaw.compareTo(s1.dateRaw));

        if (dailyStatsList.isEmpty()) {
            tvReportsEmpty.setVisibility(View.VISIBLE);
            rvReportsDaily.setVisibility(View.GONE);
        } else {
            tvReportsEmpty.setVisibility(View.GONE);
            rvReportsDaily.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private Date getZeroTimeDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    // Formatter for times
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            long remainingHours = hours % 24;
            if (remainingHours > 0) {
                return days + "d " + remainingHours + "h";
            } else {
                return days + " dias";
            }
        } else if (hours > 0) {
            long remainingMinutes = minutes % 60;
            if (remainingMinutes > 0) {
                return hours + "h " + remainingMinutes + "m";
            } else {
                return hours + " horas";
            }
        } else if (minutes > 0) {
            return minutes + " minutos";
        } else {
            return "Menos de 1m";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper model representing daily statistics
    private static class DailyStat {
        String dateKey;
        Date dateRaw;
        int ticketsCreated = 0;
        int ticketsClosed = 0;
        long totalExecutionTime = 0;
        int closedTicketsCount = 0;

        DailyStat(String dateKey, Date dateRaw) {
            this.dateKey = dateKey;
            this.dateRaw = dateRaw;
        }
    }

    // Adapter for RecyclerView
    private class DailyStatsAdapter extends RecyclerView.Adapter<DailyStatsAdapter.ViewHolder> {
        private final List<DailyStat> stats;
        private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale.getDefault());

        DailyStatsAdapter(List<DailyStat> stats) {
            this.stats = stats;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_report_date, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DailyStat stat = stats.get(position);

            holder.tvDay.setText(dayFormat.format(stat.dateRaw));
            holder.tvMonth.setText(monthFormat.format(stat.dateRaw).toUpperCase());
            
            // Format full date (first letter capitalized)
            String fullDateStr = fullDateFormat.format(stat.dateRaw);
            if (fullDateStr.length() > 0) {
                fullDateStr = fullDateStr.substring(0, 1).toUpperCase() + fullDateStr.substring(1);
            }
            holder.tvDateFull.setText(fullDateStr);

            String countStr = stat.ticketsCreated + " aberto(s)";
            if (stat.ticketsClosed > 0) {
                countStr += " • " + stat.ticketsClosed + " resolvido(s)";
            }
            holder.tvCounts.setText(countStr);

            if (stat.closedTicketsCount > 0) {
                long avgMs = stat.totalExecutionTime / stat.closedTicketsCount;
                holder.tvExecutionTime.setVisibility(View.VISIBLE);
                holder.tvExecutionTime.setText("Tempo médio de execução: " + formatDuration(avgMs));
            } else {
                holder.tvExecutionTime.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return stats.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay, tvMonth, tvDateFull, tvCounts, tvExecutionTime;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tvReportDay);
                tvMonth = itemView.findViewById(R.id.tvReportMonth);
                tvDateFull = itemView.findViewById(R.id.tvReportDateFull);
                tvCounts = itemView.findViewById(R.id.tvReportTicketCounts);
                tvExecutionTime = itemView.findViewById(R.id.tvReportExecutionTime);
            }
        }
    }
}
