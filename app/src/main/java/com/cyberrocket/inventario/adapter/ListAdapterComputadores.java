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
    private boolean isSelectionMode = false;
    private OnSelectionChangeListener selectionChangeListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    public ListAdapterComputadores(ArrayList<Computador> dados, Context contexto) {
        this.dados = dados;
        this.contexto = contexto;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        if (!selectionMode) {
            for (Computador c : dados) {
                c.setSelected(false);
            }
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        if (!isSelectionMode) return;
        boolean allSelected = true;
        for (Computador c : dados) {
            if (!c.isSelected()) {
                allSelected = false;
                break;
            }
        }
        
        int count = 0;
        for (Computador c : dados) {
            c.setSelected(!allSelected);
            if (!allSelected) count++;
        }
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(count);
        }
    }

    public ArrayList<Computador> getSelectedItems() {
        ArrayList<Computador> selectedItems = new ArrayList<>();
        if (dados != null) {
            for (Computador c : dados) {
                if (c.isSelected()) {
                    selectedItems.add(c);
                }
            }
        }
        return selectedItems;
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
            
            String serialId = computador.getSerial() != null && !computador.getSerial().isEmpty() ? computador.getSerial() : "SN: Indefinido";
            holder.mTvSerialComputador.setText(serialId);
            
            String marca = computador.getFabricante() != null ? computador.getFabricante() : "";
            String tipo = computador.getTipo() != null ? computador.getTipo() : "";
            String info = marca;
            if (!tipo.isEmpty()) {
                if (!info.isEmpty()) info += " | ";
                info += tipo;
            }
            holder.mTvInfoComputador.setText(info);
            
            holder.mTvLocalComputador.setText(computador.getLocalizacao() != null ? computador.getLocalizacao() : "");
            
            // Dynamic Icon Selection purely based on Tipo from GLPI
            String tipoLower = (computador.getTipo() != null ? computador.getTipo() : "").toLowerCase();
            int iconColor;
            if (tipoLower.contains("monitor")) {
                holder.mImgIconComputador.setImageResource(R.drawable.monitor_24);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_monitor_pink); // Pink for monitors
            } else if (tipoLower.contains("notebook")) {
                holder.mImgIconComputador.setImageResource(R.drawable.ic_laptop_24dp);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_maintenance_green); // Green for laptops
            } else if (tipoLower.contains("servidor virtual")) {
                holder.mImgIconComputador.setImageResource(R.drawable.desktop_cloud_stack);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_virtual_server_red); // Red for Virtual Servers
            } else if (tipoLower.contains("servidor f") || tipoLower.equals("servidor")) {
                holder.mImgIconComputador.setImageResource(R.drawable.desktop_tower);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_server_tower_indigo); // Indigo for Physical Servers
            } else if (tipoLower.contains("maquina virtual") || tipoLower.contains("máquina virtual") || tipoLower.contains("vmware")) {
                holder.mImgIconComputador.setImageResource(R.drawable.ic_virtual_machine_24dp);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_storage_cyan); // Cyan for VMs
            } else if (tipoLower.contains("mini pc")) {
                holder.mImgIconComputador.setImageResource(R.drawable.desktop_classic);
                iconColor = androidx.core.content.ContextCompat.getColor(contexto, R.color.color_minipc_teal); // Teal for Mini PCs
            } else {
                // Fallback / Desktop
                holder.mImgIconComputador.setImageResource(R.drawable.desktop_tower_monitor);
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

            if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView;
                card.setCheckable(true);
                card.setChecked(computador.isSelected());
            } else {
                if (computador.isSelected()) {
                    holder.itemView.setBackgroundColor(Color.parseColor("#33000000"));
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                }
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isSelectionMode) {
                        computador.setSelected(!computador.isSelected());
                        notifyItemChanged(position);
                        
                        int selectedCount = getSelectedItems().size();
                        if (selectedCount == 0) {
                            setSelectionMode(false);
                        } else if (selectionChangeListener != null) {
                            selectionChangeListener.onSelectionChanged(selectedCount);
                        }
                    } else {
                        Intent intent = new Intent(contexto, ScannerActivity.class);
                        intent.putExtra("id", computador.getId());
                        String itemType = tipoLower.contains("monitor") ? "Monitor" : "Computer";
                        intent.putExtra("item_type", itemType);
                        contexto.startActivity(intent);
                    }
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!isSelectionMode) {
                        isSelectionMode = true;
                        computador.setSelected(true);
                        notifyItemChanged(position);
                        if (selectionChangeListener != null) {
                            selectionChangeListener.onSelectionChanged(1);
                        }
                        return true;
                    }
                    return false;
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
        public TextView mTvSerialComputador;
        public TextView mTvInfoComputador;
        public TextView mTvLocalComputador;
        public ImageView mImgStatusComputador;
        public ImageView mImgIconComputador;

        public ViewHolderComputador(final View itemView) {
            super(itemView);
            mTvIdComputador = itemView.findViewById(R.id.TvIdComputador);
            mTvNomeComputador = itemView.findViewById(R.id.TvNomeComputador);
            mTvSerialComputador = itemView.findViewById(R.id.TvSerialComputador);
            mTvInfoComputador = itemView.findViewById(R.id.TvInfoComputador);
            mTvLocalComputador = itemView.findViewById(R.id.TvLocalComputador);
            mImgStatusComputador = itemView.findViewById(R.id.ImgStatusComputador);
            mImgIconComputador = itemView.findViewById(R.id.ImgIconComputador);
        }
    }
}
