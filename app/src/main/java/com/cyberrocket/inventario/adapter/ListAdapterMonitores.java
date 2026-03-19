package com.cyberrocket.inventario.adapter;


import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
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
                    ShowDialogEstado();
                }
            });

            mBtRemover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DesvincularMonitor();
                }
            });

            mLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(contexto, ScannerActivity.class);
                    intent.putExtra("id", mTvId.getText().toString());
                    intent.putExtra("item_type", "Monitor");
                    contexto.startActivity(intent);
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
            con.GetArray("/apirest.php/Computer/"+ mIdEquipamento +"/Computer_Item/?range=0-2000", new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    JSONArray jsonArray = new JSONArray();
                    try {
                        jsonArray = new JSONArray(response);
                    }catch (JSONException err){
                        Log.d("ParseError", err.toString());
                    }
                    try {
                        boolean found = false;
                        for (int i = jsonArray.length() - 1; i >= 0; i--) {
                            JSONObject local = jsonArray.getJSONObject(i);
                            String itemTypeStr = local.optString("itemtype", "");
                            String itemsIdStr = local.optString("items_id", "");
                            if (itemTypeStr.equalsIgnoreCase("Monitor") && itemsIdStr.equals(mTvId.getText().toString().trim())) {
                                mIdConexao = local.getString("id");
                                showDialog();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            Log.d("MonitorLookup", "Computer_Item array from server: " + response);
                            Toast.makeText(contexto, "Conexão do monitor não encontrada no servidor", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(contexto, "Conexão deletada com sucesso!", Toast.LENGTH_SHORT).show();
                    LimparStatusMonitor();
                }
                @Override
                public void onVolleyFailure(String errorMsg) {
                    Log.d("DesvincularErro", errorMsg);
                    Toast.makeText(contexto, "Erro ao deletar conexão: "+ errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        }

        private void LimparStatusMonitor() {
            JSONObject postparams = new JSONObject();
            JSONObject finalarray = new JSONObject();
            try {
                postparams.put("id", mTvId.getText().toString());
                postparams.put("states_id", "0");
                finalarray.put("input", postparams);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            GLPIConnect con = new GLPIConnect(contexto);
            con.UpdateItem("/apirest.php/Monitor/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    Toast.makeText(contexto, "Status do monitor atualizado para disponível!", Toast.LENGTH_SHORT).show();
                    Intent it = new Intent(contexto, ScannerActivity.class);
                    it.putExtra("id", mIdEquipamento);
                    IrPara(it);
                }

                @Override
                public void onVolleyFailure(String errorMsg) {
                    Log.d("LimparStatusErro", errorMsg);
                    Toast.makeText(contexto, "Falha ao limpar o status do monitor: " + errorMsg, Toast.LENGTH_LONG).show();
                    Intent it = new Intent(contexto, ScannerActivity.class);
                    it.putExtra("id", mIdEquipamento);
                    IrPara(it);
                }
            });
        }

        private void ShowDialogEstado(){
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

            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(contexto)
                    .setTitle("Escolher o Estado")
                    .setView(view);

            PreencherListaStatus(mAdapter, mListaNomes, mListaId);

            dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListaLocais.getSelectedItem() != null) {
                        String nome = mListaLocais.getSelectedItem().toString();
                        int index = mListaNomes.indexOf(nome);
                        if (index != -1) {
                            String id = mListaId.get(index).toString();
                            AlterarStatus(id);
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
            try {
                postparams.put("id", mTvId.getText().toString());
                postparams.put("states_id", statusId);
                finalarray.put("input", postparams);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            GLPIConnect con = new GLPIConnect(contexto);
            con.UpdateItem("/apirest.php/Monitor/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    Toast.makeText(contexto, "Estado do monitor atualizado!", Toast.LENGTH_SHORT).show();
                    Intent it = new Intent(contexto, ScannerActivity.class);
                    it.putExtra("id", mIdEquipamento);
                    IrPara(it);
                }

                @Override
                public void onVolleyFailure(String url) {
                    Toast.makeText(contexto, "Erro ao alterar estado: "+ url, Toast.LENGTH_LONG).show();
                }
            });
        }

        private void showDialog(){
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(contexto)
                    .setTitle("Desvincular o Monitor?")
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Desvincular monitor
                            DeleteConexao("/apirest.php/Computer_Item/"+mIdConexao+"?force_purge=true");
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