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
import java.util.ArrayList;

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
                VincularMonitorDialog();
            }
        });

        mSwipeRefreshListEquipamento.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                IrPara(ScannerActivity.class, true);
            }
        });
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
            con.GetItem("/apirest.php/Computer/" + idequipamento + "?expand_dropdowns=true&with_connections=true&with_problems=true", new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = new JSONObject(response);
                    } catch (JSONException err) {
                        Log.d("ParseError", err.toString());
                    }
                    try {
                        //Pega dados do equipamento
                        CriarListaEquipamentos("Nome:", jsonObject.getString("name"), View.GONE);
                        CriarListaEquipamentos("Localização:", jsonObject.getString("locations_id"), View.VISIBLE);
                        CriarListaEquipamentos("Inventário:", jsonObject.getString("otherserial"), View.GONE);
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

                        //Pega as mudanças
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

    private void VincularMonitorDialog(){
        View view = LayoutInflater.from(ScannerActivity.this).inflate(R.layout.activity_vincular_monitor, null);
        TextInputEditText edittext = view.findViewById(R.id.TvNomeMonitorVincularMonitor);
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ScannerActivity.this)
            .setTitle("Nome do Monitor")
            .setView(view)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Pega o ID do
                    GetIdMonitor(edittext.getText().toString());
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
            postparams.put("computers_id", mTvIdEquipamento.getText());
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
            public void onVolleyFailure(String url) {
                Log.d("sessiontoken", url);
                Toast.makeText(ScannerActivity.this, "Erro: "+ url, Toast.LENGTH_LONG).show();
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
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    //Pega dados do equipamento
                    String idmonitor = jsonObject.getString("id");
                    if (!idmonitor.equals("")) {
                        //Vincular monitor
                        AddConexao("/apirest.php/Computer_Item/", idmonitor);
                    }else{
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

}
