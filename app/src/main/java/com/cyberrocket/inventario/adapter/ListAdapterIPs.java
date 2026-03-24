package com.cyberrocket.inventario.adapter;

import android.content.Context;
import android.content.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.IPLine;

import java.util.List;

public class ListAdapterIPs extends RecyclerView.Adapter<ListAdapterIPs.ViewHolder> {

    private List<IPLine> mData;
    private LayoutInflater mInflater;
    private Context context;

    public ListAdapterIPs(List<IPLine> itemList, Context context) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.mData = itemList;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.ip_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        IPLine item = mData.get(position);
        holder.ipText.setText(item.getIp());
        holder.icon.setImageResource(item.getIconResId());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView ipText;
        ImageButton mBtCopiar;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ImgIpIcon);
            ipText = itemView.findViewById(R.id.TvIpAddress);
            mBtCopiar = itemView.findViewById(R.id.BtCopiarIPLine);

            mBtCopiar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(ipText.getText().toString());
                    Toast.makeText(context, ipText.getText().toString() + " copiado", Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.content.Intent intent = new android.content.Intent(context, com.cyberrocket.inventario.PingActivity.class);
                    intent.putExtra("IP_ADDRESS", ipText.getText().toString());
                    context.startActivity(intent);
                }
            });
        }
    }
}
