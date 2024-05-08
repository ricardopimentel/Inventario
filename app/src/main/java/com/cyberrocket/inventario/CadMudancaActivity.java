package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.cyberrocket.inventario.lib.GLPIConnect;

import org.json.JSONException;
import org.json.JSONObject;

public class CadMudancaActivity extends AppCompatActivity {
    EditText mEtTitulo;
    EditText mEtDescricao;
    Button mBt1;
    Button mBt2;
    Button mBt3;
    Button mBtSalvar;
    String mIdEquipamento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cad_manutencao);

        //Inicializações
        mEtTitulo = findViewById(R.id.EtTituloCadManutencao);
        mEtDescricao = findViewById(R.id.EtDescricaoCadmanutencao);
        mBt1 = findViewById(R.id.Bt1Cad);
        mBt2 = findViewById(R.id.Bt2Cad);
        mBt3 = findViewById(R.id.Bt3Cad);
        mBtSalvar = findViewById(R.id.BtSalvarCadManutencao);

        //Métodos automaticos
        getExtras();

        //Listeners
        mBt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetTextoDescricao(mBt1.getText().toString());
            }
        });

        mBt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetTextoDescricao(mBt2.getText().toString());
            }
        });

        mBt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetTextoDescricao(mBt3.getText().toString());
            }
        });

        mBtSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SalvarMudanca();
            }
        });
    }

    //Métodos
    private void SetTextoDescricao(String texto) {
        mEtDescricao.setText(texto);
    }

    private void getExtras() {
        Intent it = getIntent();
        mIdEquipamento = it.getStringExtra("idequipamento");
    }

    private void SalvarMudanca() {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("name", mEtTitulo.getText().toString());
            postparams.put("content", mEtDescricao.getText().toString());
            postparams.put("status", "1");

            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.InsertItem("/apirest.php/Problem/", finalarray, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject = new JSONObject(response);
                    String idmudanca = jsonObject.getString("id");
                    SalvarComputadorMudanca(idmudanca);
                }catch (JSONException err){
                    Log.d("ParseError", err.toString());
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(CadMudancaActivity.this, "Erro: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void SalvarComputadorMudanca(String idmudanca) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("items_id", mIdEquipamento);
            postparams.put("itemtype", "Computer");
            postparams.put("problems_id", idmudanca);

            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.InsertItem("/apirest.php/Problem/"+ idmudanca+"/Item_Problem/", finalarray, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                IrPara(ScannerActivity.class);
            }
            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(CadMudancaActivity.this, "Erro: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void IrPara(Class para) {
        Intent intent = new Intent(CadMudancaActivity.this, para);
        intent.putExtra("id", mIdEquipamento);
        startActivity(intent);
        finish();
    }
}
