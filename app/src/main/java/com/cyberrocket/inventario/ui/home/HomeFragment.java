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

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private SearchView searchView;
    private Spinner spinnerMarca;
    private Spinner spinnerTipo;
    private Spinner spinnerLocal;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabScanQr;

    private ArrayList<Computador> computadoresListOriginal;
    private ArrayList<Computador> computadoresList;
    private ListAdapterComputadores adapter;

    private String currentQuery = "";
    private String currentMarca = "Todas as Marcas";
    private String currentTipo = "Todos os Tipos";
    private String currentLocal = "Todos os Locais";

    private ArrayAdapter<String> adapterMarca;
    private ArrayAdapter<String> adapterTipo;
    private ArrayAdapter<String> adapterLocal;
    
    private List<String> marcasUnicas = new ArrayList<>();
    private List<String> tiposUnicos = new ArrayList<>();
    private List<String> locaisUnicos = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewComputadores);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshHome);
        emptyTextView = root.findViewById(R.id.text_home_empty);
        searchView = root.findViewById(R.id.searchViewComputadores);
        spinnerMarca = root.findViewById(R.id.spinnerMarca);
        spinnerTipo = root.findViewById(R.id.spinnerTipo);
        spinnerLocal = root.findViewById(R.id.spinnerLocal);
        fabScanQr = root.findViewById(R.id.fabScanQr);

        fabScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScannerActivity.class);
            intent.putExtra("auto_scan", true);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        computadoresListOriginal = new ArrayList<>();
        computadoresList = new ArrayList<>();
        adapter = new ListAdapterComputadores(computadoresList, getContext());
        recyclerView.setAdapter(adapter);

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
                    currentMarca = marcasUnicas.get(position);
                } else if (parent.getId() == R.id.spinnerTipo) {
                    currentTipo = tiposUnicos.get(position);
                } else if (parent.getId() == R.id.spinnerLocal) {
                    currentLocal = locaisUnicos.get(position);
                }
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerMarca.setOnItemSelectedListener(spinnerListener);
        spinnerTipo.setOnItemSelectedListener(spinnerListener);
        spinnerLocal.setOnItemSelectedListener(spinnerListener);

        swipeRefreshLayout.setOnRefreshListener(() -> loadComputadores());

        swipeRefreshLayout.setRefreshing(true);
        loadComputadores();

        return root;
    }

    private void initSpinners() {
        marcasUnicas.add("Todas as Marcas");
        tiposUnicos.add("Todos os Tipos");
        locaisUnicos.add("Todos os Locais");

        adapterMarca = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, marcasUnicas);
        spinnerMarca.setAdapter(adapterMarca);

        adapterTipo = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, tiposUnicos);
        spinnerTipo.setAdapter(adapterTipo);

        adapterLocal = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, locaisUnicos);
        spinnerLocal.setAdapter(adapterLocal);
    }

    private void updateSpinnersData() {
        HashSet<String> marcasSet = new HashSet<>();
        HashSet<String> tiposSet = new HashSet<>();
        HashSet<String> locaisSet = new HashSet<>();

        for (Computador c : computadoresListOriginal) {
            String m = c.getFabricante();
            String t = c.getTipo();
            String l = c.getLocalizacao();
            
            if (m != null && !m.isEmpty()) marcasSet.add(m);
            if (t != null && !t.isEmpty()) tiposSet.add(t);
            if (l != null && !l.isEmpty() && !l.equals("0")) locaisSet.add(l);
        }

        // Preserve selected item if possible
        String mSelected = currentMarca;
        String tSelected = currentTipo;
        String lSelected = currentLocal;

        marcasUnicas.clear();
        tiposUnicos.clear();
        locaisUnicos.clear();

        marcasUnicas.add("Todas as Marcas");
        tiposUnicos.add("Todos os Tipos");
        locaisUnicos.add("Todos os Locais");

        List<String> sortedMarcas = new ArrayList<>(marcasSet);
        Collections.sort(sortedMarcas);
        marcasUnicas.addAll(sortedMarcas);

        List<String> sortedTipos = new ArrayList<>(tiposSet);
        Collections.sort(sortedTipos);
        tiposUnicos.addAll(sortedTipos);

        List<String> sortedLocais = new ArrayList<>(locaisSet);
        Collections.sort(sortedLocais);
        locaisUnicos.addAll(sortedLocais);

        adapterMarca.notifyDataSetChanged();
        adapterTipo.notifyDataSetChanged();
        adapterLocal.notifyDataSetChanged();

        if (marcasUnicas.contains(mSelected)) {
            spinnerMarca.setSelection(marcasUnicas.indexOf(mSelected), false);
        } else {
            spinnerMarca.setSelection(0, false);
            currentMarca = "Todas as Marcas";
        }

        if (tiposUnicos.contains(tSelected)) {
            spinnerTipo.setSelection(tiposUnicos.indexOf(tSelected), false);
        } else {
            spinnerTipo.setSelection(0, false);
            currentTipo = "Todos os Tipos";
        }

        if (locaisUnicos.contains(lSelected)) {
            spinnerLocal.setSelection(locaisUnicos.indexOf(lSelected), false);
        } else {
            spinnerLocal.setSelection(0, false);
            currentLocal = "Todos os Locais";
        }
    }

    private void loadComputadores() {
        GLPIConnect glpi = new GLPIConnect(getContext());
        glpi.GetArray("/apirest.php/Computer?expand_dropdowns=true&sort=id&order=DESC&range=0-500", new GLPIConnect.VolleyResponseListener() {
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
                        
                        // Extract dropdown names instead of IDs because of expand_dropdowns=true
                        comp.setFabricante(obj.optString("manufacturers_id"));
                        comp.setTipo(obj.optString("computertypes_id"));
                        comp.setLocalizacao(obj.optString("locations_id"));
                        comp.setStatusInfo(obj.optString("states_id"));

                        // Set Status Image based on state
                        ImageView imgStatus = new ImageView(getContext());
                        // Simple placeholder status mapping based on assumed names
                        String st = comp.getStatusInfo().toLowerCase();
                        if (st.contains("em uso") || st.contains("produção") || st.isEmpty() || st.equals("0")) {
                            imgStatus.setImageResource(android.R.drawable.presence_online);
                        } else if (st.contains("estoque") || st.contains("reserva")) {
                            imgStatus.setImageResource(android.R.drawable.presence_away);
                        } else if (st.contains("descarte") || st.contains("defeito")) {
                            imgStatus.setImageResource(android.R.drawable.presence_busy);
                        } else {
                            imgStatus.setImageResource(android.R.drawable.presence_offline);
                        }
                        comp.setImagemStatus(imgStatus);

                        computadoresListOriginal.add(comp);
                    }

                    updateSpinnersData();
                    applyFilters();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Erro ao popular lista de compuatadores", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                if (!isAdded() || getContext() == null) return;
                swipeRefreshLayout.setRefreshing(false);
                emptyTextView.setVisibility(View.VISIBLE);
                emptyTextView.setText("Erro de conexão");
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void applyFilters() {
        computadoresList.clear();

        for (Computador comp : computadoresListOriginal) {
            boolean matchesQuery = true;
            boolean matchesMarca = true;
            boolean matchesTipo = true;
            boolean matchesLocal = true;

            // Search by Name or ID
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                String q = currentQuery.toLowerCase();
                String tName = comp.getNome() != null ? comp.getNome().toLowerCase() : "";
                String tId = comp.getId() != null ? comp.getId().toLowerCase() : "";
                matchesQuery = tName.contains(q) || tId.contains(q);
            }

            // Marca Filter
            if (!currentMarca.equals("Todas as Marcas")) {
                matchesMarca = currentMarca.equals(comp.getFabricante());
            }

            // Tipo Filter
            if (!currentTipo.equals("Todos os Tipos")) {
                matchesTipo = currentTipo.equals(comp.getTipo());
            }

            // Local Filter
            if (!currentLocal.equals("Todos os Locais")) {
                matchesLocal = currentLocal.equals(comp.getLocalizacao());
            }

            if (matchesQuery && matchesMarca && matchesTipo && matchesLocal) {
                computadoresList.add(comp);
            }
        }

        if (computadoresList.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
