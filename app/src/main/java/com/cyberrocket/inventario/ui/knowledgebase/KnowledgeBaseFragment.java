package com.cyberrocket.inventario.ui.knowledgebase;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.cyberrocket.inventario.adapter.ListAdapterKnowledgeBase;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.KnowbaseItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KnowledgeBaseFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private SearchView searchView;
    private Spinner spinnerCategoria;

    private ArrayList<KnowbaseItem> kbListOriginal;
    private ArrayList<KnowbaseItem> kbListFiltered;
    private ListAdapterKnowledgeBase adapter;

    private String currentQuery = "";
    private String currentCategory = "Todas as Categorias";
    private ArrayList<String> categories;
    private Map<String, String> categoriesFromApi = new HashMap<>();
    private Map<String, String> itemToCategoryMap = new HashMap<>();
    private ArrayAdapter<String> categoryAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_knowledge_base, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewKnowledgeBase);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayoutKB);
        emptyTextView = root.findViewById(R.id.text_no_kb);
        searchView = root.findViewById(R.id.searchViewKnowledgeBase);
        spinnerCategoria = root.findViewById(R.id.spinnerCategoria);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        kbListOriginal = new ArrayList<>();
        kbListFiltered = new ArrayList<>();
        adapter = new ListAdapterKnowledgeBase(kbListFiltered, getContext());
        recyclerView.setAdapter(adapter);

        categories = new ArrayList<>();
        categories.add("Todas as Categorias");
        categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategoria.setAdapter(categoryAdapter);

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

        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadKnowledgeBase();
            }
        });

        // First Load
        swipeRefreshLayout.setRefreshing(true);
        loadCategories();
        // loadKnowledgeBase will be called inside loadCategories to ensure mapping is ready

        return root;
    }

    private synchronized void updateCategorySpinner(Set<String> extraCategories) {
        if (!isAdded() || getContext() == null) return;
        
        Log.d("KB_DEBUG", "updateCategorySpinner iniciado");
        
        Set<String> allCats = new HashSet<>();
        synchronized (categoriesFromApi) {
            allCats.addAll(categoriesFromApi.values());
        }
        
        if (extraCategories != null) {
            allCats.addAll(extraCategories);
        }
        
        // Extrai categorias dos itens que já foram carregados
        for (KnowbaseItem item : kbListOriginal) {
            String c = item.getCategoryName();
            if (c != null && !c.isEmpty() && !c.equals("Sem Categoria") && !c.matches("\\d+")) {
                allCats.add(c);
            }
        }

        final ArrayList<String> newList = new ArrayList<>();
        newList.add("Todas as Categorias");
        ArrayList<String> sorted = new ArrayList<>(allCats);
        Collections.sort(sorted);
        newList.addAll(sorted);

        Log.d("KB_DEBUG", "Novas categorias para o Spinner: " + newList.toString());

        if (getView() != null) {
            getView().post(() -> {
                if (!isAdded()) return;
                
                String previous = currentCategory;
                categories.clear();
                categories.addAll(newList);
                categoryAdapter.notifyDataSetChanged();
                
                int pos = categories.indexOf(previous);
                if (pos >= 0) {
                    spinnerCategoria.setSelection(pos);
                } else {
                    spinnerCategoria.setSelection(0);
                }
                Log.d("KB_DEBUG", "Spinner UI atualizada com " + categories.size() + " itens");
            });
        }
    }

    private void loadCategories() {
        Log.d("KB_DEBUG", "Chamando loadCategories...");
        GLPIConnect glpi = new GLPIConnect(getContext());
        
        // 1. Fetch categories names
        glpi.GetArray("/apirest.php/KnowbaseItemCategory", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (!isAdded() || getContext() == null) return;
                Log.d("KB_DEBUG", "Resposta loadCategories: " + serverResponse);
                try {
                    JSONArray jsonArray = new JSONArray(serverResponse);
                    synchronized (categoriesFromApi) {
                        categoriesFromApi.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String id = obj.optString("id", "");
                            String name = obj.optString("name", "");
                            if (name.isEmpty()) name = obj.optString("completename", "");
                            if (!id.isEmpty() && !name.isEmpty()) {
                                categoriesFromApi.put(id, name);
                            }
                        }
                    }
                    updateCategorySpinner(null);
                    
                    // 2. Fetch the relationship table to map Items to Categories
                    fetchRelationshipsAndLoadItems(glpi);
                    
                } catch (JSONException e) {
                    Log.e("KB_DEBUG", "Erro parse categorias", e);
                }
            }

            @Override
            public void onVolleyFailure(String errorMsg) {
                Log.e("KB_DEBUG", "Falha loadCategories: " + errorMsg);
                updateCategorySpinner(null);
                loadKnowledgeBase();
            }
        });
    }

    private void fetchRelationshipsAndLoadItems(GLPIConnect glpi) {
        // This table stores which item belongs to which category in GLPI
        glpi.GetArray("/apirest.php/KnowbaseItem_KnowbaseItemCategory", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (!isAdded()) return;
                Log.d("KB_DEBUG", "Relacionamentos encontrados: " + serverResponse.length());
                try {
                    JSONArray rels = new JSONArray(serverResponse);
                    synchronized (itemToCategoryMap) {
                        itemToCategoryMap.clear();
                        for (int i = 0; i < rels.length(); i++) {
                            JSONObject rel = rels.getJSONObject(i);
                            String itemId = rel.optString("knowbaseitems_id");
                            String catId = rel.optString("knowbaseitemcategories_id");
                            if (!itemId.isEmpty() && !catId.isEmpty()) {
                                itemToCategoryMap.put(itemId, catId);
                            }
                        }
                    }
                    Log.d("KB_DEBUG", "Mapeamento Item->Categoria carregado: " + itemToCategoryMap.size() + " itens");
                    loadKnowledgeBase();
                } catch (JSONException e) {
                    Log.e("KB_DEBUG", "Erro parse relacionamentos", e);
                    loadKnowledgeBase();
                }
            }

            @Override
            public void onVolleyFailure(String errorMsg) {
                Log.e("KB_DEBUG", "Falha ao buscar relacionamentos: " + errorMsg);
                loadKnowledgeBase();
            }
        });
    }

    private void loadKnowledgeBase() {
        Log.d("KB_DEBUG", "Iniciando loadKnowledgeBase...");
        GLPIConnect glpi = new GLPIConnect(getContext());
        glpi.GetArray("/apirest.php/KnowbaseItem?expand_dropdowns=true", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (!isAdded() || getContext() == null) return;
                Log.d("KB_DEBUG", "KB Items API Sucesso. Tamanho: " + serverResponse.length());

                swipeRefreshLayout.setRefreshing(false);
                kbListOriginal.clear();
                Set<String> foundCatsInItems = new HashSet<>();

                try {
                    JSONArray jsonArray = new JSONArray(serverResponse);
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        KnowbaseItem item = new KnowbaseItem();

                        item.setId(obj.optString("id"));
                        item.setName(obj.optString("name"));
                        item.setContent(obj.optString("answer")); 
                        item.setDateMod(obj.optString("date_mod"));
                        
                        String catName = "Sem Categoria";
                        String itemId = item.getId();

                        // Use the manual mapping from the relationship table
                        synchronized (itemToCategoryMap) {
                            if (itemToCategoryMap.containsKey(itemId)) {
                                String catId = itemToCategoryMap.get(itemId);
                                synchronized (categoriesFromApi) {
                                    if (categoriesFromApi.containsKey(catId)) {
                                        catName = categoriesFromApi.get(catId);
                                        Log.d("KB_DEBUG", "Item #" + itemId + " mapeado via tabela de rel para: " + catName);
                                    }
                                }
                            }
                        }

                        // Last resort fallback: Check in 'links' array
                        if (catName.equals("Sem Categoria") && obj.has("links")) {
                            JSONArray links = obj.getJSONArray("links");
                            for (int j = 0; j < links.length(); j++) {
                                JSONObject link = links.getJSONObject(j);
                                if ("KnowbaseItemCategory".equals(link.optString("rel"))) {
                                    String href = link.optString("href", "");
                                    if (!href.isEmpty()) {
                                        String id = href.substring(href.lastIndexOf("/") + 1);
                                        synchronized (categoriesFromApi) {
                                            if (categoriesFromApi.containsKey(id)) {
                                                catName = categoriesFromApi.get(id);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (catName == null || catName.isEmpty() || catName.equals("0") || catName.equals("null") || catName.matches("\\d+")) {
                            catName = "Sem Categoria";
                        }

                        item.setCategoryName(catName);
                        if (!catName.equals("Sem Categoria")) {
                            foundCatsInItems.add(catName);
                        }

                        kbListOriginal.add(item);
                    }

                    Log.d("KB_DEBUG", "Itens carregados: " + kbListOriginal.size() + ". Categorias identificadas nos itens: " + foundCatsInItems.size());
                    updateCategorySpinner(foundCatsInItems);

                    Collections.sort(kbListOriginal, new Comparator<KnowbaseItem>() {
                        @Override
                        public int compare(KnowbaseItem i1, KnowbaseItem i2) {
                            if (i1.getDateMod() != null && i2.getDateMod() != null) {
                                return i2.getDateMod().compareTo(i1.getDateMod());
                            }
                            return 0;
                        }
                    });

                    applyFilters();

                } catch (JSONException e) {
                    Log.e("KB_DEBUG", "Erro ao processar JSON de items KB", e);
                }
            }

            @Override
            public void onVolleyFailure(String errorMsg) {
                Log.e("KB_DEBUG", "Falha ao carregar itens KB via API: " + errorMsg);
                if (!isAdded() || getContext() == null) return;
                swipeRefreshLayout.setRefreshing(false);
                emptyTextView.setVisibility(View.VISIBLE);
                emptyTextView.setText("Erro ao carregar base de conhecimento");
                recyclerView.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erro de conexão", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        kbListFiltered.clear();

        for (KnowbaseItem item : kbListOriginal) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;

            // Search Filter (Title or Content)
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                String query = currentQuery.toLowerCase();
                boolean titleMatch = item.getName() != null && item.getName().toLowerCase().contains(query);
                boolean contentMatch = item.getContent() != null && item.getContent().toLowerCase().contains(query);
                matchesSearch = titleMatch || contentMatch;
            }

            // Category Filter
            if (!currentCategory.equals("Todas as Categorias")) {
                matchesCategory = currentCategory.equals(item.getCategoryName());
            }

            if (matchesSearch && matchesCategory) {
                kbListFiltered.add(item);
            }
        }

        if (kbListFiltered.isEmpty()) {
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
