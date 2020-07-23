package com.cyberrocket.inventario.adapter;

import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.PlacasRedeLine;

import java.util.ArrayList;

public class ListAdapterPlacasRede extends RecyclerView.Adapter<ListAdapterPlacasRede.ViewHolderPlacasRede>{
    private ArrayList<PlacasRedeLine> dados;
    private Context contexto;

    public ListAdapterPlacasRede(ArrayList<PlacasRedeLine> dados, Context contexto){
        this.dados = dados;
        this.contexto = contexto;
    }

    @NonNull
    @Override
    public ViewHolderPlacasRede onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.placas_rede_line_view, parent, false);
        ListAdapterPlacasRede.ViewHolderPlacasRede holderPlacasRede = new ListAdapterPlacasRede.ViewHolderPlacasRede(view);
        return holderPlacasRede;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderPlacasRede holder, int position) {
        if((dados!=null)&&(dados.size()>0)){
            PlacasRedeLine placarede = dados.get(position);
            holder.mTvNome.setText(placarede.getNome());
            holder.mTvMac.setText(placarede.getMac());
            holder.mTvIp.setText(placarede.getIp());
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    //Implementa view holder
    public class ViewHolderPlacasRede extends RecyclerView.ViewHolder{
        public TextView mTvNome;
        public TextView mTvMac;
        public TextView mTvIp;
        public ImageButton mBtCopiarNome;
        public ImageButton mBtCopiarMac;
        public ImageButton mBtCopiarIp;

        public ViewHolderPlacasRede(final View itemView) {
            super(itemView);
            mTvNome = itemView.findViewById(R.id.TvNome);
            mTvMac = itemView.findViewById(R.id.TvMac);
            mTvIp = itemView.findViewById(R.id.TvIp);
            mBtCopiarNome = itemView.findViewById(R.id.BtCopiarNomePlacaRedeLine);
            mBtCopiarMac = itemView.findViewById(R.id.BtCopiarMacPlacaRedeLine);
            mBtCopiarIp = itemView.findViewById(R.id.BtCopiarIpPlacaRedeLine);

            //Listeners

            mBtCopiarNome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager Copiar = (ClipboardManager) contexto.getSystemService(Context.CLIPBOARD_SERVICE);
                    Copiar.setText(mTvNome.getText().toString());
                    Toast.makeText(contexto, mTvNome.getText().toString()+" Copiado", Toast.LENGTH_SHORT).show();
                }
            });

            mBtCopiarMac.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager Copiar = (ClipboardManager) contexto.getSystemService(Context.CLIPBOARD_SERVICE);
                    Copiar.setText(mTvMac.getText().toString());
                    Toast.makeText(contexto, mTvMac.getText().toString()+" Copiado", Toast.LENGTH_SHORT).show();
                }
            });

            mBtCopiarIp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager Copiar = (ClipboardManager) contexto.getSystemService(Context.CLIPBOARD_SERVICE);
                    Copiar.setText(mTvIp.getText().toString());
                    Toast.makeText(contexto, mTvIp.getText().toString()+" Copiado", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
