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
        Button mBtVaultSettings = root.findViewById(R.id.BtVaultSettings);
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroupTheme = root.findViewById(R.id.toggleGroupTheme);
        mCrud = new Crud();

        // Configura estado inicial do tema
        int currentTheme = com.cyberrocket.inventario.lib.ThemeUtils.getSelectedTheme(getContext());
        if (currentTheme == com.cyberrocket.inventario.lib.ThemeUtils.THEME_LIGHT) {
            toggleGroupTheme.check(R.id.btnThemeLight);
        } else if (currentTheme == com.cyberrocket.inventario.lib.ThemeUtils.THEME_DARK) {
            toggleGroupTheme.check(R.id.btnThemeDark);
        } else {
            toggleGroupTheme.check(R.id.btnThemeSystem);
        }

        //Listeners
        toggleGroupTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int selectedTheme;
                if (checkedId == R.id.btnThemeLight) {
                    selectedTheme = com.cyberrocket.inventario.lib.ThemeUtils.THEME_LIGHT;
                } else if (checkedId == R.id.btnThemeDark) {
                    selectedTheme = com.cyberrocket.inventario.lib.ThemeUtils.THEME_DARK;
                } else {
                    selectedTheme = com.cyberrocket.inventario.lib.ThemeUtils.THEME_SYSTEM;
                }
                
                if (selectedTheme != com.cyberrocket.inventario.lib.ThemeUtils.getSelectedTheme(getContext())) {
                    com.cyberrocket.inventario.lib.ThemeUtils.saveAndApplyTheme(getContext(), selectedTheme);
                }
            }
        });

        mBtSair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SairSistema();
            }
        });

        mBtVaultSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrPara(com.cyberrocket.inventario.VaultSettingsActivity.class, false);
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
