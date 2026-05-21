package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;
import com.cyberrocket.inventario.models.ComputadorLine;

import java.util.ArrayList;

public class ListAdapterComputadoresVinculados extends RecyclerView.Adapter<ListAdapterComputadoresVinculados.ViewHolderComputadores> {
    private ArrayList<ComputadorLine> dados;
    private Context contexto;
    private String mIdMonitor;

    public ListAdapterComputadoresVinculados(ArrayList<ComputadorLine> dados, Context contexto, String idMonitor) {
        this.dados = dados;
        this.contexto = contexto;
        this.mIdMonitor = idMonitor;
    }

    @NonNull
    @Override
    public ViewHolderComputadores onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.monitores_line_view, parent, false);
        return new ViewHolderComputadores(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderComputadores holder, int position) {
        if (dados != null && dados.size() > 0) {
            ComputadorLine comp = dados.get(position);
            holder.mTvNome.setText(comp.getNome());
            holder.mTvMarca.setText(comp.getMarca());
            holder.mTvModelo.setText(comp.getModelo());
            holder.mTvEstado.setText(comp.getEstado());
            holder.mTvNumeroSerie.setText(comp.getNumeroSerie());
            holder.mTvId.setText(comp.getIdComputador());
            
            // Set computer icon instead of monitor icon
            holder.mImgIcon.setImageResource(R.drawable.desktop_tower_monitor);
            
            // Exibe o botão de remover/desvincular para permitir desvincular o computador do monitor
            holder.mBtEditarNome.setVisibility(View.GONE);
            holder.mBtEditarEstado.setVisibility(View.GONE);
            holder.mBtRemover.setVisibility(View.VISIBLE);

            if (dados.size() > 1) {
                holder.mLayout.setMaxWidth(600);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    // Método para desvincular computador do monitor
    private void DesvincularComputador(final String computerId) {
        com.cyberrocket.inventario.lib.GLPIConnect con = new com.cyberrocket.inventario.lib.GLPIConnect(contexto);
        con.GetArray("/apirest.php/Monitor/" + mIdMonitor + "/Computer_Item/?range=0-2000", new com.cyberrocket.inventario.lib.GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(response);
                    boolean found = false;
                    String connectionId = "";
                    for (int i = 0; i < jsonArray.length(); i++) {
                        org.json.JSONObject conn = jsonArray.getJSONObject(i);
                        String compIdStr = conn.optString("computers_id", "");
                        String itemTypeStr = conn.optString("itemtype", "");
                        String itemsIdStr = conn.optString("items_id", "");
                        
                        if (compIdStr.equals(computerId) && itemTypeStr.equalsIgnoreCase("Monitor") && itemsIdStr.equals(mIdMonitor)) {
                            connectionId = conn.getString("id");
                            found = true;
                            break;
                        }
                    }
                    
                    if (found) {
                        final String finalConnectionId = connectionId;
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(contexto)
                                .setTitle("Desvincular o Computador?")
                                .setMessage("Tem certeza que deseja desvincular este computador do monitor?")
                                .setPositiveButton("Sim", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface dialogInterface, int which) {
                                        DeleteConexao("/apirest.php/Computer_Item/" + finalConnectionId + "?force_purge=true");
                                    }
                                })
                                .setNegativeButton("Não", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface dialogInterface, int which) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .create()
                                .show();
                    } else {
                        android.widget.Toast.makeText(contexto, "Conexão do computador não encontrada no servidor", android.widget.Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.widget.Toast.makeText(contexto, "Erro ao processar resposta do servidor", android.widget.Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onVolleyFailure(String url) {
                android.widget.Toast.makeText(contexto, "Erro ao buscar conexão:\n" + url, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void DeleteConexao(String url) {
        com.cyberrocket.inventario.lib.GLPIConnect con = new com.cyberrocket.inventario.lib.GLPIConnect(contexto);
        con.DeleteItem(url, new com.cyberrocket.inventario.lib.GLPIConnect.VolleyResponseListener() {
            @Override
            public void onVolleySuccess(String url, String response) {
                android.widget.Toast.makeText(contexto, "Conexão deletada com sucesso!", android.widget.Toast.LENGTH_SHORT).show();
                if (contexto instanceof android.app.Activity) {
                    ((android.app.Activity) contexto).recreate();
                } else {
                    Intent it = new Intent(contexto, ScannerActivity.class);
                    it.putExtra("id", mIdMonitor);
                    it.putExtra("item_type", "Monitor");
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    contexto.startActivity(it);
                }
            }
            @Override
            public void onVolleyFailure(String errorMsg) {
                android.widget.Toast.makeText(contexto, "Erro ao deletar conexão: " + errorMsg, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    public class ViewHolderComputadores extends RecyclerView.ViewHolder {
        public TextView mTvNome;
        public TextView mTvMarca;
        public TextView mTvModelo;
        public TextView mTvEstado;
        public TextView mTvId;
        public TextView mTvNumeroSerie;
        public ImageButton mBtEditarNome;
        public ImageButton mBtEditarEstado;
        public ImageButton mBtRemover;
        public ImageView mImgIcon;
        public ConstraintLayout mLayout;

        public ViewHolderComputadores(final View itemView) {
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
            mImgIcon = itemView.findViewById(R.id.imageView7);
            mLayout = itemView.findViewById(R.id.LayoutMonitorLine);

            mLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(contexto, ScannerActivity.class);
                    intent.putExtra("id", mTvId.getText().toString());
                    intent.putExtra("item_type", "Computer");
                    contexto.startActivity(intent);
                }
            });

            mBtRemover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DesvincularComputador(mTvId.getText().toString().trim());
                }
            });
        }
    }
}
