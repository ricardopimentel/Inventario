package com.cyberrocket.inventario.adapter;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.cyberrocket.inventario.AlterarLocalActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.EquipamentoLine;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListAdapterEquipamentos extends RecyclerView.Adapter<ListAdapterEquipamentos.ViewHolderEquip>{
    private ArrayList<EquipamentoLine> dados;
    private Context contexto;
    private String mIdEquipamento;

    public ListAdapterEquipamentos(ArrayList<EquipamentoLine> dados, Context contexto, String id){
        this.dados = dados;
        this.contexto = contexto;
        mIdEquipamento = id;
    }

    @NonNull
    @Override
    public ViewHolderEquip onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.equipamento_line_view, parent, false);
        ViewHolderEquip holderEquip = new ViewHolderEquip(view);
        return holderEquip;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderEquip holder, int position) {
        if((dados!=null)&&(dados.size()>0)){
            EquipamentoLine equip = dados.get(position);
            holder.mTvDescricao.setText(equip.getDescricao());
            holder.mTvConteudo.setText(Html.fromHtml(equip.getConteudo()));
            
            if (equip.getVaultType() == 1) { // Computer Vault
                holder.mBtEditar.setVisibility(View.GONE);
                holder.mBtCopiar.setVisibility(View.VISIBLE);
                holder.mBtSenhasComputador.setVisibility(View.VISIBLE);
                holder.mBtSenhasLocal.setVisibility(View.GONE);
            } else if (equip.getVaultType() == 2) { // Location Vault
                holder.mBtEditar.setVisibility(equip.getBtEditar());
                holder.mBtCopiar.setVisibility(View.VISIBLE);
                holder.mBtSenhasComputador.setVisibility(View.GONE);
                holder.mBtSenhasLocal.setVisibility(View.VISIBLE);
            } else { // Normal
                holder.mBtEditar.setVisibility(equip.getBtEditar());
                holder.mBtCopiar.setVisibility(View.VISIBLE);
                holder.mBtSenhasComputador.setVisibility(View.GONE);
                holder.mBtSenhasLocal.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    //Implementa view holder
    public class ViewHolderEquip extends RecyclerView.ViewHolder{
        public TextView mTvDescricao;
        public TextView mTvConteudo;
        public ImageButton mBtEditar;
        public ImageButton mBtCopiar;
        public ImageButton mBtSenhasComputador;
        public ImageButton mBtSenhasLocal;

        public ViewHolderEquip(final View itemView) {
            super(itemView);
            //Inicialização
            mTvDescricao = itemView.findViewById(R.id.tvDescricao);
            mTvConteudo = itemView.findViewById(R.id.tvConteudo);
            mBtEditar = itemView.findViewById(R.id.BtEditarEquipamentoLine);
            mBtCopiar = itemView.findViewById(R.id.BtCopiarEquipamentoLine);
            mBtSenhasComputador = itemView.findViewById(R.id.BtSenhasComputadorEquipamentoLine);
            mBtSenhasLocal = itemView.findViewById(R.id.BtSenhasLocalEquipamentoLine);

            //Listeners
            mBtEditar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ShowDialog(mTvDescricao.getText().toString().trim());
                }
            });

            mBtCopiar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager Copiar = (ClipboardManager) contexto.getSystemService(Context.CLIPBOARD_SERVICE);
                    Copiar.setText(mTvConteudo.getText().toString());
                    Toast.makeText(contexto, mTvConteudo.getText().toString()+" copiado", Toast.LENGTH_SHORT).show();
                }
            });

            mBtSenhasComputador.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (contexto instanceof ScannerActivity) {
                        ((ScannerActivity) contexto).AbrirCofreComputador();
                    }
                }
            });

            mBtSenhasLocal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (contexto instanceof ScannerActivity) {
                        ((ScannerActivity) contexto).AbrirCofreLocal();
                    }
                }
            });
        }

        //Métodos

        private void ShowDialog(String tipo){
            android.widget.Spinner mListaLocais;
            ArrayAdapter<String> mAdapter;
            ArrayList<String> mList;
            ArrayList mListaId;
            ArrayList mListaNomes;

            //Inicializar
            View view = LayoutInflater.from(contexto).inflate(R.layout.activity_alterar_local, null);
            mListaLocais = view.findViewById(R.id.ListLocaisAlterarlocal);
            mList = new ArrayList<String>();
            mAdapter = new ArrayAdapter<String>(contexto, android.R.layout.simple_spinner_dropdown_item, mList);
            mListaLocais.setAdapter(mAdapter);
            mListaId = new ArrayList();
            mListaNomes = new ArrayList();

            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(contexto).setView(view);

            if (tipo.equalsIgnoreCase("Estado:")) {
                PreencherListaStatus(mAdapter, mListaNomes, mListaId);
                dialogBuilder.setTitle("Escolher o Estado");
            } else if (tipo.equalsIgnoreCase("Tipo:")) {
                PreencherListaTipo(mAdapter, mListaNomes, mListaId);
                dialogBuilder.setTitle("Escolher o Tipo");
            } else {
                PreencherListaLocais(mAdapter, mListaNomes, mListaId);
                dialogBuilder.setTitle("Escolher o Local");
            }

            dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListaLocais.getSelectedItem() != null) {
                        String nome = mListaLocais.getSelectedItem().toString();
                        int index = mListaNomes.indexOf(nome);
                        if (index != -1) {
                            String id = mListaId.get(index).toString();
                            if (tipo.equalsIgnoreCase("Estado:")) {
                                AlterarStatus(id);
                            } else if (tipo.equalsIgnoreCase("Tipo:")) {
                                AlterarTipo(id);
                            } else {
                                AlterarLocalizacao(id);
                            }
                        }
                    }
                }
            });

            dialogBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            dialogBuilder.show();
        }
    }

    private void PreencherListaLocais(ArrayAdapter<String> mAdapter, ArrayList mListaNomes, ArrayList mListaId) {
        GLPIConnect con = new GLPIConnect(contexto);
        con.GetArray("/apirest.php/Location?range=0-1000", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                }catch (JSONException err){
                    Log.d("ParseError", err.toString());
                }
                try {
                    //Pega dados do equipamento
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject local = jsonArray.getJSONObject(i);
                        mAdapter.add(local.getString("name"));
                        mListaNomes.add(local.getString("name"));
                        mListaId.add(local.getString("id"));
                    }
                    mAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(contexto, "Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void AlterarLocalizacao(String local) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        String endpoint = "/apirest.php/Computer/";
        if (contexto instanceof ScannerActivity) {
            if (((ScannerActivity)contexto).mItemType.equals("Monitor")) {
                endpoint = "/apirest.php/Monitor/";
            }
        }
        try {
            postparams.put("id", mIdEquipamento);
            postparams.put("locations_id", local);
            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(contexto);
        con.UpdateItem(endpoint, finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                IrPara(ScannerActivity.class);

            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(contexto, "Erro: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void PreencherListaStatus(ArrayAdapter<String> mAdapter, ArrayList mListaNomes, ArrayList mListaId) {
        GLPIConnect con = new GLPIConnect(contexto);
        con.GetArray("/apirest.php/State?range=0-1000", new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                }catch (JSONException err){
                    Log.d("ParseError", err.toString());
                }
                try {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject local = jsonArray.getJSONObject(i);
                        mAdapter.add(local.getString("name"));
                        mListaNomes.add(local.getString("name"));
                        mListaId.add(local.getString("id"));
                    }
                    mAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(contexto, "Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void AlterarStatus(String statusId) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        String endpoint = "/apirest.php/Computer/";
        if (contexto instanceof ScannerActivity) {
            if (((ScannerActivity)contexto).mItemType.equals("Monitor")) {
                endpoint = "/apirest.php/Monitor/";
            }
        }
        try {
            postparams.put("id", mIdEquipamento);
            postparams.put("states_id", statusId);
            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GLPIConnect con = new GLPIConnect(contexto);
        con.UpdateItem(endpoint, finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                Toast.makeText(contexto, "Estado atualizado gravado no servidor!", Toast.LENGTH_SHORT).show();
                IrPara(ScannerActivity.class);
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(contexto, "Erro ao alterar estado: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void PreencherListaTipo(ArrayAdapter<String> mAdapter, ArrayList mListaNomes, ArrayList mListaId) {
        GLPIConnect con = new GLPIConnect(contexto);
        String endpoint = "/apirest.php/ComputerType?range=0-1000";
        if (contexto instanceof ScannerActivity) {
            if (((ScannerActivity)contexto).mItemType.equals("Monitor")) {
                endpoint = "/apirest.php/MonitorType?range=0-1000";
            }
        }
        con.GetArray(endpoint, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                JSONArray jsonArray = new JSONArray();
                try {
                    jsonArray = new JSONArray(response);
                }catch (JSONException err){
                    Log.d("ParseError", err.toString());
                }
                try {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject local = jsonArray.getJSONObject(i);
                        mAdapter.add(local.getString("name"));
                        mListaNomes.add(local.getString("name"));
                        mListaId.add(local.getString("id"));
                    }
                    mAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(contexto, "Erro de conexão\n"+url, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void AlterarTipo(String tipoId) {
        JSONObject postparams = new JSONObject();
        JSONObject finalarray = new JSONObject();
        String endpoint = "/apirest.php/Computer/";
        String field = "computertypes_id";
        
        if (contexto instanceof ScannerActivity) {
            if (((ScannerActivity)contexto).mItemType.equals("Monitor")) {
                endpoint = "/apirest.php/Monitor/";
                field = "monitortypes_id";
            }
        }
        
        try {
            postparams.put("id", mIdEquipamento);
            postparams.put(field, tipoId);
            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GLPIConnect con = new GLPIConnect(contexto);
        con.UpdateItem(endpoint, finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                Toast.makeText(contexto, "Tipo atualizado gravado no servidor!", Toast.LENGTH_SHORT).show();
                IrPara(ScannerActivity.class);
            }

            @Override
            public void onVolleyFailure(String url) {
                Toast.makeText(contexto, "Erro ao alterar tipo: "+ url, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void IrPara(Class para) {
        Intent intent = new Intent(contexto, para);
        intent.putExtra("id", mIdEquipamento);
        contexto.startActivity(intent);
    }
}

