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
import org.json.JSONArray;
import android.widget.TextView;

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
    private android.widget.AutoCompleteTextView spinnerTicketType;
    private Button btnSubmitTicket;
    private com.google.android.material.button.MaterialButton btnAttachImage;
    private LinearLayout layoutImagePreviews;
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private View cardAIHelp;
    private static final int PICK_IMAGES_REQUEST = 1001;
    private android.widget.AutoCompleteTextView actvTicketCategory;
    private java.util.ArrayList<String> categoryNames = new java.util.ArrayList<>();
    private java.util.HashMap<String, String> categoryNameToIdMap = new java.util.HashMap<>();
    private String suggestedCategoryFromAI = null;
    private static final int REQUEST_CODE_SPEECH_INPUT = 5000;
    private EditText activeAIDialogEditText = null;

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
        cardAIHelp = findViewById(R.id.cardAIHelp);
        cardAIHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAIDialog();
            }
        });

        String[] types = {"Incidente", "Requisição"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerTicketType.setAdapter(adapter);
        spinnerTicketType.setText("Incidente", false);
        spinnerTicketType.setOnClickListener(v -> spinnerTicketType.showDropDown());

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

        actvTicketCategory = findViewById(R.id.actvTicketCategory);
        actvTicketCategory.setOnClickListener(v -> actvTicketCategory.showDropDown());
        loadTicketCategories();
    }

    private void loadTicketCategories() {
        GLPIConnect glpi = new GLPIConnect(this);
        glpi.GetArray("/apirest.php/ITILCategory", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String serverResponse) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(serverResponse);
                    categoryNames.clear();
                    categoryNameToIdMap.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        org.json.JSONObject obj = jsonArray.getJSONObject(i);
                        String id = obj.optString("id", "");
                        String name = obj.optString("completename", "");
                        if (name.isEmpty()) {
                            name = obj.optString("name", "");
                        }
                        if (!id.isEmpty() && !name.isEmpty()) {
                            categoryNames.add(name);
                            categoryNameToIdMap.put(name, id);
                        }
                    }
                    java.util.Collections.sort(categoryNames);
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateTicketActivity.this,
                            android.R.layout.simple_dropdown_item_1line, categoryNames);
                    actvTicketCategory.setAdapter(adapter);
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                android.util.Log.e("GLPI_Category", "Error loading categories: " + error);
            }
        });
    }

    private void startVoiceRecognition() {
        android.content.Intent intent = new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault());
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Fale agora para ditar seu relato...");
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (android.content.ActivityNotFoundException a) {
            Toast.makeText(this, "Seu dispositivo não suporta ditado por voz.", Toast.LENGTH_SHORT).show();
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
        } else if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<String> result = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty() && activeAIDialogEditText != null) {
                String spokenText = result.get(0);
                String currentText = activeAIDialogEditText.getText().toString();
                if (!currentText.isEmpty()) {
                    activeAIDialogEditText.setText(currentText + " " + spokenText);
                } else {
                    activeAIDialogEditText.setText(spokenText);
                }
                // Place cursor at the end of the new text
                activeAIDialogEditText.setSelection(activeAIDialogEditText.getText().length());
            }
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
        String selectedType = spinnerTicketType.getText().toString().trim();
        int glpiType = selectedType.equals("Requisição") ? 2 : 1; // 1 = Incident, 2 = Request
        String selectedCategoryName = actvTicketCategory.getText().toString().trim();

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
            input.put("status", 1);

            if (!selectedCategoryName.isEmpty() && categoryNameToIdMap.containsKey(selectedCategoryName)) {
                String catId = categoryNameToIdMap.get(selectedCategoryName);
                input.put("itilcategories_id", Integer.parseInt(catId));
            }

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
                            redirectToTicketDetails(ticketId);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
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

    private void redirectToTicketDetails(String ticketId) {
        Intent intent = new Intent(this, TicketDetailsActivity.class);
        intent.putExtra("id", ticketId);
        intent.putExtra("title", etTicketTitle.getText().toString().trim());
        intent.putExtra("description", etTicketDescription.getText().toString().trim());
        
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            String currentDate = sdf.format(new java.util.Date());
            intent.putExtra("creationDate", currentDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        startActivity(intent);
        finish();
    }

    private void uploadNextImage(final String ticketId, final int index) {
        if (index >= selectedImageUris.size()) {
            Toast.makeText(CreateTicketActivity.this, "Chamado criado com sucesso!", Toast.LENGTH_SHORT).show();
            redirectToTicketDetails(ticketId);
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

    private void showAIDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ai_helper, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        
        // Remove standard dialog background padding and background color to enable a floating card look
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        dialog.show();
        dialog.setOnDismissListener(d -> {
            activeAIDialogEditText = null;
        });
        
        // Window blur settings MUST be configured AFTER dialog.show() is called,
        // as the DecorView is only created during show(), preventing a NullPointerException.
        if (dialog.getWindow() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                dialog.getWindow().setBackgroundBlurRadius(80); // 80px blur radius inside the translucent card (recommended by Google)
                dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                dialog.getWindow().getAttributes().setBlurBehindRadius(20); // soft blur behind the whole dialog window
            }
        }
        
        com.google.android.material.card.MaterialCardView cardRoot = dialogView.findViewById(R.id.cardAIDialogRoot);
        if (cardRoot != null && cardRoot.getBackground() != null) {
            // Set background alpha to 230 (approx 90% opacity) dynamically to maintain translucent theme support
            cardRoot.getBackground().setAlpha(230);
        }

        android.widget.ViewFlipper viewFlipper = dialogView.findViewById(R.id.viewFlipperAI);
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroupMode = dialogView.findViewById(R.id.toggleGroupMode);
        EditText etUserReport = dialogView.findViewById(R.id.etAIUserReport);
        activeAIDialogEditText = etUserReport;
        
        com.google.android.material.textfield.TextInputLayout tilAIUserReport = dialogView.findViewById(R.id.tilAIUserReport);
        if (tilAIUserReport != null) {
            tilAIUserReport.setEndIconOnClickListener(v -> {
                startVoiceRecognition();
            });
        }

        android.widget.AutoCompleteTextView spinnerTone = dialogView.findViewById(R.id.spinnerAITone);
        EditText etApiKey = dialogView.findViewById(R.id.etGeminiApiKey);
        LinearLayout layoutSettings = dialogView.findViewById(R.id.layoutAISettings);
        ImageButton btnSettings = dialogView.findViewById(R.id.btnAISettings);
        android.view.View btnGenerate = dialogView.findViewById(R.id.btnAIGenerate);
        View layoutLoading = dialogView.findViewById(R.id.ivAIBtnLoadingSpark);
        View layoutResult = dialogView.findViewById(R.id.layoutAIResult);
        TextView tvSuggestedTitle = dialogView.findViewById(R.id.tvAISuggestedTitle);
        TextView tvSuggestedDescription = dialogView.findViewById(R.id.tvAISuggestedDescription);
        TextView tvSuggestedCategory = dialogView.findViewById(R.id.tvAISuggestedCategory);
        ImageButton btnBackToInput = dialogView.findViewById(R.id.btnAIBackToInput);
        Button btnApply = dialogView.findViewById(R.id.btnAIApply);
        Button btnCancel = dialogView.findViewById(R.id.btnAICancel);
        Button btnCancelPreview = dialogView.findViewById(R.id.btnAICancelPreview);
        
        String[] tones = {"Profissional", "Técnico", "Conciso"};
        ArrayAdapter<String> toneAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tones);
        spinnerTone.setAdapter(toneAdapter);
        spinnerTone.setText("Profissional", false);
        spinnerTone.setOnClickListener(v -> spinnerTone.showDropDown());
        
        android.content.SharedPreferences prefs = getSharedPreferences("GEMINI_PREFS", MODE_PRIVATE);
        String savedKey = prefs.getString("api_key", "");
        etApiKey.setText(savedKey);
        
        btnSettings.setOnClickListener(v -> {
            if (layoutSettings.getVisibility() == View.VISIBLE) {
                layoutSettings.setVisibility(View.GONE);
            } else {
                layoutSettings.setVisibility(View.VISIBLE);
            }
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnCancelPreview != null) {
            btnCancelPreview.setOnClickListener(v -> dialog.dismiss());
        }
        
        if (btnBackToInput != null) {
            btnBackToInput.setOnClickListener(v -> {
                if (viewFlipper != null) {
                    viewFlipper.setInAnimation(CreateTicketActivity.this, R.anim.slide_in_left);
                    viewFlipper.setOutAnimation(CreateTicketActivity.this, R.anim.slide_out_right);
                    viewFlipper.setDisplayedChild(0);
                }
            });
        }
        
        btnGenerate.setOnClickListener(v -> {
            String report = etUserReport.getText().toString().trim();
            String key = etApiKey.getText().toString().trim();
            String selectedTone = spinnerTone.getText().toString().trim();
            boolean isPgd = toggleGroupMode.getCheckedButtonId() == R.id.btnModePGD;
            
            if (report.isEmpty()) {
                Toast.makeText(CreateTicketActivity.this, "Por favor, relate o problema primeiro.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            generateAIContent(isPgd, report, selectedTone, key, layoutLoading, layoutResult, tvSuggestedTitle, tvSuggestedDescription, btnGenerate, dialog);
        });
        
        btnApply.setOnClickListener(v -> {
            etTicketTitle.setText(tvSuggestedTitle.getText().toString());
            etTicketDescription.setText(tvSuggestedDescription.getText().toString());
            
            if (suggestedCategoryFromAI != null && !suggestedCategoryFromAI.isEmpty()) {
                actvTicketCategory.setText(suggestedCategoryFromAI, false);
            }
            
            // Automatically set Type to 'Requisição' if PGD is selected (as PGD is remote activity logging)
            if (toggleGroupMode.getCheckedButtonId() == R.id.btnModePGD) {
                spinnerTicketType.setText("Requisição", false);
            }
            
            Toast.makeText(CreateTicketActivity.this, "Sugestões aplicadas com sucesso!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Touch gesture listener to swipe between slides
        View.OnTouchListener swipeListener = new View.OnTouchListener() {
            private float startX;
            private float startY;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        float diffX = endX - startX;
                        float diffY = endY - startY;
                        
                        if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 100) {
                            if (diffX > 0) {
                                // Swipe to the right (finger goes left-to-right) -> "se deslizar para a direita mostra o texto que foi gerado"
                                if (viewFlipper != null && viewFlipper.getDisplayedChild() == 0) {
                                    if (tvSuggestedTitle.getText().toString().equals("[Título Sugerido]") || tvSuggestedTitle.getText().toString().isEmpty()) {
                                        Toast.makeText(CreateTicketActivity.this, "Gere a sugestão com IA primeiro.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        viewFlipper.setInAnimation(CreateTicketActivity.this, R.anim.slide_in_right);
                                        viewFlipper.setOutAnimation(CreateTicketActivity.this, R.anim.slide_out_left);
                                        viewFlipper.setDisplayedChild(1);
                                    }
                                }
                            } else {
                                // Swipe to the left (finger goes right-to-left) -> "se deslizar para a esquerda volta para a tela de gerar o texto com ia"
                                if (viewFlipper != null && viewFlipper.getDisplayedChild() == 1) {
                                    viewFlipper.setInAnimation(CreateTicketActivity.this, R.anim.slide_in_left);
                                    viewFlipper.setOutAnimation(CreateTicketActivity.this, R.anim.slide_out_right);
                                    viewFlipper.setDisplayedChild(0);
                                }
                            }
                            return true;
                        }
                        break;
                }
                return false;
            }
        };

        // Set swipeListener on dialogView, cardRoot, viewFlipper, and child views so touch events propagate correctly
        dialogView.setOnTouchListener(swipeListener);
        if (cardRoot != null) cardRoot.setOnTouchListener(swipeListener);
        if (viewFlipper != null) {
            viewFlipper.setOnTouchListener(swipeListener);
            View scroll1 = viewFlipper.getChildAt(0);
            View scroll2 = viewFlipper.getChildAt(1);
            if (scroll1 != null) scroll1.setOnTouchListener(swipeListener);
            if (scroll2 != null) scroll2.setOnTouchListener(swipeListener);
        }
    }

    private void setAILoadingState(boolean loading, android.app.Dialog dialog) {
        if (dialog == null) return;
        android.widget.ImageButton btnGenerate = dialog.findViewById(R.id.btnAIGenerate);
        android.widget.ImageView ivAIBtnLoadingSpark = dialog.findViewById(R.id.ivAIBtnLoadingSpark);
        
        if (btnGenerate != null) {
            btnGenerate.setEnabled(!loading);
            if (loading) {
                btnGenerate.setImageAlpha(0); // hide the wizard icon
            } else {
                btnGenerate.setImageResource(R.drawable.wizard_24);
                btnGenerate.setImageAlpha(255); // restore the wizard icon
            }
        }
        
        if (ivAIBtnLoadingSpark != null) {
            if (loading) {
                ivAIBtnLoadingSpark.setVisibility(android.view.View.VISIBLE);
                android.view.animation.Animation rotateAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_spark);
                ivAIBtnLoadingSpark.startAnimation(rotateAnim);
            } else {
                ivAIBtnLoadingSpark.clearAnimation();
                ivAIBtnLoadingSpark.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void generateAIContent(boolean isPgd, String userReport, String tone, String apiKey,
            View layoutLoading, View layoutResult,
            TextView tvSuggestedTitle, TextView tvSuggestedDescription,
            View btnGenerate, android.app.Dialog dialog) {
        // Primary model - free tier on v1beta
        generateAIContentInternal("gemini-2.5-flash", isPgd, userReport, tone, apiKey,
                layoutLoading, layoutResult, tvSuggestedTitle, tvSuggestedDescription,
                btnGenerate, dialog);
    }

    private void generateAIContentInternal(final String modelName, final boolean isPgd, final String userReport, final String tone, final String apiKey,
                                           final View layoutLoading, final View layoutResult, final TextView tvSuggestedTitle,
                                           final TextView tvSuggestedDescription, final View btnGenerate, final android.app.Dialog dialog) {
        if (apiKey.trim().isEmpty()) {
            Toast.makeText(this, "Por favor, insira a Chave de API do Gemini nas configurações do assistente.", Toast.LENGTH_LONG).show();
            return;
        }
        getSharedPreferences("GEMINI_PREFS", MODE_PRIVATE).edit().putString("api_key", apiKey.trim()).apply();
        setAILoadingState(true, dialog);
        layoutResult.setVisibility(View.GONE);
        suggestedCategoryFromAI = null;

        // Build list of eligible categories
        java.util.ArrayList<String> eligibleCategories = new java.util.ArrayList<>();
        if (isPgd) {
            for (String cat : categoryNames) {
                if (cat.toLowerCase().startsWith("pgd >") || cat.toLowerCase().equals("pgd")) {
                    eligibleCategories.add(cat);
                }
            }
            // Fallback to all categories if no PGD categories are parsed yet
            if (eligibleCategories.isEmpty()) {
                eligibleCategories.addAll(categoryNames);
            }
        } else {
            eligibleCategories.addAll(categoryNames);
        }

        StringBuilder categoriesListBuilder = new StringBuilder();
        for (String cat : eligibleCategories) {
            if (categoriesListBuilder.length() > 0) {
                categoriesListBuilder.append(", ");
            }
            categoriesListBuilder.append("\"").append(cat).append("\"");
        }
        String categoriesJsonList = categoriesListBuilder.toString();

        String prompt;
        if (isPgd) {
            prompt = "Você é um profissional registrando uma atividade de trabalho remoto (PGD) no sistema de chamados. Com base no relato do usuário, formule um título curto e profissional (máximo 60 caracteres) e uma descrição técnica detalhada e bem estruturada (usando quebras de linha limpas) em português. O tom deve ser afirmativo, claro e formal, descrevendo a execução ativa/conclusão da atividade (e NÃO como se estivesse solicitando suporte ou relatando uma falha).\n\n"
                    + "Adicionalmente, classifique a atividade selecionando obrigatoriamente a melhor categoria disponível na seguinte lista de categorias permitidas para PGD:\n"
                    + "[" + categoriesJsonList + "]\n\n"
                    + "Relato da atividade: \"" + userReport.replace("\"", "\\\"").replace("\\n", "\\\\n") + "\"\n"
                    + "Tom desejado: " + tone + "\n\n"
                    + "Retorne APENAS um objeto JSON válido, sem blocos de código markdown (como ```json), sem texto explicativo antes ou depois, com o seguinte formato exato:\n"
                    + "{\n"
                    + "  \"titulo\": \"Título da atividade aqui\",\n"
                    + "  \"descricao\": \"Descrição detalhada e estruturada da execução da atividade aqui\",\n"
                    + "  \"categoria\": \"Categoria exatamente como escrita na lista fornecida acima\"\n"
                    + "}";
        } else {
            prompt = "Você é um assistente de suporte de TI especialista. Com base no relato do usuário, gere um título curto e profissional (máximo 60 caracteres) e uma descrição técnica detalhada e bem estruturada (usando quebras de linha limpas) em português relatando o problema/solicitação.\n\n"
                    + "Adicionalmente, classifique o chamado selecionando a melhor categoria disponível a partir da seguinte lista de categorias permitidas:\n"
                    + "[" + categoriesJsonList + "]\n\n"
                    + "Relato do problema: \"" + userReport.replace("\"", "\\\"").replace("\\n", "\\\\n") + "\"\n"
                    + "Tom desejado: " + tone + "\n\n"
                    + "Retorne APENAS um objeto JSON válido, sem blocos de código markdown (como ```json), sem texto explicativo antes ou depois, com o seguinte formato exato:\n"
                    + "{\n"
                    + "  \"titulo\": \"Título do chamado aqui\",\n"
                    + "  \"descricao\": \"Descrição detalhada e estruturada do problema aqui\",\n"
                    + "  \"categoria\": \"Categoria exatamente como escrita na lista fornecida acima\"\n"
                    + "}";
        }

        // v1beta is required for all current Gemini models available on AI Studio free tier
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();
            partObj.put("text", prompt);
            partsArray.put(partObj);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);
            jsonBody.put("contents", contentsArray);

            // Force JSON output to guarantee a valid JSON response from the model
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("responseMimeType", "application/json");
            jsonBody.put("generationConfig", generationConfig);

            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                    com.android.volley.Request.Method.POST, url, jsonBody,
                    response -> {
                        try {
                            setAILoadingState(false, dialog);
                            JSONArray candidates = response.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String textResponse = parts.getJSONObject(0).getString("text");
                            
                            // Robust cleanup to extract JSON even if wrapped in markdown blocks or having extra text
                            String cleanedText = textResponse.trim();
                            int firstBrace = cleanedText.indexOf('{');
                            int lastBrace = cleanedText.lastIndexOf('}');
                            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                                cleanedText = cleanedText.substring(firstBrace, lastBrace + 1);
                            }
                            
                            JSONObject resultJson = new JSONObject(cleanedText);
                            String titulo = resultJson.getString("titulo");
                            String descricao = resultJson.getString("descricao");
                            String categoria = resultJson.optString("categoria", "");
                            
                            suggestedCategoryFromAI = categoria;
                            tvSuggestedTitle.setText(titulo);
                            tvSuggestedDescription.setText(descricao);
                            
                            TextView tvSuggestedCategory = dialog.findViewById(R.id.tvAISuggestedCategory);
                            if (tvSuggestedCategory != null) {
                                tvSuggestedCategory.setText(categoria);
                            }
                            
                            android.widget.ViewFlipper viewFlipper = dialog.findViewById(R.id.viewFlipperAI);
                            if (viewFlipper != null) {
                                viewFlipper.setInAnimation(CreateTicketActivity.this, R.anim.slide_in_right);
                                viewFlipper.setOutAnimation(CreateTicketActivity.this, R.anim.slide_out_left);
                                viewFlipper.setDisplayedChild(1);
                            }
                            
                            layoutResult.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                            setAILoadingState(false, dialog);
                            Toast.makeText(CreateTicketActivity.this, "Erro ao processar resposta da IA. Tente novamente.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        int statusCode = error.networkResponse != null ? error.networkResponse.statusCode : -1;
                        boolean isTimeout = error instanceof com.android.volley.TimeoutError;

                        // Fallback chain: gemini-2.5-flash -> gemini-2.5-flash-lite -> gemini-2.0-flash -> gemini-2.0-flash-lite -> give up
                        if (statusCode == 404 || statusCode == 429 || isTimeout) {
                            if (modelName.equals("gemini-2.5-flash")) {
                                generateAIContentInternal("gemini-2.5-flash-lite", isPgd, userReport, tone, apiKey,
                                        layoutLoading, layoutResult, tvSuggestedTitle, tvSuggestedDescription,
                                        btnGenerate, dialog);
                                return;
                            } else if (modelName.equals("gemini-2.5-flash-lite")) {
                                generateAIContentInternal("gemini-2.0-flash", isPgd, userReport, tone, apiKey,
                                        layoutLoading, layoutResult, tvSuggestedTitle, tvSuggestedDescription,
                                        btnGenerate, dialog);
                                return;
                            } else if (modelName.equals("gemini-2.0-flash")) {
                                generateAIContentInternal("gemini-2.0-flash-lite", isPgd, userReport, tone, apiKey,
                                        layoutLoading, layoutResult, tvSuggestedTitle, tvSuggestedDescription,
                                        btnGenerate, dialog);
                                return;
                            }
                        }

                        setAILoadingState(false, dialog);
                        String errMsg = "Erro de conexão com o Gemini.";
                        if (error.networkResponse != null) {
                            String responseBody = new String(error.networkResponse.data);
                            android.util.Log.e("Gemini", "Error code: " + statusCode + ", body: " + responseBody);
                            if (statusCode == 429) {
                                errMsg = "Cota da API esgotada. Gere uma nova chave de API no Google AI Studio e tente novamente.";
                            } else if (statusCode == 400) {
                                errMsg = "Erro 400: Verifique sua chave de API.";
                            } else if (statusCode == 404) {
                                errMsg = "Erro 404: Modelo não encontrado ou desativado.";
                            }
                        } else if (isTimeout) {
                            errMsg = "Tempo limite de conexão esgotado ao gerar o conteúdo.";
                        }
                        Toast.makeText(CreateTicketActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
            );
            
            // Set 20-second timeout and 0 retries to avoid rate-limiting/spamming due to client-side timeouts
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    20000,
                    0,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(this);
            queue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            layoutLoading.setVisibility(View.GONE);
            btnGenerate.setEnabled(true);
            Toast.makeText(this, "Erro ao criar corpo da requisição.", Toast.LENGTH_SHORT).show();
        }
    }
}
