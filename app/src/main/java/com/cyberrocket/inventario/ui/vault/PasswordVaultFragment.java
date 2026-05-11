package com.cyberrocket.inventario.ui.vault;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.VaultSettingsActivity;
import com.cyberrocket.inventario.adapter.ListAdapterVaultGroups;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.lib.PasswordVaultHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PasswordVaultFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private SearchView searchView;
    private MaterialButtonToggleGroup toggleGroup;
    private Spinner spinnerComputerType;
    private FloatingActionButton fabAdd;

    private PasswordVaultHelper vaultHelper;
    private ArrayList<ListAdapterVaultGroups.VaultItem> vaultItemsOriginal = new ArrayList<>();
    private ArrayList<ListAdapterVaultGroups.VaultItem> vaultItemsFiltered = new ArrayList<>();
    private ListAdapterVaultGroups adapter;

    private String currentTab = "Computer";
    private String currentQuery = "";
    private String selectedComputerTypeId = "0"; // 0 means all

    private ArrayList<String> computerTypeNames = new ArrayList<>();
    private ArrayList<String> computerTypeIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_password_vault, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewVault);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshVault);
        progressBar = root.findViewById(R.id.pbVault);
        emptyTextView = root.findViewById(R.id.tvVaultEmpty);
        searchView = root.findViewById(R.id.searchViewVault);
        toggleGroup = root.findViewById(R.id.toggleGroupVault);
        spinnerComputerType = root.findViewById(R.id.spinnerComputerTypeVault);
        fabAdd = root.findViewById(R.id.fabAddVaultLink);

        vaultHelper = new PasswordVaultHelper(getContext());
        
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ListAdapterVaultGroups(vaultItemsFiltered, getContext(), vaultHelper, new PasswordVaultHelper.VaultOperationListener() {
            @Override
            public void onLoading(boolean loading) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onVaultConfigError() {
                Toast.makeText(getContext(), "Configure o Cofre Infisical primeiro.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(getContext(), VaultSettingsActivity.class));
            }
        });
        recyclerView.setAdapter(adapter);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                currentTab = checkedId == R.id.btnTabComputadores ? "Computer" : "Location";
                spinnerComputerType.setVisibility(currentTab.equals("Computer") ? View.VISIBLE : View.GONE);
                loadVaultItems();
            }
        });

        loadComputerTypes();

        spinnerComputerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedComputerTypeId = computerTypeIds.get(position);
                applyFilters();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

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

        swipeRefreshLayout.setOnRefreshListener(this::loadVaultItems);

        fabAdd.setOnClickListener(v -> showAddItemDialog());

        loadVaultItems();

        return root;
    }

    private void loadVaultItems() {
        if (getContext() == null) return;
        swipeRefreshLayout.setRefreshing(true);
        GLPIConnect con = new GLPIConnect(getContext());
        
        String endpoint = "/apirest.php/PluginFields" + currentTab + "cofredesenha/?range=0-500";
        con.GetArray(endpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                if (!isAdded()) return;
                try {
                    JSONArray array = new JSONArray(response);
                    ArrayList<String> idsWithSecrets = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String vaultId = obj.optString("vaultsecretidfield", "");
                        if (!vaultId.isEmpty() && !vaultId.equals("null")) {
                            idsWithSecrets.add(obj.optString("items_id"));
                        }
                    }
                    if (idsWithSecrets.isEmpty()) {
                        vaultItemsOriginal.clear();
                        applyFilters();
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    String itemsEndpoint = "/apirest.php/" + currentTab + "/?expand_dropdowns=true&range=0-1000";
                    con.GetArray(itemsEndpoint, new GLPIConnect.VolleyResponseListener() {
                        @Override
                        public void onVolleySuccess(String url2, String response2) {
                            if (!isAdded()) return;
                            try {
                                JSONArray itemsArray = new JSONArray(response2);
                                vaultItemsOriginal.clear();
                                for (int i = 0; i < itemsArray.length(); i++) {
                                    JSONObject itemObj = itemsArray.getJSONObject(i);
                                    String id = itemObj.optString("id");
                                    if (idsWithSecrets.contains(id)) {
                                        String fullName = itemObj.optString(currentTab.equals("Location") ? "completename" : "name");
                                        String name = fullName;
                                        if (currentTab.equals("Location") && name.contains(">")) {
                                            String[] parts = name.split(">");
                                            name = parts[parts.length - 1].trim();
                                        }
                                        String typeId = "0";
                                        if (currentTab.equals("Computer")) {
                                            JSONArray links = itemObj.optJSONArray("links");
                                            if (links != null) {
                                                for (int j = 0; j < links.length(); j++) {
                                                    JSONObject l = links.getJSONObject(j);
                                                    if (l.optString("rel").equals("ComputerType")) {
                                                        String h = l.optString("href");
                                                        typeId = h.substring(h.lastIndexOf("/") + 1);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        vaultItemsOriginal.add(new ListAdapterVaultGroups.VaultItem(id, name, fullName, currentTab, typeId));
                                    }
                                }
                                applyFilters();
                            } catch (JSONException e) { e.printStackTrace(); }
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        @Override public void onVolleyFailure(String error) { if (!isAdded()) return; swipeRefreshLayout.setRefreshing(false); }
                    });
                } catch (JSONException e) { e.printStackTrace(); swipeRefreshLayout.setRefreshing(false); }
            }
            @Override public void onVolleyFailure(String errorMessage) { if (!isAdded()) return; swipeRefreshLayout.setRefreshing(false); }
        });
    }



    private void showAddItemDialog() {
        String[] types = {"Computador", "Localidade"};
        new MaterialAlertDialogBuilder(getContext())
            .setTitle("Vincular Senhas a...")
            .setItems(types, (dialog, which) -> {
                String selectedType = which == 0 ? "Computer" : "Location";
                showSearchItemDialog(selectedType);
            })
            .show();
    }

    private void showSearchItemDialog(String type) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.activity_vincular_monitor, null);
        AutoCompleteTextView actv = view.findViewById(R.id.TvNomeMonitorVincularMonitor);
        
        actv.setHint("Digite o nome...");
        
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, names);
        actv.setAdapter(searchAdapter);

        GLPIConnect con = new GLPIConnect(getContext());
        String endpoint = type.equals("Computer") ? "/apirest.php/Computer" : "/apirest.php/Location";
        
        con.GetArray(endpoint + "?range=0-1000", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String name = obj.getString("name");
                        
                        // Simplify location names in search results
                        if (type.equals("Location") && name.contains(">")) {
                            String[] parts = name.split(">");
                            name = parts[parts.length - 1].trim();
                        }
                        
                        names.add(name);
                        ids.add(obj.getString("id"));
                    }
                    searchAdapter.notifyDataSetChanged();
                } catch (JSONException e) { e.printStackTrace(); }
            }
            @Override public void onVolleyFailure(String error) {}
        });

        new MaterialAlertDialogBuilder(getContext())
            .setTitle(type.equals("Computer") ? "Selecionar Computador" : "Selecionar Localidade")
            .setView(view)
            .setPositiveButton("Selecionar", (dialog, which) -> {
                String selectedName = actv.getText().toString();
                int index = names.indexOf(selectedName);
                if (index != -1) {
                    String selectedId = ids.get(index);
                    vaultHelper.showPasswordDialog(type, selectedId, new PasswordVaultHelper.VaultOperationListener() {
                        @Override public void onLoading(boolean loading) { progressBar.setVisibility(loading ? View.VISIBLE : View.GONE); }
                        @Override public void onVaultConfigError() { /* Already handled */ }
                    });
                } else {
                    Toast.makeText(getContext(), "Item não selecionado.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
            
        // Hide "Novo Monitor" button from activity_vincular_monitor if present
        view.findViewById(R.id.BtNovoMonitorVincular).setVisibility(View.GONE);
    }
    private void applyFilters() {
        vaultItemsFiltered.clear();
        for (ListAdapterVaultGroups.VaultItem item : vaultItemsOriginal) {
            boolean matchesSearch = currentQuery.isEmpty() || 
                                   item.name.toLowerCase().contains(currentQuery.toLowerCase()) || 
                                   item.fullName.toLowerCase().contains(currentQuery.toLowerCase()) ||
                                   item.id.contains(currentQuery);
            
            boolean matchesType = true;
            if (currentTab.equals("Computer") && !selectedComputerTypeId.equals("0")) {
                matchesType = item.computerTypeId != null && item.computerTypeId.equals(selectedComputerTypeId);
            }
            
            if (matchesSearch && matchesType) {
                vaultItemsFiltered.add(item);
            }
        }
        
        if (adapter != null) adapter.notifyDataSetChanged();
        
        if (vaultItemsFiltered.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadComputerTypes() {
        if (getContext() == null) return;
        GLPIConnect con = new GLPIConnect(getContext());
        con.GetArray("/apirest.php/ComputerType/", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    computerTypeNames.clear();
                    computerTypeIds.clear();
                    
                    computerTypeNames.add("Todos os Tipos");
                    computerTypeIds.add("0");
                    
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        computerTypeNames.add(obj.optString("name"));
                        computerTypeIds.add(obj.optString("id"));
                    }
                    
                    if (isAdded() && getContext() != null) {
                        ArrayAdapter<String> adapterType = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, computerTypeNames);
                        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerComputerType.setAdapter(adapterType);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override public void onVolleyFailure(String error) {}
        });
    }
}
