package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class WriteFeedbackActivity extends BaseCustomerActivity {

    private TextView tvEmpty;
    private WriteFeedbackAdapter adapter;
    private final List<FeedbackItem> itemList = new ArrayList<>();

    static class FeedbackItem {

        OrderItem orderItem;
        Feedback existing;

        float tempRating = 5;
        String tempComment = "";

        FeedbackItem(OrderItem orderItem, Feedback existing) {

            this.orderItem = orderItem;
            this.existing = existing;

            if (existing != null) {
                tempRating = existing.getRating();
                tempComment = existing.getComment() != null ? existing.getComment() : "";
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_feedback);

        ImageView btnBack = findViewById(R.id.btnBackWriteFeedback);
        RecyclerView rvWriteFeedback = findViewById(R.id.rvWriteFeedback);
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

                ApiClient.createFeedback(
                        this,
                        token,
                        item.orderItem.getProductId(),
                        orderId,
                        rating,
                        comment,
                        new ApiClient.DataCallback<Feedback>() {

                            @Override
                            public void onSuccess(Feedback fb, String message) {

                                item.existing = fb;
                                item.tempRating = fb.getRating();
                                item.tempComment = fb.getComment();

                                adapter.notifyItemChanged(position);

                                Toast.makeText(
                                        WriteFeedbackActivity.this,
                                        message,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onError(String error) {

                                Toast.makeText(
                                        WriteFeedbackActivity.this,
                                        error,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });

            } else {

                ApiClient.updateFeedback(
                        this,
                        token,
                        item.existing.getFeedbackId(),
                        rating,
                        comment,
                        new ApiClient.DataCallback<Feedback>() {

                            @Override
                            public void onSuccess(Feedback fb, String message) {

                                item.existing = fb;
                                item.tempRating = fb.getRating();
                                item.tempComment = fb.getComment();

                                adapter.notifyItemChanged(position);

                                Toast.makeText(
                                        WriteFeedbackActivity.this,
                                        message,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onError(String error) {

                                Toast.makeText(
                                        WriteFeedbackActivity.this,
                                        error,
                                        Toast.LENGTH_SHORT
                                ).show();
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

        ApiClient.getOrderById(this, token, orderId,
                new ApiClient.DataCallback<List<OrderItem>>() {

                    @Override
                    public void onSuccess(List<OrderItem> orderItems, String msg) {

                        itemList.clear();

                        if (orderItems.isEmpty()) {

                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        final int[] loaded = {0};

                        for (OrderItem oi : orderItems) {

                            ApiClient.getFeedbacksByProductAndOrder(
                                    WriteFeedbackActivity.this,
                                    token,
                                    oi.getProductId(),
                                    orderId,
                                    new ApiClient.DataCallback<List<Feedback>>() {

                                        @SuppressLint("NotifyDataSetChanged")
                                        @Override
                                        public void onSuccess(List<Feedback> data, String m) {

                                            Feedback existing = null;

                                            if (data != null && !data.isEmpty()) {
                                                existing = data.get(0);
                                            }

                                            itemList.add(new FeedbackItem(oi, existing));

                                            loaded[0]++;

                                            if (loaded[0] == orderItems.size()) {

                                                adapter.notifyDataSetChanged();

                                                tvEmpty.setVisibility(
                                                        itemList.isEmpty()
                                                                ? View.VISIBLE
                                                                : View.GONE
                                                );
                                            }
                                        }

                                        @Override
                                        public void onError(String error) {

                                            itemList.add(new FeedbackItem(oi, null));

                                            loaded[0]++;

                                            if (loaded[0] == orderItems.size()) {

                                                adapter.notifyDataSetChanged();

                                                tvEmpty.setVisibility(
                                                        itemList.isEmpty()
                                                                ? View.VISIBLE
                                                                : View.GONE
                                                );
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(String error) {

                        Toast.makeText(
                                WriteFeedbackActivity.this,
                                error,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    interface SaveCallback {
        void onSave(FeedbackItem item, int rating, String comment, int position);
    }

    static class WriteFeedbackAdapter extends RecyclerView.Adapter<WriteFeedbackAdapter.VH> {

        private final Context context;
        private final List<FeedbackItem> list;
        private final SaveCallback callback;

        WriteFeedbackAdapter(Context context,
                             List<FeedbackItem> list,
                             SaveCallback callback) {

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
        public void onBindViewHolder(VH holder, @SuppressLint("RecyclerView") int position) {

            FeedbackItem item = list.get(position);
            OrderItem oi = item.orderItem;

            holder.tvProductName.setText(oi.getProductName());

            String imageUrl = oi.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {

                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.imgProduct);

            } else {

                holder.imgProduct.setImageResource(R.mipmap.ic_launcher);
            }

            holder.rbRating.setOnRatingBarChangeListener(null);
            holder.rbRating.setRating(item.tempRating);
            updateRatingText(holder.tvRatingText, item.tempRating);

            holder.rbRating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {

                if (fromUser) {
                    item.tempRating = rating;
                    updateRatingText(holder.tvRatingText, rating);
                }
            });

            holder.edtComment.clearFocus();
            holder.edtComment.setText(item.tempComment);

            holder.edtComment.addTextChangedListener(new TextWatcher() {

                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.tempComment = s.toString();
                }

                @Override public void afterTextChanged(Editable s) {}
            });

            if (item.existing != null) {

                holder.btnSave.setText("Cập nhật đánh giá");

                String adminReply = item.existing.getAdminReply();

                if (adminReply != null && !adminReply.isEmpty()) {

                    holder.layoutAdminReply.setVisibility(View.VISIBLE);
                    holder.tvAdminReplyText.setText(adminReply);

                } else {

                    holder.layoutAdminReply.setVisibility(View.GONE);
                }

            } else {

                holder.btnSave.setText("Lưu đánh giá");
                holder.layoutAdminReply.setVisibility(View.GONE);
            }

            holder.btnSave.setOnClickListener(v -> {

                int rating = (int) item.tempRating;
                String comment = item.tempComment.trim();

                if (rating == 0) {

                    Toast.makeText(context,
                            "Vui lòng chọn số sao",
                            Toast.LENGTH_SHORT).show();

                    return;
                }

                callback.onSave(item, rating, comment, position);
            });
        }

        private void updateRatingText(TextView tv, float rating) {

            String text;
            int color;

            if (rating <= 1) {
                text = "Very Bad";
                color = Color.RED;

            } else if (rating == 2) {
                text = "Bad";
                color = Color.RED;

            } else if (rating == 3) {
                text = "Neutral";
                color = Color.GRAY;

            } else if (rating == 4) {
                text = "Good";
                color = Color.parseColor("#FF9800");

            } else {
                text = "Excellent";
                color = Color.parseColor("#128A17");
            }

            tv.setText(text);
            tv.setTextColor(color);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {

            ImageView imgProduct;
            TextView tvProductName;
            TextView tvRatingText;
            TextView tvAdminReplyText;

            RatingBar rbRating;
            EditText edtComment;
            Button btnSave;
            LinearLayout layoutAdminReply;

            VH(View v) {

                super(v);

                imgProduct = v.findViewById(R.id.imgWriteFbProduct);
                tvProductName = v.findViewById(R.id.tvWriteFbProductName);
                rbRating = v.findViewById(R.id.rbWriteFbRating);
                tvRatingText = v.findViewById(R.id.tvWriteFbRatingText);
                edtComment = v.findViewById(R.id.edtWriteFbComment);
                btnSave = v.findViewById(R.id.btnSaveFeedback);
                layoutAdminReply = v.findViewById(R.id.layoutAdminReply);
                tvAdminReplyText = v.findViewById(R.id.tvAdminReplyText);
            }
        }
    }
}