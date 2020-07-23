package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.cyberrocket.inventario.lib.GLPIConnect;
import org.json.JSONException;
import org.json.JSONObject;

public class FinalizarMudancaActivity extends AppCompatActivity {
    Button mBt1;
    Button mBt2;
    Button mBt3;
    Button mBtFinalizarMudanca;
    String mIdEquipamento;
    EditText mEtDescricaoFinalizarMudanca;
    TextView mTvIdMudanca;
    String mProblema;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalizar_mudanca);

        //Inicializações
        mBt1 = findViewById(R.id.Bt1);
        mBt2 = findViewById(R.id.Bt2);
        mBt3 = findViewById(R.id.Bt3);
        mBtFinalizarMudanca = findViewById(R.id.BtFinalizarMudanca);
        mEtDescricaoFinalizarMudanca = findViewById(R.id.EtDescricaoFinMudanca);
        mTvIdMudanca = findViewById(R.id.TvIdMudancaFinalizar);

        // Métodos automáticos
        getExtras();

        //Listeners
        mBt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreencherDescricao(mBt1.getText().toString());
            }
        });

        mBt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreencherDescricao(mBt2.getText().toString());
            }
        });

        mBt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreencherDescricao(mBt3.getText().toString());
            }
        });

        mBtFinalizarMudanca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FinalizarMudanca();
            }
        });

    }

    //Metodos

    private void getExtras() {
        Intent it = getIntent();
        mTvIdMudanca.setText(it.getStringExtra("idmudanca"));
        mIdEquipamento = it.getStringExtra("idequipamento");
        mEtDescricaoFinalizarMudanca.setText("Problema: "+ it.getStringExtra("descricaomudanca"));
        mProblema = mEtDescricaoFinalizarMudanca.getText().toString();
    }

    private void PreencherDescricao(String text) {
        mEtDescricaoFinalizarMudanca.setText("Solução: "+ text+ "\n"+ mProblema);
    }

    private void FinalizarMudanca() {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        try {
            postparams.put("id", mTvIdMudanca.getText().toString());
            postparams.put("content", mEtDescricaoFinalizarMudanca.getText().toString());
            postparams.put("status", "6");

            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(getApplicationContext());
        con.UpdateItem("/apirest.php/Change/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                IrPara(ScannerActivity.class);
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(FinalizarMudancaActivity.this, "Erro: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void IrPara(Class para) {
        Intent intent = new Intent(FinalizarMudancaActivity.this, para);
        intent.putExtra("id", mIdEquipamento);
        startActivity(intent);
        finish();
    }
}
