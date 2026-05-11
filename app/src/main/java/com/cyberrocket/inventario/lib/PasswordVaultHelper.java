package com.cyberrocket.inventario.lib;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.adapter.ListAdapterSenhas;
import com.cyberrocket.inventario.models.SenhaItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PasswordVaultHelper {

    private final Context context;
    private final GLPIConnect con;
    private final InfisicalConnect infisical;

    public interface VaultOperationListener {
        void onLoading(boolean loading);
        void onVaultConfigError();
    }

    public PasswordVaultHelper(Context context) {
        this.context = context;
        this.con = new GLPIConnect(context);
        this.infisical = new InfisicalConnect(context);
    }

    public void showPasswordDialog(String itemtype, String itemId, VaultOperationListener listener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_gerenciar_senhas, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RecyclerView rvSenhas = dialogView.findViewById(R.id.RvSenhas);
        TextView tvSenhasEmpty = dialogView.findViewById(R.id.TvSenhasEmpty);
        TextInputEditText edtDesc = dialogView.findViewById(R.id.EdtDescricaoSenha);
        TextInputEditText edtValor = dialogView.findViewById(R.id.EdtValorSenha);
        ImageButton btnAdd = dialogView.findViewById(R.id.BtnAddSenha);
        final Button btnFechar = dialogView.findViewById(R.id.BtnFecharSenhas);
        TextView tvTitle = dialogView.findViewById(R.id.TvDialogTitle);

        btnFechar.setVisibility(View.GONE);
        btnFechar.setText("Salvar no Servidor e Fechar");
        
        tvTitle.setText(itemtype.equals("Computer") ? "Senhas da Máquina" : "Senhas do Local");

        rvSenhas.setLayoutManager(new LinearLayoutManager(context));
        
        final ArrayList<SenhaItem> currentPasswords = new ArrayList<>();
        final int[] editingPosition = {-1};

        final String[] existingSecretId = {null};
        final String[] existingFieldsItemId = {null};
        final String[] containerId = {"1"};

        final boolean[] isDirty = {false};

        Runnable updateButtonVisibility = () -> {
            String d = edtDesc.getText().toString().trim();
            String s = edtValor.getText().toString().trim();
            if (!d.isEmpty() || !s.isEmpty() || editingPosition[0] != -1 || isDirty[0]) {
                btnFechar.setVisibility(View.VISIBLE);
            } else {
                btnFechar.setVisibility(View.GONE);
            }
        };

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateButtonVisibility.run(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtDesc.addTextChangedListener(watcher);
        edtValor.addTextChangedListener(watcher);

        ListAdapterSenhas adapter = new ListAdapterSenhas(currentPasswords, context, new ListAdapterSenhas.OnSenhaInteractionListener() {
            @Override
            public void onDeleteClick(int position, SenhaItem item) {
                currentPasswords.remove(position);
                if(rvSenhas.getAdapter() != null) rvSenhas.getAdapter().notifyDataSetChanged();
                if (currentPasswords.isEmpty()) tvSenhasEmpty.setVisibility(View.VISIBLE);
                
                persistirAlteracoesNoVault(currentPasswords, itemtype, itemId, existingSecretId, existingFieldsItemId, containerId, null, false, isDirty, updateButtonVisibility, listener);

                if (editingPosition[0] == position) {
                    editingPosition[0] = -1;
                    edtDesc.setText("");
                    edtValor.setText("");
                    btnAdd.setImageResource(android.R.drawable.ic_input_add);
                } else if (editingPosition[0] > position) {
                    editingPosition[0]--;
                }
                updateButtonVisibility.run();
            }

            @Override
            public void onEditClick(int position, SenhaItem item) {
                edtDesc.setText(item.getDescricao());
                edtValor.setText(item.getSenha());
                editingPosition[0] = position;
                btnAdd.setImageResource(R.drawable.save_24);
                updateButtonVisibility.run();
            }
        });
        rvSenhas.setAdapter(adapter);

        if (listener != null) listener.onLoading(true);

        if (!infisical.isConfigured()) {
            if (listener != null) {
                listener.onLoading(false);
                listener.onVaultConfigError();
            }
            return;
        }

        String fieldsEndpoint = "/apirest.php/PluginFields" + itemtype + "cofredesenha/?searchText[items_id]=" + itemId;
        
        con.GetArray(fieldsEndpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                try {
                    JSONArray results = new JSONArray(response);
                    
                    if (results.length() > 0) {
                        JSONObject row = results.getJSONObject(0);
                        
                        existingFieldsItemId[0] = row.optString("id");
                        String cId = row.optString("plugin_fields_containers_id");
                        if(cId != null && !cId.isEmpty() && !cId.equals("null")) {
                            containerId[0] = cId;
                        }
                        
                        String vaultId = row.optString("vaultsecretidfield", "");
                        
                        if(existingFieldsItemId[0] != null && existingFieldsItemId[0].equals("0")) { existingFieldsItemId[0] = null; }
                        
                        if (!vaultId.isEmpty() && !vaultId.equals("null")) {
                            existingSecretId[0] = vaultId;
                            infisical.GetSecret(vaultId, new InfisicalConnect.VolleyResponseListener() {
                                @Override
                                public void onVolleySuccess(String infResponse) {
                                    if (listener != null) listener.onLoading(false);
                                    try {
                                        JSONObject respObj = new JSONObject(infResponse);
                                        JSONObject secretObj = respObj.getJSONObject("secret");
                                        String secretValue = secretObj.getString("secretValue");
                                        
                                        JSONArray secretsArray = new JSONArray(secretValue);
                                        for (int i = 0; i < secretsArray.length(); i++) {
                                            JSONObject s = secretsArray.getJSONObject(i);
                                            currentPasswords.add(new SenhaItem(s.optString("descricao"), s.optString("senha")));
                                        }
                                        
                                        if (currentPasswords.isEmpty()) tvSenhasEmpty.setVisibility(View.VISIBLE);
                                        else tvSenhasEmpty.setVisibility(View.GONE);
                                        
                                        adapter.notifyDataSetChanged();
                                        dialog.show();
                                        
                                    } catch (JSONException e) {
                                        tvSenhasEmpty.setVisibility(View.VISIBLE);
                                        dialog.show();
                                    }
                                }
                                 @Override
                                 public void onVolleyFailure(String error) {
                                     if (listener != null) listener.onLoading(false);
                                     Log.e("VaultHelper", "Erro Infisical: " + error);
                                     
                                     if (error != null && error.contains("401")) {
                                         if (listener != null) listener.onVaultConfigError();
                                     } else {
                                         Toast.makeText(context, "Erro no Cofre: " + error, Toast.LENGTH_SHORT).show();
                                         tvSenhasEmpty.setVisibility(View.VISIBLE);
                                         dialog.show();
                                     }
                                 }
                            });
                            return;
                        }
                    }
                    
                    if (listener != null) listener.onLoading(false);
                    tvSenhasEmpty.setVisibility(View.VISIBLE);
                    dialog.show();
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (listener != null) listener.onLoading(false);
                    tvSenhasEmpty.setVisibility(View.VISIBLE);
                    dialog.show();
                }
            }

            @Override
            public void onVolleyFailure(String error) {
                if (listener != null) listener.onLoading(false);
                tvSenhasEmpty.setVisibility(View.VISIBLE);
                dialog.show();
            }
        });

        Runnable addOrUpdateAction = () -> {
            String d = edtDesc.getText().toString().trim();
            String s = edtValor.getText().toString().trim();
            if (d.isEmpty() || s.isEmpty()) return;

            if (editingPosition[0] != -1) {
                SenhaItem item = currentPasswords.get(editingPosition[0]);
                item.setDescricao(d);
                item.setSenha(s);
                editingPosition[0] = -1;
                isDirty[0] = true;
                btnAdd.setImageResource(android.R.drawable.ic_input_add);
            } else {
                SenhaItem newItem = new SenhaItem(d, s);
                currentPasswords.add(newItem);
                isDirty[0] = true;
            }
            adapter.notifyDataSetChanged();
            tvSenhasEmpty.setVisibility(View.GONE);
            edtDesc.setText("");
            edtValor.setText("");
            updateButtonVisibility.run();
        };

        btnAdd.setOnClickListener(v -> {
            String d = edtDesc.getText().toString().trim();
            String s = edtValor.getText().toString().trim();
            if (d.isEmpty() || s.isEmpty()) {
                Toast.makeText(context, "Preencha usuário e senha.", Toast.LENGTH_SHORT).show();
                return;
            }
            addOrUpdateAction.run();
        });

        btnFechar.setOnClickListener(v -> {
            addOrUpdateAction.run();
            persistirAlteracoesNoVault(currentPasswords, itemtype, itemId, existingSecretId, existingFieldsItemId, containerId, dialog, true, isDirty, updateButtonVisibility, listener);
        });
    }

    private void persistirAlteracoesNoVault(ArrayList<SenhaItem> currentPasswords, String itemtype, String itemId, String[] existingSecretId, String[] existingFieldsItemId, String[] containerId, AlertDialog dialog, boolean closeAfter, boolean[] isDirty, Runnable updateButtonVisibility, VaultOperationListener listener) {
        if (!infisical.isConfigured()) {
            if (dialog != null) Toast.makeText(context, "Configure o Infisical primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) listener.onLoading(true);

        JSONArray arrayToSave = new JSONArray();
        try {
            for (SenhaItem item : currentPasswords) {
                JSONObject o = new JSONObject();
                o.put("descricao", item.getDescricao());
                o.put("senha", item.getSenha());
                arrayToSave.put(o);
            }
        } catch (JSONException e) { e.printStackTrace(); }
        
        String jsonPayload = arrayToSave.toString();
        
        if (currentPasswords.isEmpty() && existingSecretId[0] != null) {
            infisical.UpdateSecret(existingSecretId[0], "[]", new InfisicalConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String response) {
                    existingSecretId[0] = null;
                    limparCustomFieldGLPI(itemtype, itemId, existingFieldsItemId[0], dialog, closeAfter, isDirty, updateButtonVisibility, listener);
                }
                @Override
                public void onVolleyFailure(String error) {
                    if (listener != null) listener.onLoading(false);
                    Toast.makeText(context, "Erro apagando secret (update []): " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        if (currentPasswords.isEmpty()) {
            if (listener != null) listener.onLoading(false);
            if (closeAfter && dialog != null) dialog.dismiss();
            return;
        }

        if (existingSecretId[0] != null) {
            infisical.UpdateSecret(existingSecretId[0], jsonPayload, new InfisicalConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String response) {
                    if (listener != null) listener.onLoading(false);
                    isDirty[0] = false;
                    if (closeAfter) {
                        Toast.makeText(context, "Senhas salvas no Vault!", Toast.LENGTH_SHORT).show();
                        if (dialog != null) dialog.dismiss();
                    } else {
                        updateButtonVisibility.run();
                    }
                }
                @Override
                public void onVolleyFailure(String error) {
                    if (listener != null) listener.onLoading(false);
                    Toast.makeText(context, "Erro atualizando secret", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            String newKey = "GLPI_" + itemtype.toUpperCase() + "_" + itemId + "_CRED";
            infisical.CreateSecret(newKey, jsonPayload, new InfisicalConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String response) {
                    existingSecretId[0] = newKey; 
                    atualizarCustomFieldGLPI(itemtype, itemId, existingFieldsItemId[0], containerId[0], newKey, dialog, closeAfter, isDirty, updateButtonVisibility, listener);
                }
                @Override
                public void onVolleyFailure(String error) {
                    if (error.contains("already exists") || error.contains("Secret already exists")) {
                        existingSecretId[0] = newKey;
                        infisical.UpdateSecret(newKey, jsonPayload, new InfisicalConnect.VolleyResponseListener() {
                            @Override
                            public void onVolleySuccess(String updateResponse) {
                                atualizarCustomFieldGLPI(itemtype, itemId, existingFieldsItemId[0], containerId[0], newKey, dialog, closeAfter, isDirty, updateButtonVisibility, listener);
                            }
                            @Override
                            public void onVolleyFailure(String updateError) {
                                if (listener != null) listener.onLoading(false);
                                Toast.makeText(context, "Erro atualizando secret: " + updateError, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        if (listener != null) listener.onLoading(false);
                        Toast.makeText(context, "Erro criando secret: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void atualizarCustomFieldGLPI(String itemtype, String itemId, String existingRowId, String containerId, String newKey, AlertDialog dialog, boolean closeAfter, boolean[] isDirty, Runnable updateButtonVisibility, VaultOperationListener listener) {
        String endpoint = "/apirest.php/PluginFields" + itemtype + "cofredesenha/";
        int method = Request.Method.POST;
        
        if (existingRowId != null && !existingRowId.isEmpty()) {
            endpoint += existingRowId;
            method = Request.Method.PUT;
        }

        JSONObject payload = new JSONObject();
        JSONObject input = new JSONObject();
        try {
            if (method == Request.Method.PUT) {
                input.put("id", Integer.parseInt(existingRowId));
            }
            input.put("items_id", Integer.parseInt(itemId));
            input.put("itemtype", itemtype);
            input.put("plugin_fields_containers_id", 1);
            input.put("vaultsecretidfield", newKey);
            
            payload.put("input", input);
            
            con.UpdateItemRaw(endpoint, payload, method, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String u, String r) {
                    if (listener != null) listener.onLoading(false);
                    isDirty[0] = false;
                    if (closeAfter) {
                        Toast.makeText(context, "Senhas salvas no Vault!", Toast.LENGTH_SHORT).show();
                        if (dialog != null) dialog.dismiss();
                    } else {
                        updateButtonVisibility.run();
                    }
                }
                @Override
                public void onVolleyFailure(String e) {
                    if (listener != null) listener.onLoading(false);
                    Toast.makeText(context, "Erro Atualizando GLPI: " + e, Toast.LENGTH_LONG).show();
                    if (closeAfter && dialog != null) dialog.dismiss();
                }
            });
            
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
            if (listener != null) listener.onLoading(false);
            if (dialog != null) dialog.dismiss();
        }
    }

    private void limparCustomFieldGLPI(String itemtype, String itemId, String existingRowId, AlertDialog dialog, boolean closeAfter, boolean[] isDirty, Runnable updateButtonVisibility, VaultOperationListener listener) {
         if (existingRowId == null || existingRowId.isEmpty()) {
             if (listener != null) listener.onLoading(false);
             isDirty[0] = false;
             Toast.makeText(context, "Senha removida.", Toast.LENGTH_SHORT).show();
             if (closeAfter && dialog != null) dialog.dismiss();
             else updateButtonVisibility.run();
             return;
         }
         try {
             String endpoint = "/apirest.php/PluginFields" + itemtype + "cofredesenha/" + existingRowId;
             JSONObject payload = new JSONObject();
             JSONObject input = new JSONObject();
             
             input.put("id", Integer.parseInt(existingRowId));
             input.put("items_id", Integer.parseInt(itemId));
             input.put("itemtype", itemtype);
             input.put("plugin_fields_containers_id", 1);
             input.put("vaultsecretidfield", "");
             
             payload.put("input", input);
             con.UpdateItemRaw(endpoint, payload, Request.Method.PUT, new GLPIConnect.VolleyResponseListener(){
                  @Override
                  public void onVolleySuccess(String url, String response) {
                       if (listener != null) listener.onLoading(false);
                       isDirty[0] = false;
                       Toast.makeText(context, "Senha removida.", Toast.LENGTH_SHORT).show();
                       if (closeAfter) {
                           if (dialog != null) dialog.dismiss();
                       } else {
                           updateButtonVisibility.run();
                       }
                  }
                  @Override
                  public void onVolleyFailure(String error) {
                       if (listener != null) listener.onLoading(false); 
                       Toast.makeText(context, "Erro Removendo no GLPI: " + error, Toast.LENGTH_LONG).show();
                       if (closeAfter && dialog != null) dialog.dismiss();
                  }
             });
         } catch(JSONException e){ 
             e.printStackTrace(); 
             if (listener != null) listener.onLoading(false); 
             if (closeAfter && dialog != null) dialog.dismiss(); 
         }
    }
}
