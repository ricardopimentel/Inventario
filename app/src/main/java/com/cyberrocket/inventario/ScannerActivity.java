package com.cyberrocket.inventario;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.volley.Request;
import com.cyberrocket.inventario.adapter.ListAdapterEquipamentos;
import com.cyberrocket.inventario.adapter.ListAdapterMonitores;
import com.cyberrocket.inventario.adapter.ListAdapterMudancas;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.lib.InfisicalConnect;
import com.cyberrocket.inventario.models.EquipamentoLine;
import com.cyberrocket.inventario.models.MonitorLine;
import com.cyberrocket.inventario.models.MudancasLine;
import com.cyberrocket.inventario.models.PlacasRedeLine;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.ArrayList;

import android.widget.ArrayAdapter;
import com.cyberrocket.inventario.adapter.ListAdapterSenhas;
import com.cyberrocket.inventario.lib.PasswordManager;
import com.cyberrocket.inventario.models.SenhaItem;
import android.widget.LinearLayout;
import android.app.AlertDialog;

public class ScannerActivity extends AppCompatActivity {
    TextView mTvIdEquipamento;
    CoordinatorLayout mCLayout;
    ImageView mImvImgEquipamento;
    ProgressBar mPgbProgresso;
    RecyclerView mListaEquipamentos;
    RecyclerView mListaMudancas;
    RecyclerView mListaMonitores;
    Button mBtNovaManutencao;
    ImageButton mBtAddMonitorScanner;
    ImageView mImvSyncDevice;

    Button mBtSenhasComputador;
    Button mBtSenhasLocal;
    LinearLayout mLayoutCofreBotoes;

    FloatingActionButton mBtLerEquipamento;
    LinearLayoutManager linearLayoutManagerEquipamento;
    StaggeredGridLayoutManager gridLayoutManagerEquipamento;
    LinearLayoutManager linearLayoutManagerMudancas;
    StaggeredGridLayoutManager gridLayoutManagerMudancas;
    LinearLayoutManager linearLayoutManagerMonitores;
    StaggeredGridLayoutManager gridLayoutManagerMonitores;
    Crud mCrud;
    ArrayList<EquipamentoLine> listaequipamentos;
    ArrayList<MudancasLine> listamudancas;
    ArrayList<MonitorLine> listamonitores;
    Boolean existemanutencaoaberta = false;
    ConstraintLayout mLayoutEquipamentos;
    ConstraintLayout mLayoutMonitores;
    ConstraintLayout mLayoutManutencoes;
    String mIdMonitor;
    SwipeRefreshLayout mSwipeRefreshListEquipamento;

    // Autocomplete for Monitor
    ArrayList<String> mMonitorNamesList;
    ArrayList<String> mMonitorIdsList;
    ArrayAdapter<String> mMonitorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Metodos automáticos
        inicializarViews();
        inicializarListas();
        getParametros();

        //Listeners
        mBtLerEquipamento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inicializarListas();
                IntentIntegrator integrator = new IntentIntegrator(ScannerActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Aponte para o Código");
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(true);
                integrator.setOrientationLocked(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });

