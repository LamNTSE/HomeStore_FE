package com.example.productmanager;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteFeedbackActivity extends AppCompatActivity {

    private RecyclerView rvWriteFeedback;
    private TextView tvEmpty;
    private WriteFeedbackAdapter adapter;
    private List<FeedbackItem> itemList = new ArrayList<>();

    // Represents one product in the order with its current feedback state
    static class FeedbackItem {
        OrderItem orderItem;
        Feedback existing; // null = not yet reviewed

        FeedbackItem(OrderItem orderItem, Feedback existing) {
            this.orderItem = orderItem;
            this.existing = existing;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_feedback);

        ImageView btnBack = findViewById(R.id.btnBackWriteFeedback);
        rvWriteFeedback = findViewById(R.id.rvWriteFeedback);
        tvEmpty = findViewById(R.id.tvEmptyWriteFeedback);

        btnBack.setOnClickListener(v -> finish());

        int orderId = getIntent().getIntExtra("orderId", -1);
        if (orderId < 0) {
            finish();
            return;
        }

        adapter = new WriteFeedbackAdapter(this, itemList, (item, rating, comment, position) -> {
            String token = SessionManager.getToken(this);
            if (item.existing == null) {
                ApiClient.createFeedback(this, token, item.orderItem.getProductId(), rating, comment,
                        new ApiClient.DataCallback<Feedback>() {
                            @Override
                            public void onSuccess(Feedback fb, String message) {
                                item.existing = fb;
                                adapter.notifyItemChanged(position);
                                Toast.makeText(WriteFeedbackActivity.this, message, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(WriteFeedbackActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                ApiClient.updateFeedback(this, token, item.existing.getFeedbackId(), rating, comment,
                        new ApiClient.DataCallback<Feedback>() {
                            @Override
                            public void onSuccess(Feedback fb, String message) {
                                item.existing = fb;
                                adapter.notifyItemChanged(position);
                                Toast.makeText(WriteFeedbackActivity.this, message, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(WriteFeedbackActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        rvWriteFeedback.setLayoutManager(new LinearLayoutManager(this));
        rvWriteFeedback.setAdapter(adapter);

        loadData(orderId);
    }

    private void loadData(int orderId) {
        String token = SessionManager.getToken(this);

        // Load order items and my feedbacks in parallel via nesting
        ApiClient.getOrderById(this, token, orderId, new ApiClient.DataCallback<List<OrderItem>>() {
            @Override
            public void onSuccess(List<OrderItem> orderItems, String msg) {
                ApiClient.getMyFeedbacks(WriteFeedbackActivity.this, token,
                        new ApiClient.DataCallback<List<Feedback>>() {
                            @Override
                            public void onSuccess(List<Feedback> feedbacks, String m) {
                                // Map productId -> Feedback
                                Map<Integer, Feedback> fbMap = new HashMap<>();
                                for (Feedback fb : feedbacks) fbMap.put(fb.getProductId(), fb);

                                itemList.clear();
                                for (OrderItem oi : orderItems) {
                                    itemList.add(new FeedbackItem(oi, fbMap.get(oi.getProductId())));
                                }
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(itemList.isEmpty() ? View.VISIBLE : View.GONE);
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(WriteFeedbackActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(WriteFeedbackActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -----------------------------------------------------------------------
    // Inner adapter
    // -----------------------------------------------------------------------

    interface SaveCallback {
        void onSave(FeedbackItem item, int rating, String comment, int position);
    }

    static class WriteFeedbackAdapter extends RecyclerView.Adapter<WriteFeedbackAdapter.VH> {

        private final Context context;
        private final List<FeedbackItem> list;
        private final SaveCallback callback;

        WriteFeedbackAdapter(Context context, List<FeedbackItem> list, SaveCallback callback) {
            this.context = context;
            this.list = list;
            this.callback = callback;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_write_feedback, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            FeedbackItem item = list.get(position);
            OrderItem oi = item.orderItem;

            holder.tvProductName.setText(oi.getProductName());

            if (oi.getImageUrl() != null && !oi.getImageUrl().isEmpty()) {
                Glide.with(context).load(oi.getImageUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.mipmap.ic_launcher);
            }

            if (item.existing != null) {
                holder.rbRating.setRating(item.existing.getRating());
                holder.edtComment.setText(item.existing.getComment() != null ? item.existing.getComment() : "");
                holder.btnSave.setText("Cập nhật đánh giá");

                String adminReply = item.existing.getAdminReply();
                if (adminReply != null && !adminReply.isEmpty()) {
                    holder.layoutAdminReply.setVisibility(View.VISIBLE);
                    holder.tvAdminReplyText.setText(adminReply);
                } else {
                    holder.layoutAdminReply.setVisibility(View.GONE);
                }
            } else {
                holder.rbRating.setRating(5);
                holder.edtComment.setText("");
                holder.btnSave.setText("Lưu đánh giá");
                holder.layoutAdminReply.setVisibility(View.GONE);
            }

            holder.btnSave.setOnClickListener(v -> {
                int rating = (int) holder.rbRating.getRating();
                String comment = holder.edtComment.getText().toString().trim();
                if (rating == 0) {
                    Toast.makeText(context, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (callback != null) callback.onSave(item, rating, comment, position);
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView imgProduct;
            TextView tvProductName, tvAdminReplyText;
            RatingBar rbRating;
            EditText edtComment;
            Button btnSave;
            LinearLayout layoutAdminReply;

            VH(View v) {
                super(v);
                imgProduct       = v.findViewById(R.id.imgWriteFbProduct);
                tvProductName    = v.findViewById(R.id.tvWriteFbProductName);
                rbRating         = v.findViewById(R.id.rbWriteFbRating);
                edtComment       = v.findViewById(R.id.edtWriteFbComment);
                btnSave          = v.findViewById(R.id.btnSaveFeedback);
                layoutAdminReply = v.findViewById(R.id.layoutAdminReply);
                tvAdminReplyText = v.findViewById(R.id.tvAdminReplyText);
            }
        }
    }
}
