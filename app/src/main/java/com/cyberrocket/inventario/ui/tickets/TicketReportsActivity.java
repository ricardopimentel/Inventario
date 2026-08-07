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

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.text.TextPaint;
import android.text.StaticLayout;
import android.text.Layout;
import android.net.Uri;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.os.ParcelFileDescriptor;
import java.io.FileOutputStream;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TicketReportsActivity extends AppCompatActivity {

    private TextView tvStartDateText;
    private TextView tvEndDateText;
    private TextView tvUserDashboardLabel;
    
    private TextView tvMetricTotal;
    private TextView tvMetricAvgTime;
    private TextView tvMetricCompletionRate;
    private TextView tvMetricPending;
    private TextView tvMetricClosedCount;
    
    private View statusSegmentNew;
    private View statusSegmentPending;
    private View statusSegmentClosed;
    
    private View typeSegmentIncident;
    private View typeSegmentRequest;
    
    private TextView tvLegendNew;
    private TextView tvLegendPending;
    private TextView tvLegendClosed;
    private TextView tvLegendIncident;
    private TextView tvLegendRequest;
    
    private View cardReportsEmpty;
    
    private RecyclerView rvReportsDaily;
    private TextView tvReportsEmpty;
    private ProgressBar pbReportsLoading;

    // PGD Dashboard integration
    private View llMetricsDashboardContainer;
    private View cardDistributions;
    private View llDetailedSectionContainer;
    private View llPgdDashboardSection;
    private View cardPgdAiGenerator;
    private View cardPgdAiLoading;
    private View cardPgdReportResult;
    private TextView tvPgdDashboardReportText;
    private String pgdReportText = "";

    private Calendar calendarStart;
    private Calendar calendarEnd;
    
    private String loggedUser = "";
    private ArrayList<Chamado> allTickets = new ArrayList<>();
    private ArrayList<DailyStat> dailyStatsList = new ArrayList<>();
    private DailyStatsAdapter adapter;

    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat keyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private int currentTab = 0;
    private FloatingActionButton fabExportPdf;
    private ActivityResultLauncher<String> createPdfLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_reports);

        createPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> {
                if (uri != null) {
                    generatePdfToUri(uri);
                }
            }
        );

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
        tvMetricPending = findViewById(R.id.tvMetricPending);
        tvMetricClosedCount = findViewById(R.id.tvMetricClosedCount);
        
        statusSegmentNew = findViewById(R.id.statusSegmentNew);
        statusSegmentPending = findViewById(R.id.statusSegmentPending);
        statusSegmentClosed = findViewById(R.id.statusSegmentClosed);
        
        typeSegmentIncident = findViewById(R.id.typeSegmentIncident);
        typeSegmentRequest = findViewById(R.id.typeSegmentRequest);
        
        tvLegendNew = findViewById(R.id.tvLegendNew);
        tvLegendPending = findViewById(R.id.tvLegendPending);
        tvLegendClosed = findViewById(R.id.tvLegendClosed);
        tvLegendIncident = findViewById(R.id.tvLegendIncident);
        tvLegendRequest = findViewById(R.id.tvLegendRequest);
        
        cardReportsEmpty = findViewById(R.id.cardReportsEmpty);
        
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

        // Initialize new views for PGD slider
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroupDashboard = findViewById(R.id.toggleGroupDashboard);
        llMetricsDashboardContainer = findViewById(R.id.llMetricsDashboardContainer);
        cardDistributions = findViewById(R.id.cardDistributions);
        llDetailedSectionContainer = findViewById(R.id.llDetailedSectionContainer);
        llPgdDashboardSection = findViewById(R.id.llPgdDashboardSection);
        cardPgdAiGenerator = findViewById(R.id.cardPgdAiGenerator);
        cardPgdAiLoading = findViewById(R.id.cardPgdAiLoading);
        cardPgdReportResult = findViewById(R.id.cardPgdReportResult);
        tvPgdDashboardReportText = findViewById(R.id.tvPgdDashboardReportText);
        if (tvPgdDashboardReportText != null) {
            tvPgdDashboardReportText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
        Button btnGeneratePgdAiReport = findViewById(R.id.btnGeneratePgdAiReport);

        if (toggleGroupDashboard != null) {
            toggleGroupDashboard.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btnDashboardChamados) {
                        currentTab = 0;
                        if (llMetricsDashboardContainer != null) llMetricsDashboardContainer.setVisibility(View.VISIBLE);
                        if (cardDistributions != null) cardDistributions.setVisibility(View.VISIBLE);
                        if (llDetailedSectionContainer != null) llDetailedSectionContainer.setVisibility(View.VISIBLE);
                        if (llPgdDashboardSection != null) llPgdDashboardSection.setVisibility(View.GONE);
                        calculateAndDisplayStatistics();
                    } else if (checkedId == R.id.btnDashboardPGD) {
                        currentTab = 1;
                        if (llMetricsDashboardContainer != null) llMetricsDashboardContainer.setVisibility(View.GONE);
                        if (cardDistributions != null) cardDistributions.setVisibility(View.GONE);
                        if (llDetailedSectionContainer != null) llDetailedSectionContainer.setVisibility(View.GONE);
                        if (cardReportsEmpty != null) cardReportsEmpty.setVisibility(View.GONE);
                        if (llPgdDashboardSection != null) llPgdDashboardSection.setVisibility(View.VISIBLE);
                        updatePgdSectionVisibility();
                    }
                }
            });
        }

        if (btnGeneratePgdAiReport != null) {
            btnGeneratePgdAiReport.setOnClickListener(v -> triggerPgdAiReportGeneration());
        }

        fabExportPdf = findViewById(R.id.fabExportPdf);
        if (fabExportPdf != null) {
            fabExportPdf.setOnClickListener(v -> {
                if (currentTab == 0) {
                    String fileName = "Relatorio_Chamados_" + System.currentTimeMillis() + ".pdf";
                    createPdfLauncher.launch(fileName);
                } else {
                    if (pgdReportText == null || pgdReportText.trim().isEmpty()) {
                        Toast.makeText(this, "Gere o relatório da IA antes de exportar em PDF.", Toast.LENGTH_SHORT).show();
                    } else {
                        String fileName = "Relatorio_PGD_" + System.currentTimeMillis() + ".pdf";
                        createPdfLauncher.launch(fileName);
                    }
                }
            });
        }

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
            pgdReportText = "";
            updatePgdSectionVisibility();
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
                        String lastUpdater = obj.optString("users_id_lastupdater", "");
                        
                        // Filter by current user (case insensitive match in either requester or last updater)
                        boolean match = false;
                        if (!loggedUser.isEmpty()) {
                            String lowerUser = loggedUser.toLowerCase();
                            if (!requester.isEmpty() && (requester.toLowerCase().contains(lowerUser) || lowerUser.contains(requester.toLowerCase()))) {
                                match = true;
                            }
                            if (!lastUpdater.isEmpty() && (lastUpdater.toLowerCase().contains(lowerUser) || lowerUser.contains(lastUpdater.toLowerCase()))) {
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
                            chamado.setCategoria(obj.optString("itilcategories_id"));

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
            tvMetricCompletionRate.setText("0%");
            if (tvMetricClosedCount != null) tvMetricClosedCount.setText("0 resolvidos");
            if (tvMetricPending != null) tvMetricPending.setText("0");
            
            if (cardReportsEmpty != null) {
                cardReportsEmpty.setVisibility(View.VISIBLE);
            } else {
                tvReportsEmpty.setVisibility(View.VISIBLE);
            }
            rvReportsDaily.setVisibility(View.GONE);
            
            if (tvLegendNew != null) tvLegendNew.setText("Novo (0)");
            if (tvLegendPending != null) tvLegendPending.setText("Pendente (0)");
            if (tvLegendClosed != null) tvLegendClosed.setText("Resolvido (0)");
            if (tvLegendIncident != null) tvLegendIncident.setText("Incidente (0)");
            if (tvLegendRequest != null) tvLegendRequest.setText("Requisição (0)");
            
            if (statusSegmentNew != null) statusSegmentNew.setVisibility(View.GONE);
            if (statusSegmentPending != null) statusSegmentPending.setVisibility(View.GONE);
            if (statusSegmentClosed != null) statusSegmentClosed.setVisibility(View.GONE);
            if (typeSegmentIncident != null) typeSegmentIncident.setVisibility(View.GONE);
            if (typeSegmentRequest != null) typeSegmentRequest.setVisibility(View.GONE);
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

        // Breakdown counters for Segmented Charts
        int countNew = 0;
        int countPending = 0;
        int countClosed = 0;
        int countIncident = 0;
        int countRequest = 0;

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
                        countClosed++;
                    } else if ("4".equals(status)) {
                        countPending++;
                    } else {
                        countNew++;
                    }

                    // Ticket type breakdown (1 = Incident, 2 = Request)
                    String type = ticket.getTipo();
                    if ("2".equals(type)) {
                        countRequest++;
                    } else {
                        countIncident++;
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
            tvMetricCompletionRate.setText(rate + "%");
            if (tvMetricClosedCount != null) {
                tvMetricClosedCount.setText(totalClosed + " resolvidos");
            }
        } else {
            tvMetricCompletionRate.setText("0%");
            if (tvMetricClosedCount != null) {
                tvMetricClosedCount.setText("0 resolvidos");
            }
        }

        if (tvMetricPending != null) {
            tvMetricPending.setText(String.valueOf(countNew + countPending));
        }

        // Update dynamic legends
        if (tvLegendNew != null) tvLegendNew.setText("Novo (" + countNew + ")");
        if (tvLegendPending != null) tvLegendPending.setText("Pendente (" + countPending + ")");
        if (tvLegendClosed != null) tvLegendClosed.setText("Resolvido (" + countClosed + ")");
        if (tvLegendIncident != null) tvLegendIncident.setText("Incidente (" + countIncident + ")");
        if (tvLegendRequest != null) tvLegendRequest.setText("Requisição (" + countRequest + ")");

        // Update segmented bars
        if (statusSegmentNew != null && statusSegmentPending != null && statusSegmentClosed != null) {
            updateSegmentedBar(statusSegmentNew, countNew, statusSegmentPending, countPending, statusSegmentClosed, countClosed);
        }
        if (typeSegmentIncident != null && typeSegmentRequest != null) {
            updateSegmentedBar2(typeSegmentIncident, countIncident, typeSegmentRequest, countRequest);
        }

        // Populate daily reports list
        dailyStatsList.clear();
        dailyStatsList.addAll(dailyStatsMap.values());

        // Sort dates decrescendo (most recent first)
        Collections.sort(dailyStatsList, (s1, s2) -> s2.dateRaw.compareTo(s1.dateRaw));

        if (dailyStatsList.isEmpty()) {
            if (cardReportsEmpty != null) {
                cardReportsEmpty.setVisibility(View.VISIBLE);
            } else {
                tvReportsEmpty.setVisibility(View.VISIBLE);
            }
            rvReportsDaily.setVisibility(View.GONE);
        } else {
            if (cardReportsEmpty != null) {
                cardReportsEmpty.setVisibility(View.GONE);
            } else {
                tvReportsEmpty.setVisibility(View.GONE);
            }
            rvReportsDaily.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void updateSegmentedBar(View seg1, int count1, View seg2, int count2, View seg3, int count3) {
        int total = count1 + count2 + count3;
        if (total == 0) {
            seg1.setVisibility(View.GONE);
            seg2.setVisibility(View.GONE);
            seg3.setVisibility(View.GONE);
            return;
        }

        float w1 = (count1 * 100f) / total;
        float w2 = (count2 * 100f) / total;
        float w3 = (count3 * 100f) / total;

        List<View> visibleSegs = new ArrayList<>();
        List<String> colors = new ArrayList<>();

        if (count1 > 0) {
            seg1.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) seg1.getLayoutParams();
            lp.weight = w1;
            seg1.setLayoutParams(lp);
            visibleSegs.add(seg1);
            colors.add("#2196F3");
        } else {
            seg1.setVisibility(View.GONE);
        }

        if (count2 > 0) {
            seg2.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) seg2.getLayoutParams();
            lp.weight = w2;
            seg2.setLayoutParams(lp);
            visibleSegs.add(seg2);
            colors.add("#FF9800");
        } else {
            seg2.setVisibility(View.GONE);
        }

        if (count3 > 0) {
            seg3.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) seg3.getLayoutParams();
            lp.weight = w3;
            seg3.setLayoutParams(lp);
            visibleSegs.add(seg3);
            colors.add("#4CAF50");
        } else {
            seg3.setVisibility(View.GONE);
        }

        if (visibleSegs.size() == 1) {
            visibleSegs.get(0).setBackgroundResource(R.drawable.bg_segment_all);
            visibleSegs.get(0).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(0))));
        } else if (visibleSegs.size() == 2) {
            visibleSegs.get(0).setBackgroundResource(R.drawable.bg_segment_left);
            visibleSegs.get(0).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(0))));
            
            visibleSegs.get(1).setBackgroundResource(R.drawable.bg_segment_right);
            visibleSegs.get(1).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(1))));
        } else if (visibleSegs.size() == 3) {
            visibleSegs.get(0).setBackgroundResource(R.drawable.bg_segment_left);
            visibleSegs.get(0).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(0))));
            
            visibleSegs.get(1).setBackgroundResource(R.drawable.bg_segment_middle);
            visibleSegs.get(1).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(1))));
            
            visibleSegs.get(2).setBackgroundResource(R.drawable.bg_segment_right);
            visibleSegs.get(2).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(2))));
        }
    }

    private void updateSegmentedBar2(View seg1, int count1, View seg2, int count2) {
        int total = count1 + count2;
        if (total == 0) {
            seg1.setVisibility(View.GONE);
            seg2.setVisibility(View.GONE);
            return;
        }

        float w1 = (count1 * 100f) / total;
        float w2 = (count2 * 100f) / total;

        List<View> visibleSegs = new ArrayList<>();
        List<String> colors = new ArrayList<>();

        if (count1 > 0) {
            seg1.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) seg1.getLayoutParams();
            lp.weight = w1;
            seg1.setLayoutParams(lp);
            visibleSegs.add(seg1);
            colors.add("#9C27B0");
        } else {
            seg1.setVisibility(View.GONE);
        }

        if (count2 > 0) {
            seg2.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) seg2.getLayoutParams();
            lp.weight = w2;
            seg2.setLayoutParams(lp);
            visibleSegs.add(seg2);
            colors.add("#009688");
        } else {
            seg2.setVisibility(View.GONE);
        }

        if (visibleSegs.size() == 1) {
            visibleSegs.get(0).setBackgroundResource(R.drawable.bg_segment_all);
            visibleSegs.get(0).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(0))));
        } else if (visibleSegs.size() == 2) {
            visibleSegs.get(0).setBackgroundResource(R.drawable.bg_segment_left);
            visibleSegs.get(0).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(0))));
            
            visibleSegs.get(1).setBackgroundResource(R.drawable.bg_segment_right);
            visibleSegs.get(1).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colors.get(1))));
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
                holder.tvExecutionTime.setText("Média: " + formatDuration(avgMs));
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

    private void updatePgdSectionVisibility() {
        if (pgdReportText.isEmpty()) {
            if (cardPgdAiGenerator != null) cardPgdAiGenerator.setVisibility(View.VISIBLE);
            if (cardPgdAiLoading != null) cardPgdAiLoading.setVisibility(View.GONE);
            if (cardPgdReportResult != null) cardPgdReportResult.setVisibility(View.GONE);
        } else {
            if (cardPgdAiGenerator != null) cardPgdAiGenerator.setVisibility(View.GONE);
            if (cardPgdAiLoading != null) cardPgdAiLoading.setVisibility(View.GONE);
            if (cardPgdReportResult != null) cardPgdReportResult.setVisibility(View.VISIBLE);
            if (tvPgdDashboardReportText != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    tvPgdDashboardReportText.setText(android.text.Html.fromHtml(pgdReportText, android.text.Html.FROM_HTML_MODE_LEGACY));
                } else {
                    tvPgdDashboardReportText.setText(android.text.Html.fromHtml(pgdReportText));
                }
            }
        }
    }

    private void triggerPgdAiReportGeneration() {
        ArrayList<Chamado> pgdTickets = new ArrayList<>();
        Date rangeStart = getZeroTimeDate(calendarStart.getTime());
        Calendar endCal = (Calendar) calendarEnd.clone();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        Date rangeEnd = endCal.getTime();

        for (Chamado c : allTickets) {
            String creationStr = c.getDataCriacao();
            if (creationStr == null || creationStr.isEmpty()) continue;
            try {
                Date creationDate = apiDateFormat.parse(creationStr);
                if (creationDate != null && creationDate.after(rangeStart) && creationDate.before(rangeEnd)) {
                    if (c.getCategoria() != null && c.getCategoria().toLowerCase().startsWith("pgd")) {
                        pgdTickets.add(c);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pgdTickets.isEmpty()) {
            Toast.makeText(this, "Nenhuma atividade PGD encontrada neste período.", Toast.LENGTH_LONG).show();
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("GEMINI_PREFS", MODE_PRIVATE);
        String apiKey = prefs.getString("api_key", "");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(this, "Por favor, configure sua chave de API do Gemini nas Configurações de Perfil.", Toast.LENGTH_LONG).show();
            return;
        }

        if (cardPgdAiGenerator != null) cardPgdAiGenerator.setVisibility(View.GONE);
        if (cardPgdAiLoading != null) cardPgdAiLoading.setVisibility(View.VISIBLE);
        if (cardPgdReportResult != null) cardPgdReportResult.setVisibility(View.GONE);

        android.content.SharedPreferences glpiPrefs = getSharedPreferences("GLPI_PREFS", MODE_PRIVATE);
        String host = glpiPrefs.getString("glpi_host", "");
        String dbHost = new Crud().SelectItem(this, "CONFIG", 1, 1);
        String baseUrl = (dbHost != null && !dbHost.isEmpty()) ? dbHost : host;

        StringBuilder ticketsData = new StringBuilder();
        for (Chamado c : pgdTickets) {
            String ticketUrl = baseUrl + "/front/ticket.form.php?id=" + c.getId();
            ticketsData.append("- ID: ").append(c.getId()).append("\n");
            ticketsData.append("  Categoria: ").append(c.getCategoria()).append("\n");
            ticketsData.append("  Título: ").append(c.getTitulo()).append("\n");
            ticketsData.append("  Descrição: ").append(c.getDescricao()).append("\n");
            ticketsData.append("  Link: ").append(ticketUrl).append("\n\n");
        }

        String prompt = "Você é um assistente inteligente. Analise a seguinte lista de atividades de trabalho remoto (PGD) registradas pelo usuário. "
                + "Seu objetivo é gerar um relatório consolidado e profissional em português de tudo o que foi realizado, agrupado obrigatoriamente por categoria/subcategory.\n\n"
                + "Para cada categoria encontrada nas atividades:\n"
                + "1. Informe a quantidade de chamados encontrados sob aquela categoria (exemplo: '<b>Quantidade de Chamados:</b> X').\n"
                + "2. Faça um resumo conciso e formal do que foi concluído pelo usuário naquela categoria (consolidando atividades similares se necessário).\n"
                + "3. Insira no final da categoria ou do relatório as referências aos chamados associados, com um chamado por linha (usando a tag HTML <br> para que fiquem um abaixo do outro de forma bem organizada). Cada referência deve ser no formato de link HTML com o número e o título do chamado (exemplo: '<b>Referências:</b><br><a href=\"[Link]\">Chamado #[ID] - [Título]</a><br><a href=\"[Link]\">Chamado #[ID2] - [Título2]</a>').\n\n"
                + "Importante: Retorne a resposta formatada EXCLUSIVAMENTE em tags HTML básicas como <b>, <i>, <ul>, <li>, <br> e <a href=\"...\"> para que possa ser exibida diretamente em um TextView Android com links clicáveis. Não adicione tags de bloco markdown como ```html ou ```. Seja elegante e direto.\n\n"
                + "Lista de Atividades PGD:\n"
                + ticketsData.toString();

        generatePgdAiReportInternal("gemini-2.5-flash", prompt, apiKey);
    }

    private void generatePgdAiReportInternal(final String modelName, final String prompt, final String apiKey) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
        
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();
            partObj.put("text", prompt);
            partsArray.put(partObj);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);
            jsonBody.put("contents", contentsArray);

            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                    com.android.volley.Request.Method.POST, url, jsonBody,
                    response -> {
                        try {
                            JSONArray candidates = response.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String textResponse = parts.getJSONObject(0).getString("text");

                            pgdReportText = textResponse.replace("```html", "").replace("```", "").trim();
                            updatePgdSectionVisibility();
                        } catch (Exception e) {
                            e.printStackTrace();
                            updatePgdSectionVisibility();
                            Toast.makeText(this, "Erro ao processar resposta do Gemini.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        int statusCode = error.networkResponse != null ? error.networkResponse.statusCode : -1;
                        boolean isTimeout = error instanceof com.android.volley.TimeoutError;

                        if (statusCode == 404 || statusCode == 429 || isTimeout) {
                            if (modelName.equals("gemini-2.5-flash")) {
                                generatePgdAiReportInternal("gemini-2.5-flash-lite", prompt, apiKey);
                                return;
                            } else if (modelName.equals("gemini-2.5-flash-lite")) {
                                generatePgdAiReportInternal("gemini-2.0-flash", prompt, apiKey);
                                return;
                            } else if (modelName.equals("gemini-2.0-flash")) {
                                generatePgdAiReportInternal("gemini-2.0-flash-lite", prompt, apiKey);
                                return;
                            }
                        }

                        updatePgdSectionVisibility();
                        String errMsg = "Erro de conexão com o Gemini.";
                        if (error.networkResponse != null) {
                            String responseBody = new String(error.networkResponse.data);
                            android.util.Log.e("GeminiPGD", "Code: " + statusCode + ", body: " + responseBody);
                            if (statusCode == 429) {
                                errMsg = "Cota da API do Gemini esgotada.";
                            }
                        }
                        Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                    }
            );

            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    30000,
                    0,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            com.android.volley.toolbox.Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
            updatePgdSectionVisibility();
            Toast.makeText(this, "Erro ao preparar requisição do Gemini.", Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePdfToUri(Uri uri) {
        PdfDocument pdfDocument = new PdfDocument();
        int pageHeight = 1120;
        int pageWidth = 792;
        int margins = 40;
        
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        
        try {
            if (currentTab == 0) {
                // Generate Chamados Report
                int currentPage = 0;
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                
                int y = margins + 30;
                
                // Draw Title
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(22f);
                canvas.drawText("Relatório de Estatísticas de Chamados", margins, y, paint);
                y += 25;
                
                // Draw period
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(12f);
                String startStr = displayDateFormat.format(calendarStart.getTime());
                String endStr = displayDateFormat.format(calendarEnd.getTime());
                canvas.drawText("Período: " + startStr + " até " + endStr, margins, y, paint);
                y += 18;
                
                // Draw user
                canvas.drawText("Solicitante: " + loggedUser, margins, y, paint);
                y += 20;
                
                // Divider line
                paint.setStrokeWidth(1.5f);
                canvas.drawLine(margins, y, pageWidth - margins, y, paint);
                y += 25;
                
                // Metrics
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(14f);
                canvas.drawText("Métricas Consolidadas", margins, y, paint);
                y += 20;
                
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(12f);
                canvas.drawText("• Total de Chamados: " + tvMetricTotal.getText().toString(), margins + 15, y, paint);
                y += 18;
                canvas.drawText("• Tempo Médio de Atendimento: " + tvMetricAvgTime.getText().toString(), margins + 15, y, paint);
                y += 18;
                canvas.drawText("• Taxa de Conclusão: " + tvMetricCompletionRate.getText().toString(), margins + 15, y, paint);
                y += 18;
                canvas.drawText("• Chamados Pendentes: " + (tvMetricPending != null ? tvMetricPending.getText().toString() : "0"), margins + 15, y, paint);
                y += 18;
                canvas.drawText("• Chamados Resolvidos: " + (tvMetricClosedCount != null ? tvMetricClosedCount.getText().toString() : "0"), margins + 15, y, paint);
                y += 30;
                
                // Daily statistics header
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(14f);
                canvas.drawText("Histórico Diário de Atividades", margins, y, paint);
                y += 20;
                
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(11f);
                
                SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale.getDefault());
                
                for (DailyStat stat : dailyStatsList) {
                    // Check if we need a new page
                    if (y + 50 > pageHeight - margins) {
                        pdfDocument.finishPage(page);
                        currentPage++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage + 1).create();
                        page = pdfDocument.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = margins + 30;
                        
                        // Header on new page
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        paint.setTextSize(12f);
                        canvas.drawText("Histórico Diário de Atividades (continuação)", margins, y, paint);
                        y += 20;
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        paint.setTextSize(11f);
                    }
                    
                    String fullDateStr = fullDateFormat.format(stat.dateRaw);
                    if (fullDateStr.length() > 0) {
                        fullDateStr = fullDateStr.substring(0, 1).toUpperCase() + fullDateStr.substring(1);
                    }
                    
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText(fullDateStr, margins + 10, y, paint);
                    y += 15;
                    
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    String countStr = "  • Criados: " + stat.ticketsCreated + " | Resolvidos: " + stat.ticketsClosed;
                    if (stat.closedTicketsCount > 0) {
                        long avgMs = stat.totalExecutionTime / stat.closedTicketsCount;
                        countStr += " | Tempo Médio: " + formatDuration(avgMs);
                    }
                    canvas.drawText(countStr, margins + 10, y, paint);
                    y += 20;
                }
                
                pdfDocument.finishPage(page);
                
            } else {
                // Transform PGD HTML references for PDF by appending the visible URL in parentheses inside the anchor.
                // We use a bulletproof regex to match variations of HTML anchors with single/double quotes, spaces, other attributes, and nested formatting tags.
                String pdfHtml = pgdReportText.replaceAll("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>((?:(?!</a>).)+)</a>", "<a href=\"$1\">$2 ($1)</a>");
                CharSequence spannedText;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    spannedText = android.text.Html.fromHtml(pdfHtml, android.text.Html.FROM_HTML_MODE_LEGACY);
                } else {
                    spannedText = android.text.Html.fromHtml(pdfHtml);
                }
                
                int currentPage = 0;
                int startY = margins + 100;
                int printableWidth = pageWidth - 2 * margins;
                
                TextPaint textPaintObj = new TextPaint(paint);
                textPaintObj.setColor(Color.BLACK);
                textPaintObj.setTextSize(12f);
                textPaintObj.linkColor = Color.parseColor("#0066CC"); // Explicitly set a visible color for links so they don't draw transparent
                
                StaticLayout staticLayout;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    staticLayout = StaticLayout.Builder.obtain(spannedText, 0, spannedText.length(), textPaintObj, printableWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0.0f, 1.1f)
                        .setIncludePad(false)
                        .build();
                } else {
                    staticLayout = new StaticLayout(spannedText, textPaintObj, printableWidth, Layout.Alignment.ALIGN_NORMAL, 1.1f, 0.0f, false);
                }
                
                int totalLines = staticLayout.getLineCount();
                int firstLineOfPage = 0;
                
                while (firstLineOfPage < totalLines) {
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage + 1).create();
                    PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();
                    
                    int currentStartY = startY;
                    if (currentPage == 0) {
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        paint.setTextSize(22f);
                        canvas.drawText("Relatório PGD", margins, margins + 30, paint);
                        
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        paint.setTextSize(12f);
                        String startStr = displayDateFormat.format(calendarStart.getTime());
                        String endStr = displayDateFormat.format(calendarEnd.getTime());
                        canvas.drawText("Período: " + startStr + " até " + endStr, margins, margins + 55, paint);
                        canvas.drawText("Solicitante: " + loggedUser, margins, margins + 72, paint);
                        
                        paint.setStrokeWidth(1.5f);
                        canvas.drawLine(margins, margins + 82, pageWidth - margins, margins + 82, paint);
                    } else {
                        currentStartY = margins + 40;
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        paint.setTextSize(10f);
                        canvas.drawText("Relatório PGD - Continuação - Página " + (currentPage + 1), margins, margins + 10, paint);
                        paint.setStrokeWidth(1f);
                        canvas.drawLine(margins, margins + 20, pageWidth - margins, margins + 20, paint);
                    }
                    
                    int maxPageHeight = pageHeight - currentStartY - margins;
                    
                    int lastFittingLine = firstLineOfPage;
                    while (lastFittingLine < totalLines && 
                           (staticLayout.getLineBottom(lastFittingLine) - staticLayout.getLineTop(firstLineOfPage)) <= maxPageHeight) {
                        lastFittingLine++;
                    }
                    if (lastFittingLine == firstLineOfPage) {
                        lastFittingLine++; // ensure at least one line is drawn to avoid infinite loop
                    }
                    
                    int drawHeight = staticLayout.getLineBottom(lastFittingLine - 1) - staticLayout.getLineTop(firstLineOfPage);
                    
                    canvas.save();
                    canvas.clipRect(margins, currentStartY, pageWidth - margins, currentStartY + drawHeight);
                    canvas.translate(margins, currentStartY - staticLayout.getLineTop(firstLineOfPage));
                    staticLayout.draw(canvas);
                    canvas.restore();
                    
                    pdfDocument.finishPage(page);
                    firstLineOfPage = lastFittingLine;
                    currentPage++;
                }
            }
            
            // Save document to URI
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                pdfDocument.writeTo(fos);
                pdfDocument.close();
                fos.close();
                pfd.close();
                Toast.makeText(this, "PDF Exportado com Sucesso!", Toast.LENGTH_LONG).show();
                
                // Open PDF automatically
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Nenhum aplicativo para ler PDF foi encontrado.", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar o PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            try {
                pdfDocument.close();
            } catch (Exception ignored) {}
        }
    }
}
