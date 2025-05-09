package com.cyberrocket.inventario.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.cyberrocket.inventario.CadEquipamentoActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;

public class HomeFragment extends Fragment {


    private HomeViewModel homeViewModel;
    Button mBtGoScanner;
    Button mBtGoCadastro;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        //Inicialização
        mBtGoScanner = root.findViewById(R.id.BtGoScanner);
        mBtGoCadastro = root.findViewById(R.id.BtGoCadastro);

        //Listeners
        mBtGoScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IrPara(ScannerActivity.class);
            }
        });

        mBtGoCadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IrPara(CadEquipamentoActivity.class);
            }
        });

        return root;
    }

    private void IrPara(Class para) {
        Intent intent = new Intent(getContext(), para);
        startActivity(intent);
    }
}
