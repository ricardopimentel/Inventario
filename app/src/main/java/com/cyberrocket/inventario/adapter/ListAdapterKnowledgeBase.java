package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.KnowbaseItem;
import com.cyberrocket.inventario.ui.knowledgebase.KnowbaseDetailsActivity;

import java.util.ArrayList;

public class ListAdapterKnowledgeBase extends RecyclerView.Adapter<ListAdapterKnowledgeBase.ViewHolderKB> {

    private ArrayList<KnowbaseItem> dados;
    private Context contexto;

    public ListAdapterKnowledgeBase(ArrayList<KnowbaseItem> dados, Context contexto) {
        this.dados = dados;
        this.contexto = contexto;
    }

    @NonNull
    @Override
    public ViewHolderKB onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.knowbase_line_view, parent, false);
        return new ViewHolderKB(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderKB holder, int position) {
        if (dados != null && dados.size() > 0) {
            KnowbaseItem item = dados.get(position);
            holder.mTvTitulo.setText(item.getName() != null ? item.getName() : "Sem Título");
            holder.mTvData.setText(item.getDateMod() != null ? "Modificado em: " + item.getDateMod() : "");
            holder.mTvId.setText(item.getId() != null ? "#" + item.getId() : "");
            holder.mTvCategoria.setText(item.getCategoryName() != null ? "Categoria: " + item.getCategoryName() : "Sem Categoria");

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(contexto, KnowbaseDetailsActivity.class);
                    intent.putExtra("id", item.getId());
                    intent.putExtra("name", item.getName());
                    intent.putExtra("content", item.getContent());
                    intent.putExtra("date_mod", item.getDateMod());
                    contexto.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    public class ViewHolderKB extends RecyclerView.ViewHolder {
        public TextView mTvTitulo;
        public TextView mTvId;
        public TextView mTvData;
        public TextView mTvCategoria;

        public ViewHolderKB(final View itemView) {
            super(itemView);
            mTvTitulo = itemView.findViewById(R.id.TvTituloKB);
            mTvId = itemView.findViewById(R.id.TvIdKB);
            mTvData = itemView.findViewById(R.id.TvDataKB);
            mTvCategoria = itemView.findViewById(R.id.TvCategoriaKB);
        }
    }
}
