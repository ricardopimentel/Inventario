package com.cyberrocket.inventario.ui.perfil;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.cyberrocket.inventario.LoginActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;

public class PerfilFragment extends Fragment {

    private PerfilViewModel perfilViewModel;
    Button mBtSair;
    Crud mCrud;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        perfilViewModel = ViewModelProviders.of(this).get(PerfilViewModel.class);
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);
        final TextView textView = root.findViewById(R.id.text_perfil);
        perfilViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        //Inicializações
        mBtSair = root.findViewById(R.id.BtSair);
        mCrud = new Crud();

        //Listeners
        mBtSair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SairSistema();
            }
        });

        return root;
    }

    //Metodos
    private void SairSistema() {
        GLPIConnect glpi = new GLPIConnect(getContext());
        glpi.LogoffGLPI(new GLPIConnect.VolleyResponseListener() {// não passa mais o sessiontoken no método logoff
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                //Alterar no banco de dados
                ContentValues values = new ContentValues();
                values.put("SESSION_TOKEN", "");
                if(mCrud.UpdateItem(getContext(), "CONFIG", 1, values)){
                }else{
                    Toast.makeText(getContext(), "Erro", Toast.LENGTH_LONG).show();
                }

                IrPara(LoginActivity.class, true);
            }

            @Override
            public void onVolleyFailure(String erro) {
                //Alterar no banco de dados
                ContentValues values = new ContentValues();
                values.put("SESSION_TOKEN", "");
                mCrud.UpdateItem(getContext(), "CONFIG", 1, values);
                IrPara(LoginActivity.class, true);
            }
        });
    }

    private void IrPara(Class para, Boolean matar){
        Intent intent = new Intent(getContext(), para);
        startActivity(intent);
        if(matar){
            ((Activity) getContext()).finish();
        }
    }
}
