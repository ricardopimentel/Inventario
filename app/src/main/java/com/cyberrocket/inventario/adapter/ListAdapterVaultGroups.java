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
        public String fullName;
        public String type; // "Computer" or "Location"
        public String computerTypeId; // ID of the computer type (Desktop, etc.)

        public VaultItem(String id, String name, String fullName, String type, String computerTypeId) {
            this.id = id;
            this.name = name;
            this.fullName = fullName;
            this.type = type;
            this.computerTypeId = computerTypeId;
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
        View view = LayoutInflater.from(contexto).inflate(R.layout.vault_item_view, parent, false);
        return new ViewHolderVault(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderVault holder, int position) {
        VaultItem item = dados.get(position);
        holder.mTvNome.setText(item.name);
        holder.mTvId.setText("ID: " + item.id);
        holder.mTvFullName.setText(item.fullName);

        int color;
        if (item.type.equals("Computer")) {
            holder.mImvIcon.setImageResource(R.drawable.ic_computer_24dp);
            color = contexto.getResources().getColor(R.color.vault_color_computer, null);
        } else {
            holder.mImvIcon.setImageResource(R.drawable.ic_place_24dp);
            color = contexto.getResources().getColor(R.color.vault_color_location, null);
        }

        holder.mViewBg.getBackground().mutate().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        holder.mImvIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

        holder.itemView.setOnClickListener(v -> {
            helper.showPasswordDialog(item.type, item.id, listener);
        });
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    public class ViewHolderVault extends RecyclerView.ViewHolder {
        public TextView mTvNome;
        public TextView mTvId;
        public TextView mTvFullName;
        public ImageView mImvIcon;
        public View mViewBg;

        public ViewHolderVault(View itemView) {
            super(itemView);
            mTvNome = itemView.findViewById(R.id.TvVaultName);
            mTvId = itemView.findViewById(R.id.TvVaultId);
            mTvFullName = itemView.findViewById(R.id.TvVaultFullName);
            mImvIcon = itemView.findViewById(R.id.ImgVaultIcon);
            mViewBg = itemView.findViewById(R.id.ViewIconBg);
        }
    }
}
