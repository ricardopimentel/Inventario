package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.MonitorLine;

import java.util.ArrayList;


public class ListAdapterMonitores extends RecyclerView.Adapter<ListAdapterMonitores.ViewHolderMonitores>{
    private ArrayList<MonitorLine> dados;
    private Context contexto;
    String mIdEquipamento;

    //Construtor da classe
    public ListAdapterMonitores(ArrayList<MonitorLine> dados, Context contexto, String IdEquipamento){
        this.dados = dados;
        this.contexto = contexto;
        mIdEquipamento = IdEquipamento;
        Log.d("grub", "entrei no construtor");
    }

    @NonNull
    @Override
    public ListAdapterMonitores.ViewHolderMonitores onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.monitores_line_view, parent, false);
        ListAdapterMonitores.ViewHolderMonitores holderMonitor = new ListAdapterMonitores.ViewHolderMonitores(view);
        Log.d("grub", "entrei no adaptador");
        return holderMonitor;
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapterMonitores.ViewHolderMonitores holder, int position) {
        Log.d("grub", "entrei no adaptador1");
        if((dados!=null)&&(dados.size()>0)){
            MonitorLine monitor = dados.get(position);
            holder.mTvNome.setText(monitor.getNome());
            holder.mTvMarca.setText(monitor.getMarca());
            holder.mTvModelo.setText(monitor.getModelo());
            holder.mTvEstado.setText(monitor.getEstado());
            holder.mTvId.setText(monitor.getIdMonitor());
        }
    }

    @Override
    public int getItemCount() {
        return dados.size();
    }

    //Implementa view holder
    public class ViewHolderMonitores extends RecyclerView.ViewHolder{
        public TextView mTvNome;
        public TextView mTvMarca;
        public TextView mTvModelo;
        public TextView mTvEstado;
        public TextView mTvId;
        public ImageButton mBtEditarNome;
        public ImageButton mBtRemover;

        public ViewHolderMonitores(final View itemView) {
            super(itemView);
            mTvNome = itemView.findViewById(R.id.TvMonitoresViewNome);
            mTvMarca = itemView.findViewById(R.id.TvMonitoresViewMarca);
            mTvModelo = itemView.findViewById(R.id.TvMonitoresViewModelo);
            mTvEstado = itemView.findViewById(R.id.TvMonitoresViewEstado);
            mTvId = itemView.findViewById(R.id.TvMonitoresViewId);
            mBtEditarNome = itemView.findViewById(R.id.BtMonitoresViewEdit);
            mBtRemover = itemView.findViewById(R.id.BtMonitoresViewRemover);


            //Listeners
            mBtEditarNome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(contexto, "Não fiz essa função, to com preguiça", Toast.LENGTH_LONG).show();
                    //IrPara(FinalizarMudancaActivity.class, mTvIdMudancas.getText().toString(), mTvDescricao.getText().toString());
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
