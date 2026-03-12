package com.example.productmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeedbackAdminAdapter extends RecyclerView.Adapter<FeedbackAdminAdapter.ViewHolder> {

    public interface ReplyCallback {
        void onSaveReply(int feedbackId, String reply, int position);
    }

    private final List<Feedback> list;
    private final ReplyCallback callback;

    public FeedbackAdminAdapter(List<Feedback> list, ReplyCallback callback) {
        this.list = list;
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Feedback fb = list.get(position);

        holder.tvProductName.setText(fb.getProductName());
        holder.tvUserName.setText(fb.getUserFullName());
        holder.rbRating.setRating(fb.getRating());
        holder.tvComment.setText(fb.getComment() != null ? fb.getComment() : "");
        holder.tvDate.setText(fb.getCreatedAt() != null
                ? fb.getCreatedAt().substring(0, Math.min(10, fb.getCreatedAt().length())) : "");

        String existing = fb.getAdminReply();
        holder.edtAdminReply.setText(existing != null ? existing : "");

        holder.btnSaveReply.setOnClickListener(v -> {
            String reply = holder.edtAdminReply.getText().toString().trim();
            if (reply.isEmpty()) return;
            if (callback != null) callback.onSaveReply(fb.getFeedbackId(), reply, position);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateReply(int position, String reply) {
        if (position >= 0 && position < list.size()) {
            list.get(position).setAdminReply(reply);
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvUserName, tvComment, tvDate;
        RatingBar rbRating;
        EditText edtAdminReply;
        Button btnSaveReply;

        ViewHolder(View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvFbProductName);
            tvUserName    = itemView.findViewById(R.id.tvFbUserName);
            rbRating      = itemView.findViewById(R.id.rbFbRating);
            tvComment     = itemView.findViewById(R.id.tvFbComment);
            tvDate        = itemView.findViewById(R.id.tvFbDate);
            edtAdminReply = itemView.findViewById(R.id.edtAdminReply);
            btnSaveReply  = itemView.findViewById(R.id.btnSaveReply);
        }
    }
}
