package com.cyberrocket.inventario.ui.tickets;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
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

        rvChatMessages = findViewById(R.id.rvChatMessages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        
        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messagesList, this);
        rvChatMessages.setAdapter(chatAdapter);

        btnSendMessage.setOnClickListener(v -> handleSendClick());

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
                    null,
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
        String msgContent = etMessageInput.getText().toString().trim();
        if (msgContent.isEmpty()) {
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
                input.put("content", msgContent);

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
                input.put("content", msgContent);

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
        if (message.getMessageType() == TicketMessage.TYPE_REPLY && message.getId() != null) {
            CharSequence[] options = new CharSequence[]{"Editar", "Excluir"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Opções");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Start editing
                    editingFollowupId = message.getId();
                    String rawHtml = message.getContent();
                    // Just removing basic html tags to edit (simplistic approach for now)
                    String plainText = android.text.Html.fromHtml(android.text.Html.fromHtml(rawHtml).toString()).toString();
                    etMessageInput.setText(plainText);
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
