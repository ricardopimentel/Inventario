package com.cyberrocket.inventario.widget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.models.Chamado;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TicketRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<Chamado> ticketList;

    public TicketRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.ticketList = new ArrayList<>();
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        ticketList.clear();
        Crud crud = new Crud();
        String baseUrl = crud.SelectItem(context, "CONFIG", 1, 1);
        String sessionToken = crud.SelectItem(context, "CONFIG", 1, 2);

        if (baseUrl == null || baseUrl.isEmpty() || sessionToken == null || sessionToken.isEmpty()) {
            return;
        }

        try {
            URL url = new URL(baseUrl + "/apirest.php/Ticket?expand_dropdowns=true&sort=id&order=DESC&range=0-50");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");
            conn.setRequestProperty("Session-Token", sessionToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 206) {
                InputStream inputStream = conn.getInputStream();
                Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
                String responseString = scanner.hasNext() ? scanner.next() : "";

                JSONArray jsonArray = new JSONArray(responseString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    int status = obj.optInt("status", 1);
                    if (status == 1 || status == 2 || status == 3 || status == 4) { // Including pendentes to be safe, but mostly new/ongoing
                        if (status == 4) continue; // Exclude pending as requested
                        Chamado chamado = new Chamado();
                        chamado.setId(obj.optString("id"));
                        chamado.setTitulo(obj.optString("name"));
                        chamado.setDescricao(obj.optString("content"));
                        chamado.setDataCriacao(obj.optString("date"));
                        chamado.setDataFechamento(obj.optString("closedate"));
                        chamado.setTipo(String.valueOf(obj.optInt("type", 1)));
                        chamado.setStatusInfo(String.valueOf(status));
                        ticketList.add(chamado);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TicketWidget", "Error fetching tickets", e);
        }
    }

    @Override
    public void onDestroy() {
        ticketList.clear();
    }

    @Override
    public int getCount() {
        return ticketList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION || ticketList == null || position >= ticketList.size()) {
            return null;
        }

        Chamado chamado = ticketList.get(position);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_ticket_item);
        
        rv.setTextViewText(R.id.widget_item_title, chamado.getTitulo() != null && !chamado.getTitulo().isEmpty() ? chamado.getTitulo() : "Sem Título");

        String desc = chamado.getDescricao() != null ? chamado.getDescricao() : "";
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                desc = android.text.Html.fromHtml(desc, android.text.Html.FROM_HTML_MODE_COMPACT).toString();
            } else {
                desc = android.text.Html.fromHtml(desc).toString();
            }
        } catch (Exception e) {
            // ignore
        }
        desc = desc.trim().replaceAll("\\s+", " ");
        rv.setTextViewText(R.id.widget_item_desc, desc);

        String dateStr = chamado.getDataCriacao();
        String formattedDate = "";
        if (dateStr != null && !dateStr.isEmpty()) {
            String datePart = dateStr.contains(" ") ? dateStr.split(" ")[0] : dateStr;
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                java.util.Locale currentLocale = context.getResources().getConfiguration().locale;
                if (currentLocale.getLanguage().equals("pt")) {
                    formattedDate = parts[2] + "/" + parts[1] + "/" + parts[0];
                } else {
                    formattedDate = parts[1] + "/" + parts[2] + "/" + parts[0];
                }
            } else {
                formattedDate = dateStr;
            }
        }
        rv.setTextViewText(R.id.widget_item_date, formattedDate);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("id", chamado.getId());
        fillInIntent.putExtra("title", chamado.getTitulo());
        fillInIntent.putExtra("description", chamado.getDescricao());
        fillInIntent.putExtra("creationDate", chamado.getDataCriacao());
        fillInIntent.putExtra("closingDate", chamado.getDataFechamento());
        fillInIntent.putExtra("requester", chamado.getUsuarioRequerente());
        fillInIntent.putExtra("assigned", chamado.getUsuarioAtribuido());
        fillInIntent.putExtra("type", chamado.getTipo());
        fillInIntent.putExtra("status", chamado.getStatusInfo());

        rv.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
