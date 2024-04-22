package com.cyberrocket.inventario;

import android.content.Intent;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cyberrocket.inventario.lib.DatePickerFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class EmprestimoActivity extends AppCompatActivity {
    TextView mTvPrevisaoDevolucao;
    TextView mTvIdEquipamento;
    TextView mTvIdPessoa;
    ImageButton mBtnCalendario;
    Button mBtnRegistrar;
    private RequestQueue mQueue;
    Boolean sucesso = false;
    ArrayList equipamento = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emprestimo);
        mTvPrevisaoDevolucao = findViewById(R.id.tvPrevDevolucao);
        mTvIdEquipamento = findViewById(R.id.tvIdEquipamento);
        mTvIdPessoa = findViewById(R.id.tvIdPessoa);
        mBtnCalendario = findViewById(R.id.btnCalendario);
        mBtnRegistrar = findViewById(R.id.btnRegistrar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bundle bundle = getIntent().getExtras();
        equipamento = bundle.getStringArrayList("DadosEquipamento");
        if (equipamento.size() > 0) {
            mTvIdEquipamento.setText(equipamento.get(0).toString());
        }

        mBtnCalendario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(view);
            }
        });

        mBtnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTvPrevisaoDevolucao.getText().toString().equals("")){
                    Toast.makeText(EmprestimoActivity.this, "Informe a previsão de devolução", Toast.LENGTH_LONG).show();
                }else{
                    String url = "http://10.9.10.165:8000/api/list-emprestimos";
                    JSONObject postparams = new JSONObject();
                    try {
                        if(equipamento.size() == 5) {
                            postparams.put("datahoraemprestimo", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
                            postparams.put("datahoradevolucao", "1940-01-01T00:00:00");
                            postparams.put("previsaodevolucao", mTvPrevisaoDevolucao.getText().toString());
                            postparams.put("equipamento", mTvIdEquipamento.getText().toString());
                            postparams.put("pessoa", mTvIdPessoa.getText().toString());
                            postData(url, postparams, Request.Method.POST);
                            url = "http://10.9.10.165:8000/api/equipamento/" + equipamento.get(0).toString();
                            postparams = new JSONObject();
                            postparams.put("id", equipamento.get(0).toString());
                            postparams.put("descricao", equipamento.get(1).toString());
                            postparams.put("nserie", equipamento.get(2).toString());
                            postparams.put("modelo", equipamento.get(3).toString());
                            postparams.put("tipo", equipamento.get(4).toString());
                            postparams.put("status", "e");
                            postData(url, postparams, Request.Method.PUT);
                            Intent i = new Intent(EmprestimoActivity.this, ScanActivity.class);
                            startActivity(i);
                        }else{
                            Toast.makeText(EmprestimoActivity.this, "Algum erro", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment(mTvPrevisaoDevolucao);
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void postData(String url, JSONObject postparams, int method){
        mQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(method,
                url, postparams, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(EmprestimoActivity.this, "Sucesso!", Toast.LENGTH_LONG).show();
                        sucesso = true;
                    }
                }
                ,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(EmprestimoActivity.this, "Erro:\n" + error, Toast.LENGTH_LONG).show();
                        sucesso = false;
                    }
                });
        // Adding the request to the queue along with a unique string tag
        mQueue.add(jsonObjReq);
    }
}