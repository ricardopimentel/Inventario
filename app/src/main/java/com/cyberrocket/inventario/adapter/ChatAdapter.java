package com.cyberrocket.inventario.adapter;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberrocket.inventario.R;
import com.cyberrocket.inventario.models.TicketMessage;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMessageLongClickListener {
        void onMessageLongClick(TicketMessage message);
    }

    private ArrayList<TicketMessage> messages;
    private OnMessageLongClickListener longClickListener;

    public ChatAdapter(ArrayList<TicketMessage> messages, OnMessageLongClickListener longClickListener) {
        this.messages = messages;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getMessageType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TicketMessage.TYPE_TICKET) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble_ticket, parent, false);
            return new TicketViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble_reply, parent, false);
            return new ReplyViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TicketMessage message = messages.get(position);

        if (holder instanceof TicketViewHolder) {
            TicketViewHolder ticketHolder = (TicketViewHolder) holder;
            ticketHolder.tvAuthor.setText("por " + (message.getAuthorName() != null ? message.getAuthorName() : "Desconhecido"));
            ticketHolder.tvCreationDate.setText("Criado em: " + (message.getCreationDate() != null ? message.getCreationDate() : "N/D"));
            
            if (message.getAuthorName() != null && !message.getAuthorName().isEmpty()) {
                String[] parts = message.getAuthorName().split(" ");
                String initials = parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1) {
                    initials += parts[parts.length - 1].substring(0, 1).toUpperCase();
                }
                ticketHolder.tvAvatar.setText(initials);
            } else {
                ticketHolder.tvAvatar.setText("?");
            }

            if (message.getContent() != null) {
                ticketHolder.tvContent.setText(parseHtmlWithoutColors(message.getContent()));
            } else {
                ticketHolder.tvContent.setText("");
            }

            ticketHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(message);
                    }
                    return true;
                }
            });

        } else if (holder instanceof ReplyViewHolder) {
            ReplyViewHolder replyHolder = (ReplyViewHolder) holder;
            replyHolder.tvAuthor.setText(message.getAuthorName() != null ? message.getAuthorName() : "Desconhecido");
            replyHolder.tvCreationDate.setText("Criado em: " + (message.getCreationDate() != null ? message.getCreationDate() : "N/D"));
            
            if (message.getAuthorName() != null && !message.getAuthorName().isEmpty()) {
                String[] parts = message.getAuthorName().split(" ");
                String initials = parts[0].substring(0, 1).toUpperCase();
                if (parts.length > 1) {
                    initials += parts[parts.length - 1].substring(0, 1).toUpperCase();
                }
                replyHolder.tvAvatar.setText(initials);
            } else {
                replyHolder.tvAvatar.setText("?");
            }

            if (message.getContent() != null) {
                replyHolder.tvContent.setText(parseHtmlWithoutColors(message.getContent()));
            } else {
                replyHolder.tvContent.setText("");
            }

            replyHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(message);
                    }
                    return true;
                }
            });
        }
    }

    private CharSequence parseHtmlWithoutColors(String htmlString) {
        if (htmlString == null) return "";

        Spanned spanned;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT);
        } else {
            spanned = Html.fromHtml(htmlString);
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(spanned);
        ForegroundColorSpan[] fgSpans = ssb.getSpans(0, ssb.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : fgSpans) {
            ssb.removeSpan(span);
        }

        BackgroundColorSpan[] bgSpans = ssb.getSpans(0, ssb.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : bgSpans) {
            ssb.removeSpan(span);
        }

        return ssb;
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        TextView tvCreationDate;
        TextView tvAuthor;
        TextView tvContent;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvCreationDate = itemView.findViewById(R.id.tvCreationDate);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        TextView tvCreationDate;
        TextView tvAuthor;
        TextView tvContent;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvCreationDate = itemView.findViewById(R.id.tvCreationDate);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }
}
