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
                ticketHolder.tvContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
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
            
            bindImagesAndAttachments(holder.itemView.getContext(), message, ticketHolder.llInlineImages, ticketHolder.svAttachments, ticketHolder.llAttachmentsContainer);

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
                replyHolder.tvContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
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
            
            bindImagesAndAttachments(holder.itemView.getContext(), message, replyHolder.llInlineImages, replyHolder.svAttachments, replyHolder.llAttachmentsContainer);
        }
    }

    private CharSequence parseHtmlWithoutColors(String htmlString) {
        if (htmlString == null) return "";

        // Convert simple markdown to HTML tags
        // 1. Markdown links [text](url) -> <a href="url">text</a>
        htmlString = htmlString.replaceAll("\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)", "<a href=\"$2\">$1</a>");
        // 2. Bold **text** -> <b>text</b>
        htmlString = htmlString.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        // 3. Italic *text* -> <i>text</i>
        htmlString = htmlString.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
        // 4. Inline code `code` -> <tt>code</tt>
        htmlString = htmlString.replaceAll("`(.*?)`", "<tt>$1</tt>");

        Spanned spanned;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT);
        } else {
            spanned = Html.fromHtml(htmlString);
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(spanned);
        
        // Remove foreground/background color spans (keeps links, bold, italic intact)
        ForegroundColorSpan[] fgSpans = ssb.getSpans(0, ssb.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : fgSpans) {
            ssb.removeSpan(span);
        }

        BackgroundColorSpan[] bgSpans = ssb.getSpans(0, ssb.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : bgSpans) {
            ssb.removeSpan(span);
        }

        // Linkify raw URLs without overriding existing Spans (like HTML link tags)
        android.text.util.Linkify.addLinks(ssb, android.text.util.Linkify.WEB_URLS);

        return ssb;
    }

    private void bindImagesAndAttachments(android.content.Context context, TicketMessage message, 
                                          android.widget.LinearLayout llInlineImages, 
                                          android.widget.HorizontalScrollView svAttachments, 
                                          android.widget.LinearLayout llAttachmentsContainer) {
        llInlineImages.removeAllViews();
        llAttachmentsContainer.removeAllViews();
        
        com.cyberrocket.inventario.lib.Crud crud = new com.cyberrocket.inventario.lib.Crud();
        String sessionToken = crud.SelectItem(context, "CONFIG", 1, 2);
        String baseUrl = crud.SelectItem(context, "CONFIG", 1, 1);
        
        // Inline Images
        if (message.getInlineImages() != null && !message.getInlineImages().isEmpty()) {
            llInlineImages.setVisibility(View.VISIBLE);
            for (String url : message.getInlineImages()) {
                if(!url.startsWith("http")) url = baseUrl + url;
                
                android.widget.ImageView iv = new android.widget.ImageView(context);
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 16, 0, 16);
                iv.setLayoutParams(params);
                iv.setAdjustViewBounds(true);
                iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                
                llInlineImages.addView(iv);
                loadImageSecurely(context, url, sessionToken, iv, null);
            }
        } else {
            llInlineImages.setVisibility(View.GONE);
        }
        
        // Attachments Carousel
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            svAttachments.setVisibility(View.VISIBLE);
            for (com.cyberrocket.inventario.models.Attachment attachment : message.getAttachments()) {
                View carouselItem = LayoutInflater.from(context).inflate(R.layout.item_attachment_carousel, llAttachmentsContainer, false);
                android.widget.ImageView ivThumb = carouselItem.findViewById(R.id.ivAttachmentImage);
                TextView tvName = carouselItem.findViewById(R.id.tvAttachmentName);
                
                tvName.setText(attachment.getFilename());
                
                String attUrl = attachment.getUrl();
                if(!attUrl.startsWith("http")) attUrl = baseUrl + attUrl;
                
                if (attachment.getMimeType() != null && attachment.getMimeType().startsWith("image/")) {
                    loadImageSecurely(context, attUrl, sessionToken, ivThumb, tvName);
                } else {
                    ivThumb.setImageResource(android.R.drawable.ic_menu_agenda); // generic file icon
                }
                
                // Click listener to open FullScreen viewer (only for images usually, but we can try for all)
                String finalUrl = attUrl;
                carouselItem.setOnClickListener(v -> {
                    if(attachment.getMimeType() != null && attachment.getMimeType().startsWith("image/")) {
                        showFullScreenImage(context, finalUrl, sessionToken);
                    } else {
                        android.widget.Toast.makeText(context, "Apenas anexos de imagem podem ser visualizados na tela cheia", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
                
                llAttachmentsContainer.addView(carouselItem);
            }
        } else {
            svAttachments.setVisibility(View.GONE);
        }
    }
    
    private void loadImageSecurely(android.content.Context context, String url, String sessionToken, android.widget.ImageView imageView, TextView errorView) {
        com.android.volley.toolbox.ImageRequest request = new com.android.volley.toolbox.ImageRequest(url,
                new com.android.volley.Response.Listener<android.graphics.Bitmap>() {
                    @Override
                    public void onResponse(android.graphics.Bitmap response) {
                        imageView.setImageBitmap(response);
                    }
                }, 0, 0, android.widget.ImageView.ScaleType.CENTER_INSIDE, android.graphics.Bitmap.Config.RGB_565,
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(com.android.volley.VolleyError error) {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                        String errMsg = error != null ? error.toString() : "null";
                        if (error != null && error.networkResponse != null) {
                            errMsg += " Code: " + error.networkResponse.statusCode;
                        }
                        android.util.Log.e("VolleyImageError", "Failed URL: " + url + " Error: " + errMsg);
                        
                        // Show error on the screen for debugging
                        if (errorView != null) {
                            errorView.setText("Erro: " + errMsg);
                        } else {
                            android.widget.Toast.makeText(context, "Erro IMG: " + errMsg, android.widget.Toast.LENGTH_LONG).show();
                        }
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Session-Token", sessionToken);
                // GLPI sometimes requires App-Token format, but if it worked for other queries, it's fine.
                // Let's explicitly accept octet-stream to avoid GLPI returning JSON metadata
                headers.put("Accept", "application/octet-stream");
                return headers;
            }
        };
        com.android.volley.toolbox.Volley.newRequestQueue(context).add(request);
    }
    
    private void showFullScreenImage(android.content.Context context, String url, String sessionToken) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        
        android.widget.ImageView ivFull = dialog.findViewById(R.id.ivFullScreenImage);
        android.widget.ImageButton btnClose = dialog.findViewById(R.id.btnCloseFullScreen);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        loadImageSecurely(context, url, sessionToken, ivFull, null);
        
        dialog.show();
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
        android.widget.LinearLayout llInlineImages;
        android.widget.HorizontalScrollView svAttachments;
        android.widget.LinearLayout llAttachmentsContainer;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvCreationDate = itemView.findViewById(R.id.tvCreationDate);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            llInlineImages = itemView.findViewById(R.id.llInlineImages);
            svAttachments = itemView.findViewById(R.id.svAttachments);
            llAttachmentsContainer = itemView.findViewById(R.id.llAttachmentsContainer);
        }
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        TextView tvCreationDate;
        TextView tvAuthor;
        TextView tvContent;
        android.widget.LinearLayout llInlineImages;
        android.widget.HorizontalScrollView svAttachments;
        android.widget.LinearLayout llAttachmentsContainer;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvCreationDate = itemView.findViewById(R.id.tvCreationDate);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            llInlineImages = itemView.findViewById(R.id.llInlineImages);
            svAttachments = itemView.findViewById(R.id.svAttachments);
            llAttachmentsContainer = itemView.findViewById(R.id.llAttachmentsContainer);
        }
    }
}
