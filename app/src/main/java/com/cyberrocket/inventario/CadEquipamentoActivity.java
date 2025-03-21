package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cyberrocket.inventario.adapter.ListAdapterEquipamentos;
import com.cyberrocket.inventario.adapter.ListAdapterMudancas;
import com.cyberrocket.inventario.adapter.ListAdapterPlacasRede;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CadEquipamentoActivity extends AppCompatActivity {
    Spinner mSlTipoEquipamento;
    Spinner mSlLocalizacao;
    Spinner mSlEstado;
    List<String> mListTipoEquipamento;
    List<String> mListLocais;
    List<String> mListEstados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cad_equipamento);

        //Inicializações
        mSlTipoEquipamento = findViewById(R.id.SlTipoEquipamentoCadastro);
        mSlLocalizacao = findViewById(R.id.SlLocaisCadastroEquipamento);
        mSlEstado = findViewById(R.id.SlEstadosCadastroEquipamento);
        mListTipoEquipamento = new ArrayList<String>();
        mListLocais = new ArrayList<String>();
        mListEstados = new ArrayList<String>();

        mSlTipoEquipamento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(CadEquipamentoActivity.this, R.color.design_default_color_on_secondary));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Métodos automaticos
        PreencherListaTipoEquipamento();
        PreencherListaLocais();
        PreencherListaEstados();
    }

    //Métodos

    private void PreencherListaTipoEquipamento() {
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetArray("/apirest.php/ComputerType/", new GLPIConnect.VolleyResponseListener() {
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
                        JSONObject tipo = jsonArray.getJSONObject(i);
                        //Preenche a lista de tipos de equipamentos
                        mListTipoEquipamento.add(tipo.getString("name"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(getApplicationContext(),"Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });

        //Asssocia a lista ao spinner
        ArrayAdapter<String> adp2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mListTipoEquipamento);
        adp2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSlTipoEquipamento.setAdapter(adp2);
    }

    private void PreencherListaLocais(){
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetArray("/apirest.php/Location/", new GLPIConnect.VolleyResponseListener() {
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
                        JSONObject local = jsonArray.getJSONObject(i);
                        //Preenche a lista de tipos de equipamentos
                        mListLocais.add(local.getString("name"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(getApplicationContext(),"Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });

        //Asssocia a lista ao spinner
        ArrayAdapter<String> adp2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mListLocais);
        adp2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSlLocalizacao.setAdapter(adp2);
    }

    private void PreencherListaEstados(){
        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.GetArray("/apirest.php/State/", new GLPIConnect.VolleyResponseListener() {
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
                        JSONObject estado = jsonArray.getJSONObject(i);
                        //Preenche a lista de tipos de equipamentos
                        mListEstados.add(estado.getString("name"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(getApplicationContext(),"Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });

        //Asssocia a lista ao spinner
        ArrayAdapter<String> adp2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mListEstados);
        adp2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSlEstado.setAdapter(adp2);
    }
}