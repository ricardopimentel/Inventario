package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AlterarNomeMonitorActivity extends AppCompatActivity {
    ListView mListaLocais;
    ArrayAdapter<String> mAdapter;
    ArrayList<String> mList;
    String mIdMonitor;
    String mIdEquipamento;
    String mNomeEquipamento;
    TextInputEditText mTvNomeEquipamento;
    Button mBtOk;
    Button mBtCancelar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_nome_monitor);

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
            postparams.put("name", mTvNomeEquipamento.getText());

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

    private void Inicializar() {
        mList = new ArrayList<String>();
        mTvNomeEquipamento = findViewById(R.id.TvNomeMonitorAlterarMonitor);
        mBtOk = findViewById(R.id.BtOkAlterarMonitor);
        mBtCancelar = findViewById(R.id.BtCancelarAlterarMonitor);
    }

    private void getExtras(){
        //Pegar id do equipamento vindo da activity anterior
        Intent it = getIntent();
        mIdMonitor = it.getStringExtra("idmonitor");
        mIdEquipamento = it.getStringExtra("idequipamento");
        mNomeEquipamento = it.getStringExtra("nomemonitor");
        mTvNomeEquipamento.setText(mNomeEquipamento);
    }
}