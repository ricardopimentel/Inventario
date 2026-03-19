package com.cyberrocket.inventario.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.ArmazenamentoLine;

import java.util.List;

public class ListAdapterArmazenamentoFisico extends RecyclerView.Adapter<ListAdapterArmazenamentoFisico.ViewHolderArmazenamentoFisico> {
    private List<ArmazenamentoLine> dados;
    private Activity activity;

    public ListAdapterArmazenamentoFisico(List<ArmazenamentoLine> dados, Activity activity) {
        this.dados = dados;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolderArmazenamentoFisico onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.armazenamento_fisico_item, parent, false);
        return new ViewHolderArmazenamentoFisico(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderArmazenamentoFisico holder, int position) {
        if (dados != null && dados.size() > 0) {
            ArmazenamentoLine disk = dados.get(position);
            holder.mTvDiskFisicoCapacity.setText(disk.getCapacidade());
            holder.mTvDiskFisicoName.setText(disk.getNome());
            holder.mTvDiskFisicoType.setText(disk.getTipo());
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    public class ViewHolderArmazenamentoFisico extends RecyclerView.ViewHolder {
        public TextView mTvDiskFisicoCapacity;
        public TextView mTvDiskFisicoName;
        public TextView mTvDiskFisicoType;

        public ViewHolderArmazenamentoFisico(View itemView) {
            super(itemView);
            mTvDiskFisicoCapacity = itemView.findViewById(R.id.TvDiskFisicoCapacity);
            mTvDiskFisicoName = itemView.findViewById(R.id.TvDiskFisicoName);
            mTvDiskFisicoType = itemView.findViewById(R.id.TvDiskFisicoType);
        }
    }
}
