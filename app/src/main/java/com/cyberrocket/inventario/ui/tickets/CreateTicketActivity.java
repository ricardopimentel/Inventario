package com.cyberrocket.inventario.ui.tickets;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.lib.GLPIConnect;

import org.json.JSONException;
import org.json.JSONObject;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText etTicketTitle;
    private EditText etTicketDescription;
    private Spinner spinnerTicketType;
    private Button btnSubmitTicket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ticket);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Novo Chamado");
        }

        etTicketTitle = findViewById(R.id.etTicketTitle);
        etTicketDescription = findViewById(R.id.etTicketDescription);
        spinnerTicketType = findViewById(R.id.spinnerTicketType);
        btnSubmitTicket = findViewById(R.id.btnSubmitTicket);

        String[] types = {"Incidente", "Requisição"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerTicketType.setAdapter(adapter);

        btnSubmitTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitTicket();
            }
        });
    }

    private void submitTicket() {
        String title = etTicketTitle.getText().toString().trim();
        String description = etTicketDescription.getText().toString().trim();
        int typePosition = spinnerTicketType.getSelectedItemPosition();
        int glpiType = (typePosition == 0) ? 1 : 2; // 1 represents Incident, 2 represents Request

        if (title.isEmpty()) {
            Toast.makeText(this, "Por favor, insira um título.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Por favor, insira uma descrição.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitTicket.setEnabled(false);
        GLPIConnect glpi = new GLPIConnect(this);

        try {
            JSONObject input = new JSONObject();
            input.put("name", title);
            input.put("content", description);
            input.put("type", glpiType);
            
            // Definição do status como Novo (1)
            input.put("status", 1); 

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            String endpoint = "/apirest.php/Ticket";
            
            glpi.InsertItem(endpoint, payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String serverResponse) {
                    Toast.makeText(CreateTicketActivity.this, "Chamado criado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // volta para o fragment list
                }

                @Override
                public void onVolleyFailure(String error) {
                    btnSubmitTicket.setEnabled(true);
                    Toast.makeText(CreateTicketActivity.this, "Erro ao criar chamado. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            btnSubmitTicket.setEnabled(true);
            Toast.makeText(this, "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
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
