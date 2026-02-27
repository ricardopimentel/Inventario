package com.cyberrocket.inventario.ui.tickets;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.adapter.ChatAdapter;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.TicketMessage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TicketDetailsActivity extends AppCompatActivity implements ChatAdapter.OnMessageLongClickListener {

    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private ArrayList<TicketMessage> messagesList;
    private String ticketId;

    private EditText etMessageInput;
    private ImageButton btnSendMessage;
    private FloatingActionButton fabEditMetadata;
    
    // holds the ITILFollowup id if we are editing an existing message. null if it's a new message
    private String editingFollowupId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalhes do Chamado");
        }

        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        fabEditMetadata = findViewById(R.id.fabEditMetadata);

        rvChatMessages = findViewById(R.id.rvChatMessages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        
        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messagesList, this);
        rvChatMessages.setAdapter(chatAdapter);

        btnSendMessage.setOnClickListener(v -> handleSendClick());
        fabEditMetadata.setOnClickListener(v -> loadLocationsAndShowMetadataDialog());

        populateInitialTicketData();
    }

    private void populateInitialTicketData() {
        if (getIntent() != null) {
            ticketId = getIntent().getStringExtra("id");
            String title = getIntent().getStringExtra("title");
            String description = getIntent().getStringExtra("description");
            String creationDate = getIntent().getStringExtra("creationDate");
            String requester = getIntent().getStringExtra("requester");

            // Format title to be bold for the first message
            String content = "<b>" + (title != null ? title : "Sem Título") + "</b><br><br>";
            content += (description != null && !description.isEmpty()) ? description : "Sem descrição fornecida.";

            TicketMessage mainTicket = new TicketMessage(
                    ticketId,
                    TicketMessage.TYPE_TICKET,
                    requester != null ? requester : "Desconhecido",
                    creationDate != null ? creationDate : "N/D",
                    content
            );
            
            messagesList.add(mainTicket);
            chatAdapter.notifyDataSetChanged();
            
            // Load followups from GLPI
            if (ticketId != null && !ticketId.isEmpty()) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Chamado #" + ticketId);
                }
                loadFollowups();
            }
        }
    }

    private void loadFollowups() {
        GLPIConnect glpi = new GLPIConnect(this);
        // Endpoint to grab ITILFollowup specific to this ticket
        String endpoint = "/apirest.php/Ticket/" + ticketId + "/ITILFollowup?expand_dropdowns=true&sort=date&order=ASC";
        
        glpi.GetArray(endpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (isFinishing() || isDestroyed()) return;
                
                try {
                    JSONArray jsonArray = new JSONArray(serverResponse);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        
                        String followupId = obj.optString("id");
                        String content = obj.optString("content");
                        String date = obj.optString("date");
                        String author = obj.optString("users_id"); // expanded dropdown will return string name

                        TicketMessage followup = new TicketMessage(
                                followupId,
                                TicketMessage.TYPE_REPLY,
                                author,
                                date,
                                content
                        );
                        messagesList.add(followup);
                    }
                    
                    chatAdapter.notifyDataSetChanged();
                    // Scroll to bottom if there are many messages
                    if (messagesList.size() > 0) {
                        rvChatMessages.scrollToPosition(messagesList.size() - 1);
                    }
                    
                } catch (JSONException e) {
                    Log.e("TicketDetailsActivity", "List ITILFollowup parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(TicketDetailsActivity.this, "Erro ao carregar acompanhamentos do chamado", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleSendClick() {
        String msgContentHtml;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            msgContentHtml = android.text.Html.toHtml(etMessageInput.getText(), android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        } else {
            msgContentHtml = android.text.Html.toHtml(etMessageInput.getText());
        }

        if (etMessageInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Digite uma mensagem primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendMessage.setEnabled(false);
        GLPIConnect glpi = new GLPIConnect(this);

        try {
            if (editingFollowupId != null) {
                // UPDATE existing message
                JSONObject input = new JSONObject();
                input.put("id", editingFollowupId);
                input.put("content", msgContentHtml);

                JSONObject payload = new JSONObject();
                payload.put("input", input);

                String endpoint = "/apirest.php/ITILFollowup/" + editingFollowupId;
                glpi.UpdateItem(endpoint, payload, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                    @Override
                    public void onVolleySuccess(String url, String serverResponse) {
                        btnSendMessage.setEnabled(true);
                        etMessageInput.setText("");
                        editingFollowupId = null; // reset to new message mode
                        Toast.makeText(TicketDetailsActivity.this, "Comentário atualizado", Toast.LENGTH_SHORT).show();
                        
                        // reload messages
                        messagesList.clear();
                        populateInitialTicketData();
                    }

                    @Override
                    public void onVolleyFailure(String error) {
                        btnSendMessage.setEnabled(true);
                        Toast.makeText(TicketDetailsActivity.this, "Falha ao atualizar", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                // INSERT new message
                JSONObject input = new JSONObject();
                input.put("itemtype", "Ticket");
                input.put("items_id", ticketId);
                input.put("content", msgContentHtml);

                JSONObject payload = new JSONObject();
                payload.put("input", input);

                String endpoint = "/apirest.php/Ticket/" + ticketId + "/ITILFollowup";
                glpi.InsertItem(endpoint, payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                    @Override
                    public void onVolleySuccess(String url, String serverResponse) {
                        btnSendMessage.setEnabled(true);
                        etMessageInput.setText("");
                        
                        // reload messages
                        messagesList.clear();
                        populateInitialTicketData();
                    }

                    @Override
                    public void onVolleyFailure(String error) {
                        btnSendMessage.setEnabled(true);
                        Toast.makeText(TicketDetailsActivity.this, "Falha ao enviar comentário", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            btnSendMessage.setEnabled(true);
        }
    }

    @Override
    public void onMessageLongClick(TicketMessage message) {
        if (message.getMessageType() == TicketMessage.TYPE_TICKET && message.getId() != null) {
            CharSequence[] options = new CharSequence[]{"Editar Chamado"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Opções do Chamado");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showEditTicketDialog();
                }
            });
            builder.show();
        } else if (message.getMessageType() == TicketMessage.TYPE_REPLY && message.getId() != null) {
            CharSequence[] options = new CharSequence[]{"Editar", "Excluir"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Opções");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Start editing
                    editingFollowupId = message.getId();
                    String rawHtml = message.getContent();
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        etMessageInput.setText(android.text.Html.fromHtml(rawHtml, android.text.Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        etMessageInput.setText(android.text.Html.fromHtml(rawHtml));
                    }
                    
                    etMessageInput.setSelection(etMessageInput.getText().length());
                    etMessageInput.requestFocus();
                } else if (which == 1) {
                    // Confirm deletion
                    new AlertDialog.Builder(this)
                            .setTitle("Confirmar Exclusão")
                            .setMessage("Tem certeza que deseja apagar este comentário?")
                            .setPositiveButton("Sim", (d, w) -> deleteFollowup(message.getId()))
                            .setNegativeButton("Não", null)
                            .show();
                }
            });
            builder.show();
        }
    }

    private void showEditTicketDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Chamado");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText titleBox = new EditText(this);
        titleBox.setHint("Título do Chamado");
        String currentTitle = getIntent().getStringExtra("title");
        titleBox.setText(currentTitle != null ? currentTitle : "");
        layout.addView(titleBox);

        final EditText descBox = new EditText(this);
        descBox.setHint("Descrição (Opcional)");
        String currentDesc = getIntent().getStringExtra("description");
        if (currentDesc != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                descBox.setText(android.text.Html.fromHtml(currentDesc, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                descBox.setText(android.text.Html.fromHtml(currentDesc));
            }
        } else {
            descBox.setText("");
        }
        layout.addView(descBox);

        builder.setView(layout);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String newTitle = titleBox.getText().toString().trim();
            
            String newDescHtml;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                newDescHtml = android.text.Html.toHtml(descBox.getText(), android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
            } else {
                newDescHtml = android.text.Html.toHtml(descBox.getText());
            }

            if (newTitle.isEmpty()) {
                Toast.makeText(TicketDetailsActivity.this, "O título não pode estar vazio", Toast.LENGTH_SHORT).show();
                return;
            }
            updateTicket(newTitle, newDescHtml);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateTicket(String newTitle, String newDesc) {
        GLPIConnect glpi = new GLPIConnect(this);
        String endpoint = "/apirest.php/Ticket/" + ticketId;

        try {
            JSONObject input = new JSONObject();
            input.put("id", ticketId);
            input.put("name", newTitle);
            input.put("content", newDesc);

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.UpdateItem(endpoint, payload, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String serverResponse) {
                    Toast.makeText(TicketDetailsActivity.this, "Chamado atualizado com sucesso", Toast.LENGTH_SHORT).show();
                    // update intent data so reload preserves changes
                    getIntent().putExtra("title", newTitle);
                    getIntent().putExtra("description", newDesc);

                    messagesList.clear();
                    populateInitialTicketData();
                }

                @Override
                public void onVolleyFailure(String error) {
                    Toast.makeText(TicketDetailsActivity.this, "Erro ao atualizar chamado", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // --- Metadata Editing Methods ---

    private void loadLocationsAndShowMetadataDialog() {
        GLPIConnect glpi = new GLPIConnect(this);
        // GET locations
        glpi.GetArray("/apirest.php/Location?range=0-200", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                ArrayList<String> locationNames = new ArrayList<>();
                ArrayList<Integer> locationIds = new ArrayList<>();
                
                // Add a blank option
                locationNames.add("Nenhuma");
                locationIds.add(0);

                try {
                    JSONArray array = new JSONArray(serverResponse);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        locationIds.add(obj.optInt("id"));
                        locationNames.add(obj.optString("completename", obj.optString("name")));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Now fetch current ticket to know current locations_id
                glpi.GetArray("/apirest.php/Ticket/" + ticketId, new GLPIConnect.VolleyResponseListener() {
                    @Override
                    public void onVolleySuccess(String url, String ticketResponse) {
                        int currentStatus = 1;
                        int currentLocationId = 0;
                        try {
                            // Single ticket get returns object, not array, usually. Let's handle both.
                            JSONObject ticketObj;
                            if (ticketResponse.startsWith("[")) {
                                ticketObj = new JSONArray(ticketResponse).getJSONObject(0);
                            } else {
                                ticketObj = new JSONObject(ticketResponse);
                            }
                            
                            currentStatus = ticketObj.optInt("status", 1);
                            currentLocationId = ticketObj.optInt("locations_id", 0);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        showMetadataDialog(locationNames, locationIds, currentStatus, currentLocationId);
                    }

                    @Override
                    public void onVolleyFailure(String error) {
                        showMetadataDialog(locationNames, locationIds, 1, 0); // Fallback
                    }
                });
            }

            @Override
            public void onVolleyFailure(String error) {
                Toast.makeText(TicketDetailsActivity.this, "Erro ao carregar localizações", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMetadataDialog(ArrayList<String> locationNames, ArrayList<Integer> locationIds, int currentStatus, int currentLocationId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Informações do Chamado");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Status Spinner
        TextView tvStatusLabel = new TextView(this);
        tvStatusLabel.setText("Status:");
        layout.addView(tvStatusLabel);

        Spinner statusSpinner = new Spinner(this);
        String[] statusArray = {"Novo", "Atribuído", "Planejado", "Pendente", "Resolvido", "Fechado"};
        int[] statusValues = {1, 2, 3, 4, 5, 6};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusArray);
        statusSpinner.setAdapter(statusAdapter);
        
        // Find current status index
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i] == currentStatus) {
                statusSpinner.setSelection(i);
                break;
            }
        }
        layout.addView(statusSpinner);

        // Location Spinner
        TextView tvLocLabel = new TextView(this);
        tvLocLabel.setText("Localização:");
        tvLocLabel.setPadding(0, 30, 0, 0);
        layout.addView(tvLocLabel);

        Spinner locSpinner = new Spinner(this);
        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, locationNames);
        locSpinner.setAdapter(locAdapter);

        // Find current location index
        for (int i = 0; i < locationIds.size(); i++) {
            if (locationIds.get(i) == currentLocationId) {
                locSpinner.setSelection(i);
                break;
            }
        }
        layout.addView(locSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            int selectedStatus = statusValues[statusSpinner.getSelectedItemPosition()];
            int selectedLocId = locationIds.get(locSpinner.getSelectedItemPosition());

            updateTicketMetadata(selectedStatus, selectedLocId);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void updateTicketMetadata(int newStatus, int newLocId) {
        GLPIConnect glpi = new GLPIConnect(this);
        String endpoint = "/apirest.php/Ticket/" + ticketId;

        try {
            JSONObject input = new JSONObject();
            input.put("id", ticketId);
            input.put("status", newStatus);
            input.put("locations_id", newLocId);

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.UpdateItem(endpoint, payload, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String serverResponse) {
                    Toast.makeText(TicketDetailsActivity.this, "Informações atualizadas!", Toast.LENGTH_SHORT).show();
                    // update intent data for status if necessary
                    getIntent().putExtra("status", String.valueOf(newStatus));
                }

                @Override
                public void onVolleyFailure(String error) {
                    Toast.makeText(TicketDetailsActivity.this, "Erro ao atualizar", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteFollowup(String followupId) {
        GLPIConnect glpi = new GLPIConnect(this);
        String endpoint = "/apirest.php/ITILFollowup/" + followupId;
        
        glpi.DeleteItem(endpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                Toast.makeText(TicketDetailsActivity.this, "Comentário excluído", Toast.LENGTH_SHORT).show();
                // reload messages
                messagesList.clear();
                populateInitialTicketData();
            }

            @Override
            public void onVolleyFailure(String error) {
                Toast.makeText(TicketDetailsActivity.this, "Falha ao excluir comentário", Toast.LENGTH_SHORT).show();
            }
        });
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
