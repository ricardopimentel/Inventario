package com.cyberrocket.inventario;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.cyberrocket.inventario.lib.Crud;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CadEquipamentoActivity extends AppCompatActivity {

    private AutoCompleteTextView autoTipo, autoMarca, autoModelo, autoEstado, autoLocal;
    private TextInputEditText edtSerie, edtInventario, edtNome;
    private Button btnSalvar;
    private ProgressBar pgb;
    private MaterialSwitch switchManterDados;

    private ArrayList<String> typeNames = new ArrayList<>();   
    private ArrayList<String> typeIds = new ArrayList<>();
    
    private ArrayList<String> brandNames = new ArrayList<>();  
    private ArrayList<String> brandIds = new ArrayList<>();
    
    private ArrayList<String> modelNames = new ArrayList<>();  
    private ArrayList<String> modelIds = new ArrayList<>();
    
    private ArrayList<String> stateNames = new ArrayList<>();  
    private ArrayList<String> stateIds = new ArrayList<>();
    
    private ArrayList<String> locationNames = new ArrayList<>(); 
    private ArrayList<String> locationIds = new ArrayList<>();

    private int currentScanType = 0; // 1 = Serie, 2 = Inventario

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cad_equipamento);

        Toolbar toolbar = findViewById(R.id.toolbarCadComputador);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        autoTipo = findViewById(R.id.AutoTipoComputador);
        autoMarca = findViewById(R.id.AutoMarcaComputador);
        autoModelo = findViewById(R.id.AutoModeloComputador);
        autoEstado = findViewById(R.id.AutoEstadoComputador);
        autoLocal = findViewById(R.id.AutoLocalComputador);

        edtSerie = findViewById(R.id.EdtSerieComputador);
        edtInventario = findViewById(R.id.EdtInventarioComputador);
        edtNome = findViewById(R.id.EdtNomeComputador);

        btnSalvar = findViewById(R.id.BtnSalvarComputador);
        pgb = findViewById(R.id.PgbCadComputador);
        switchManterDados = findViewById(R.id.SwitchManterDados);

        SharedPreferences prefs = getSharedPreferences("InventarioPrefs", MODE_PRIVATE);
        boolean manterDados = prefs.getBoolean("manter_dados_cadastro", false);
        switchManterDados.setChecked(manterDados);

        switchManterDados.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("InventarioPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("manter_dados_cadastro", isChecked)
                    .apply();
        });

        TextInputLayout tilSerie = findViewById(R.id.TilSerieComputador);
        tilSerie.setEndIconOnClickListener(v -> {
            currentScanType = 1;
            startScanner("Aponte para o Código de Série");
        });

        TextInputLayout tilInventario = findViewById(R.id.TilInventarioComputador);
        tilInventario.setEndIconOnClickListener(v -> {
            currentScanType = 2;
            startScanner("Aponte para o Código de Inventário");
        });

        edtInventario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String inv = s.toString().trim();
                if (!inv.isEmpty()) {
                    Crud crud = new Crud();
                    String prefixo = crud.SelectItem(CadEquipamentoActivity.this, "CONFIG", 1, 5);
                    if (prefixo == null) prefixo = "";
                    if (prefixo.endsWith("-")) {
                        edtNome.setText(prefixo + inv);
                    } else if (prefixo.isEmpty()) {
                        edtNome.setText(inv);
                    } else {
                        edtNome.setText(prefixo + "-" + inv);
                    }
                } else {
                    edtNome.setText("");
                }
            }
        });

        btnSalvar.setOnClickListener(v -> salvarComputador());

        loadAllDropdowns();
    }

    private void startScanner(String prompt) {
        IntentIntegrator integrator = new IntentIntegrator(CadEquipamentoActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt(prompt);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedCode = result.getContents();
                if (currentScanType == 1) edtSerie.setText(scannedCode);
                else if (currentScanType == 2) edtInventario.setText(scannedCode);
                currentScanType = 0;
            } else {
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadAllDropdowns() {
        GLPIConnect glpi = new GLPIConnect(this);
        
        // Load Types
        glpi.GetArray("/apirest.php/ComputerType", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                parseDropdownData(response, typeNames, typeIds, autoTipo);
            }
            @Override public void onVolleyFailure(String error) {}
        });
        
        // Load Manufacturers (Brands)
        glpi.GetArray("/apirest.php/Manufacturer?range=0-500", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                parseDropdownData(response, brandNames, brandIds, autoMarca);
            }
            @Override public void onVolleyFailure(String error) {}
        });

        // Load Models
        glpi.GetArray("/apirest.php/ComputerModel?range=0-500", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                parseDropdownData(response, modelNames, modelIds, autoModelo);
            }
            @Override public void onVolleyFailure(String error) {}
        });

        // Load States
        glpi.GetArray("/apirest.php/State", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                parseDropdownData(response, stateNames, stateIds, autoEstado);
                // Pre-select "Produção" if exists
                for (int i = 0; i < stateNames.size(); i++) {
                    if (stateNames.get(i).toLowerCase().contains("produção")) {
                        autoEstado.setText(stateNames.get(i), false);
                        break;
                    }
                }
            }
            @Override public void onVolleyFailure(String error) {}
        });

        // Load Locations
        glpi.GetArray("/apirest.php/Location?range=0-500", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                parseDropdownData(response, locationNames, locationIds, autoLocal);
            }
            @Override public void onVolleyFailure(String error) {}
        });
    }

    private void parseDropdownData(String response, ArrayList<String> names, ArrayList<String> ids, AutoCompleteTextView autoComplete) {
        try {
            JSONArray array = new JSONArray(response);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                names.add(obj.getString("name"));
                ids.add(obj.getString("id"));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(CadEquipamentoActivity.this,
                    android.R.layout.simple_dropdown_item_1line, names);
            autoComplete.setAdapter(adapter);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void salvarComputador() {
        String tipoStr = autoTipo.getText().toString();
        String marcaStr = autoMarca.getText().toString();
        String modeloStr = autoModelo.getText().toString();
        String estadoStr = autoEstado.getText().toString();
        String localStr = autoLocal.getText().toString();

        String serie = edtSerie.getText().toString().trim();
        String inventario = edtInventario.getText().toString().trim();
        String nome = edtNome.getText().toString().trim();

        int tipoIdx = typeNames.indexOf(tipoStr);
        int marcaIdx = brandNames.indexOf(marcaStr);
        int modeloIdx = modelNames.indexOf(modeloStr);
        int estadoIdx = stateNames.indexOf(estadoStr);
        int localIdx = locationNames.indexOf(localStr);

        pgb.setVisibility(View.VISIBLE);
        btnSalvar.setEnabled(false);

        GLPIConnect glpi = new GLPIConnect(this);
        try {
            JSONObject input = new JSONObject();
            input.put("name", nome);
            input.put("serial", serie);
            input.put("otherserial", inventario);

            if (tipoIdx != -1) input.put("computertypes_id", typeIds.get(tipoIdx));
            if (marcaIdx != -1) input.put("manufacturers_id", brandIds.get(marcaIdx));
            if (modeloIdx != -1) input.put("computermodels_id", modelIds.get(modeloIdx));
            if (estadoIdx != -1) input.put("states_id", stateIds.get(estadoIdx));
            if (localIdx != -1) input.put("locations_id", locationIds.get(localIdx));

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.InsertItem("/apirest.php/Computer", payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    pgb.setVisibility(View.GONE);
                    if (switchManterDados != null && switchManterDados.isChecked()) {
                        Toast.makeText(CadEquipamentoActivity.this, "Computador cadastrado! Insira o próximo.", Toast.LENGTH_SHORT).show();
                        edtSerie.setText("");
                        edtInventario.setText("");
                        edtNome.setText("");
                        btnSalvar.setEnabled(true);
                        edtSerie.requestFocus();
                    } else {
                        Toast.makeText(CadEquipamentoActivity.this, "Computador cadastrado com sucesso!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onVolleyFailure(String error) {
                    pgb.setVisibility(View.GONE);
                    btnSalvar.setEnabled(true);
                    Toast.makeText(CadEquipamentoActivity.this, "Erro ao cadastrar computador: " + error, Toast.LENGTH_LONG).show();
                }
            });

        } catch (JSONException e) {
            pgb.setVisibility(View.GONE);
            btnSalvar.setEnabled(true);
            Toast.makeText(this, "Erro ao processar dados", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}