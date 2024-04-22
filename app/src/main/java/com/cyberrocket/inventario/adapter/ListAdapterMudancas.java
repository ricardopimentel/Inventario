package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.FinalizarMudancaActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.MudancasLine;

import java.util.ArrayList;

public class ListAdapterMudancas extends RecyclerView.Adapter<ListAdapterMudancas.ViewHolderMudanc>{
    private ArrayList<MudancasLine> dados;
    private Context contexto;
    String mIdEquipamento;

    public ListAdapterMudancas(ArrayList<MudancasLine> dados, Context contexto, String IdEquipamento){
        this.dados = dados;
        this.contexto = contexto;
        mIdEquipamento = IdEquipamento;
    }

    @NonNull
    @Override
    public ListAdapterMudancas.ViewHolderMudanc onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.mudancas_line_view, parent, false);
        ListAdapterMudancas.ViewHolderMudanc holderEquip = new ListAdapterMudancas.ViewHolderMudanc(view);
        return holderEquip;
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapterMudancas.ViewHolderMudanc holder, int position) {
        if((dados!=null)&&(dados.size()>0)){
            MudancasLine equip = dados.get(position);
            holder.mTvTitulo.setText(equip.getTitulo());
            holder.mTvUsuarioCriacao.setText(equip.getUsuarioCriacao());
            holder.mTvDescricao.setText(equip.getTexto());
            holder.mTvDataManutencao.setText(equip.getDataManutencao());
            holder.mTvDataFinalizacao.setText(equip.getDataFinalizacao());
            holder.mTvUsuarioFinalizacao.setText(equip.getUsuarioFinalizacao());
            holder.mTvIdMudancas.setText(equip.getIdMudanca());
            holder.mImvStatus.setImageDrawable(equip.getImagemStatus().getDrawable());
            holder.mBtFinalizarManutencao.setVisibility(equip.getBtFinalizarManutencao().getVisibility());
        }

    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    //Implementa view holder
    public class ViewHolderMudanc extends RecyclerView.ViewHolder{
        public TextView mTvTitulo;
        public TextView mTvUsuarioCriacao;
        public TextView mTvDescricao;
        public TextView mTvDataManutencao;
        public TextView mTvDataFinalizacao;
        public TextView mTvUsuarioFinalizacao;
        public TextView mTvIdMudancas;
        public ImageView mImvStatus;
        public Button mBtFinalizarManutencao;

        public ViewHolderMudanc(final View itemView) {
            super(itemView);
            mTvTitulo = itemView.findViewById(R.id.TvTitulo);
            mTvUsuarioCriacao = itemView.findViewById(R.id.TvUsuarioCriacao);
            mTvDescricao = itemView.findViewById(R.id.TvDescricaoMudanca);
            mTvDataManutencao = itemView.findViewById(R.id.TvDataMudanca);
            mTvDataFinalizacao = itemView.findViewById(R.id.TvDataFinalizacao);
            mTvUsuarioFinalizacao = itemView.findViewById(R.id.TvUsuarioFinalizacao);
            mTvIdMudancas = itemView.findViewById(R.id.TvIdMudancaScanner);
            mImvStatus = itemView.findViewById(R.id.ImgImagemStatus);
            mBtFinalizarManutencao = itemView.findViewById(R.id.BtFinalizarManutencao);


            //Listeners
            mBtFinalizarManutencao.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    IrPara(FinalizarMudancaActivity.class, mTvIdMudancas.getText().toString(), mTvDescricao.getText().toString());
                }
            });
        }

        //Metodos
        private void IrPara(Class para, String IdMudanca, String Descricao) {
            Intent it = new Intent(contexto, para);
            it.putExtra("idmudanca", IdMudanca);
            it.putExtra("descricaomudanca", Descricao);
            it.putExtra("idequipamento", mIdEquipamento);
            contexto.startActivity(it);
        }
    }
}