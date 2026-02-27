package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.Chamado;

import java.util.ArrayList;

public class ListAdapterChamados extends RecyclerView.Adapter<ListAdapterChamados.ViewHolderChamado> {
    public interface OnChamadoInteractionListener {
        void onDeleteClick(Chamado chamado);
        void onSelectionChanged();
    }

    private ArrayList<Chamado> dados;
    private Context contexto;
    private OnChamadoInteractionListener listener;

    public ListAdapterChamados(ArrayList<Chamado> dados, Context contexto, OnChamadoInteractionListener listener) {
        this.dados = dados;
        this.contexto = contexto;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListAdapterChamados.ViewHolderChamado onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.chamados_line_view, parent, false);
        return new ViewHolderChamado(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapterChamados.ViewHolderChamado holder, int position) {
        if (dados != null && dados.size() > 0) {
            Chamado chamado = dados.get(position);
            holder.mTvTitulo.setText(chamado.getTitulo() != null ? chamado.getTitulo() : "Sem Título");
            holder.mTvDataManutencao.setText(chamado.getDataCriacao() != null ? chamado.getDataCriacao() : "");
            holder.mTvIdMudancas.setText(chamado.getId() != null ? "#" + chamado.getId() : "");
            
            if (chamado.getImagemStatus() != null && chamado.getImagemStatus().getDrawable() != null) {
                holder.mImvStatus.setImageDrawable(chamado.getImagemStatus().getDrawable());
            }

            holder.mCbSelect.setOnCheckedChangeListener(null);
            holder.mCbSelect.setChecked(chamado.isSelected());
            
            holder.mCbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    chamado.setSelected(isChecked);
                    if (listener != null) {
                        listener.onSelectionChanged();
                    }
                }
            });

            holder.mImgDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDeleteClick(chamado);
                    }
                }
            });

            // Ocultar o botão "Finalizar Manutenção" no adapter de chamados, pois não foi solicitado e 
            // a interface de chamados não precisa desse botão inicialmente.
            holder.mBtFinalizarManutencao.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(contexto, com.cyberrocket.inventario.ui.tickets.TicketDetailsActivity.class);
                    intent.putExtra("id", chamado.getId());
                    intent.putExtra("title", chamado.getTitulo());
                    intent.putExtra("description", chamado.getDescricao());
                    intent.putExtra("creationDate", chamado.getDataCriacao());
                    intent.putExtra("closingDate", chamado.getDataFechamento());
                    intent.putExtra("requester", chamado.getUsuarioRequerente());
                    intent.putExtra("assigned", chamado.getUsuarioAtribuido());
                    intent.putExtra("type", chamado.getTipo());
                    intent.putExtra("status", chamado.getStatusInfo());
                    contexto.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    public class ViewHolderChamado extends RecyclerView.ViewHolder {
        public TextView mTvTitulo;
        public TextView mTvUsuarioCriacao;
        public TextView mTvDescricao;
        public TextView mTvDataManutencao;
        public TextView mTvDataFinalizacao;
        public TextView mTvUsuarioFinalizacao;
        public TextView mTvIdMudancas;
        public ImageView mImvStatus;
        public ImageView mImgDelete;
        public CheckBox mCbSelect;
        public Button mBtFinalizarManutencao;

        public ViewHolderChamado(final View itemView) {
            super(itemView);
            mTvTitulo = itemView.findViewById(R.id.TvTitulo);
            mTvUsuarioCriacao = itemView.findViewById(R.id.TvUsuarioCriacao);
            mTvDescricao = itemView.findViewById(R.id.TvDescricaoMudanca);
            mTvDataManutencao = itemView.findViewById(R.id.TvDataMudanca);
            mTvDataFinalizacao = itemView.findViewById(R.id.TvDataFinalizacao);
            mTvUsuarioFinalizacao = itemView.findViewById(R.id.TvUsuarioFinalizacao);
            mTvIdMudancas = itemView.findViewById(R.id.TvIdMudancaScanner);
            mImvStatus = itemView.findViewById(R.id.ImgImagemStatus);
            mImgDelete = itemView.findViewById(R.id.imgDelete);
            mCbSelect = itemView.findViewById(R.id.cbSelect);
            mBtFinalizarManutencao = itemView.findViewById(R.id.BtFinalizarManutencao);
        }
    }
}
