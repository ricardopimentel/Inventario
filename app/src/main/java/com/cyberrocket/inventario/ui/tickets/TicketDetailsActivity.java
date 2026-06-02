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
import android.net.Uri;
import android.content.Intent;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

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

    private ImageButton btnAttachImage;
    private LinearLayout layoutImagePreviews;
    private HorizontalScrollView hsvImagePreviews;
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private static final int PICK_IMAGES_REQUEST = 1002;

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
        btnAttachImage = findViewById(R.id.btnAttachImage);
        layoutImagePreviews = findViewById(R.id.layoutImagePreviews);
        hsvImagePreviews = findViewById(R.id.hsvImagePreviews);

        btnAttachImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Selecione as Imagens"), PICK_IMAGES_REQUEST);
            }
        });

        rvChatMessages = findViewById(R.id.rvChatMessages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        
        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messagesList, this);
        rvChatMessages.setAdapter(chatAdapter);

        btnSendMessage.setOnClickListener(v -> handleSendClick());
        fabEditMetadata.setOnClickListener(v -> loadLocationsAndShowMetadataDialog());

        populateInitialTicketData();
    }

    private void parseImages(TicketMessage message) {
        String content = message.getContent();
        if (content == null) return;
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>").matcher(content);
        java.util.List<String> imageUrls = new java.util.ArrayList<>();
        while(m.find()) {
            String src = m.group(1);
            // Transform frontend document URL into REST API URL
            java.util.regex.Matcher docIdMatcher = java.util.regex.Pattern.compile("docid=(\\d+)").matcher(src);
            if (docIdMatcher.find()) {
                String extractedDocId = docIdMatcher.group(1);
                src = "/apirest.php/Document/" + extractedDocId + "?alt=media";
            }
            imageUrls.add(src);
        }
        message.setInlineImages(imageUrls);
        
        // Remove img tags so they don't render as missing object characters
        content = content.replaceAll("(?i)<img[^>]*>", "");
        message.setContent(content);
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
            
            parseImages(mainTicket);
            messagesList.add(mainTicket);
            chatAdapter.notifyDataSetChanged();
            
            // Load followups from GLPI
            if (ticketId != null && !ticketId.isEmpty()) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Chamado #" + ticketId);
                }
                loadFollowups();
                loadTicketAttachments(mainTicket);
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
                        parseImages(followup);
                        messagesList.add(followup);
                        
                        // Load attachments for this specific reply
                        loadFollowupAttachments(followupId, followup);
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

    private void loadTicketAttachments(TicketMessage mainTicket) {
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.GetArray("/apirest.php/Ticket/" + ticketId + "/Document_Item", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    JSONArray array = new JSONArray(serverResponse);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String docId = obj.optString("documents_id");
                        if (docId != null && !docId.isEmpty() && !docId.equals("0")) {
                            fetchDocumentDetails(docId, mainTicket);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onVolleyFailure(String error) {}
        });
    }

    private void loadFollowupAttachments(String followupId, TicketMessage followupMessage) {
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.GetArray("/apirest.php/ITILFollowup/" + followupId + "/Document_Item", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    JSONArray array = new JSONArray(serverResponse);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String docId = obj.optString("documents_id");
                        if (docId != null && !docId.isEmpty() && !docId.equals("0")) {
                            fetchDocumentDetails(docId, followupMessage);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onVolleyFailure(String error) {}
        });
    }

    private void fetchDocumentDetails(String docId, TicketMessage message) {
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.GetItem("/apirest.php/Document/" + docId, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    JSONObject doc = new JSONObject(response);
                    String filename = doc.optString("filename", "Anexo");
                    // Force the safe REST API download link instead of doc.optString("link")
                    String downloadLink = "/apirest.php/Document/" + docId + "?alt=media";
                    String mime = doc.optString("mime", "");
                    if (!downloadLink.isEmpty()) {
                        com.cyberrocket.inventario.models.Attachment attachment = new com.cyberrocket.inventario.models.Attachment(docId, filename, downloadLink, mime);
                        message.getAttachments().add(attachment);
                        chatAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {}
            }
            @Override
            public void onVolleyFailure(String error) {}
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
        btnAttachImage.setEnabled(false);
        etMessageInput.setEnabled(false);
        
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
                        btnAttachImage.setEnabled(true);
                        etMessageInput.setEnabled(true);
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
                        btnAttachImage.setEnabled(true);
                        etMessageInput.setEnabled(true);
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
                        try {
                            JSONObject resp = new JSONObject(serverResponse);
                            String followupId = resp.getString("id");
                            if (!selectedImageUris.isEmpty()) {
                                uploadNextFollowupImage(followupId, 0);
                            } else {
                                btnSendMessage.setEnabled(true);
                                btnAttachImage.setEnabled(true);
                                etMessageInput.setEnabled(true);
                                etMessageInput.setText("");
                                
                                // reload messages
                                messagesList.clear();
                                populateInitialTicketData();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // fallback
                            btnSendMessage.setEnabled(true);
                            btnAttachImage.setEnabled(true);
                            etMessageInput.setEnabled(true);
                            etMessageInput.setText("");
                            
                            // reload messages
                            messagesList.clear();
                            populateInitialTicketData();
                        }
                    }

                    @Override
                    public void onVolleyFailure(String error) {
                        btnSendMessage.setEnabled(true);
                        btnAttachImage.setEnabled(true);
                        etMessageInput.setEnabled(true);
                        Toast.makeText(TicketDetailsActivity.this, "Falha ao enviar comentário", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            btnSendMessage.setEnabled(true);
            btnAttachImage.setEnabled(true);
            etMessageInput.setEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    if (!selectedImageUris.contains(imageUri)) {
                        selectedImageUris.add(imageUri);
                    }
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                if (!selectedImageUris.contains(imageUri)) {
                    selectedImageUris.add(imageUri);
                }
            }
            updateImagePreviews();
        }
    }

    private void updateImagePreviews() {
        layoutImagePreviews.removeAllViews();
        if (selectedImageUris.isEmpty()) {
            hsvImagePreviews.setVisibility(View.GONE);
            return;
        }
        hsvImagePreviews.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final Uri uri : selectedImageUris) {
            View view = inflater.inflate(R.layout.item_image_preview, layoutImagePreviews, false);
            ImageView ivThumbnail = view.findViewById(R.id.ivThumbnail);
            ImageButton btnRemovePreview = view.findViewById(R.id.btnRemovePreview);

            ivThumbnail.setImageURI(uri);

            btnRemovePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImageUris.remove(uri);
                    updateImagePreviews();
                }
            });

            layoutImagePreviews.addView(view);
        }
    }

    private byte[] getBytesFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(android.content.ContentResolver.SCHEME_CONTENT)) {
            mimeType = getContentResolver().getType(uri);
        } else {
            String fileExtension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        return mimeType;
    }

    private void uploadNextFollowupImage(final String followupId, final int index) {
        if (index >= selectedImageUris.size()) {
            selectedImageUris.clear();
            updateImagePreviews();
            btnSendMessage.setEnabled(true);
            btnAttachImage.setEnabled(true);
            etMessageInput.setEnabled(true);
            etMessageInput.setHint("Escreva um comentário...");
            etMessageInput.setText("");
            Toast.makeText(TicketDetailsActivity.this, "Comentário enviado com sucesso!", Toast.LENGTH_SHORT).show();
            
            messagesList.clear();
            populateInitialTicketData();
            return;
        }

        Uri uri = selectedImageUris.get(index);
        final String fileName = getFileName(uri);
        final byte[] fileData = getBytesFromUri(uri);
        final String mimeType = getMimeType(uri);

        if (fileData == null) {
            Toast.makeText(this, "Erro ao carregar a imagem: " + fileName, Toast.LENGTH_SHORT).show();
            uploadNextFollowupImage(followupId, index + 1);
            return;
        }

        etMessageInput.setHint("Enviando imagem " + (index + 1) + " de " + selectedImageUris.size() + "...");
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.UploadDocument(fileName, fileData, mimeType, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                try {
                    JSONObject resp = new JSONObject(serverResponse);
                    String documentId = resp.getString("id");
                    associateDocumentToFollowup(documentId, followupId, index);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(TicketDetailsActivity.this, "Erro ao enviar imagem " + fileName, Toast.LENGTH_SHORT).show();
                    uploadNextFollowupImage(followupId, index + 1);
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                Toast.makeText(TicketDetailsActivity.this, "Falha no envio de " + fileName + ": " + error, Toast.LENGTH_SHORT).show();
                uploadNextFollowupImage(followupId, index + 1);
            }
        });
    }

    private void associateDocumentToFollowup(final String documentId, final String followupId, final int index) {
        GLPIConnect glpi = new GLPIConnect(this);
        try {
            JSONObject input = new JSONObject();
            input.put("documents_id", documentId);
            input.put("itemtype", "ITILFollowup");
            input.put("items_id", followupId);

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.InsertItem("/apirest.php/Document_Item", payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    uploadNextFollowupImage(followupId, index + 1);
                }

                @Override
                public void onVolleyFailure(String error) {
                    Toast.makeText(TicketDetailsActivity.this, "Erro ao associar documento: " + error, Toast.LENGTH_SHORT).show();
                    uploadNextFollowupImage(followupId, index + 1);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            uploadNextFollowupImage(followupId, index + 1);
        }
    }

    @Override
    public void onMessageLongClick(TicketMessage message) {
        if (message.getMessageType() == TicketMessage.TYPE_TICKET && message.getId() != null) {
            CharSequence[] options = new CharSequence[]{"Editar Chamado", "Copiar Texto"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Opções do Chamado");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showEditTicketDialog();
                } else if (which == 1) {
                    copyToClipboard(message.getContent());
                }
            });
            builder.show();
        } else if (message.getMessageType() == TicketMessage.TYPE_REPLY && message.getId() != null) {
            CharSequence[] options = new CharSequence[]{"Editar", "Excluir", "Copiar Texto"};
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
                } else if (which == 2) {
                    copyToClipboard(message.getContent());
                }
            });
            builder.show();
        }
    }

    private void copyToClipboard(String htmlContent) {
        String plainText;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            plainText = android.text.Html.fromHtml(htmlContent, android.text.Html.FROM_HTML_MODE_COMPACT).toString();
        } else {
            plainText = android.text.Html.fromHtml(htmlContent).toString();
        }
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Chamado", plainText.trim());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Texto copiado para a área de transferência", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditTicketDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Editar Informações do Chamado");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_metadata, null);
        builder.setView(dialogView);

        android.widget.AutoCompleteTextView actvStatus = dialogView.findViewById(R.id.actvMetadataStatus);
        android.widget.AutoCompleteTextView actvLocation = dialogView.findViewById(R.id.actvMetadataLocation);

        String[] statusArray = {"Novo", "Atribuído", "Planejado", "Pendente", "Resolvido", "Fechado"};
        final int[] statusValues = {1, 2, 3, 4, 5, 6};
        
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusArray);
        actvStatus.setAdapter(statusAdapter);
        actvStatus.setOnClickListener(v -> actvStatus.showDropDown());
        
        // Find and set current status
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i] == currentStatus) {
                actvStatus.setText(statusArray[i], false);
                break;
            }
        }

        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, locationNames);
        actvLocation.setAdapter(locAdapter);
        actvLocation.setOnClickListener(v -> actvLocation.showDropDown());

        // Find and set current location
        for (int i = 0; i < locationIds.size(); i++) {
            if (locationIds.get(i) == currentLocationId) {
                actvLocation.setText(locationNames.get(i), false);
                break;
            }
        }

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String selectedStatusStr = actvStatus.getText().toString();
            int selectedStatus = 1;
            for (int i = 0; i < statusArray.length; i++) {
                if (statusArray[i].equals(selectedStatusStr)) {
                    selectedStatus = statusValues[i];
                    break;
                }
            }

            String selectedLocStr = actvLocation.getText().toString();
            int selectedLocId = 0;
            for (int i = 0; i < locationNames.size(); i++) {
                if (locationNames.get(i).equals(selectedLocStr)) {
                    selectedLocId = locationIds.get(i);
                    break;
                }
            }

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
