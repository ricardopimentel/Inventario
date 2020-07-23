package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentProviderClient;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.cyberrocket.inventario.adapter.ListAdapterEquipamentos;
import com.cyberrocket.inventario.adapter.ListAdapterPlacasRede;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AlterarLocalActivity extends AppCompatActivity {
    ListView mListaLocais;
    ArrayAdapter<String> mAdapter;
    ArrayList<String> mList;
    ArrayList mListaId;
    String mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_local);

        //Inicializar
        mListaLocais = findViewById(R.id.ListLocaisAlterarlocal);
        mList = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, mList);
        mListaLocais.setAdapter(mAdapter);
        mListaId = new ArrayList();

        //Métodos automáticos
        PreencherListaLocais();
        getExtras();

        //Listeners
        mListaLocais.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                AlterarLocalizacao(mListaId.get(position).toString());
            }
        });
    }

    //Métodos
    private void getExtras(){
        //Pegar id do equipamento vindo da activity anterior
        Intent it = getIntent();
        mId = it.getStringExtra("idequipamento");
    }

    private void PreencherListaLocais() {
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetArray("/apirest.php/Location", new GLPIConnect.VolleyResponseListener() {
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

    private void AlterarLocalizacao(String local) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("id", mId);
            postparams.put("locations_id", local);

            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.UpdateItem("/apirest.php/Computer/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
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

    private void IrPara(Class para) {
        Intent intent = new Intent(getApplicationContext(), para);
        intent.putExtra("id", mId);
        startActivity(intent);
        finish();
    }
}