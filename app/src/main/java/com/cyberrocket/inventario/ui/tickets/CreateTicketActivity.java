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

import android.net.Uri;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.view.LayoutInflater;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText etTicketTitle;
    private EditText etTicketDescription;
    private Spinner spinnerTicketType;
    private Button btnSubmitTicket;
    private com.google.android.material.button.MaterialButton btnAttachImage;
    private LinearLayout layoutImagePreviews;
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private static final int PICK_IMAGES_REQUEST = 1001;

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
        btnAttachImage = findViewById(R.id.btnAttachImage);
        layoutImagePreviews = findViewById(R.id.layoutImagePreviews);

        String[] types = {"Incidente", "Requisição"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerTicketType.setAdapter(adapter);

        btnSubmitTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitTicket();
            }
        });

        btnAttachImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Selecione as Imagens"), PICK_IMAGES_REQUEST);
            }
        });
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
        btnAttachImage.setEnabled(false);
        btnSubmitTicket.setText("ABRINDO CHAMADO...");
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
                    try {
                        JSONObject resp = new JSONObject(serverResponse);
                        String ticketId = resp.getString("id");
                        if (!selectedImageUris.isEmpty()) {
                            uploadNextImage(ticketId, 0);
                        } else {
                            Toast.makeText(CreateTicketActivity.this, "Chamado criado com sucesso!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // fallback
                        Toast.makeText(CreateTicketActivity.this, "Chamado criado com sucesso!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onVolleyFailure(String error) {
                    btnSubmitTicket.setEnabled(true);
                    btnAttachImage.setEnabled(true);
                    btnSubmitTicket.setText("ABRIR CHAMADO");
                    Toast.makeText(CreateTicketActivity.this, "Erro ao criar chamado. Verifique sua conexão.", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            btnSubmitTicket.setEnabled(true);
            btnAttachImage.setEnabled(true);
            btnSubmitTicket.setText("ABRIR CHAMADO");
            Toast.makeText(this, "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadNextImage(final String ticketId, final int index) {
        if (index >= selectedImageUris.size()) {
            Toast.makeText(CreateTicketActivity.this, "Chamado criado com sucesso!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri uri = selectedImageUris.get(index);
        final String fileName = getFileName(uri);
        final byte[] fileData = getBytesFromUri(uri);
        final String mimeType = getMimeType(uri);

        if (fileData == null) {
            Toast.makeText(this, "Erro ao carregar a imagem: " + fileName, Toast.LENGTH_SHORT).show();
            uploadNextImage(ticketId, index + 1);
            return;
        }

        btnSubmitTicket.setText("Enviando Imagem " + (index + 1) + " de " + selectedImageUris.size() + "...");
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.UploadDocument(fileName, fileData, mimeType, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                try {
                    JSONObject resp = new JSONObject(serverResponse);
                    String documentId = resp.getString("id");
                    associateDocumentToTicket(documentId, ticketId, index);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(CreateTicketActivity.this, "Erro ao enviar imagem " + fileName, Toast.LENGTH_SHORT).show();
                    uploadNextImage(ticketId, index + 1);
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                Toast.makeText(CreateTicketActivity.this, "Falha no envio de " + fileName + ": " + error, Toast.LENGTH_SHORT).show();
                uploadNextImage(ticketId, index + 1);
            }
        });
    }

    private void associateDocumentToTicket(final String documentId, final String ticketId, final int index) {
        GLPIConnect glpi = new GLPIConnect(this);
        try {
            JSONObject input = new JSONObject();
            input.put("documents_id", documentId);
            input.put("itemtype", "Ticket");
            input.put("items_id", ticketId);

            JSONObject payload = new JSONObject();
            payload.put("input", input);

            glpi.InsertItem("/apirest.php/Document_Item", payload, Request.Method.POST, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    uploadNextImage(ticketId, index + 1);
                }

                @Override
                public void onVolleyFailure(String error) {
                    Toast.makeText(CreateTicketActivity.this, "Erro ao associar documento: " + error, Toast.LENGTH_SHORT).show();
                    uploadNextImage(ticketId, index + 1);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            uploadNextImage(ticketId, index + 1);
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
