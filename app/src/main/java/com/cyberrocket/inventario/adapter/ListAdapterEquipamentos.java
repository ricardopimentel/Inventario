package com.cyberrocket.inventario.adapter;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.AlterarLocalActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.EquipamentoLine;

import java.util.ArrayList;

public class ListAdapterEquipamentos extends RecyclerView.Adapter<ListAdapterEquipamentos.ViewHolderEquip>{
    private ArrayList<EquipamentoLine> dados;
    private Context contexto;
    private String mIdEquipamento;

    public ListAdapterEquipamentos(ArrayList<EquipamentoLine> dados, Context contexto, String id){
        this.dados = dados;
        this.contexto = contexto;
        mIdEquipamento = id;
    }

    @NonNull
    @Override
    public ViewHolderEquip onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.equipamento_line_view, parent, false);
        ViewHolderEquip holderEquip = new ViewHolderEquip(view);
        return holderEquip;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderEquip holder, int position) {
        if((dados!=null)&&(dados.size()>0)){
            EquipamentoLine equip = dados.get(position);
            holder.mTvDescricao.setText(equip.getDescricao());
            holder.mTvConteudo.setText(equip.getConteudo());
            holder.mBtEditar.setVisibility(equip.getBtEditar());
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    //Implementa view holder
    public class ViewHolderEquip extends RecyclerView.ViewHolder{
        public TextView mTvDescricao;
        public TextView mTvConteudo;
        public ImageButton mBtEditar;
        public ImageButton mBtCopiar;

        public ViewHolderEquip(final View itemView) {
            super(itemView);
            //Inicialização
            mTvDescricao = itemView.findViewById(R.id.tvDescricao);
            mTvConteudo = itemView.findViewById(R.id.tvConteudo);
            mBtEditar = itemView.findViewById(R.id.BtEditarEquipamentoLine);
            mBtCopiar = itemView.findViewById(R.id.BtCopiarEquipamentoLine);

            //Listeners
            mBtEditar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    IrPara(AlterarLocalActivity.class);
                }
            });

            mBtCopiar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager Copiar = (ClipboardManager) contexto.getSystemService(Context.CLIPBOARD_SERVICE);
                    Copiar.setText(mTvConteudo.getText().toString());
                    Toast.makeText(contexto, mTvConteudo.getText().toString()+" copiado", Toast.LENGTH_SHORT).show();
                }
            });
        }

        //Métodos
        private void IrPara(Class para) {
            Intent intent = new Intent(contexto, para);
            intent.putExtra("idequipamento", mIdEquipamento);
            contexto.startActivity(intent);
        }
    }
}

