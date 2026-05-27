package com.cyberrocket.inventario.ui.tickets;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.adapter.ListAdapterChamados;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.Chamado;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TicketsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyTextView;
    private SearchView searchView;
    private Spinner spinnerTipo;
    private Spinner spinnerStatus;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTicket;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabDeleteSelected;
    private LinearLayout linearLayoutForms;
    private HorizontalScrollView hsvForms;
    
    private ArrayList<Chamado> chamadosListOriginal;
    private ArrayList<Chamado> chamadosList;
    private ListAdapterChamados adapter;

    private String currentQuery = "";
    private int currentTipoPos = 0;
    private int currentStatusPos = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tickets, container, false);
        setHasOptionsMenu(true);

        recyclerView = root.findViewById(R.id.recyclerViewChamados);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        emptyTextView = root.findViewById(R.id.text_tickets);
        searchView = root.findViewById(R.id.searchViewChamados);
        spinnerTipo = root.findViewById(R.id.spinnerTipo);
        spinnerStatus = root.findViewById(R.id.spinnerStatus);
        fabAddTicket = root.findViewById(R.id.fabAddTicket);
        fabDeleteSelected = root.findViewById(R.id.fabDeleteSelected);
        linearLayoutForms = root.findViewById(R.id.linearLayoutForms);
        hsvForms = root.findViewById(R.id.hsvForms);

        fabAddTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CreateTicketActivity.class);
                startActivity(intent);
            }
        });

        fabDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteSelected();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chamadosListOriginal = new ArrayList<>();
        chamadosList = new ArrayList<>();
        adapter = new ListAdapterChamados(chamadosList, getContext(), new ListAdapterChamados.OnChamadoInteractionListener() {
            @Override
            public void onDeleteClick(Chamado chamado) {
                confirmDeleteSingle(chamado);
            }

            @Override
            public void onSelectionChanged() {
                updateDeleteFabVisibility();
            }
        });
        recyclerView.setAdapter(adapter);

        // Configuração dos Spinners
        String[] tipos = {"Todos os Tipos", "Incidente", "Requisição"};
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapterTipo);

        String[] status = {"Todos os Status", "Novo / Em Andamento", "Pendente", "Resolvido / Fechado"};
        ArrayAdapter<String> adapterStatus = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, status);
        spinnerStatus.setAdapter(adapterStatus);
        spinnerStatus.setSelection(1, false);

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
                if (parent.getId() == R.id.spinnerTipo) currentTipoPos = position;
                if (parent.getId() == R.id.spinnerStatus) currentStatusPos = position;
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerTipo.setOnItemSelectedListener(spinnerListener);
        spinnerStatus.setOnItemSelectedListener(spinnerListener);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadChamados();
            }
        });


        // Carregar a primeira vez
        swipeRefreshLayout.setRefreshing(true);
        loadChamados();
        loadForms();

        return root;
    }

    private void loadForms() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("GLPI_PREFS", android.content.Context.MODE_PRIVATE);
        String host = prefs.getString("glpi_host", "");
        String token = prefs.getString("session_token", "");
        
        // Algumas versoes do BD interno salvam os dados em "CONFIG". Usaremos o GLPIConnect pra pegar o host e token
        com.cyberrocket.inventario.lib.Crud crud = new com.cyberrocket.inventario.lib.Crud();
        String dbHost = crud.SelectItem(getContext(), "CONFIG", 1, 1);
        String dbToken = crud.SelectItem(getContext(), "CONFIG", 1, 2);
        
        final String baseUrl = (dbHost != null && !dbHost.isEmpty()) ? dbHost : host;
        final String sessionToken = (dbToken != null && !dbToken.isEmpty()) ? dbToken : token;
        final String usuarioFinal = crud.SelectItem(getContext(), "CONFIG", 1, 3);
        final String senhaFinal = crud.SelectItem(getContext(), "CONFIG", 1, 4);

        if (baseUrl.isEmpty() || sessionToken.isEmpty()) {
            hsvForms.setVisibility(View.GONE);
            return;
        }

        // GLPI 11 usa ItemType com namespace: Glpi\Form\Form (encoded as %5C)
        String url = baseUrl + "/apirest.php/Glpi%5CForm%5CForm/";
        Log.d("TicketsFragment", "Carregando formulários GLPI 11: " + url);
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(getContext());
        com.android.volley.toolbox.StringRequest stringRequest = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (!isAdded() || getContext() == null) return;
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            linearLayoutForms.removeAllViews();
                            
                            if(jsonArray.length() == 0) {
                                tryFallbackForms(baseUrl, sessionToken, 1); // Tenta PluginFormcreatorForm
                                return;
                            } else {
                                hsvForms.setVisibility(View.VISIBLE);
                            }

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                String formId = obj.optString("id"); 
                                String formName = obj.optString("name");
                                
                                if (formId.isEmpty() || formName.isEmpty()) continue;
                                
                                androidx.appcompat.widget.AppCompatButton btn = new androidx.appcompat.widget.AppCompatButton(getContext());
                                btn.setText(formName);
                                btn.setAllCaps(false);
                                btn.setBackgroundResource(R.drawable.form_btn_bg);
                                btn.setMinimumHeight(0);
                                btn.setMinHeight(0);
                                btn.setMinimumWidth(0);
                                btn.setMinWidth(0);
                                
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 0, 16, 0);
                                btn.setLayoutParams(params);
                                
                                android.util.TypedValue typedValue = new android.util.TypedValue();
                                getContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
                                int colorPrimary = typedValue.data;
                                getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                                int colorBackground = typedValue.data;
                                
                                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimary));
                                btn.setTextColor(colorBackground);

                                btn.setOnClickListener(v -> {
                                    Intent intent = new Intent(getContext(), FormActivity.class);
                                    intent.putExtra("url", baseUrl + "/Form/Render/" + formId);
                                    intent.putExtra("session_token", sessionToken);
                                    intent.putExtra("usuario", usuarioFinal);
                                    intent.putExtra("senha", senhaFinal);
                                    startActivity(intent);
                                });
                                
                                linearLayoutForms.addView(btn);
                            }
                            if(linearLayoutForms.getChildCount() == 0) {
                                hsvForms.setVisibility(View.GONE);
                            }
                        } catch (JSONException e) {
                            tryFallbackForms(baseUrl, sessionToken, 1);
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(com.android.volley.VolleyError error) {
                        tryFallbackForms(baseUrl, sessionToken, 1);
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", sessionToken);
                return params;
            }
        };

        queue.add(stringRequest);
    }
    
    /**
     * Tenta carregar formulários de ItemTypes alternativos caso o padrão do GLPI 11 falhe.
     * step 1: PluginFormcreatorForm
     * step 2: Form
     */
    private void tryFallbackForms(final String baseUrl, final String sessionToken, final int step) {
        final com.cyberrocket.inventario.lib.Crud crud = new com.cyberrocket.inventario.lib.Crud();
        String itemType = (step == 1) ? "PluginFormcreatorForm" : "Form";
        String url = baseUrl + "/apirest.php/" + itemType + "/";
        
        Log.d("TicketsFragment", "Tentando fallback (step " + step + "): " + url);
        
        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(getContext());
        com.android.volley.toolbox.StringRequest stringRequest = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (!isAdded() || getContext() == null) return;
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if(jsonArray.length() == 0) {
                                if(step == 1) tryFallbackForms(baseUrl, sessionToken, 2);
                                else hsvForms.setVisibility(View.GONE);
                                return;
                            }
                            
                            hsvForms.setVisibility(View.VISIBLE);
                            linearLayoutForms.removeAllViews();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                String formId = obj.optString("id"); 
                                String formName = obj.optString("name");
                                if (formId.isEmpty() || formName.isEmpty()) continue;
                                
                                androidx.appcompat.widget.AppCompatButton btn = new androidx.appcompat.widget.AppCompatButton(getContext());
                                btn.setText(formName);
                                btn.setAllCaps(false);
                                btn.setBackgroundResource(R.drawable.form_btn_bg);
                                btn.setMinimumHeight(0);
                                btn.setMinHeight(0);
                                btn.setMinimumWidth(0);
                                btn.setMinWidth(0);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 0, 16, 0);
                                btn.setLayoutParams(params);
                                
                                android.util.TypedValue typedValue = new android.util.TypedValue();
                                getContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
                                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(typedValue.data));
                                getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
                                btn.setTextColor(typedValue.data);

                                btn.setOnClickListener(v -> {
                                    Intent intent = new Intent(getContext(), FormActivity.class);
                                    intent.putExtra("url", baseUrl + "/Form/Render/" + formId);
                                    intent.putExtra("session_token", sessionToken);
                                    intent.putExtra("usuario", crud.SelectItem(getContext(), "CONFIG", 1, 3));
                                    intent.putExtra("senha", crud.SelectItem(getContext(), "CONFIG", 1, 4));
                                    startActivity(intent);
                                });
                                linearLayoutForms.addView(btn);
                            }
                        } catch (Exception e) {
                            if(step == 1) tryFallbackForms(baseUrl, sessionToken, 2);
                            else hsvForms.setVisibility(View.GONE);
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(com.android.volley.VolleyError error) {
                        if(step == 1) tryFallbackForms(baseUrl, sessionToken, 2);
                        else hsvForms.setVisibility(View.GONE);
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("Content-type", "application/json");
                params.put("Session-Token", sessionToken);
                return params;
            }
        };
        queue.add(stringRequest);
    }


    private void loadChamados() {
        GLPIConnect glpi = new GLPIConnect(getContext());
        // Endpoint do GLPI para tickets com expand_dropdowns para obter nomes reais, e order=DESC&sort=id para buscar os mais recentes da API
        glpi.GetArray("/apirest.php/Ticket?expand_dropdowns=true&sort=id&order=DESC", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                swipeRefreshLayout.setRefreshing(false);
                chamadosListOriginal.clear();
                updateDeleteFabVisibility();

                try {
                    JSONArray jsonArray = new JSONArray(serverResponse);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        Chamado chamado = new Chamado();

                        chamado.setId(obj.optString("id"));
                        chamado.setTitulo(obj.optString("name"));
                        chamado.setDescricao(obj.optString("content"));
                        chamado.setDataCriacao(obj.optString("date"));
                        chamado.setDataFechamento(obj.optString("closedate"));
                        
                        String req = obj.optString("users_id_recipient");
                        if (req == null || req.isEmpty() || req.equals("0")) {
                            req = obj.optString("users_id_lastupdater");
                        }
                        chamado.setUsuarioRequerente(req);
                        
                        // Map type (1 = Incident, 2 = Request)
                        int tipo = obj.optInt("type", 1);
                        chamado.setTipo(String.valueOf(tipo));

                        // Status Info
                        int status = obj.optInt("status", 1);
                        chamado.setStatusInfo(String.valueOf(status));

                        // Tenta definir o ícone baseando-se no status
                        ImageView imgStatus = new ImageView(getContext());
                        if (status == 1 || status == 2 || status == 3) { // Novo ou em atendimento
                            imgStatus.setImageResource(R.drawable.circle24);
                        } else if (status == 5 || status == 6) { // Resolvido ou Fechado
                            imgStatus.setImageResource(R.drawable.checkcircle24);
                        } else {
                            imgStatus.setImageResource(R.drawable.checkcircle24);
                        }
                        chamado.setImagemStatus(imgStatus);

                        chamadosListOriginal.add(chamado);
                    }

                    // Ordenar a lista do mais recente para o mais antigo com base na data de criação
                    Collections.sort(chamadosListOriginal, new Comparator<Chamado>() {
                        @Override
                        public int compare(Chamado c1, Chamado c2) {
                            if (c1.getDataCriacao() != null && c2.getDataCriacao() != null) {
                                // Ordem decrescente
                                return c2.getDataCriacao().compareTo(c1.getDataCriacao());
                            }
                            return 0;
                        }
                    });

                    applyFilters();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onVolleyFailure(String errorMsg) {
                swipeRefreshLayout.setRefreshing(false);
                emptyTextView.setVisibility(View.VISIBLE);
                emptyTextView.setText("Erro ao carregar chamados");
                recyclerView.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erro de conexão", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        chamadosList.clear();

        for (Chamado chamado : chamadosListOriginal) {
            boolean matchesTitle = true;
            boolean matchesTipo = true;
            boolean matchesStatus = true;

            // Filtro de Título
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                matchesTitle = chamado.getTitulo() != null && chamado.getTitulo().toLowerCase().contains(currentQuery.toLowerCase());
            }

            // Filtro de Tipo ("Todos os Tipos", "Incidente", "Requisição") -> GLPI Type 1/2
            if (currentTipoPos == 1) { // Incidente
                matchesTipo = "1".equals(chamado.getTipo());
            } else if (currentTipoPos == 2) { // Requisição
                matchesTipo = "2".equals(chamado.getTipo());
            }

            // Filtro de Status ("Todos os Status", "Novo / Em Andamento", "Pendente", "Resolvido / Fechado")
            if (currentStatusPos == 1) { // Novo ou andamento: GLPI 1, 2, 3
                matchesStatus = "1".equals(chamado.getStatusInfo()) || "2".equals(chamado.getStatusInfo()) || "3".equals(chamado.getStatusInfo());
            } else if (currentStatusPos == 2) { // Pendente: GLPI 4
                matchesStatus = "4".equals(chamado.getStatusInfo());
            } else if (currentStatusPos == 3) { // Resolvido/Fechado: GLPI 5, 6
                matchesStatus = "5".equals(chamado.getStatusInfo()) || "6".equals(chamado.getStatusInfo());
            }

            if (matchesTitle && matchesTipo && matchesStatus) {
                chamadosList.add(chamado);
            }
        }

        if (chamadosList.isEmpty()) {
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

    private void updateDeleteFabVisibility() {
        boolean hasSelection = false;
        for (Chamado c : chamadosList) {
            if (c.isSelected()) {
                hasSelection = true;
                break;
            }
        }
        if (hasSelection) {
            fabDeleteSelected.setVisibility(View.VISIBLE);
        } else {
            fabDeleteSelected.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteSingle(Chamado chamado) {
        new AlertDialog.Builder(getContext())
                .setTitle("Excluir Chamado")
                .setMessage("Tem certeza que deseja apagar o chamado #" + chamado.getId() + " - " + chamado.getTitulo() + "?")
                .setPositiveButton("Sim, Apagar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTickets(new ArrayList<>(Collections.singletonList(chamado)));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeleteSelected() {
        ArrayList<Chamado> toDelete = new ArrayList<>();
        for (Chamado c : chamadosList) {
            if (c.isSelected()) {
                toDelete.add(c);
            }
        }

        if (toDelete.isEmpty()) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Excluir Múltiplos Chamados")
                .setMessage("Tem certeza que deseja apagar " + toDelete.size() + " chamados selecionados?")
                .setPositiveButton("Sim, Apagar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTickets(toDelete);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteTickets(ArrayList<Chamado> ticketsToDelete) {
        fabDeleteSelected.setEnabled(false);
        swipeRefreshLayout.setRefreshing(true);
        GLPIConnect glpi = new GLPIConnect(getContext());
        
        final int[] remainingToDelete = {ticketsToDelete.size()};
        final boolean[] successFlag = {true};

        for (Chamado c : ticketsToDelete) {
            glpi.DeleteItem("/apirest.php/Ticket/" + c.getId() + "?force_purge=true", new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String serverResponse) {
                    remainingToDelete[0]--;
                    checkDeleteCompletion(remainingToDelete[0], successFlag[0]);
                }

                @Override
                public void onVolleyFailure(String error) {
                    successFlag[0] = false;
                    remainingToDelete[0]--;
                    checkDeleteCompletion(remainingToDelete[0], successFlag[0]);
                }
            });
        }
    }

    private void checkDeleteCompletion(int remaining, boolean success) {
        if (remaining <= 0) {
            if (!isAdded() || getContext() == null) return;
            fabDeleteSelected.setEnabled(true);
            if (success) {
                Toast.makeText(getContext(), "Chamado(s) apagado(s) com sucesso!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Alguns chamados não puderam ser apagados.", Toast.LENGTH_SHORT).show();
            }
            loadChamados();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater inflater) {
        inflater.inflate(R.menu.tickets_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_reports) {
            Intent intent = new Intent(getContext(), TicketReportsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
