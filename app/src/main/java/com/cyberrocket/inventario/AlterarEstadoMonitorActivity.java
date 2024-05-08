package com.cyberrocket.inventario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AlterarEstadoMonitorActivity extends AppCompatActivity {
    Spinner mSpListaEstados;
    String mIdMonitor;
    String mIdEquipamento;
    String mEstadoEquipamento;
    Button mBtOk;
    Button mBtCancelar;
    ArrayList<String> mList;
    ArrayAdapter<String> mAdapter;
    ArrayList mListaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_estado_monitor);

        //Inicializar
        Inicializar();

        //Métodos automáticos
        getExtras();

        //Listeners
        mBtCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finaliza essa activity e volta para anterior
            }
        });

        mBtOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SalvarAlteracao();
            }
        });
    }

    private void SalvarAlteracao() {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("id", mIdMonitor);
            postparams.put("states_id", mListaId.get(mAdapter.getPosition(mSpListaEstados.getSelectedItem().toString())));
            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.UpdateItem("/apirest.php/Monitor/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                IrPara(ScannerActivity.class);
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(getApplicationContext(), "Erro: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void IrPara(Class<ScannerActivity> para) {
        Intent intent = new Intent(getApplicationContext(), para);
        intent.putExtra("id", mIdEquipamento);
        startActivity(intent);
        finish();
    }

    private void PreencherListaEstados() {
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetArray("/apirest.php/State", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                }catch (JSONException err){
                    Log.d("ParseError", err.toString());
                }
                try {
                    //Pega dados do equipamento
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject local = jsonArray.getJSONObject(i);
                        mAdapter.add(local.getString("name"));
                        mListaId.add(local.getString("id"));
                    }
                    mAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(getApplicationContext(), "Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void Inicializar() {
        mSpListaEstados = findViewById(R.id.SpAlterarEstadoMonitor);
        mBtOk = findViewById(R.id.BtOkAlterarEstadoMonitor);
        mBtCancelar = findViewById(R.id.BtCancelarAlterarEstadoMonitor);
        mList = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, mList);
        mListaId = new ArrayList();
        mSpListaEstados.setAdapter(mAdapter);
        PreencherListaEstados();
    }

    private void getExtras(){
        //Pegar id do equipamento vindo da activity anterior
        Intent it = getIntent();
        mIdMonitor = it.getStringExtra("idmonitor");
        mIdEquipamento = it.getStringExtra("idequipamento");
        mEstadoEquipamento = it.getStringExtra("estadomonitor");
    }
}
