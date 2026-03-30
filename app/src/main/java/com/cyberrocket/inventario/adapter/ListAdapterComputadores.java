package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;
import com.cyberrocket.inventario.models.Computador;

import java.util.ArrayList;

public class ListAdapterComputadores extends RecyclerView.Adapter<ListAdapterComputadores.ViewHolderComputador> {

    private ArrayList<Computador> dados;
    private Context contexto;

    public ListAdapterComputadores(ArrayList<Computador> dados, Context contexto) {
        this.dados = dados;
        this.contexto = contexto;
    }

    @NonNull
    @Override
    public ListAdapterComputadores.ViewHolderComputador onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.computador_line_view, parent, false);
        return new ViewHolderComputador(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapterComputadores.ViewHolderComputador holder, int position) {
        if (dados != null && dados.size() > 0) {
            Computador computador = dados.get(position);
            
            holder.mTvIdComputador.setText(computador.getId() != null ? "#" + computador.getId() : "");
            holder.mTvNomeComputador.setText(computador.getNome() != null ? computador.getNome() : "Sem Nome");
            
            String marca = computador.getFabricante() != null ? computador.getFabricante() : "";
            String tipo = computador.getTipo() != null ? computador.getTipo() : "";
            String info = marca;
            if (!tipo.isEmpty()) {
                if (!info.isEmpty()) info += " | ";
                info += tipo;
            }
            holder.mTvInfoComputador.setText(info);
            
            holder.mTvLocalComputador.setText(computador.getLocalizacao() != null ? computador.getLocalizacao() : "");
            
            // Dynamic Icon Selection based on Tipo
            String tipoLower = (computador.getTipo() != null ? computador.getTipo() : "").toLowerCase();
            int iconColor;
            if (tipoLower.contains("monitor")) {
                holder.mImgIconComputador.setImageResource(R.drawable.monitor_24);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_monitor_pink); // Pink for monitors
            } else if (tipoLower.contains("notebook") || tipoLower.contains("laptop")) {
                holder.mImgIconComputador.setImageResource(R.drawable.ic_laptop_24dp);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_maintenance_green); // Green for laptops
            } else if (tipoLower.contains("servidor") || tipoLower.contains("server")) {
                holder.mImgIconComputador.setImageResource(R.drawable.ic_dns_24dp);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_server_purple); // Purple for servers
            } else if (tipoLower.contains("virtual") || tipoLower.contains("vmware") || tipoLower.contains("vm")) {
                holder.mImgIconComputador.setImageResource(R.drawable.ic_virtual_machine_24dp);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_storage_cyan); // Cyan for VMs
            } else {
                // Default to desktop for Mini PC, Low Profile, etc.
                holder.mImgIconComputador.setImageResource(R.drawable.ic_computer_24dp);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_equipment_orange); // Orange for Desktops
            }

            // Apply color to background (border) only, and clear previous color filters on the icon
            holder.mImgIconComputador.clearColorFilter();
            if (holder.mImgIconComputador.getBackground() != null) {
                holder.mImgIconComputador.getBackground().mutate().setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if (computador.getImagemStatus() != null && computador.getImagemStatus().getDrawable() != null) {
                holder.mImgStatusComputador.setImageDrawable(computador.getImagemStatus().getDrawable());
                
                String statusMsg = (computador.getStatusInfo() != null ? computador.getStatusInfo() : "").toLowerCase();
                if (statusMsg.contains("produção")) {
                    holder.mImgStatusComputador.setColorFilter(Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    holder.mImgStatusComputador.setColorFilter(Color.parseColor("#F44336"), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(contexto, ScannerActivity.class);
                    intent.putExtra("id", computador.getId());
                    String itemType = tipoLower.contains("monitor") ? "Monitor" : "Computer";
                    intent.putExtra("item_type", itemType);
                    contexto.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    public class ViewHolderComputador extends RecyclerView.ViewHolder {
        public TextView mTvIdComputador;
        public TextView mTvNomeComputador;
        public TextView mTvInfoComputador;
        public TextView mTvLocalComputador;
        public ImageView mImgStatusComputador;
        public ImageView mImgIconComputador;

        public ViewHolderComputador(final View itemView) {
            super(itemView);
            mTvIdComputador = itemView.findViewById(R.id.TvIdComputador);
            mTvNomeComputador = itemView.findViewById(R.id.TvNomeComputador);
            mTvInfoComputador = itemView.findViewById(R.id.TvInfoComputador);
            mTvLocalComputador = itemView.findViewById(R.id.TvLocalComputador);
            mImgStatusComputador = itemView.findViewById(R.id.ImgStatusComputador);
            mImgIconComputador = itemView.findViewById(R.id.ImgIconComputador);
        }
    }
}
