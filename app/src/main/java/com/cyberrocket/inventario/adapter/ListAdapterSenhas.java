package com.cyberrocket.inventario.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.SenhaItem;

import java.util.ArrayList;

public class ListAdapterSenhas extends RecyclerView.Adapter<ListAdapterSenhas.ViewHolderSenha> {

    private ArrayList<SenhaItem> dados;
    private Context contexto;
    private OnSenhaInteractionListener listener;

    public interface OnSenhaInteractionListener {
        void onDeleteClick(int position, SenhaItem item);
    }

    public ListAdapterSenhas(ArrayList<SenhaItem> dados, Context contexto, OnSenhaInteractionListener listener) {
        this.dados = dados;
        this.contexto = contexto;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolderSenha onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.senha_line_view, parent, false);
        return new ViewHolderSenha(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderSenha holder, int position) {
        if (dados != null && dados.size() > 0) {
            SenhaItem senhaItem = dados.get(position);

            holder.mTvDescricaoSenha.setText(senhaItem.getDescricao() != null ? senhaItem.getDescricao() : "");
            holder.mTvValorSenha.setText(senhaItem.getSenha() != null ? senhaItem.getSenha() : "");
            
            // Mascarar senha por padrão
            holder.mTvValorSenha.setTransformationMethod(PasswordTransformationMethod.getInstance());
            holder.senhaVisivel = false;

            holder.mBtnVerSenha.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.senhaVisivel) {
                        holder.mTvValorSenha.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        holder.senhaVisivel = false;
                    } else {
                        holder.mTvValorSenha.setTransformationMethod(null);
                        holder.senhaVisivel = true;
                    }
                }
            });

            holder.mBtnCopiarSenha.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) contexto.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Senha Copiada", senhaItem.getSenha());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(contexto, "Senha copiada para a área de transferência", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            holder.mBtnRemoverSenha.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDeleteClick(position, senhaItem);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    public class ViewHolderSenha extends RecyclerView.ViewHolder {
        public TextView mTvDescricaoSenha;
        public TextView mTvValorSenha;
        public ImageButton mBtnCopiarSenha;
        public ImageButton mBtnVerSenha;
        public ImageButton mBtnRemoverSenha;
        public boolean senhaVisivel = false;

        public ViewHolderSenha(final View itemView) {
            super(itemView);
            mTvDescricaoSenha = itemView.findViewById(R.id.TvDescricaoSenha);
            mTvValorSenha = itemView.findViewById(R.id.TvValorSenha);
            mBtnCopiarSenha = itemView.findViewById(R.id.BtnCopiarSenha);
            mBtnVerSenha = itemView.findViewById(R.id.BtnVerSenha);
            mBtnRemoverSenha = itemView.findViewById(R.id.BtnRemoverSenha);
        }
    }
}
