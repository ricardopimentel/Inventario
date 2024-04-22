package com.cyberrocket.inventario;

import android.content.Intent;
import android.os.Bundle;

import com.cyberrocket.inventario.adapter.ListAdapterMudancas;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.EquipamentoLine;
import com.cyberrocket.inventario.adapter.ListAdapterEquipamentos;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cyberrocket.inventario.models.MudancasLine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

//Não utilizado
public class ScanActivity extends AppCompatActivity {
    TextView mTvIdEquipamento;
    ConstraintLayout mCLayout;
    ImageView mImvImgEquipamento;
    Button mBtEmprestar;
    ProgressBar mPgbProgresso;
    RecyclerView mListaEquipamentos;
    RecyclerView mListaMudancas;
    private ListAdapterEquipamentos mAdapter;
    FloatingActionButton mBtLerEquipamento;
    LinearLayoutManager linearLayoutManagerEquipamento;
    StaggeredGridLayoutManager gridLayoutManagerEquipamento;
    LinearLayoutManager linearLayoutManagerMudancas;
    StaggeredGridLayoutManager gridLayoutManagerMudancas;
    Crud mCrud;
    ArrayList<EquipamentoLine> listaequipamentos;
    ArrayList<MudancasLine> listamudancas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //Metodos automáticos
        initializeViews();
        inicializaListas();

        //Listeners
        mBtLerEquipamento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inicializaListas();
                IntentIntegrator integrator = new IntentIntegrator(ScanActivity.this);
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

        mBtEmprestar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList l = new ArrayList();
                l.add(mTvIdEquipamento.getText().toString());   //id
                l.add(listaequipamentos.get(0).getConteudo());              //descricao
                l.add(listaequipamentos.get(1).getConteudo());              //nserie
                l.add(listaequipamentos.get(3).getConteudo());              //modelo
                l.add(listaequipamentos.get(5).getConteudo());              //tipo
                Intent i = new Intent(ScanActivity.this, EmprestimoActivity.class);
                i.putExtra("DadosEquipamento", l );
                startActivity(i);
            }
        });
    }

    // Métodos
    private void initializeViews() {
        mCLayout = findViewById(R.id.container);
        mTvIdEquipamento = findViewById(R.id.tvID);
        mImvImgEquipamento = findViewById(R.id.imvImgEquipamento);
        mBtEmprestar = findViewById(R.id.btnEmprestar);
        mPgbProgresso = findViewById(R.id.pgbProgresso);

        mListaEquipamentos = findViewById(R.id.RvDetalhesEquipamento);
        mListaMudancas = findViewById(R.id.RvMudancas);

        linearLayoutManagerEquipamento = new LinearLayoutManager(this);
        gridLayoutManagerEquipamento = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL);

        linearLayoutManagerMudancas = new LinearLayoutManager(this);
        gridLayoutManagerMudancas = new StaggeredGridLayoutManager(1, LinearLayoutManager.HORIZONTAL);

        mListaEquipamentos.setLayoutManager(gridLayoutManagerEquipamento);
        mListaMudancas.setLayoutManager(gridLayoutManagerMudancas);
        mBtLerEquipamento = findViewById(R.id.btnLerEquipamento);
        mCrud = new Crud();
    }

    public void CriarListaEquipamentos(String desc, String cont, Integer img){
        EquipamentoLine equip = new EquipamentoLine();
        equip.setDescricao(desc);
        equip.setConteudo(cont);
        equip.setBtEditar(img);
        listaequipamentos.add(equip);
    }

    private void inicializaListas() {
        listamudancas = new ArrayList<MudancasLine>();
        listaequipamentos = new ArrayList<EquipamentoLine>();
    }

    public void CriarListaMudancas(String titulo, String data, String status){
        MudancasLine mud = new MudancasLine();
        mud.setTitulo(titulo); ;
        mud.setUsuarioCriacao(data);
        mud.setTexto(status);
        listamudancas.add(mud);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null) {
            if(result.getContents() == null) {
                // as duas linhas seguintes devem ser descomentadas assim que seja necessário testar lenco um código
                //Log.d("MainActivity", "Escaneamento cancelado");
                //Toast.makeText(this, "Escaneamento cancelado", Toast.LENGTH_LONG).show();
                // as linhas seguintes devem ser excluidas, são para teste
                mTvIdEquipamento.setText("1");
                BuscarListaEquipamentos();
                BuscarListaMudancas();
            } else {
                Log.d("MainActivity", "Escaneado");
                //Toast.makeText(this, "Escaneado: " + result.getContents(), Toast.LENGTH_LONG).show();
                mTvIdEquipamento.setText(result.getContents());
                BuscarListaEquipamentos();
                BuscarListaMudancas();
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
        }
    }

    private void BuscarListaEquipamentos() {
        //Inicializar variaveis
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        //Inicia conexão
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
                    CriarListaEquipamentos("Nome:", jsonObject.getString("name"), View.GONE);
                    CriarListaEquipamentos("Inventário:", jsonObject.getString("otherserial"), View.GONE);
                    CriarListaEquipamentos("Nº Série:", jsonObject.getString("serial"), View.GONE);
                    CriarListaEquipamentos("Modificado:", jsonObject.getString("date_mod"), View.GONE);
                    CriarListaEquipamentos("Estado:", jsonObject.getString("states_id"), View.GONE);
                    CriarListaEquipamentos("Marca:", jsonObject.getString("manufacturers_id"), View.GONE);
                    CriarListaEquipamentos("Tipo:", jsonObject.getString("computertypes_id"), View.GONE);
                    CriarListaEquipamentos("Localização:", jsonObject.getString("locations_id"), View.VISIBLE);

                    mPgbProgresso.setIndeterminate(false);

                    mAdapter = new ListAdapterEquipamentos(listaequipamentos, ScanActivity.this,"");
                    mListaEquipamentos.setAdapter(mAdapter);

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
        con.GetArray("/apirest.php/Computer/"+ mTvIdEquipamento.getText().toString()+"/Change?expand_dropdowns=true", new GLPIConnect.VolleyResponseListener() {
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
                        CriarListaMudancas(mudanca.getString("name"), mudanca.getString("content"), mudanca.getString("users_id_recipient"));
                    }

                    mPgbProgresso.setIndeterminate(false);

                    ListAdapterMudancas adapter = new ListAdapterMudancas(listamudancas, ScanActivity.this, "");
                    mListaMudancas.setAdapter(adapter);

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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //Salvar instancia dos itens da tela quando a atividade for destruida
        //Bitmap bitmap = ((BitmapDrawable)mImvImgEquipamento.getDrawable()).getBitmap();
        //savedInstanceState.putParcelable("Imagem", bitmap);
        //savedInstanceState.putString("ID", mTvIdEquipamento.getText().toString());
        //savedInstanceState.putSerializable("Lista", lista);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Restaurar dados da activity
        //mImvImgEquipamento.setImageBitmap((Bitmap) savedInstanceState.getParcelable("Imagem"));
        //lista = (ArrayList<EquipamentoLine>) savedInstanceState.getSerializable("Lista");
        //mAdapter = new ListAdapter(lista, MainActivity.this);
        //mLista.setAdapter(mAdapter);
    }
}
