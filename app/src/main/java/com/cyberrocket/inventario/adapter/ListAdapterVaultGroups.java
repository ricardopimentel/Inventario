package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.lib.PasswordVaultHelper;

import java.util.ArrayList;

public class ListAdapterVaultGroups extends RecyclerView.Adapter<ListAdapterVaultGroups.ViewHolderVault> {

    public static class VaultItem {
        public String id;
        public String name;
        public String type; // "Computer" or "Location"

        public VaultItem(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    private ArrayList<VaultItem> dados;
    private Context contexto;
    private PasswordVaultHelper helper;
    private PasswordVaultHelper.VaultOperationListener listener;

    public ListAdapterVaultGroups(ArrayList<VaultItem> dados, Context contexto, PasswordVaultHelper helper, PasswordVaultHelper.VaultOperationListener listener) {
        this.dados = dados;
        this.contexto = contexto;
        this.helper = helper;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolderVault onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.computador_line_view, parent, false);
        return new ViewHolderVault(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderVault holder, int position) {
        VaultItem item = dados.get(position);
        holder.mTvNome.setText(item.name != null ? item.name : "Sem Nome");
        holder.mTvId.setText("ID: " + item.id);
        
        if (item.type.equals("Computer")) {
            holder.mImvStatus.setImageResource(R.drawable.ic_computer_24dp);
        } else {
            holder.mImvStatus.setImageResource(R.drawable.baseline_change_circle_24); // Representing location
        }

        holder.itemView.setOnClickListener(v -> {
            helper.showPasswordDialog(item.type, item.id, listener);
        });
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    public class ViewHolderVault extends RecyclerView.ViewHolder {
        public TextView mTvNome;
        public TextView mTvId;
        public ImageView mImvStatus;

        public ViewHolderVault(View itemView) {
            super(itemView);
            mTvNome = itemView.findViewById(R.id.TvNomeComputador);
            mTvId = itemView.findViewById(R.id.TvIdComputador);
            mImvStatus = itemView.findViewById(R.id.ImgIconComputador);
            
            // Hide elements not needed from computador_line_view
            itemView.findViewById(R.id.TvSerialComputador).setVisibility(View.GONE);
            itemView.findViewById(R.id.TvInfoComputador).setVisibility(View.GONE);
            itemView.findViewById(R.id.TvLocalComputador).setVisibility(View.GONE);
            itemView.findViewById(R.id.ImgStatusComputador).setVisibility(View.GONE);
            
            mTvId.setVisibility(View.VISIBLE); // It was GONE in XML
        }
    }
}
