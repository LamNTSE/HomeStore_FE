package com.example.productmanager;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FeedbackPublicAdapter extends RecyclerView.Adapter<FeedbackPublicAdapter.ViewHolder> {

    private final List<Feedback> list;

    public FeedbackPublicAdapter(List<Feedback> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback_public, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Feedback fb = list.get(position);

        // USER NAME
        String userName = fb.getUserFullName();
        holder.tvUser.setText(userName != null && !userName.isEmpty()
                ? userName
                : "Ẩn danh");

        // DATE (safe substring)
        String date = fb.getCreatedAt();
        if (date != null && date.length() >= 10) {
            holder.tvDate.setText(date.substring(0, 10));
        } else {
            holder.tvDate.setText("");
        }

        // RATING (read only)
        holder.rbRating.setIsIndicator(true);
        holder.rbRating.setRating(fb.getRating());

        // COMMENT
        String comment = fb.getComment();
        holder.tvComment.setText(comment != null && !comment.isEmpty()
                ? comment
                : "Không có bình luận");

        // ADMIN REPLY
        String adminReply = fb.getAdminReply();

        if (adminReply != null && !adminReply.isEmpty()) {

            holder.layoutAdminReply.setVisibility(View.VISIBLE);
            holder.tvAdminReply.setText("Admin: " + adminReply);

        } else {

            holder.layoutAdminReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // UPDATE DATA METHOD (use when reload API)
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Feedback> newList) {

        list.clear();

        if (newList != null) {
            list.addAll(newList);
        }

        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvUser;
        TextView tvDate;
        TextView tvComment;
        TextView tvAdminReply;

        RatingBar rbRating;
        LinearLayout layoutAdminReply;

        ViewHolder(View v) {

            super(v);

            tvUser = v.findViewById(R.id.tvFbPublicUser);
            tvDate = v.findViewById(R.id.tvFbPublicDate);
            rbRating = v.findViewById(R.id.rbFbPublic);
            tvComment = v.findViewById(R.id.tvFbPublicComment);

            layoutAdminReply = v.findViewById(R.id.layoutFbPublicAdminReply);
            tvAdminReply = v.findViewById(R.id.tvFbPublicAdminReply);
        }
    }
}
