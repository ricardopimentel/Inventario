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

import com.android.volley.Request;
import com.cyberrocket.inventario.AlterarNomeMonitorActivity;
import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.ScannerActivity;
import com.cyberrocket.inventario.lib.GLPIConnect;
import com.cyberrocket.inventario.models.MonitorLine;

import org.json.JSONException;
import org.json.JSONObject;

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
    }

    @NonNull
    @Override
    public ListAdapterMonitores.ViewHolderMonitores onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.monitores_line_view, parent, false);
        ListAdapterMonitores.ViewHolderMonitores holderMonitor = new ListAdapterMonitores.ViewHolderMonitores(view);
        return holderMonitor;
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapterMonitores.ViewHolderMonitores holder, int position) {
        if((dados!=null)&&(dados.size()>0)){
            MonitorLine monitor = dados.get(position);
            holder.mTvNome.setText(monitor.getNome());
            holder.mTvMarca.setText(monitor.getMarca());
            holder.mTvModelo.setText(monitor.getModelo());
            holder.mTvEstado.setText(monitor.getEstado());
            holder.mTvNumeroSerie.setText(monitor.getNumeroSerie());
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
        public TextView mTvNumeroSerie;
        public ImageButton mBtEditarNome;
        public ImageButton mBtEditarEstado;
        public ImageButton mBtRemover;

        public ViewHolderMonitores(final View itemView) {
            super(itemView);
            mTvNome = itemView.findViewById(R.id.TvMonitoresViewNome);
            mTvMarca = itemView.findViewById(R.id.TvMonitoresViewMarca);
            mTvModelo = itemView.findViewById(R.id.TvMonitoresViewModelo);
            mTvEstado = itemView.findViewById(R.id.TvMonitoresViewEstado);
            mTvId = itemView.findViewById(R.id.TvMonitoresViewId);
            mTvNumeroSerie = itemView.findViewById(R.id.TvMonitoresViewNumeroSerie);
            mBtEditarNome = itemView.findViewById(R.id.BtMonitoresViewEditNome);
            mBtEditarEstado = itemView.findViewById(R.id.BtMonitoresViewEditEstado);
            mBtRemover = itemView.findViewById(R.id.BtMonitoresViewRemover);


            //Listeners
            mBtEditarNome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    IrPara(AlterarNomeMonitorActivity.class, mTvId.getText().toString(), mTvNome.getText().toString());
                }
            });

            mBtEditarEstado.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    IrPara(AlterarNomeMonitorActivity.class, mTvId.getText().toString(), mTvNome.getText().toString());
                }
            });

            mBtRemover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DenvincularMonitor();
                }
            });
        }

        //Metodos
        private void IrPara(Class para, String IdMonitor, String NomeMonitor) {
            Intent it = new Intent(contexto, para);
            it.putExtra("idmonitor", IdMonitor);
            it.putExtra("nomemonitor", NomeMonitor);
            it.putExtra("idequipamento", mIdEquipamento);
            contexto.startActivity(it);
        }

        private void DenvincularMonitor() {
            JSONObject postparams = new JSONObject();
            JSONObject finalarray = new JSONObject();
            try {
                postparams.put("id", "mIdMonitor");
                postparams.put("name", "mTvNomeEquipamento.getText()");

                finalarray.put("input", postparams);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.e("sessiontoken", finalarray.toString());

            GLPIConnect con = new GLPIConnect(contexto);
            con.UpdateItem("/apirest.php/Computador/", finalarray, Request.Method.PUT, new GLPIConnect.VolleyResponseListener() {
                @Override
                public void onVolleySuccess(String url, String response) {
                    //IrPara(ScannerActivity.class);
                }

                @Override
                public void onVolleyFailure(String url) {
                    //Toast.makeText(getApplicationContext(), "Erro: "+ url, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
