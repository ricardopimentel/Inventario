package com.cyberrocket.inventario;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.textfield.TextInputEditText;

public class VaultSettingsActivity extends AppCompatActivity {

    private TextInputEditText edtClientId, edtClientSecret, edtWorkspaceId, edtEnvironment;
    private Button btnSalvar;

    public static final String PREF_FILE_NAME = "secret_vault_prefs";
    public static final String KEY_CLIENT_ID = "infisical_client_id";
    public static final String KEY_CLIENT_SECRET = "infisical_client_secret";
    public static final String KEY_WORKSPACE_ID = "infisical_workspace_id";
    public static final String KEY_ENVIRONMENT = "infisical_environment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault_settings);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cofre Infisical");
        }

        edtClientId = findViewById(R.id.EdtClientId);
        edtClientSecret = findViewById(R.id.EdtClientSecret);
        edtWorkspaceId = findViewById(R.id.EdtWorkspaceId);
        edtEnvironment = findViewById(R.id.EdtEnvironment);
        btnSalvar = findViewById(R.id.BtnSalvarVault);

        loadPreferences();

        btnSalvar.setOnClickListener(v -> savePreferences());
    }

    private SharedPreferences getEncryptedSharedPreferences() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    private void loadPreferences() {
        try {
            SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
            edtClientId.setText(sharedPreferences.getString(KEY_CLIENT_ID, ""));
            edtClientSecret.setText(sharedPreferences.getString(KEY_CLIENT_SECRET, ""));
            edtWorkspaceId.setText(sharedPreferences.getString(KEY_WORKSPACE_ID, ""));
            edtEnvironment.setText(sharedPreferences.getString(KEY_ENVIRONMENT, "prod"));
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar credenciais seguras.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void savePreferences() {
        String clientId = edtClientId.getText() != null ? edtClientId.getText().toString().trim() : "";
        String clientSecret = edtClientSecret.getText() != null ? edtClientSecret.getText().toString().trim() : "";
        String workspaceId = edtWorkspaceId.getText() != null ? edtWorkspaceId.getText().toString().trim() : "";
        String environment = edtEnvironment.getText() != null ? edtEnvironment.getText().toString().trim() : "prod";

        if (clientId.isEmpty() || clientSecret.isEmpty() || workspaceId.isEmpty() || environment.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_CLIENT_ID, clientId);
            editor.putString(KEY_CLIENT_SECRET, clientSecret);
            editor.putString(KEY_WORKSPACE_ID, workspaceId);
            editor.putString(KEY_ENVIRONMENT, environment);
            editor.apply();

            Toast.makeText(this, "Credenciais do Cofre salvas com segurança!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar credenciais.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
