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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
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
import com.google.android.material.tabs.TabLayout;

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
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;

    private PasswordVaultHelper vaultHelper;
    private ArrayList<ListAdapterVaultGroups.VaultItem> vaultItemsOriginal = new ArrayList<>();
    private ArrayList<ListAdapterVaultGroups.VaultItem> vaultItemsFiltered = new ArrayList<>();
    private ListAdapterVaultGroups adapter;

    private String currentTab = "Computer";
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_password_vault, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewVault);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshVault);
        progressBar = root.findViewById(R.id.pbVault);
        emptyTextView = root.findViewById(R.id.tvVaultEmpty);
        searchView = root.findViewById(R.id.searchViewVault);
        tabLayout = root.findViewById(R.id.tabLayoutVault);
        fabAdd = root.findViewById(R.id.fabAddVaultLink);

        vaultHelper = new PasswordVaultHelper(getContext());
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition() == 0 ? "Computer" : "Location";
                loadVaultItems();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
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
        
        // Endpoint to get all items that HAVE a vault secret link
        // Note the trailing slash before the ? - GLPI is very picky about this
        String endpoint = "/apirest.php/PluginFields" + currentTab + "cofredesenha/?expand_dropdowns=true&range=0-500";
        
        con.GetArray(endpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                if (!isAdded()) return;
                swipeRefreshLayout.setRefreshing(false);
                vaultItemsOriginal.clear();
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String vaultId = obj.optString("vaultsecretidfield", "");
                        if (!vaultId.isEmpty() && !vaultId.equals("null")) {
                            String itemId = obj.optString("items_id");
                            // The 'items_id' in PluginFields often comes as a name if expand_dropdowns is true
                            // but we need the ID too. GLPI API is tricky here.
                            // Usually, there's a field like 'Computer' or 'Location' with the name.
                            String name = obj.optString(currentTab, "Item #" + itemId);
                            vaultItemsOriginal.add(new ListAdapterVaultGroups.VaultItem(itemId, name, currentTab));
                        }
                    }
                    applyFilters();
                } catch (JSONException e) {
                    e.printStackTrace();
                    applyFilters();
                }
            }

            @Override
            public void onVolleyFailure(String errorMessage) {
                if (!isAdded()) return;
                swipeRefreshLayout.setRefreshing(false);
                Log.e("VaultFragment", "Erro GLPI: " + errorMessage);
                Toast.makeText(getContext(), "Erro GLPI: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilters() {
        vaultItemsFiltered.clear();
        for (ListAdapterVaultGroups.VaultItem item : vaultItemsOriginal) {
            if (currentQuery.isEmpty() || item.name.toLowerCase().contains(currentQuery.toLowerCase()) || item.id.contains(currentQuery)) {
                vaultItemsFiltered.add(item);
            }
        }
        
        if (vaultItemsFiltered.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
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
                        names.add(obj.getString("name"));
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
}
