package com.cyberrocket.inventario;

import android.os.Bundle;
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
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.google.android.material.textfield.TextInputEditText;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.cyberrocket.inventario.lib.Crud;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class CadMonitorActivity extends AppCompatActivity {

    private AutoCompleteTextView autoModelo;
    private TextInputEditText edtSerie, edtInventario, edtNome;
    private Button btnSalvar;
    private ProgressBar pgb;
    private String computersId;

    private ArrayList<String> modelNames = new ArrayList<>();
    private ArrayList<String> modelIds = new ArrayList<>();
    private int currentScanType = 0; // 1 for Serie, 2 for Inventario

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cad_monitor);

        computersId = getIntent().getStringExtra("computers_id");

        Toolbar toolbar = findViewById(R.id.toolbarCadMonitor);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        autoModelo = findViewById(R.id.AutoModeloMonitor);
        edtSerie = findViewById(R.id.EdtSerieMonitor);
        edtInventario = findViewById(R.id.EdtInventarioMonitor);
        edtNome = findViewById(R.id.EdtNomeMonitor);
        btnSalvar = findViewById(R.id.BtnSalvarMonitor);
        pgb = findViewById(R.id.PgbCadMonitor);

        TextInputLayout tilSerieMonitor = findViewById(R.id.TilSerieMonitor);
        tilSerieMonitor.setEndIconOnClickListener(v -> {
            currentScanType = 1;
            IntentIntegrator integrator = new IntentIntegrator(CadMonitorActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Aponte para o Código de Série");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(false);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        TextInputLayout tilInventarioMonitor = findViewById(R.id.TilInventarioMonitor);
        tilInventarioMonitor.setEndIconOnClickListener(v -> {
            currentScanType = 2;
            IntentIntegrator integrator = new IntentIntegrator(CadMonitorActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Aponte para o Código de Inventário");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(false);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
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
                    String prefixo = crud.SelectItem(CadMonitorActivity.this, "CONFIG", 1, 5);
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

        btnSalvar.setOnClickListener(v -> salvarMonitor());

        loadModels();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedCode = result.getContents();
                if (currentScanType == 1) {
                    edtSerie.setText(scannedCode);
                } else if (currentScanType == 2) {
                    edtInventario.setText(scannedCode);
                }
                currentScanType = 0;
            } else {
                Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadModels() {
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.GetArray("/apirest.php/MonitorModel", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        modelNames.add(obj.getString("name"));
                        modelIds.add(obj.getString("id"));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CadMonitorActivity.this,
                            android.R.layout.simple_dropdown_item_1line, modelNames);
                    autoModelo.setAdapter(adapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                Toast.makeText(CadMonitorActivity.this, "Erro ao carregar modelos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void salvarMonitor() {
        String modeloStr = autoModelo.getText().toString();
        String serie = edtSerie.getText().toString().trim();
        String inventario = edtInventario.getText().toString().trim();
        String nome = edtNome.getText().toString().trim();

        int modelIdx = modelNames.indexOf(modeloStr);
        if (modelIdx == -1) {
            Toast.makeText(this, "Selecione um modelo válido", Toast.LENGTH_SHORT).show();
            return;
        }

        pgb.setVisibility(View.VISIBLE);
        btnSalvar.setEnabled(false);

        GLPIConnect glpi = new GLPIConnect(this);
        try {
            JSONObject input = new JSONObject();
            input.put("name", nome);
            input.put("serial", serie);
            input.put("otherserial", inventario);
            input.put("monitormodels_id", modelIds.get(modelIdx));
            
            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.InsertItem("/apirest.php/Monitor", payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    try {
                        JSONObject resp = new JSONObject(response);
                        String monitorId = resp.getString("id");
                        vincularMonitor(monitorId);
                    } catch (JSONException e) {
                        onError("Erro ao ler resposta do servidor");
                    }
                }

                @Override
                public void onVolleyFailure(String error) {
                    onError("Erro ao cadastrar monitor: " + error);
                }
            });

        } catch (JSONException e) {
            onError("Erro ao processar dados");
        }
    }

    private void vincularMonitor(String monitorId) {
        GLPIConnect glpi = new GLPIConnect(this);
        try {
            JSONObject input = new JSONObject();
            input.put("computers_id", computersId);
            input.put("itemtype", "Monitor");
            input.put("items_id", monitorId);

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.InsertItem("/apirest.php/Computer_Item", payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    Toast.makeText(CadMonitorActivity.this, "Monitor cadastrado e vinculado!", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onVolleyFailure(String error) {
                    onError("Monitor cadastrado, mas erro ao vincular: " + error);
                }
            });
        } catch (JSONException e) {
            onError("Erro ao vincular");
        }
    }

    private void onError(String msg) {
        pgb.setVisibility(View.GONE);
        btnSalvar.setEnabled(true);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
