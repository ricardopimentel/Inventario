package com.cyberrocket.inventario.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;
import com.cyberrocket.inventario.adapter.ListAdapterComputadores;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.Computador;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.ActivityNotFoundException;
import com.cyberrocket.inventario.lib.Crud;
import androidx.annotation.Nullable;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private SearchView searchView;
    private Spinner spinnerCategory; // Replaces spinnerMarca
    private Spinner spinnerTipo;
    private Spinner spinnerLocal;
    private TextView tvItemCount;
    private ImageView btnScanSearch;
    
    private View cardSelectionActionMode;
    private Button btnSelectAll;
    private Button btnExportPdf;
    private TextView tvSelectedCount;
    private ActivityResultLauncher<String> createPdfLauncher;

    private ArrayList<Computador> computadoresListOriginal;
    private ArrayList<Computador> computadoresList;
    private ListAdapterComputadores adapter;

    private String currentQuery = "";
    private String currentCategory = "Computadores"; // Default
    private String currentTipo = "Todos os Tipos";
    private String currentLocal = "Todos os Locais";

    private ArrayAdapter<String> adapterCategory;
    private ArrayAdapter<String> adapterTipo;
    private ArrayAdapter<String> adapterLocal;
    
    private List<String> categorias = new ArrayList<>();
    private List<String> tiposUnicos = new ArrayList<>();
    private List<String> locaisUnicos = new ArrayList<>();

    public HomeFragment() {
        createPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> {
                if (uri != null) {
                    generatePdfToUri(uri);
                } else {
                    Toast.makeText(getContext(), "Exportação cancelada", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewComputadores);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshHome);
        emptyTextView = root.findViewById(R.id.text_home_empty);
        searchView = root.findViewById(R.id.searchViewComputadores);
        spinnerCategory = root.findViewById(R.id.spinnerMarca);
        spinnerTipo = root.findViewById(R.id.spinnerTipo);
        spinnerLocal = root.findViewById(R.id.spinnerLocal);
        tvItemCount = root.findViewById(R.id.tvItemCount);
        btnScanSearch = root.findViewById(R.id.btnScanSearch);
        
        View cardCadComputador = root.findViewById(R.id.cardCadComputador);
        View cardCadMonitor = root.findViewById(R.id.cardCadMonitor);

        cardCadComputador.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.cyberrocket.inventario.CadEquipamentoActivity.class);
            startActivity(intent);
        });

        cardCadMonitor.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.cyberrocket.inventario.CadMonitorActivity.class);
            startActivity(intent);
        });

        btnScanSearch.setOnClickListener(v -> {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(HomeFragment.this);
            integrator.setPrompt("Ler Código de Barras / QR Code");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        computadoresListOriginal = new ArrayList<>();
        computadoresList = new ArrayList<>();
        adapter = new ListAdapterComputadores(computadoresList, getContext());
        
        cardSelectionActionMode = root.findViewById(R.id.cardSelectionActionMode);
        btnSelectAll = root.findViewById(R.id.btnSelectAll);
        btnExportPdf = root.findViewById(R.id.btnExportPdf);
        tvSelectedCount = root.findViewById(R.id.tvSelectedCount);
        
        adapter.setOnSelectionChangeListener(selectedCount -> {
            if (selectedCount > 0) {
                cardSelectionActionMode.setVisibility(View.VISIBLE);
                btnScanSearch.setVisibility(View.GONE);
                tvSelectedCount.setText(String.format("(%d)", selectedCount));
            } else {
                cardSelectionActionMode.setVisibility(View.GONE);
                btnScanSearch.setVisibility(View.VISIBLE);
            }
        });

        btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        btnExportPdf.setOnClickListener(v -> {
            List<Computador> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) return;

            swipeRefreshLayout.setRefreshing(true);
            Toast.makeText(getContext(), "Coletando dados de rede...", Toast.LENGTH_SHORT).show();

            int totalToFetch = selected.size();
            int[] fetched = {0}; // use array to allow modification inside lambda
            String fileName = "Inventario_Export_" + System.currentTimeMillis() + ".pdf";

            GLPIConnect glpi = new GLPIConnect(getContext());

            for (Computador c : selected) {
                if (c.getIpAddress() != null && !c.getIpAddress().isEmpty() && !c.getIpAddress().equals("Não Encontrado")) {
                    fetched[0]++;
                    if (fetched[0] == totalToFetch) {
                        swipeRefreshLayout.setRefreshing(false);
                        createPdfLauncher.launch(fileName);
                    }
                    continue;
                }

                String endpoint = currentCategory.equals("Monitores") ? "/apirest.php/Monitor/" : "/apirest.php/Computer/";
                glpi.GetItem(endpoint + c.getId() + "?with_networkports=true", new GLPIConnect.VolleyResponseListener() {
                    @Override
                    public void onVolleySuccess(String url, String response) {
                        StringBuilder foundIps = new StringBuilder();
                        java.util.Set<String> uniqueIps = new java.util.LinkedHashSet<>();
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONObject networkPortsObj = obj.optJSONObject("_networkports");
                            if (networkPortsObj != null) {
                                String[] portTypes = {"NetworkPortEthernet", "NetworkPortWifi", "NetworkPortWireless"};
                                for (String type : portTypes) {
                                    JSONArray ports = networkPortsObj.optJSONArray(type);
                                    if (ports != null) {
                                        for (int j = 0; j < ports.length(); j++) {
                                            JSONObject port = ports.optJSONObject(j);
                                            if (port == null) continue;
                                            JSONObject netName = port.optJSONObject("NetworkName");
                                            if (netName != null) {
                                                JSONArray ips = netName.optJSONArray("IPAddress");
                                                if (ips != null) {
                                                    for (int k = 0; k < ips.length(); k++) {
                                                        String ipCandidate = ips.optJSONObject(k).optString("name");
                                                        if (!ipCandidate.contains(":") && !ipCandidate.equals("127.0.0.1") && !ipCandidate.equals("0.0.0.0") && !ipCandidate.isEmpty()) {
                                                            uniqueIps.add(ipCandidate);
                                                        }
                                                    }
                                                }
                                            }
                                            JSONArray altIps = port.optJSONArray("_ipaddresses");
                                            if (altIps != null) {
                                                for (int k = 0; k < altIps.length(); k++) {
                                                    String ipCandidate = altIps.optJSONObject(k).optString("name");
                                                    if (!ipCandidate.contains(":") && !ipCandidate.equals("127.0.0.1") && !ipCandidate.equals("0.0.0.0") && !ipCandidate.isEmpty()) {
                                                        uniqueIps.add(ipCandidate);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {}
                        
                        if (!uniqueIps.isEmpty()) {
                            for (String ip : uniqueIps) {
                                if (foundIps.length() > 0) foundIps.append(" | ");
                                foundIps.append(ip);
                            }
                            c.setIpAddress(foundIps.toString());
                        } else {
                            c.setIpAddress("Não Encontrado");
                        }
                        
                        fetched[0]++;
                        if (fetched[0] == totalToFetch) {
                            swipeRefreshLayout.setRefreshing(false);
                            createPdfLauncher.launch(fileName);
                        }
                    }

                    @Override
                    public void onVolleyFailure(String error) {
                        c.setIpAddress("Falha na Rede");
                        fetched[0]++;
                        if (fetched[0] == totalToFetch) {
                            swipeRefreshLayout.setRefreshing(false);
                            createPdfLauncher.launch(fileName);
                        }
                    }
                });
            }
        });

        recyclerView.setAdapter(adapter);

        if (currentCategory.equals("Computadores")) {
            spinnerTipo.setVisibility(View.VISIBLE);
        } else {
            spinnerTipo.setVisibility(View.GONE);
        }

        // Inicializar Spinners
        initSpinners();


        // Listeners
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                applyFilters();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                applyFilters();
                return false;
            }
        });

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getId() == R.id.spinnerMarca) {
                    String selected = categorias.get(position);
                    if (!selected.equals(currentCategory)) {
                        currentCategory = selected;
                        if (currentCategory.equals("Computadores")) {
                            spinnerTipo.setVisibility(View.VISIBLE);
                        } else {
                            spinnerTipo.setVisibility(View.GONE);
                            currentTipo = "Todos os Tipos";
                        }
                        swipeRefreshLayout.setRefreshing(true);
                        loadEquipamentos();
                    }
                } else if (parent.getId() == R.id.spinnerTipo) {
                    currentTipo = tiposUnicos.get(position);
                    applyFilters();
                } else if (parent.getId() == R.id.spinnerLocal) {
                    currentLocal = locaisUnicos.get(position);
                    applyFilters();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerCategory.setOnItemSelectedListener(spinnerListener);
        spinnerTipo.setOnItemSelectedListener(spinnerListener);
        spinnerLocal.setOnItemSelectedListener(spinnerListener);

        swipeRefreshLayout.setOnRefreshListener(() -> loadEquipamentos());

        swipeRefreshLayout.setRefreshing(true);
        loadEquipamentos();

        return root;
    }

    private void initSpinners() {
        categorias.add("Computadores");
        categorias.add("Monitores");

        tiposUnicos.add("Todos os Tipos");
        locaisUnicos.add("Todos os Locais");

        adapterCategory = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categorias);
        spinnerCategory.setAdapter(adapterCategory);

        adapterTipo = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, tiposUnicos);
        spinnerTipo.setAdapter(adapterTipo);

        adapterLocal = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, locaisUnicos);
        spinnerLocal.setAdapter(adapterLocal);
    }

    private void updateSpinnersData() {
        HashSet<String> locaisSet = new HashSet<>();
        HashSet<String> tiposSet = new HashSet<>();

        for (Computador c : computadoresListOriginal) {
            String l = c.getLocalizacao();
            if (l != null && !l.isEmpty() && !l.equals("0")) locaisSet.add(l);
            
            String t = c.getTipo();
            if (t != null && !t.isEmpty() && !t.equals("0") && !t.equals("Monitor")) tiposSet.add(t);
        }

        String lSelected = currentLocal;
        String tSelected = currentTipo;

        locaisUnicos.clear();
        locaisUnicos.add("Todos os Locais");

        tiposUnicos.clear();
        tiposUnicos.add("Todos os Tipos");

        List<String> sortedLocais = new ArrayList<>(locaisSet);
        Collections.sort(sortedLocais);
        locaisUnicos.addAll(sortedLocais);

        List<String> sortedTipos = new ArrayList<>(tiposSet);
        Collections.sort(sortedTipos);
        tiposUnicos.addAll(sortedTipos);

        adapterLocal.notifyDataSetChanged();
        adapterTipo.notifyDataSetChanged();

        if (locaisUnicos.contains(lSelected)) {
            spinnerLocal.setSelection(locaisUnicos.indexOf(lSelected), false);
        } else {
            spinnerLocal.setSelection(0, false);
            currentLocal = "Todos os Locais";
        }

        if (tiposUnicos.contains(tSelected)) {
            spinnerTipo.setSelection(tiposUnicos.indexOf(tSelected), false);
        } else {
            spinnerTipo.setSelection(0, false);
            currentTipo = "Todos os Tipos";
        }
    }

    private void loadEquipamentos() {
        String endpoint = currentCategory.equals("Monitores") ? "/apirest.php/Monitor" : "/apirest.php/Computer";
        GLPIConnect glpi = new GLPIConnect(getContext());
        glpi.GetArray(endpoint + "?expand_dropdowns=true&sort=id&order=DESC&range=0-500", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (!isAdded() || getContext() == null) return;
                swipeRefreshLayout.setRefreshing(false);
                computadoresListOriginal.clear();

                try {
                    JSONArray jsonArray = new JSONArray(serverResponse);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        Computador comp = new Computador();

                        comp.setId(obj.optString("id"));
                        comp.setNome(obj.optString("name"));
                        comp.setSerial(obj.optString("serial"));
                        
                        comp.setFabricante(obj.optString("manufacturers_id"));
                        // Computer has computertypes_id/computermodels_id, Monitor has monitortypes_id/monitormodels_id
                        if (currentCategory.equals("Monitores")) {
                            comp.setTipo("Monitor");
                            comp.setModelo(obj.optString("monitormodels_id"));
                        } else {
                            comp.setTipo(obj.optString("computertypes_id"));
                            comp.setModelo(obj.optString("computermodels_id"));
                        }
                        comp.setLocalizacao(obj.optString("locations_id"));
                        comp.setStatusInfo(obj.optString("states_id"));

                        // Extract IP
                        StringBuilder foundIps = new StringBuilder();
                        java.util.Set<String> uniqueIps = new java.util.LinkedHashSet<>();
                        JSONObject networkPortsObj = obj.optJSONObject("_networkports");
                        if (networkPortsObj != null) {
                            String[] portTypes = {"NetworkPortEthernet", "NetworkPortWifi", "NetworkPortWireless"};
                            for (String type : portTypes) {
                                JSONArray ports = networkPortsObj.optJSONArray(type);
                                if (ports != null) {
                                    for (int j = 0; j < ports.length(); j++) {
                                        JSONObject port = ports.optJSONObject(j);
                                        if (port == null) continue;
                                        JSONObject netName = port.optJSONObject("NetworkName");
                                        if (netName != null) {
                                            JSONArray ips = netName.optJSONArray("IPAddress");
                                            if (ips != null) {
                                                for (int k = 0; k < ips.length(); k++) {
                                                    String ipCandidate = ips.optJSONObject(k).optString("name");
                                                    if (!ipCandidate.contains(":") && !ipCandidate.equals("127.0.0.1") && !ipCandidate.equals("0.0.0.0") && !ipCandidate.isEmpty()) {
                                                        uniqueIps.add(ipCandidate);
                                                    }
                                                }
                                            }
                                        }
                                        JSONArray altIps = port.optJSONArray("_ipaddresses");
                                        if (altIps != null) {
                                            for (int k = 0; k < altIps.length(); k++) {
                                                String ipCandidate = altIps.optJSONObject(k).optString("name");
                                                if (!ipCandidate.contains(":") && !ipCandidate.equals("127.0.0.1") && !ipCandidate.equals("0.0.0.0") && !ipCandidate.isEmpty()) {
                                                    uniqueIps.add(ipCandidate);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (!uniqueIps.isEmpty()) {
                            for (String ip : uniqueIps) {
                                if (foundIps.length() > 0) foundIps.append(" | ");
                                foundIps.append(ip);
                            }
                            comp.setIpAddress(foundIps.toString());
                        } else {
                            comp.setIpAddress("Não Encontrado");
                        }

                        // Set Status Image based on state
                        String st = comp.getStatusInfo().toLowerCase();
                        ImageView imgStatus = new ImageView(getContext());
                        if (st.contains("produção")) {
                            imgStatus.setImageResource(R.drawable.checkcircle24);
                        } else {
                            imgStatus.setImageResource(R.drawable.uncheckcircle24);
                        }
                        comp.setImagemStatus(imgStatus);

                        computadoresListOriginal.add(comp);
                    }

                    updateSpinnersData();
                    applyFilters();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Erro ao popular lista", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                if (!isAdded() || getContext() == null) return;
                swipeRefreshLayout.setRefreshing(false);
                emptyTextView.setVisibility(View.VISIBLE);
                emptyTextView.setText("Erro de conexão");
                recyclerView.setVisibility(View.GONE);
                tvItemCount.setText("(0)");
            }
        });
    }

    private void applyFilters() {
        computadoresList.clear();

        for (Computador comp : computadoresListOriginal) {
            boolean matchesQuery = true;
            boolean matchesLocal = true;

            // Search by Name, ID or Serial
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                String q = currentQuery.toLowerCase();
                String tName = comp.getNome() != null ? comp.getNome().toLowerCase() : "";
                String tId = comp.getId() != null ? comp.getId().toLowerCase() : "";
                String tSerial = comp.getSerial() != null ? comp.getSerial().toLowerCase() : "";
                matchesQuery = tName.contains(q) || tId.contains(q) || tSerial.contains(q);
            }

            // Local Filter
            if (!currentLocal.equals("Todos os Locais")) {
                matchesLocal = currentLocal.equals(comp.getLocalizacao());
            }
            
            boolean matchesTipo = true;
            if (!currentTipo.equals("Todos os Tipos") && currentCategory.equals("Computadores")) {
                matchesTipo = currentTipo.equals(comp.getTipo());
            }

            if (matchesQuery && matchesLocal && matchesTipo) {
                computadoresList.add(comp);
            }
        }

        if (computadoresList.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            String itemType = currentCategory.equals("Monitores") ? "monitor" : "computador";
            emptyTextView.setText("Nenhum " + itemType + " encontrado");
            recyclerView.setVisibility(View.GONE);
            tvItemCount.setText("(0)");
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            tvItemCount.setText("(" + computadoresList.size() + ")");
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void generatePdfToUri(Uri uri) {
        if (getContext() == null || adapter == null) return;
        List<Computador> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum item selecionado", Toast.LENGTH_SHORT).show();
            return;
        }

        Crud crud = new Crud();
        String prefixo = crud.SelectItem(getContext(), "CONFIG", 1, 5);
        if (prefixo == null) prefixo = "";

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        int pageHeight = 1120;
        int pageWidth = 792;
        int margins = 40;
        int cols = 5;
        float cellWidth = (pageWidth - 2 * margins) / (float) cols;
        float cellHeight = 150f;
        
        int rowsPerPage = (int) ((pageHeight - margins - 80) / cellHeight);
        int itemsPerPage = rowsPerPage * cols;
        int totalItems = selected.size();
        int pages = (int) Math.ceil((double) totalItems / itemsPerPage);

        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

        try {
            int itemIndex = 0;
            for (int p = 0; p < pages; p++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, p + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                // Header
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(24f);
                canvas.drawText("Painel de Etiquetas", margins, margins + 30, paint);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(14f);
                canvas.drawText("Página " + (p+1) + " de " + pages + " - Corte nas linhas pretas", margins, margins + 50, paint);

                paint.setStrokeWidth(2f);
                canvas.drawLine(margins, margins + 60, pageWidth - margins, margins + 60, paint);

                float startY = margins + 80;
                int col = 0;
                float currentY = startY;

                for (int i = 0; i < itemsPerPage && itemIndex < totalItems; i++, itemIndex++) {
                    Computador comp = selected.get(itemIndex);
                    
                    String rawName = comp.getNome() != null ? comp.getNome() : comp.getId();
                    String cleanName = rawName;
                    if (!prefixo.isEmpty() && rawName.startsWith(prefixo)) {
                        cleanName = rawName.substring(prefixo.length());
                    }

                    float cellX = margins + col * cellWidth;
                    
                    // Draw Border for cutting
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1f);
                    paint.setColor(Color.BLACK);
                    canvas.drawRect(cellX, currentY, cellX + cellWidth, currentY + cellHeight, paint);
                    
                    paint.setStyle(Paint.Style.FILL);

                    String serial = comp.getSerial() != null && !comp.getSerial().isEmpty() ? comp.getSerial() : "";
                    boolean isMonitor = currentCategory.equals("Monitores");

                    String qrData;
                    if (isMonitor && !serial.isEmpty()) {
                        qrData = serial;
                    } else {
                        qrData = cleanName;
                    }

                    // Generate and draw QR Code
                    try {
                        Bitmap qrBitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 100, 100);
                        float qrX = cellX + (cellWidth - 100) / 2;
                        canvas.drawBitmap(qrBitmap, qrX, currentY + 10, null);
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }

                    // Texts alignment
                    paint.setTextAlign(Paint.Align.CENTER);
                    
                    // Box Text: Name
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    paint.setTextSize(10f);
                    String drawName = cleanName;
                    if (drawName.length() > 22) drawName = drawName.substring(0, 20) + "...";
                    canvas.drawText(drawName, cellX + cellWidth / 2, currentY + 112, paint);

                    // Box Text: Brand | Model
                    String brand = comp.getFabricante() != null ? comp.getFabricante() : "";
                    String model = comp.getModelo() != null ? comp.getModelo() : "";
                    String brandModel = brand;
                    if (!model.isEmpty()) {
                        if (!brandModel.isEmpty()) brandModel += " ";
                        brandModel += model;
                    }
                    if (!brandModel.isEmpty()) {
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        paint.setTextSize(8f);
                        if (brandModel.length() > 25) brandModel = brandModel.substring(0, 23) + "...";
                        canvas.drawText(brandModel, cellX + cellWidth / 2, currentY + 120, paint);
                    }

                    // Box Text: Serial
                    if (!serial.isEmpty()) {
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        paint.setTextSize(8f);
                        canvas.drawText("SN: " + serial, cellX + cellWidth / 2, currentY + 128, paint);
                    }

                    // Box Text: IP
                    String ip = comp.getIpAddress();
                    if (ip != null && !ip.equals("Não Encontrado") && !ip.equals("Falha na Rede") && !ip.isEmpty()) {
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        paint.setTextSize(7f); // Slightly smaller font for IPs
                        String[] ipsArray = ip.split(" \\| ");
                        float ipY = currentY + 136;
                        for (int k = 0; k < Math.min(ipsArray.length, 2); k++) { // Show up to 2 IPs
                            canvas.drawText(ipsArray[k], cellX + cellWidth / 2, ipY, paint);
                            ipY += 8;
                        }
                    }
                    
                    // Resets
                    paint.setTextAlign(Paint.Align.LEFT);

                    col++;
                    if (col == cols) {
                        col = 0;
                        currentY += cellHeight;
                    }
                }

                pdfDocument.finishPage(page);
            }

            ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                pdfDocument.writeTo(fos);
                pdfDocument.close();
                fos.close();
                pfd.close();
                Toast.makeText(getContext(), "PDF Exportado com Sucesso!", Toast.LENGTH_LONG).show();
                adapter.setSelectionMode(false); // Reset UI after export
                
                // Open PDF automatically
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    getContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), "Nenhum aplicativo para ler PDF foi encontrado.", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erro ao salvar o PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            pdfDocument.close();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scanned = result.getContents().trim();
                handleScannedInput(scanned);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleScannedInput(String scanned) {
        if (getContext() == null) return;
        boolean isSerial = false;
        for (Computador c : computadoresListOriginal) {
            if (c.getSerial() != null && c.getSerial().equalsIgnoreCase(scanned)) {
                isSerial = true;
                break;
            }
        }
        
        Crud crud = new Crud();
        String prefixo = crud.SelectItem(getContext(), "CONFIG", 1, 5);
        if (prefixo == null) prefixo = "";
        
        String query;
        if (isSerial) {
            query = scanned;
        } else {
            if (!prefixo.isEmpty() && scanned.startsWith(prefixo)) {
                query = scanned;
            } else {
                query = prefixo + scanned;
            }
        }
        
        if (searchView != null) {
            searchView.setQuery(query, true);
        }
    }
}
