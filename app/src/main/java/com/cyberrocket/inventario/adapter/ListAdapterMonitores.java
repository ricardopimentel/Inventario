package com.cyberrocket.inventario.adapter;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.cyberrocket.inventario.AlterarEstadoMonitorActivity;
import com.cyberrocket.inventario.AlterarNomeMonitorActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.MonitorLine;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ListAdapterMonitores extends RecyclerView.Adapter<ListAdapterMonitores.ViewHolderMonitores>{
    private ArrayList<MonitorLine> dados;
    private Context contexto;
    String mIdEquipamento;
    String mIdConexao;

    //Construtor da classe
    public ListAdapterMonitores(ArrayList<MonitorLine> dados, Context contexto, String IdEquipamento){
        this.dados = dados;
        this.contexto = contexto;
        mIdEquipamento = IdEquipamento;

    }

    @NonNull
    @Override
    public ListAdapterMonitores.ViewHolderMonitores onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.monitores_line_view, parent, false);
        ListAdapterMonitores.ViewHolderMonitores holderMonitor = new ListAdapterMonitores.ViewHolderMonitores(view);
        return holderMonitor;
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapterMonitores.ViewHolderMonitores holder, int position) {
        if((dados!=null)&&(dados.size()>0)){
            MonitorLine monitor = dados.get(position);
            holder.mTvNome.setText(monitor.getNome());
            holder.mTvMarca.setText(monitor.getMarca());
            holder.mTvModelo.setText(monitor.getModelo());
            holder.mTvEstado.setText(monitor.getEstado());
            holder.mTvNumeroSerie.setText(monitor.getNumeroSerie());
            holder.mTvId.setText(monitor.getIdMonitor());
            if(dados.size()>1){
                holder.mLayout.setMaxWidth(600);
                holder.mLayout.getLayoutParams();
            }
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    //Implementa view holder
    public class ViewHolderMonitores extends RecyclerView.ViewHolder{
        public TextView mTvNome;
        public TextView mTvMarca;
        public TextView mTvModelo;
        public TextView mTvEstado;
        public TextView mTvId;
        public TextView mTvNumeroSerie;
        public ImageButton mBtEditarNome;
        public ImageButton mBtEditarEstado;
        public ImageButton mBtRemover;
        public ConstraintLayout mLayout;

        public ViewHolderMonitores(final View itemView) {
            super(itemView);
            mTvNome = itemView.findViewById(R.id.TvMonitoresViewNome);
            mTvMarca = itemView.findViewById(R.id.TvMonitoresViewMarca);
            mTvModelo = itemView.findViewById(R.id.TvMonitoresViewModelo);
            mTvEstado = itemView.findViewById(R.id.TvMonitoresViewEstado);
            mTvId = itemView.findViewById(R.id.TvMonitoresViewId);
            mTvNumeroSerie = itemView.findViewById(R.id.TvMonitoresViewNumeroSerie);
            mBtEditarNome = itemView.findViewById(R.id.BtMonitoresViewEditNome);
            mBtEditarEstado = itemView.findViewById(R.id.BtMonitoresViewEditEstado);
            mBtRemover = itemView.findViewById(R.id.BtMonitoresViewRemover);
            mLayout = itemView.findViewById(R.id.LayoutMonitorLine);

            //Listeners
            mBtEditarNome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Chama a caixa de dialogo pra alterar o nome do monitor
                    AlterarNome();
                }
            });

            mBtEditarEstado.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent it = new Intent(contexto, AlterarEstadoMonitorActivity.class);
                    it.putExtra("idmonitor", mTvId.getText().toString());
                    it.putExtra("estadomonitor", mTvNome.getText().toString());
                    it.putExtra("idequipamento", mIdEquipamento);
                    IrPara(it);
                }
            });

            mBtRemover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DesvincularMonitor();
                }
            });
        }


        //Metodos
        private void IrPara(Intent it) {

            contexto.startActivity(it);
        }

        private void AlterarNome() {
            //Cria a caixa de dialogo
            View view = LayoutInflater.from(contexto).inflate(R.layout.activity_alterar_nome_monitor, null);
            TextInputEditText edittext = view.findViewById(R.id.TvNomeMonitorAlterarEstadoMonitor);
            //Preenche o campo de texo com o nome que já está no monitor
            edittext.setText(mTvNome.getText().toString());
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(contexto)
                    .setTitle("Digite o nome")
                    .setView(view)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Salva a alteração passando o texto digitado pelo usuário
                            SalvarAlteracaoNome(edittext.getText().toString());
                            dialogInterface.dismiss();
                        }
                    }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            dialog.create();
            dialog.show();
        }

        private void SalvarAlteracaoNome(String novonome){
            //Chama o volley para alterar o nome no glpi
            JSONObject postparams = new JSONObject();
            JSONObject finalarray = new JSONObject();
            try {
                postparams.put("id", mTvId.getText());
                postparams.put("name", novonome);

                finalarray.put("input", postparams);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.e("sessiontoken", finalarray.toString());

            GLPIConnect con = new GLPIConnect(contexto);
            con.UpdateItem("/apirest.php/Monitor/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    Intent it = new Intent(contexto, ScannerActivity.class);
                    it.putExtra("id", mIdEquipamento);
                    IrPara(it);
                }

                @Override
                public void onVolleyFailure(String url) {
                    Toast.makeText(contexto, "Erro: "+ url, Toast.LENGTH_LONG).show();
                }
            });
        }

        private void DesvincularMonitor() {
            //Faz a conexão
            GLPIConnect con = new GLPIConnect(contexto);
            con.GetArray("/apirest.php/Monitor/"+ mTvId.getText().toString()+"/Computer_Item/", new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    JSONArray jsonArray = new JSONArray();
                    try {
                        jsonArray = new JSONArray(response);
                    }catch (JSONException err){
                        Log.d("ParseError", err.toString());
                    }
                    try {
                        //Pega id da conexao
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject local = jsonArray.getJSONObject(i);
                            mIdConexao = local.getString("id");
                            showDialog();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onVolleyFailure(String url) {
                    Toast.makeText(contexto, "Erro: "+ url, Toast.LENGTH_LONG).show();
                }
            });
        }
        private void DeleteConexao(String url) {
            Log.d("Desvincular", url);
            //Faz a conexão
            GLPIConnect con = new GLPIConnect(contexto);
            con.DeleteItem(url, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    Intent it = new Intent(contexto, ScannerActivity.class);
                    it.putExtra("id", mIdEquipamento);
                    IrPara(it);
                }
                @Override
                public void onVolleyFailure(String url) {
                    Log.d("Desvincular", url);
                    Toast.makeText(contexto, "Erro: "+ url, Toast.LENGTH_LONG).show();
                }
            });
        }

        private void showDialog(){
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(contexto)
                .setTitle("Desvincular o Monitor?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Deslincular monitor
                        DeleteConexao("/apirest.php/Monitor/"+ mTvId.getText().toString()+"/Computer_Item/"+mIdConexao+"?force_purge=true");
                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            dialog.create();
            dialog.show();
        }
    }
}
