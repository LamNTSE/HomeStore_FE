package com.example.productmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final Context context;
    private JSONArray messages;
    private final int currentUserId;

    public ChatAdapter(Context context, JSONArray messages, int currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void updateMessages(JSONArray newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        JSONObject msg = messages.optJSONObject(position);
        if (msg == null) return;

        int senderId = msg.optInt("senderId");
        boolean isSent = (senderId == currentUserId);

        String content = msg.optString("content", "");
        String senderName = msg.optString("senderName", "");
        String senderAvatarUrl = msg.optString("senderAvatarUrl", null);
        String receiverName = msg.optString("receiverName", "");
        String receiverAvatarUrl = msg.optString("receiverAvatarUrl", null);
        String sentAt = msg.optString("sentAt", "");

        String timeFormatted = formatTime(sentAt);

        if (isSent) {
            holder.layoutSent.setVisibility(View.VISIBLE);
            holder.layoutReceived.setVisibility(View.GONE);

            holder.txtMessageSent.setText(content);
            holder.txtTimeSent.setText(timeFormatted);
            holder.txtNameSent.setText(senderName);

            // Load sender avatar (current user)
            loadAvatar(senderAvatarUrl, holder.imgAvatarSent);
        } else {
            holder.layoutReceived.setVisibility(View.VISIBLE);
            holder.layoutSent.setVisibility(View.GONE);

            holder.txtMessageReceived.setText(content);
            holder.txtTimeReceived.setText(timeFormatted);
            holder.txtNameReceived.setText(senderName);

            // Load sender avatar (the other person)
            loadAvatar(senderAvatarUrl, holder.imgAvatarReceived);
        }
    }

    private void loadAvatar(String avatarUrl, ImageView imageView) {
        if (avatarUrl != null && !avatarUrl.equals("null") && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_avatar);
        }
    }

    private String formatTime(String isoTime) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoTime.split("\\.")[0]);
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            displayFormat.setTimeZone(TimeZone.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.length() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutSent, layoutReceived;
        TextView txtMessageSent, txtTimeSent, txtNameSent;
        TextView txtMessageReceived, txtTimeReceived, txtNameReceived;
        ImageView imgAvatarSent, imgAvatarReceived;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            txtMessageSent = itemView.findViewById(R.id.txtMessageSent);
            txtTimeSent = itemView.findViewById(R.id.txtTimeSent);
            txtNameSent = itemView.findViewById(R.id.txtNameSent);
            txtMessageReceived = itemView.findViewById(R.id.txtMessageReceived);
            txtTimeReceived = itemView.findViewById(R.id.txtTimeReceived);
            txtNameReceived = itemView.findViewById(R.id.txtNameReceived);
            imgAvatarSent = itemView.findViewById(R.id.imgAvatarSent);
            imgAvatarReceived = itemView.findViewById(R.id.imgAvatarReceived);
        }
    }
}
