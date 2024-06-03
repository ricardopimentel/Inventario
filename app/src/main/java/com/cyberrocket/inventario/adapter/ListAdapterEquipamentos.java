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
            holder.mBtEditar.setVisibility(equip.getBtEditar());
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

        public ViewHolderEquip(final View itemView) {
            super(itemView);
            //Inicialização
            mTvDescricao = itemView.findViewById(R.id.tvDescricao);
            mTvConteudo = itemView.findViewById(R.id.tvConteudo);
            mBtEditar = itemView.findViewById(R.id.BtEditarEquipamentoLine);
            mBtCopiar = itemView.findViewById(R.id.BtCopiarEquipamentoLine);

            //Listeners
            mBtEditar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ShowDialog();
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
        }

        //Métodos

        private void ShowDialog(){
            AutoCompleteTextView mListaLocais;
            ArrayAdapter<String> mAdapter;
            ArrayList<String> mList;
            ArrayList mListaId;
            ArrayList mListaNomes;
            String mId;

            //Inicializar
            //Cria a caixa de dialogo
            View view = LayoutInflater.from(contexto).inflate(R.layout.activity_alterar_local, null);
            mListaLocais = view.findViewById(R.id.ListLocaisAlterarlocal);
            mList = new ArrayList<String>();
            mAdapter = new ArrayAdapter<String>(contexto, android.R.layout.simple_list_item_1, mList);
            mListaLocais.setAdapter(mAdapter);
            mListaId = new ArrayList();
            mListaNomes = new ArrayList();

            PreencherListaLocais(mAdapter, mListaNomes, mListaId);
            mListaLocais.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String nome = mAdapter.getItem(position).toString();
                    AlterarLocalizacao(mListaId.get(mListaNomes.indexOf(nome)).toString());
                }
            });
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(contexto)
                    .setTitle("Digite o nome")
                    .setView(view);
            dialog.create();
            dialog.show();
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
        try {
            postparams.put("id", mIdEquipamento);
            postparams.put("locations_id", local);
            finalarray.put("input", postparams);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("sessiontoken", finalarray.toString());

        GLPIConnect con = new GLPIConnect(contexto);
        con.UpdateItem("/apirest.php/Computer/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
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

    private void IrPara(Class para) {
        Intent intent = new Intent(contexto, para);
        intent.putExtra("id", mIdEquipamento);
        contexto.startActivity(intent);
    }
}

