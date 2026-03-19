package com.cyberrocket.inventario.ui.perfil;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cyberrocket.inventario.lib.Crud;

public class PerfilViewModel extends AndroidViewModel {

    private MutableLiveData<String> mText;
    private Crud mCrud;

    public PerfilViewModel(@NonNull Application application) {
        super(application);
        mText = new MutableLiveData<>();
        mCrud = new Crud();
        loadUserName();
    }

    private void loadUserName() {
        // USUARIO está na coluna 3 da tabela CONFIG
        String name = mCrud.SelectItem(getApplication(), "CONFIG", 1, 3);
        if (name == null || name.isEmpty()) {
            name = "Usuário";
        }
        mText.setValue(name);
    }

    public LiveData<String> getText() {
        return mText;
    }
}