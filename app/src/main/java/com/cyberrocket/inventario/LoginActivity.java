package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    EditText mCpUsuario;
    EditText mCpSenha;
    TextView mTvToken;
    Button mBtLogin;
    FloatingActionButton mBtnConfig;
    Crud mCrud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Instanciar elements
        mCpUsuario = findViewById(R.id.CpUsuario);
        mCpSenha = findViewById(R.id.CpSenha);
        mBtLogin = findViewById(R.id.BtLogin);
        mBtnConfig = findViewById(R.id.BtConfig);
        mTvToken = findViewById(R.id.TvToken);
        mCrud = new Crud();

        //Métodos Automaticos
        GetToken();

        //Listeners
        mBtLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logar();
            }
        });

        mBtnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrPara(ConfigActivity.class, false);
            }
        });
    }

    //Métodos
    private void Logar() {
        String usuario = mCpUsuario.getText().toString();
        String senha = mCpSenha.getText().toString();

        // verifica se o formulario foi preenchido
        if (usuario.equals("") || senha.equals("")) {
            Toast.makeText(LoginActivity.this, "Preencha usuário e senha", Toast.LENGTH_LONG).show();
        }else{
            GLPIConnect glpi = new GLPIConnect(getApplicationContext());
            glpi.LoginGLPI(usuario, senha, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    String token = "";
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = new JSONObject(response);
                    }catch (JSONException err){
                        Log.d("ParseError", err.toString());
                    }

                    try {
                        token = jsonObject.getString("session_token");
                        Log.d("SessionToken", token);

                        //Alterar no banco de dados
                        if(SalvarToken(token)){
                            IrPara(HomeActivity.class, true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onVolleyFailure(String erro) {
                    Toast.makeText(getApplicationContext(), erro, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private Boolean SalvarToken(String token) {
        ContentValues values = new ContentValues();
        values.put("SESSION_TOKEN", token);
        Crud crud = new Crud();
        if(crud.UpdateItem(getApplicationContext(), "CONFIG", 1, values)){
            Toast.makeText(getApplicationContext(), "Sucesso", Toast.LENGTH_LONG).show();
            return true;
        }else{
            Toast.makeText(getApplicationContext(), "Erro", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void IrPara(Class para, Boolean matar) {
        Intent intent = new Intent(this, para);
        startActivity(intent);
        if(matar){
            finish();
        }
    }

    private void GetToken() {
        mTvToken.setText(mCrud.SelectItem(getApplication(), "CONFIG", 1, 2));
        //Excluir depois
        Log.println(Log.ASSERT,"SessionToken", mTvToken.getText().toString());

        if(!mTvToken.getText().toString().equals("")){
            IrPara(HomeActivity.class, true);
        }
    }
}
