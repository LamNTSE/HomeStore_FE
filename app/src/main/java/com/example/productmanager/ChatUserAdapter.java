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
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

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
        if (holder.unreadBadge == null) {
            holder.unreadBadge = BadgeDrawable.create(context);
            holder.unreadBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
            holder.unreadBadge.setBadgeTextColor(Color.WHITE);
            holder.unreadBadge.setBadgeGravity(BadgeDrawable.TOP_END);
            holder.unreadBadge.setVerticalOffset(dpToPx(2));
            holder.unreadBadge.setHorizontalOffset(dpToPx(2));
        }

        if (unreadCount > 0) {
            holder.unreadBadge.setVisible(true);
            holder.unreadBadge.setNumber(unreadCount);
            BadgeUtils.attachBadgeDrawable(holder.unreadBadge, holder.imgUserAvatar, holder.layoutAvatarBadge);
        } else {
            holder.unreadBadge.clearNumber();
            holder.unreadBadge.setVisible(false);
            BadgeUtils.detachBadgeDrawable(holder.unreadBadge, holder.imgUserAvatar);
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
        FrameLayout layoutAvatarBadge;
        ImageView imgUserAvatar;
        TextView txtUserName, txtLastMessage, txtTime;
        BadgeDrawable unreadBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAvatarBadge = itemView.findViewById(R.id.layoutAvatarBadge);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
