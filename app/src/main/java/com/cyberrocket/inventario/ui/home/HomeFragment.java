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
    private Spinner spinnerCategory; // Replaces spinnerMarca
    private Spinner spinnerLocal;
    private TextView tvItemCount;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabScanQr;

    private ArrayList<Computador> computadoresListOriginal;
    private ArrayList<Computador> computadoresList;
    private ListAdapterComputadores adapter;

    private String currentQuery = "";
    private String currentCategory = "Computadores"; // Default
    private String currentLocal = "Todos os Locais";

    private ArrayAdapter<String> adapterCategory;
    private ArrayAdapter<String> adapterLocal;
    
    private List<String> categorias = new ArrayList<>();
    private List<String> locaisUnicos = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewComputadores);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshHome);
        emptyTextView = root.findViewById(R.id.text_home_empty);
        searchView = root.findViewById(R.id.searchViewComputadores);
        spinnerCategory = root.findViewById(R.id.spinnerMarca);
        spinnerLocal = root.findViewById(R.id.spinnerLocal);
        tvItemCount = root.findViewById(R.id.tvItemCount);
        fabScanQr = root.findViewById(R.id.fabScanQr);

        fabScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScannerActivity.class);
            intent.putExtra("auto_scan", true);
            String itemType = currentCategory.equals("Monitores") ? "Monitor" : "Computer";
            intent.putExtra("item_type", itemType);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
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
                    String selected = categorias.get(position);
                    if (!selected.equals(currentCategory)) {
                        currentCategory = selected;
                        swipeRefreshLayout.setRefreshing(true);
                        loadEquipamentos();
                    }
                } else if (parent.getId() == R.id.spinnerLocal) {
                    currentLocal = locaisUnicos.get(position);
                    applyFilters();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerCategory.setOnItemSelectedListener(spinnerListener);
        spinnerLocal.setOnItemSelectedListener(spinnerListener);

        swipeRefreshLayout.setOnRefreshListener(() -> loadEquipamentos());

        swipeRefreshLayout.setRefreshing(true);
        loadEquipamentos();

        return root;
    }

    private void initSpinners() {
        categorias.add("Computadores");
        categorias.add("Monitores");
        locaisUnicos.add("Todos os Locais");

        adapterCategory = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categorias);
        spinnerCategory.setAdapter(adapterCategory);

        adapterLocal = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, locaisUnicos);
        spinnerLocal.setAdapter(adapterLocal);
    }

    private void updateSpinnersData() {
        HashSet<String> locaisSet = new HashSet<>();

        for (Computador c : computadoresListOriginal) {
            String l = c.getLocalizacao();
            if (l != null && !l.isEmpty() && !l.equals("0")) locaisSet.add(l);
        }

        String lSelected = currentLocal;

        locaisUnicos.clear();
        locaisUnicos.add("Todos os Locais");

        List<String> sortedLocais = new ArrayList<>(locaisSet);
        Collections.sort(sortedLocais);
        locaisUnicos.addAll(sortedLocais);

        adapterLocal.notifyDataSetChanged();

        if (locaisUnicos.contains(lSelected)) {
            spinnerLocal.setSelection(locaisUnicos.indexOf(lSelected), false);
        } else {
            spinnerLocal.setSelection(0, false);
            currentLocal = "Todos os Locais";
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
                        
                        comp.setFabricante(obj.optString("manufacturers_id"));
                        // Computer has computertypes_id, Monitor has monitortypes_id
                        if (currentCategory.equals("Monitores")) {
                            comp.setTipo("Monitor");
                        } else {
                            comp.setTipo(obj.optString("computertypes_id"));
                        }
                        comp.setLocalizacao(obj.optString("locations_id"));
                        comp.setStatusInfo(obj.optString("states_id"));

                        // Set Status Image based on state
                        String st = comp.getStatusInfo().toLowerCase();
                        ImageView imgStatus = new ImageView(getContext());
                        if (st.contains("em uso") || st.contains("produção") || st.isEmpty() || st.equals("0")) {
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

            // Search by Name or ID
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                String q = currentQuery.toLowerCase();
                String tName = comp.getNome() != null ? comp.getNome().toLowerCase() : "";
                String tId = comp.getId() != null ? comp.getId().toLowerCase() : "";
                matchesQuery = tName.contains(q) || tId.contains(q);
            }

            // Local Filter
            if (!currentLocal.equals("Todos os Locais")) {
                matchesLocal = currentLocal.equals(comp.getLocalizacao());
            }

            if (matchesQuery && matchesLocal) {
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
}
