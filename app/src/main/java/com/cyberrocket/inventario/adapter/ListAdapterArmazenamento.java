package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.ArmazenamentoLine;

import java.util.ArrayList;

public class ListAdapterArmazenamento extends RecyclerView.Adapter<ListAdapterArmazenamento.ViewHolderArmazenamento> {
    private ArrayList<ArmazenamentoLine> dados;
    private Context contexto;
    private String mIdEquipamento;

    public ListAdapterArmazenamento(ArrayList<ArmazenamentoLine> dados, Context contexto, String id) {
        this.dados = dados;
        this.contexto = contexto;
        mIdEquipamento = id;
    }

    @NonNull
    @Override
    public ViewHolderArmazenamento onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.armazenamento_line_view, parent, false);
        return new ViewHolderArmazenamento(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderArmazenamento holder, int position) {
        if (dados != null && dados.size() > 0) {
            ArmazenamentoLine disk = dados.get(position);
            holder.mTvDiskName.setText(disk.getNome());
            holder.mTvDiskCapacity.setText(disk.getCapacidade());
            holder.mTvDiskType.setText(disk.getTipo());
            
            holder.mPbDiskUsage.setProgress(disk.getUsagePercentage());
            holder.mTvDiskStatusLabel.setText(disk.getUsagePercentage() + "% Usado");
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    public class ViewHolderArmazenamento extends RecyclerView.ViewHolder {
        public TextView mTvDiskName;
        public TextView mTvDiskCapacity;
        public TextView mTvDiskType;
        public ProgressBar mPbDiskUsage;
        public TextView mTvDiskStatusLabel;

        public ViewHolderArmazenamento(View itemView) {
            super(itemView);
            mTvDiskName = itemView.findViewById(R.id.TvDiskName);
            mTvDiskCapacity = itemView.findViewById(R.id.TvDiskCapacity);
            mTvDiskType = itemView.findViewById(R.id.TvDiskType);
            mPbDiskUsage = itemView.findViewById(R.id.PbDiskUsage);
            mTvDiskStatusLabel = itemView.findViewById(R.id.TvDiskStatusLabel);
        }
    }
}