        mBtNovaManutencao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrPara(CadMudancaActivity.class, false);
            }
        });

        mBtLerEquipamento.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DigitarManualmente();
                return true;
            }
        });

        mBtAddMonitorScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreencherListaMonitores();
            }
        });

        mSwipeRefreshListEquipamento.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                IrPara(ScannerActivity.class, true);
            }
        });

        // Check for direct actions from Home
        boolean autoScan = getIntent().getBooleanExtra("auto_scan", false);
        boolean autoType = getIntent().getBooleanExtra("auto_type", false);

        if (autoScan) {
            mBtLerEquipamento.performClick();
        } else if (autoType) {
            DigitarManualmente();
        }
    }

    //Sobrescreve a ação de voltar, redirecionando direto para a activity home
    public void onBackPressed() { //Botão BACK padrão do android
        startActivity(new Intent(this, HomeActivity.class)); //O efeito ao ser pressionado do botão (no caso abre a activity)
        finishAffinity(); //Método para matar a activity e não deixa-lá indexada na pilhagem
        return;
    }

    private void DigitarManualmente() {
        View view = LayoutInflater.from(ScannerActivity.this).inflate(R.layout.dialog_input, null);
        TextInputEditText edittext = view.findViewById(R.id.TvNome);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ScannerActivity.this)
                .setTitle("Digite o patrimônio")
                .setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mTvIdEquipamento.setText(edittext.getText().toString());
                        dialogInterface.dismiss();
                        GetIdEquipamento();
                    }
                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        dialog.create();
        dialog.show();
    }

    //Métodos

    private void IrPara(Class para, boolean finalizar) {
        Intent intent = new Intent(ScannerActivity.this, para);
        intent.putExtra("id", mTvIdEquipamento.getText().toString());
        startActivity(intent);
        if(finalizar){
            finish();
        }
    }

    private void getParametros() { //Se a activity for chamada atribuindo como parametro um id de equipamento, realiza a busca pelo equipamento, sem a necessidade de fazer a leitura do qr code
        Intent it = getIntent();
        String id = it.getStringExtra("id");
        if (id != null) {
            mTvIdEquipamento.setText(id);
            inicializarListas();
            BuscarListaEquipamentos(id);
        }
    }

    private void inicializarViews() {
        mCLayout = findViewById(R.id.CntScanner);
        mTvIdEquipamento = findViewById(R.id.TvIdEquipamento);
        mImvImgEquipamento = findViewById(R.id.ImvImgDevice);
        mPgbProgresso = findViewById(R.id.PgbScanner);
        mBtNovaManutencao = findViewById(R.id.BtNovaManutencaoScanner);
        mBtAddMonitorScanner = findViewById(R.id.BtAddMonitorScanner);

        mListaEquipamentos = findViewById(R.id.RvDetalhesEquipamentoScanner);
        mListaMudancas = findViewById(R.id.RvMudancasScanner);
        mListaMonitores = findViewById(R.id.RvMonitoresScanner);

        linearLayoutManagerEquipamento = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        gridLayoutManagerEquipamento = new StaggeredGridLayoutManager(3, LinearLayoutManager.VERTICAL);

        linearLayoutManagerMonitores = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        linearLayoutManagerMudancas = new LinearLayoutManager(this);
        gridLayoutManagerMudancas = new StaggeredGridLayoutManager(1, LinearLayoutManager.HORIZONTAL);


        mListaEquipamentos.setLayoutManager(gridLayoutManagerEquipamento);
        mListaMonitores.setLayoutManager(linearLayoutManagerMonitores);
        mListaMudancas.setLayoutManager(gridLayoutManagerMudancas);

        mLayoutEquipamentos = findViewById(R.id.LayoutEquipamentosScanner);
        mLayoutMonitores = findViewById(R.id.LayoutMonitoresScanner);
        mLayoutManutencoes = findViewById(R.id.LayoutManutencoesScanner);

        mImvSyncDevice = findViewById(R.id.ImvSyncDevice);

        mBtLerEquipamento = findViewById(R.id.BtLerEquipamentoScanner);
        mSwipeRefreshListEquipamento = findViewById(R.id.RefreshEquipamentosScanner);
        
        mLayoutCofreBotoes = findViewById(R.id.LayoutCofreBotoes);
        mBtSenhasComputador = findViewById(R.id.BtSenhasComputador);
        mBtSenhasLocal = findViewById(R.id.BtSenhasLocal);
        
        mBtSenhasComputador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbrirCofreSenhas("Computer", mTvIdEquipamento.getText().toString());
            }
        });

        mBtSenhasLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fetch Computer without expand_dropdown to get raw Location ID
                mPgbProgresso.setIndeterminate(true);
                GLPIConnect tempCon = new GLPIConnect(ScannerActivity.this);
                tempCon.GetItem("/apirest.php/Computer/" + mTvIdEquipamento.getText().toString(), new GLPIConnect.VolleyResponseListener() {
                    @Override
                    public void onVolleySuccess(String url, String response) {
                        mPgbProgresso.setIndeterminate(false);
                        try {
                            JSONObject rawComp = new JSONObject(response);
                            String rawLocId = rawComp.optString("locations_id", "0");
                            if (rawLocId.equals("0") || rawLocId.isEmpty()) {
                                Toast.makeText(ScannerActivity.this, "Este computador não possui um Local definido.", Toast.LENGTH_SHORT).show();
                            } else {
                                AbrirCofreSenhas("Location", rawLocId);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onVolleyFailure(String error) {
                        mPgbProgresso.setIndeterminate(false);
                        Toast.makeText(ScannerActivity.this, "Erro ao consultar a localização", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        mCrud = new Crud();
    }

    public class CustomGridLayoutManager extends LinearLayoutManager {
        private boolean isScrollEnabled = false;

        public CustomGridLayoutManager(Context context) {
            super(context);
        }

        public void setScrollEnabled(boolean flag) {
            this.isScrollEnabled = flag;
        }

        @Override
        public boolean canScrollVertically() {
            //Similarly you can customize "canScrollHorizontally()" for managing horizontal scroll
            return isScrollEnabled && super.canScrollHorizontally();
        }
    }

    private void inicializarListas() {
        listamudancas = new ArrayList<MudancasLine>();
        listamonitores = new ArrayList<MonitorLine>();
        listaequipamentos = new ArrayList<EquipamentoLine>();
    }

    private void BuscarListaEquipamentos(String idequipamento) {
        mTvIdEquipamento.setText(idequipamento);
        if(!idequipamento.equals("erro")) {
            GLPIConnect con = new GLPIConnect(this);
            con.GetItem("/apirest.php/Computer/" + idequipamento + "?expand_dropdowns=true&with_connections=true&with_problems=true&with_softwares=true", new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = new JSONObject(response);
                    } catch (JSONException err) {
                        Log.d("ParseError", err.toString());
                    }
                    Log.e("jason", jsonObject.toString());

                    //Pegar Versão do Agente do GLPI
                    String versaoagente = "";
                    try {
                        JSONArray softwaresarray = jsonObject.getJSONArray("_softwares");
                        for (int i = 0; i < softwaresarray.length(); i++) {
                            JSONObject software = softwaresarray.getJSONObject(i);
                            if(software.getString("softwares_id").contains("GLPI Agen")){
                                versaoagente = software.getString("softwareversions_id");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        //Pega dados do equipamento
                        CriarListaEquipamentos("Nome:", jsonObject.getString("name"), View.GONE);
                        CriarListaEquipamentos("Localização:", jsonObject.getString("locations_id"), View.VISIBLE);
                        CriarListaEquipamentos("Agente:", versaoagente, View.GONE);
                        CriarListaEquipamentos("Nº Série:", jsonObject.getString("serial"), View.GONE);
                        CriarListaEquipamentos("Modificado:", jsonObject.getString("date_mod"), View.GONE);
                        CriarListaEquipamentos("Estado:", jsonObject.getString("states_id"), View.GONE);
                        CriarListaEquipamentos("Marca:", jsonObject.getString("manufacturers_id"), View.GONE);
                        CriarListaEquipamentos("Tipo:", jsonObject.getString("computertypes_id"), View.GONE);
                        String tipo =jsonObject.getString("computertypes_id");

                        //Seta dados para a lista de detalhes do equipamento
                        ListAdapterEquipamentos equipamentoadapter = new ListAdapterEquipamentos(listaequipamentos, ScannerActivity.this, idequipamento);
                        mListaEquipamentos.setAdapter(equipamentoadapter);
                        mLayoutEquipamentos.setVisibility(View.VISIBLE);
                        
                        if (mLayoutCofreBotoes != null) mLayoutCofreBotoes.setVisibility(View.VISIBLE);

                        //Pega Monitores
                        try {
                            JSONArray monitoresarray = jsonObject.getJSONObject("_connections").getJSONArray("Monitor");
                            for (int i = 0; i < monitoresarray.length(); i++) {
                                JSONObject monitor = monitoresarray.getJSONObject(i);
                                CriarListaMonitores(monitor.getString("name"), monitor.getString("manufacturers_id"), monitor.getString("monitormodels_id"), monitor.getString("states_id"), monitor.getString("id"), monitor.getString("serial"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //Seta dados pata a lista de monitores
                        ListAdapterMonitores adaptermonitores = new ListAdapterMonitores(listamonitores, ScannerActivity.this, idequipamento);
                        mListaMonitores.setAdapter(adaptermonitores);

                        if(!tipo.equals("Notebook")){
                            mLayoutMonitores.setVisibility(View.VISIBLE);
                        }else{
                            mLayoutMonitores.setVisibility(View.GONE);
                        }

                        //Pega as problemas
                        try {
                            JSONArray changesarray = jsonObject.getJSONArray("_problems");
                            for (int i = 0; i < changesarray.length(); i++) {
                                JSONObject mudanca = changesarray.getJSONObject(i);
                                int imagem = R.drawable.status_aberto;
                                int botao = View.VISIBLE;
                                if (mudanca.getString("status").equals("6")) {//Status 6 = fechado
                                    imagem = R.drawable.status_fechado;
                                    botao = View.GONE;
                                } else { // marca true caso haja manutencao em aberto
                                    existemanutencaoaberta = true;
                                }
                                CriarListaMudancas(mudanca.getString("name"), mudanca.getString("users_id_recipient"), mudanca.getString("content"), imagem, botao, mudanca.getString("date"), mudanca.getString("closedate"), mudanca.getString("users_id_lastupdater"), mudanca.getString("id"));
                            }

                            //Confere se existe manutenção aberta, caso não exita, abre botão add nova
                            if (existemanutencaoaberta == false) {
                                mBtNovaManutencao.setVisibility(View.VISIBLE);
                            }
                            //Seta dados pata a lista de mudanças
                            ListAdapterMudancas adapter = new ListAdapterMudancas(listamudancas, ScannerActivity.this, idequipamento);
                            mListaMudancas.setAdapter(adapter);
                            //mostra a tela de manutenções
                            mLayoutManutencoes.setVisibility(View.VISIBLE);
                            mImvSyncDevice.setVisibility(View.GONE);
                            mPgbProgresso.setIndeterminate(false);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //mPgbProgresso.setIndeterminate(false);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onVolleyFailure(String url) {
                    Log.d("VolleyFailure", url);
                    Snackbar.make(
                            mCLayout,
                            "Erro de conexão\n" + url,
                            Snackbar.LENGTH_LONG
                    ).show();
                }
            });
        }else{

        }
    }

    private void GetIdEquipamento() {
        mPgbProgresso.setIndeterminate(true);
        //Limpa views
        inicializarViews();
        //Limpa listas
        inicializarListas();
        GLPIConnect con = new GLPIConnect(this);
        con.GetArray("/apirest.php/Computer?searchText[name]=PSO-"+ mTvIdEquipamento.getText().toString(), new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                } catch (JSONException e) {
                    Log.d("ParseError", e.toString());
                    e.printStackTrace();
                }
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    //Pega dados do equipamento
                    BuscarListaEquipamentos(jsonObject.getString("id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                mTvIdEquipamento.setText("erro");
                Snackbar.make(
                        mCLayout,
                        "Erro de conexão\n" + url,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    private void BuscarListaMudancas() {

    }

    public void CriarListaEquipamentos(String desc, String cont, Integer img){
        EquipamentoLine equip = new EquipamentoLine();
        equip.setDescricao(desc);
        equip.setConteudo(cont);
        equip.setBtEditar(img);
        listaequipamentos.add(equip);
    }

    private void CriarListaMonitores(String nome, String marca, String modelo, String estado, String id, String numeroserie) {
        MonitorLine monitor = new MonitorLine();
        monitor.setNome(nome);
        monitor.setMarca(marca);
        monitor.setModelo(modelo);
        monitor.setEstado(estado);
        monitor.setIdMonitor(id);
        monitor.setNumeroSerie(numeroserie);
        listamonitores.add(monitor);
    }

    public void CriarListaMudancas(String titulo, String nome, String texto, int Image, int visible, String datamanu, String datafinal, String usuariofinal, String Id){
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(Image);
        Button botao = new Button(this);
        botao.setVisibility(visible);

        MudancasLine mud = new MudancasLine();
        mud.setTitulo(titulo);
        mud.setUsuarioCriacao(nome);
        mud.setTexto(texto);
        mud.setDataManutencao(datamanu);
        mud.setDataFinalizacao(datafinal);
        mud.setUsuarioFinalizacao(usuariofinal);
        mud.setIdMudanca(Id);
        mud.setImagemStatus(imageView);
        mud.setBtFinalizarManutencao(botao);
        listamudancas.add(mud);
        Log.d("grub", titulo);
    }

    public void CriarListaPlacasRede(String titulo, String data, String status){
        PlacasRedeLine rede = new PlacasRedeLine();
        rede.setNome(titulo); ;
        rede.setMac(data);
        rede.setIp(status);
        //listaplacasrede.add(rede);
    }

    public void CriarListaPlacasWifi(String titulo, String data, String status){
        PlacasRedeLine wifi = new PlacasRedeLine();
        wifi.setNome(titulo);
        wifi.setMac(data);
        wifi.setIp(status);
        //listaplacaswifi.add(wifi);
    }

    private void PreencherListaMonitores() {
        mPgbProgresso.setIndeterminate(true);
        mMonitorNamesList = new ArrayList<>();
        mMonitorIdsList = new ArrayList<>();
        mMonitorAdapter = new ArrayAdapter<>(ScannerActivity.this, android.R.layout.simple_dropdown_item_1line, mMonitorNamesList);

        GLPIConnect con = new GLPIConnect(this);
        con.GetArray("/apirest.php/Monitor?range=0-1000", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                mPgbProgresso.setIndeterminate(false);
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject monitor = jsonArray.getJSONObject(i);
                        mMonitorNamesList.add(monitor.getString("name"));
                        mMonitorIdsList.add(monitor.getString("id"));
                    }
                    mMonitorAdapter.notifyDataSetChanged();
                    VincularMonitorDialog();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ScannerActivity.this, "Erro ao processar monitores.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                mPgbProgresso.setIndeterminate(false);
                Toast.makeText(ScannerActivity.this, "Erro de conexão ao buscar monitores:\n" + url, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void VincularMonitorDialog(){
        View view = LayoutInflater.from(ScannerActivity.this).inflate(R.layout.activity_vincular_monitor, null);
        android.widget.AutoCompleteTextView edittext = view.findViewById(R.id.TvNomeMonitorVincularMonitor);
        
        edittext.setAdapter(mMonitorAdapter);
        edittext.setThreshold(1); // Show suggestions after 1 character

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ScannerActivity.this)
            .setTitle("Nome do Monitor")
            .setView(view)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String nomeDigitado = edittext.getText().toString();
                    int index = mMonitorNamesList.indexOf(nomeDigitado);
                    if (index != -1) {
                        String idMonitorSelecionado = mMonitorIdsList.get(index);
                        AddConexao("/apirest.php/Computer_Item/", idMonitorSelecionado);
                    } else {
                        Toast.makeText(ScannerActivity.this, "Selecione um monitor válido da lista.", Toast.LENGTH_LONG).show();
                    }
                }
            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        dialog.create();
        dialog.show();
    }

    private void AddConexao(String url, String idmonitor) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("items_id", idmonitor);
            postparams.put("computers_id", mTvIdEquipamento.getText().toString());
            postparams.put("itemtype", "Monitor");
            postparams.put("is_deleted", "0");
            postparams.put("is_dynamic", "1");

            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("sessiontoken", url);
        Log.e("sessiontoken", finalarray.toString());
        //Faz a conexão
        GLPIConnect con = new GLPIConnect(ScannerActivity.this);
        con.InsertItem(url, finalarray, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                IrPara(ScannerActivity.class, true);
            }
            @Override
            public void onVolleyFailure(String errorMsg) {
                Log.d("sessiontoken", errorMsg);
                if(errorMsg != null && errorMsg.contains("ERROR_GLPI_ADD")) {
                    Toast.makeText(ScannerActivity.this, "Erro: Monitor já está vinculado a outro equipamento.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ScannerActivity.this, "Erro: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void GetIdMonitor(String nome) {
        mIdMonitor = "";
        GLPIConnect con = new GLPIConnect(this);
        con.GetArray("/apirest.php/Monitor?searchText[name]="+ nome, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                } catch (JSONException e) {
                    Log.d("ParseError", e.toString());
                    e.printStackTrace();
                }
                try {
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        //Pega dados do equipamento
                        String idmonitor = jsonObject.getString("id");
                        if (!idmonitor.equals("")) {
                            //Vincular monitor
                            AddConexao("/apirest.php/Computer_Item/", idmonitor);
                        }else{
                            Toast.makeText(ScannerActivity.this, "Monitor não encontrado", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ScannerActivity.this, "Monitor não encontrado", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                mTvIdEquipamento.setText("erro");
                Snackbar.make(
                        mCLayout,
                        "Erro de conexão\n" + url,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Escaneamento cancelado");
                Toast.makeText(this, "Escaneamento cancelado", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MainActivity", "Escaneado");
                //Toast.makeText(this, "Escaneado: " + result.getContents(), Toast.LENGTH_LONG).show();
                mTvIdEquipamento.setText(result.getContents());
                GetIdEquipamento();
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
        }
    }

    private void AbrirCofreSenhas(String itemtype, String itemId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gerenciar_senhas, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RecyclerView rvSenhas = dialogView.findViewById(R.id.RvSenhas);
        TextView tvSenhasEmpty = dialogView.findViewById(R.id.TvSenhasEmpty);
        TextInputEditText edtDesc = dialogView.findViewById(R.id.EdtDescricaoSenha);
        TextInputEditText edtValor = dialogView.findViewById(R.id.EdtValorSenha);
        ImageButton btnAdd = dialogView.findViewById(R.id.BtnAddSenha);
        final Button btnFechar = dialogView.findViewById(R.id.BtnFecharSenhas);
        TextView tvTitle = dialogView.findViewById(R.id.TvDialogTitle);

        btnFechar.setVisibility(View.GONE);
        btnFechar.setText("Salvar no Servidor e Fechar");
        
        edtDesc.setHint("Usuário / Identificador");

        tvTitle.setText(itemtype.equals("Computer") ? "Senhas da Máquina" : "Senhas do Local");

        rvSenhas.setLayoutManager(new LinearLayoutManager(this));
        
        final ArrayList<SenhaItem> currentPasswords = new ArrayList<>();
        final int[] editingPosition = {-1};

        final String[] existingSecretId = {null};
        final String[] existingFieldsItemId = {null};
        final String[] containerId = {"1"};

        GLPIConnect con = new GLPIConnect(this);
        InfisicalConnect infisical = new InfisicalConnect(this);

        final boolean[] isDirty = {false};

        Runnable updateButtonVisibility = () -> {
            String d = edtDesc.getText().toString().trim();
            String s = edtValor.getText().toString().trim();
            if (!d.isEmpty() || !s.isEmpty() || editingPosition[0] != -1 || isDirty[0]) {
                btnFechar.setVisibility(View.VISIBLE);
            } else {
                btnFechar.setVisibility(View.GONE);
            }
        };

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateButtonVisibility.run(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtDesc.addTextChangedListener(watcher);
        edtValor.addTextChangedListener(watcher);

        ListAdapterSenhas adapter = new ListAdapterSenhas(currentPasswords, this, new ListAdapterSenhas.OnSenhaInteractionListener() {
            @Override
            public void onDeleteClick(int position, SenhaItem item) {
                currentPasswords.remove(position);
                if(rvSenhas.getAdapter() != null) rvSenhas.getAdapter().notifyDataSetChanged();
                if (currentPasswords.isEmpty()) tvSenhasEmpty.setVisibility(View.VISIBLE);
                
                // Exclusão Instantânea no Servidor
                // Exclusão Instantânea no Servidor
                persistirAlteracoesNoVault(infisical, con, currentPasswords, itemtype, itemId, existingSecretId, existingFieldsItemId, containerId, null, false, isDirty, updateButtonVisibility);

                // Se o item deletado era o que estava sendo editado, reseta
                if (editingPosition[0] == position) {
                    editingPosition[0] = -1;
                    edtDesc.setText("");
                    edtValor.setText("");
                    btnAdd.setImageResource(android.R.drawable.ic_input_add);
                } else if (editingPosition[0] > position) {
                    editingPosition[0]--;
                }
                updateButtonVisibility.run();
            }

            @Override
            public void onEditClick(int position, SenhaItem item) {
                edtDesc.setText(item.getDescricao());
                edtValor.setText(item.getSenha());
                editingPosition[0] = position;
                btnAdd.setImageResource(android.R.drawable.ic_menu_save);
                updateButtonVisibility.run();
                Toast.makeText(ScannerActivity.this, "Editando: " + item.getDescricao(), Toast.LENGTH_SHORT).show();
            }
        });
        rvSenhas.setAdapter(adapter);

        mPgbProgresso.setIndeterminate(true);

        if (!infisical.isConfigured()) {
            RedirecionarParaConfigsCofre("Credenciais do Cofre não configuradas.");
            return;
        }

        String fieldsEndpoint = "/apirest.php/PluginFields" + itemtype + "cofredesenha/?searchText[items_id]=" + itemId;
        
        con.GetArray(fieldsEndpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                try {
                    JSONArray results = new JSONArray(response);
                    
                    if (results.length() > 0) {
                        JSONObject row = results.getJSONObject(0);
                        
                        existingFieldsItemId[0] = row.optString("id");
                        String cId = row.optString("plugin_fields_containers_id");
                        if(cId != null && !cId.isEmpty() && !cId.equals("null")) {
                            containerId[0] = cId;
                        }
                        
                        String vaultId = row.optString("vaultsecretidfield", "");
                        
                        // Treat generic 0 IDs from generic unassigned forms as null so we can POST
                        if(existingFieldsItemId[0] != null && existingFieldsItemId[0].equals("0")) { existingFieldsItemId[0] = null; }
                        
                        if (!vaultId.isEmpty() && !vaultId.equals("null")) {
                            existingSecretId[0] = vaultId;
                            infisical.GetSecret(vaultId, new InfisicalConnect.VolleyResponseListener() {
                                @Override
                                public void onVolleySuccess(String infResponse) {
                                    mPgbProgresso.setIndeterminate(false);
                                    try {
                                        JSONObject respObj = new JSONObject(infResponse);
                                        JSONObject secretObj = respObj.getJSONObject("secret");
                                        String secretValue = secretObj.getString("secretValue");
                                        
                                        JSONArray secretsArray = new JSONArray(secretValue);
                                        for (int i = 0; i < secretsArray.length(); i++) {
                                            JSONObject s = secretsArray.getJSONObject(i);
                                            currentPasswords.add(new SenhaItem(s.optString("descricao"), s.optString("senha")));
                                        }
                                        
                                        if (currentPasswords.isEmpty()) tvSenhasEmpty.setVisibility(View.VISIBLE);
                                        else tvSenhasEmpty.setVisibility(View.GONE);
                                        
                                        adapter.notifyDataSetChanged();
                                        dialog.show();
                                        
                                    } catch (JSONException e) {
                                        tvSenhasEmpty.setVisibility(View.VISIBLE);
                                        dialog.show();
                                    }
                                }
                                 @Override
                                 public void onVolleyFailure(String error) {
                                     mPgbProgresso.setIndeterminate(false);
                                     Log.e("ScannerActivity", "Erro Infisical: " + error);
                                     
                                     if (error != null && error.contains("401")) {
                                         RedirecionarParaConfigsCofre("Erro de Autenticação (401). Verifique suas chaves.");
                                     } else {
                                         Toast.makeText(ScannerActivity.this, "Erro Infisical: " + error, Toast.LENGTH_SHORT).show();
                                         tvSenhasEmpty.setVisibility(View.VISIBLE);
                                         dialog.show();
                                     }
                                 }
                            });
                            return; // Se encontrou, interrompe o fluxo para esperar a assincronicidade do Volley
                        }
                    }
                    
                    // Se o item não tem_plugin_fields, o campo está vazio no servidor
                    // Segue normal para exibir lista vazia
                    mPgbProgresso.setIndeterminate(false);
                    tvSenhasEmpty.setVisibility(View.VISIBLE);
                    dialog.show();
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    mPgbProgresso.setIndeterminate(false);
                    tvSenhasEmpty.setVisibility(View.VISIBLE);
                    dialog.show();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                mPgbProgresso.setIndeterminate(false);
                tvSenhasEmpty.setVisibility(View.VISIBLE);
                dialog.show();
            }
        });

        Runnable addOrUpdateAction = () -> {
            String d = edtDesc.getText().toString().trim();
            String s = edtValor.getText().toString().trim();
            if (d.isEmpty() || s.isEmpty()) return;

            if (editingPosition[0] != -1) {
                SenhaItem item = currentPasswords.get(editingPosition[0]);
                item.setDescricao(d);
                item.setSenha(s);
                editingPosition[0] = -1;
                isDirty[0] = true;
                btnAdd.setImageResource(android.R.drawable.ic_input_add);
            } else {
                SenhaItem newItem = new SenhaItem(d, s);
                currentPasswords.add(newItem);
                isDirty[0] = true;
            }
            adapter.notifyDataSetChanged();
            tvSenhasEmpty.setVisibility(View.GONE);
            edtDesc.setText("");
            edtValor.setText("");
            updateButtonVisibility.run();
        };

        btnAdd.setOnClickListener(v -> {
            String d = edtDesc.getText().toString().trim();
            String s = edtValor.getText().toString().trim();
            if (d.isEmpty() || s.isEmpty()) {
                Toast.makeText(ScannerActivity.this, "Preencha usuário e senha.", Toast.LENGTH_SHORT).show();
                return;
            }
            addOrUpdateAction.run();
        });

        btnFechar.setOnClickListener(v -> {
            // Auto-Adicionar se houver algo digitado
            addOrUpdateAction.run();
            persistirAlteracoesNoVault(infisical, con, currentPasswords, itemtype, itemId, existingSecretId, existingFieldsItemId, containerId, dialog, true, isDirty, updateButtonVisibility);
        });
    }

    private void persistirAlteracoesNoVault(InfisicalConnect infisical, GLPIConnect con, ArrayList<SenhaItem> currentPasswords, String itemtype, String itemId, String[] existingSecretId, String[] existingFieldsItemId, String[] containerId, AlertDialog dialog, boolean closeAfter, boolean[] isDirty, Runnable updateButtonVisibility) {
        if (!infisical.isConfigured()) {
            if (dialog != null) Toast.makeText(ScannerActivity.this, "Configure o Infisical primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        mPgbProgresso.setIndeterminate(true);

        JSONArray arrayToSave = new JSONArray();
        try {
            for (SenhaItem item : currentPasswords) {
                JSONObject o = new JSONObject();
                o.put("descricao", item.getDescricao());
                o.put("senha", item.getSenha());
                arrayToSave.put(o);
            }
        } catch (JSONException e) { e.printStackTrace(); }
        
        String jsonPayload = arrayToSave.toString();
        
        if (currentPasswords.isEmpty() && existingSecretId[0] != null) {
            infisical.UpdateSecret(existingSecretId[0], "[]", new InfisicalConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String response) {
                    existingSecretId[0] = null;
                    limparCustomFieldGLPI(con, itemtype, itemId, existingFieldsItemId[0], dialog, closeAfter, isDirty, updateButtonVisibility);
                }
                @Override
                public void onVolleyFailure(String error) {
                    mPgbProgresso.setIndeterminate(false);
                    Toast.makeText(ScannerActivity.this, "Erro apagando secret (update []): " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        if (currentPasswords.isEmpty()) {
            mPgbProgresso.setIndeterminate(false);
            if (closeAfter && dialog != null) dialog.dismiss();
            return;
        }

        if (existingSecretId[0] != null) {
            infisical.UpdateSecret(existingSecretId[0], jsonPayload, new InfisicalConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String response) {
                    mPgbProgresso.setIndeterminate(false);
                    isDirty[0] = false;
                    if (closeAfter) {
                        Toast.makeText(ScannerActivity.this, "Senhas salvas no Vault!", Toast.LENGTH_SHORT).show();
                        if (dialog != null) dialog.dismiss();
                    } else {
                        updateButtonVisibility.run();
                    }
                }
                @Override
                public void onVolleyFailure(String error) {
                    mPgbProgresso.setIndeterminate(false);
                    Toast.makeText(ScannerActivity.this, "Erro atualizando secret", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            String newKey = "GLPI_" + itemtype.toUpperCase() + "_" + itemId + "_CRED";
            infisical.CreateSecret(newKey, jsonPayload, new InfisicalConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String response) {
                    existingSecretId[0] = newKey; 
                    atualizarCustomFieldGLPI(con, itemtype, itemId, existingFieldsItemId[0], containerId[0], newKey, dialog, closeAfter, isDirty, updateButtonVisibility);
                }
                @Override
                public void onVolleyFailure(String error) {
                    if (error.contains("already exists") || error.contains("Secret already exists")) {
                        existingSecretId[0] = newKey;
                        infisical.UpdateSecret(newKey, jsonPayload, new InfisicalConnect.VolleyResponseListener() {
                            @Override
                            public void onVolleySuccess(String updateResponse) {
                                atualizarCustomFieldGLPI(con, itemtype, itemId, existingFieldsItemId[0], containerId[0], newKey, dialog, closeAfter, isDirty, updateButtonVisibility);
                            }
                            @Override
                            public void onVolleyFailure(String updateError) {
                                mPgbProgresso.setIndeterminate(false);
                                Toast.makeText(ScannerActivity.this, "Erro atualizando secret: " + updateError, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        mPgbProgresso.setIndeterminate(false);
                        Toast.makeText(ScannerActivity.this, "Erro criando secret: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void atualizarCustomFieldGLPI(GLPIConnect con, String itemtype, String itemId, String existingRowId, String containerId, String newKey, AlertDialog dialog, boolean closeAfter, boolean[] isDirty, Runnable updateButtonVisibility) {
        String endpoint = "/apirest.php/PluginFields" + itemtype + "cofredesenha/";
        int method = Request.Method.POST;
        
        if (existingRowId != null && !existingRowId.isEmpty()) {
            endpoint += existingRowId;
            method = Request.Method.PUT;
        }

        JSONObject payload = new JSONObject();
        JSONObject input = new JSONObject();
        try {
            if (method == Request.Method.PUT) {
                input.put("id", Integer.parseInt(existingRowId));
            }
            input.put("items_id", Integer.parseInt(itemId));
            input.put("itemtype", itemtype);
            input.put("plugin_fields_containers_id", 1);
            input.put("vaultsecretidfield", newKey);
            
            payload.put("input", input);
            Log.d("GLPI_FIELDS_MAIN", "Iniciando update no GLPI.");
            Log.d("GLPI_FIELDS_MAIN", "URL: " + endpoint);
            Log.d("GLPI_FIELDS_MAIN", "Payload enviado: " + payload.toString());
            
            con.UpdateItemRaw(endpoint, payload, method, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String u, String r) {
                    mPgbProgresso.setIndeterminate(false);
                    isDirty[0] = false;
                    Log.d("GLPI_FIELDS_MAIN", "Sucesso no update GLPI! Resposta: " + r);
                    if (closeAfter) {
                        Toast.makeText(ScannerActivity.this, "Senhas salvas no Vault!", Toast.LENGTH_SHORT).show();
                        if (dialog != null) dialog.dismiss();
                    } else {
                        updateButtonVisibility.run();
                    }
                }
                @Override
                public void onVolleyFailure(String e) {
                    mPgbProgresso.setIndeterminate(false);
                    Log.e("GLPI_FIELDS_MAIN", "Falha no update GLPI. Erro: " + e);
                    Toast.makeText(ScannerActivity.this, "Erro Atualizando GLPI: " + e, Toast.LENGTH_LONG).show();
                    if (closeAfter && dialog != null) dialog.dismiss();
                }
            });
            
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
            mPgbProgresso.setIndeterminate(false);
            Log.e("GLPI_FIELDS_MAIN", "Erro montando JSON _plugin_fields: " + e.getMessage());
            Toast.makeText(ScannerActivity.this, "Erro montando JSON _plugin_fields: " + e.getMessage(), Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    private void limparCustomFieldGLPI(GLPIConnect con, String itemtype, String itemId, String existingRowId, AlertDialog dialog, boolean closeAfter, boolean[] isDirty, Runnable updateButtonVisibility) {
         if (existingRowId == null || existingRowId.isEmpty()) {
             mPgbProgresso.setIndeterminate(false);
             isDirty[0] = false;
             Toast.makeText(ScannerActivity.this, "Senha removida.", Toast.LENGTH_SHORT).show();
             if (closeAfter && dialog != null) dialog.dismiss();
             else updateButtonVisibility.run();
             return;
         }
         try {
             String endpoint = "/apirest.php/PluginFields" + itemtype + "cofredesenha/" + existingRowId;
             JSONObject payload = new JSONObject();
             JSONObject input = new JSONObject();
             
             input.put("id", Integer.parseInt(existingRowId));
             input.put("items_id", Integer.parseInt(itemId));
             input.put("itemtype", itemtype);
             input.put("plugin_fields_containers_id", 1);
             input.put("vaultsecretidfield", "");
             
             payload.put("input", input);
             Log.d("GLPI_FIELDS_MAIN", "Iniciando clean up no GLPI.");
             Log.d("GLPI_FIELDS_MAIN", "URL: " + endpoint);
             Log.d("GLPI_FIELDS_MAIN", "Payload enviado: " + payload.toString());
             con.UpdateItemRaw(endpoint, payload, Request.Method.PUT, new GLPIConnect.VolleyResponseListener(){
                  @Override
                  public void onVolleySuccess(String url, String response) {
                       mPgbProgresso.setIndeterminate(false);
                       isDirty[0] = false;
                       Log.d("GLPI_FIELDS_MAIN", "Sucesso no clean GLPI! Resposta: " + response);
                       Toast.makeText(ScannerActivity.this, "Senha removida.", Toast.LENGTH_SHORT).show();
                       if (closeAfter) {
                           if (dialog != null) dialog.dismiss();
                       } else {
                           updateButtonVisibility.run();
                       }
                  }
                  @Override
                  public void onVolleyFailure(String error) {
                       mPgbProgresso.setIndeterminate(false); 
                       Log.e("GLPI_FIELDS_MAIN", "Falha no clean GLPI. Erro: " + error);
                       Toast.makeText(ScannerActivity.this, "Erro Removendo no GLPI: " + error, Toast.LENGTH_LONG).show();
                       if (closeAfter && dialog != null) dialog.dismiss();
                  }
             });
         } catch(JSONException e){ 
             e.printStackTrace(); 
             Log.e("GLPI_FIELDS_MAIN", "Erro montando JSON _plugin_fields: " + e.getMessage());
             mPgbProgresso.setIndeterminate(false); 
             if (closeAfter && dialog != null) dialog.dismiss(); 
         }
    }

    private void RedirecionarParaConfigsCofre(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, VaultSettingsActivity.class);
        startActivity(intent);
    }
}
