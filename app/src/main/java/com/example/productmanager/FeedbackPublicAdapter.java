package com.example.productmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedbackPublicAdapter extends RecyclerView.Adapter<FeedbackPublicAdapter.ViewHolder> {

    private final List<Feedback> list;

    public FeedbackPublicAdapter(List<Feedback> list) {
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback_public, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Feedback fb = list.get(position);

        holder.tvUser.setText(fb.getUserFullName() != null ? fb.getUserFullName() : "Ẩn danh");
        holder.tvDate.setText(fb.getCreatedAt() != null
                ? fb.getCreatedAt().substring(0, Math.min(10, fb.getCreatedAt().length())) : "");
        holder.rbRating.setRating(fb.getRating());
        holder.tvComment.setText(fb.getComment() != null ? fb.getComment() : "");

        String adminReply = fb.getAdminReply();
        if (adminReply != null && !adminReply.isEmpty()) {
            holder.layoutAdminReply.setVisibility(View.VISIBLE);
            holder.tvAdminReply.setText(adminReply);
        } else {
            holder.layoutAdminReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvDate, tvComment, tvAdminReply;
        RatingBar rbRating;
        LinearLayout layoutAdminReply;

        ViewHolder(View v) {
            super(v);
            tvUser           = v.findViewById(R.id.tvFbPublicUser);
            tvDate           = v.findViewById(R.id.tvFbPublicDate);
            rbRating         = v.findViewById(R.id.rbFbPublic);
            tvComment        = v.findViewById(R.id.tvFbPublicComment);
            layoutAdminReply = v.findViewById(R.id.layoutFbPublicAdminReply);
            tvAdminReply     = v.findViewById(R.id.tvFbPublicAdminReply);
        }
    }
}
