package com.cyberrocket.inventario;

import android.content.Intent;
import android.os.Bundle;

import com.cyberrocket.inventario.adapter.ListAdapterEquipamentos;
import com.cyberrocket.inventario.adapter.ListAdapterMudancas;
import com.cyberrocket.inventario.adapter.ListAdapterPlacasRede;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.EquipamentoLine;
import com.cyberrocket.inventario.models.MudancasLine;
import com.cyberrocket.inventario.models.PlacasRedeLine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    RecyclerView mListaPlacasRede;
    RecyclerView mListaPlacasWifi;
    Button mBtNovaManutencao;

    FloatingActionButton mBtLerEquipamento;
    LinearLayoutManager linearLayoutManagerEquipamento;
    StaggeredGridLayoutManager gridLayoutManagerEquipamento;
    LinearLayoutManager linearLayoutManagerMudancas;
    StaggeredGridLayoutManager gridLayoutManagerMudancas;
    LinearLayoutManager linearLayoutManagerPlacasRede;
    StaggeredGridLayoutManager gridLayoutManagerPlacasRede;
    LinearLayoutManager linearLayoutManagerPlacasWifi;
    StaggeredGridLayoutManager gridLayoutManagerPlacasWifi;
    Crud mCrud;
    ArrayList<EquipamentoLine> listaequipamentos;
    ArrayList<MudancasLine> listamudancas;
    ArrayList<PlacasRedeLine> listaplacasrede;
    ArrayList<PlacasRedeLine> listaplacaswifi;
    Boolean existemanutencaoaberta = false;

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
                mTvIdEquipamento.setText("1");
                inicializarListas();
                BuscarListaEquipamentos();
                BuscarListaMudancas();
            }
        });

        mBtNovaManutencao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrPara(CadMudancaActivity.class, false);
            }
        });
    }

    //Métodos

    private void IrPara(Class para, boolean finalizar) {
        Intent intent = new Intent(getApplicationContext(), para);
        intent.putExtra("idequipamento", mTvIdEquipamento.getText().toString());
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
            BuscarListaEquipamentos();
            BuscarListaMudancas();
        }
    }

    private void inicializarViews() {
        mCLayout = findViewById(R.id.CntScanner);
        mTvIdEquipamento = findViewById(R.id.TvIdEquipamento);
        mImvImgEquipamento = findViewById(R.id.ImvImgDevice);
        mPgbProgresso = findViewById(R.id.PgbScanner);
        mBtNovaManutencao = findViewById(R.id.BtNovaManutencaoScanner);

        mListaEquipamentos = findViewById(R.id.RvDetalhesEquipamentoScanner);
        mListaMudancas = findViewById(R.id.RvMudancasScanner);
        mListaPlacasRede = findViewById(R.id.RvPlacasRedeScanner);
        mListaPlacasWifi = findViewById(R.id.RvPlacasWifiScanner);

        linearLayoutManagerEquipamento = new LinearLayoutManager(this);
        gridLayoutManagerEquipamento = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL);

        linearLayoutManagerMudancas = new LinearLayoutManager(this);
        gridLayoutManagerMudancas = new StaggeredGridLayoutManager(1, LinearLayoutManager.HORIZONTAL);

        linearLayoutManagerPlacasRede = new LinearLayoutManager(this);
        gridLayoutManagerPlacasRede = new StaggeredGridLayoutManager(1, LinearLayoutManager.HORIZONTAL);

        linearLayoutManagerPlacasWifi = new LinearLayoutManager(this);
        gridLayoutManagerPlacasWifi = new StaggeredGridLayoutManager(1, LinearLayoutManager.HORIZONTAL);

        mListaEquipamentos.setLayoutManager(gridLayoutManagerEquipamento);
        mListaMudancas.setLayoutManager(gridLayoutManagerMudancas);
        mListaPlacasRede.setLayoutManager(gridLayoutManagerPlacasRede);
        mListaPlacasWifi.setLayoutManager(gridLayoutManagerPlacasWifi);

        mBtLerEquipamento = findViewById(R.id.BtLerEquipamentoScanner);
        mCrud = new Crud();
    }

    private void inicializarListas() {
        listamudancas = new ArrayList<MudancasLine>();
        listaplacasrede = new ArrayList<PlacasRedeLine>();
        listaequipamentos = new ArrayList<EquipamentoLine>();
        listaplacaswifi = new ArrayList<PlacasRedeLine>();
    }

    private void BuscarListaEquipamentos() {
        mPgbProgresso.setIndeterminate(true);
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetItem("/apirest.php/Computer/"+ mTvIdEquipamento.getText().toString()+"?expand_dropdowns=true&with_networkports=true", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject = new JSONObject(response);
                }catch (JSONException err){
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

                    //Seta dados para a lista de detalhes do equipamento
                    ListAdapterEquipamentos equipamentoadapter = new ListAdapterEquipamentos(listaequipamentos, ScannerActivity.this, mTvIdEquipamento.getText().toString());
                    mListaEquipamentos.setAdapter(equipamentoadapter);

                    //Pegar Placas de rede
                    JSONArray networkportsethernet = jsonObject.getJSONObject("_networkports").getJSONArray("NetworkPortEthernet");
                    for (int i = 0; i < networkportsethernet.length(); i++) {
                        JSONObject ethernet = networkportsethernet.getJSONObject(i);
                        CriarListaPlacasRede(ethernet.getString("name"), ethernet.getString("mac"), ethernet.getJSONObject("NetworkName").getJSONArray("IPAddress").getJSONObject(0).getString("name"));
                    }

                    //Seta dados para a lista de rede cabeada
                    ListAdapterPlacasRede redeadapter = new ListAdapterPlacasRede(listaplacasrede, ScannerActivity.this);
                    mListaPlacasRede.setAdapter(redeadapter);

                    //Pegar Placas de rede wifi
                    JSONArray networkportswifi = jsonObject.getJSONObject("_networkports").getJSONArray("NetworkPortWifi");
                    for (int i = 0; i < networkportswifi.length(); i++) {
                        JSONObject wifi = networkportswifi.getJSONObject(i);
                        CriarListaPlacasWifi(wifi.getString("name"), wifi.getString("mac"), wifi.getJSONObject("NetworkName").getJSONArray("IPAddress").getJSONObject(0).getString("name"));
                    }

                    //Seta dados para a lista de rede wifi
                    ListAdapterPlacasRede wifiadapter = new ListAdapterPlacasRede(listaplacaswifi, ScannerActivity.this);
                    mListaPlacasWifi.setAdapter(wifiadapter);

                    //mPgbProgresso.setIndeterminate(false);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Snackbar.make(
                        mCLayout,
                        "Erro de conexão\n"+url,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    private void BuscarListaMudancas() {
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetArray("/apirest.php/Computer/"+ mTvIdEquipamento.getText().toString()+"/Change?expand_dropdowns=true&order=DESC", new GLPIConnect.VolleyResponseListener() {
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
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject mudanca = jsonArray.getJSONObject(i);
                        int imagem = R.drawable.status_aberto;
                        int botao = View.VISIBLE;
                        if(mudanca.getString("status").equals("6")){//Status 6 = fechado
                            imagem = R.drawable.status_fechado;
                            botao = View.GONE;
                        }else{ // marca true caso haja manutencao em aberto
                            existemanutencaoaberta = true;
                        }
                        CriarListaMudancas(mudanca.getString("name"), mudanca.getString("users_id_recipient"), mudanca.getString("content"), imagem, botao, mudanca.getString("date"), mudanca.getString("closedate"), mudanca.getString("users_id_lastupdater"), mudanca.getString("id"));
                    }

                    //Confere se existe manutenção aberta, caso não exita, abre botão add nova
                    if (existemanutencaoaberta == false) {
                        mBtNovaManutencao.setVisibility(View.VISIBLE);
                    }

                    ListAdapterMudancas adapter = new ListAdapterMudancas(listamudancas, ScannerActivity.this, mTvIdEquipamento.getText().toString());
                    mListaMudancas.setAdapter(adapter);
                    mPgbProgresso.setIndeterminate(false);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Snackbar.make(
                        mCLayout,
                        "Erro de conexão\n"+url,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    public void CriarListaEquipamentos(String desc, String cont, Integer img){
        EquipamentoLine equip = new EquipamentoLine();
        equip.setDescricao(desc);
        equip.setConteudo(cont);
        equip.setBtEditar(img);
        listaequipamentos.add(equip);
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
    }

    public void CriarListaPlacasRede(String titulo, String data, String status){
        PlacasRedeLine rede = new PlacasRedeLine();
        rede.setNome(titulo); ;
        rede.setMac(data);
        rede.setIp(status);
        listaplacasrede.add(rede);
    }

    public void CriarListaPlacasWifi(String titulo, String data, String status){
        PlacasRedeLine wifi = new PlacasRedeLine();
        wifi.setNome(titulo);
        wifi.setMac(data);
        wifi.setIp(status);
        listaplacaswifi.add(wifi);
    }
}
