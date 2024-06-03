package com.cyberrocket.inventario;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cyberrocket.inventario.lib.CriarBanco;
import com.cyberrocket.inventario.lib.Crud;

public class ConfigActivity extends AppCompatActivity {
    EditText mCpURL;
    Button mBtSalvar;
    Boolean salvar;
    Crud mCrud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        //Instanciar elementos
        mCpURL = findViewById(R.id.CpURL);
        mBtSalvar = findViewById(R.id.BtSalvar);
        salvar = true;
        mCrud = new Crud();

        //Métodos Automaticos
        GetURL();


        //Listeners
        mBtSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(salvar){
                    ContentValues values = new ContentValues();
                    values.put("URL", mCpURL.getText().toString());
                    if(mCrud.InsertItem(getApplication(), "CONFIG", values)){ //Verifica se salvou
                        Toast.makeText(ConfigActivity.this, "Sucesso", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ConfigActivity.this, "Erro", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    // Editar Configuração da url
                    ContentValues values = new ContentValues();
                    values.put("URL", mCpURL.getText().toString());
                    if(mCrud.UpdateItem(getApplication(), "CONFIG", 1, values)){ //verifica se alterou
                        Toast.makeText(ConfigActivity.this, "Sucesso", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ConfigActivity.this, "Erro", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    //Métodos

    private void GetURL() {
        mCpURL.setText(mCrud.SelectItem(getApplication(), "CONFIG", 1, 1));
        if(mCpURL.getText().toString().length() > 0){
            salvar = false;
        }else{
            salvar = true;
        }
    }

}
