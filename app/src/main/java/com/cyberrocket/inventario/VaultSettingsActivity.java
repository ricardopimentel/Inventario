package com.cyberrocket.inventario;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

public class VaultSettingsActivity extends AppCompatActivity {

    private TextInputEditText edtClientId, edtClientSecret, edtWorkspaceId, edtEnvironment;
    private Button btnSalvar, btnScanQR, btnLimpar;

    public static final String PREF_FILE_NAME = "secret_vault_prefs";
    public static final String KEY_CLIENT_ID = "infisical_client_id";
    public static final String KEY_CLIENT_SECRET = "infisical_client_secret";
    public static final String KEY_WORKSPACE_ID = "infisical_workspace_id";
    public static final String KEY_ENVIRONMENT = "infisical_environment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault_settings);

        com.cyberrocket.inventario.lib.StatusBarHelper.setupStatusBar(this, findViewById(R.id.containerVault), findViewById(R.id.statusBarBackground));
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarVault);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Cofre Infisical");
            }
        }

        edtClientId = findViewById(R.id.EdtClientId);
        edtClientSecret = findViewById(R.id.EdtClientSecret);
        edtWorkspaceId = findViewById(R.id.EdtWorkspaceId);
        edtEnvironment = findViewById(R.id.EdtEnvironment);
        btnSalvar = findViewById(R.id.BtnSalvarVault);
        btnScanQR = findViewById(R.id.BtnScanVaultQR);
        btnLimpar = findViewById(R.id.BtnLimparVault);

        loadPreferences();

        btnSalvar.setOnClickListener(v -> savePreferences());
        btnLimpar.setOnClickListener(v -> clearPreferences());
        
        btnScanQR.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt("Aponte para o QR Code de Configuração");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(false);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Escaneamento cancelado", Toast.LENGTH_LONG).show();
            } else {
                try {
                    JSONObject json = new JSONObject(result.getContents());
                    String clientId = json.optString("clientId", "");
                    String clientSecret = json.optString("clientSecret", "");
                    String workspaceId = json.optString("workspaceId", "");
                    String environment = json.optString("environment", "prod");

                    if (clientId.isEmpty() || clientSecret.isEmpty() || workspaceId.isEmpty()) {
                        Toast.makeText(this, "QR Code inválido ou incompleto.", Toast.LENGTH_LONG).show();
                    } else {
                        edtClientId.setText(clientId);
                        edtClientSecret.setText(clientSecret);
                        edtWorkspaceId.setText(workspaceId);
                        edtEnvironment.setText(environment);
                        
                        Toast.makeText(this, "Configurações importadas! Clique em Salvar.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Log.e("VaultQR", "Erro ao processar QR: " + e.getMessage());
                    Toast.makeText(this, "Erro: QR Code não contém um JSON válido.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static SharedPreferences getEncryptedSharedPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e("VaultSettings", "EncryptedSharedPreferences falhou (chave incompativel/restaurada). Resetando arquivo de prefs...", e);
            try {
                context.deleteSharedPreferences(PREF_FILE_NAME);

                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                return EncryptedSharedPreferences.create(
                        context,
                        PREF_FILE_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (Exception e2) {
                Log.e("VaultSettings", "Fallback para SharedPreferences padrao", e2);
                return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            }
        }
    }

    private void loadPreferences() {
        try {
            SharedPreferences sharedPreferences = getEncryptedSharedPreferences(this);
            edtClientId.setText(sharedPreferences.getString(KEY_CLIENT_ID, ""));
            edtClientSecret.setText(sharedPreferences.getString(KEY_CLIENT_SECRET, ""));
            edtWorkspaceId.setText(sharedPreferences.getString(KEY_WORKSPACE_ID, ""));
            edtEnvironment.setText(sharedPreferences.getString(KEY_ENVIRONMENT, "prod"));
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar credenciais seguras.", Toast.LENGTH_SHORT).show();
            Log.e("VaultSettings", "Erro ao carregar credenciais", e);
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
            SharedPreferences sharedPreferences = getEncryptedSharedPreferences(this);
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
            Log.e("VaultSettings", "Erro ao salvar credenciais", e);
        }
    }

    private void clearPreferences() {
        try {
            SharedPreferences sharedPreferences = getEncryptedSharedPreferences(this);
            sharedPreferences.edit().clear().apply();
            deleteSharedPreferences(PREF_FILE_NAME);
            edtClientId.setText("");
            edtClientSecret.setText("");
            edtWorkspaceId.setText("");
            edtEnvironment.setText("prod");
            Toast.makeText(this, "Credenciais do cofre limpas!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao limpar credenciais.", Toast.LENGTH_SHORT).show();
            Log.e("VaultSettings", "Erro ao limpar credenciais", e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
