package com.example.productmanager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ChatUserAdapter extends RecyclerView.Adapter<ChatUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(int userId, String fullName, String avatarUrl);
    }

    private final Context context;
    private JSONArray users;
    private final OnUserClickListener listener;
    private final Map<Integer, Integer> unreadCountByUserId = new HashMap<>();

    public ChatUserAdapter(Context context, JSONArray users, OnUserClickListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
    }

    public void updateUsers(JSONArray newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public void updateUnreadCounts(Map<Integer, Integer> unreadMap) {
        unreadCountByUserId.clear();
        if (unreadMap != null) {
            unreadCountByUserId.putAll(unreadMap);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject user = users.optJSONObject(position);
        if (user == null) return;

        int userId = user.optInt("userId");
        String fullName = user.optString("fullName", "");
        String avatarUrl = user.optString("avatarUrl", null);
        String lastMessage = user.optString("lastMessage", "");
        String lastMessageTime = user.optString("lastMessageTime", "");
        int unreadCount = unreadCountByUserId.getOrDefault(userId, 0);

        holder.txtUserName.setText(fullName);
        holder.txtLastMessage.setText(lastMessage);
        holder.txtTime.setText(formatTime(lastMessageTime));
        bindUnreadBadge(holder, unreadCount);

        if (avatarUrl != null && !avatarUrl.equals("null") && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(holder.imgUserAvatar);
        } else {
            holder.imgUserAvatar.setImageResource(R.drawable.ic_avatar);
        }

        holder.itemView.setOnClickListener(v -> listener.onUserClick(userId, fullName, avatarUrl));
    }

    private void bindUnreadBadge(ViewHolder holder, int unreadCount) {
        if (unreadCount > 0) {
            holder.txtUnreadCount.setVisibility(View.VISIBLE);
            holder.txtUnreadCount.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(9));
            bg.setColor(Color.parseColor("#D32F2F"));
            holder.txtUnreadCount.setBackground(bg);
        } else {
            holder.txtUnreadCount.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatTime(String isoTime) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoTime.split("\\.")[0]);
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            displayFormat.setTimeZone(TimeZone.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.length() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUserAvatar;
        TextView txtUserName, txtLastMessage, txtTime, txtUnreadCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtUnreadCount = itemView.findViewById(R.id.txtUnreadCount);
        }
    }
}
