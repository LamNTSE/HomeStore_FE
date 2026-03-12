package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Paint;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    ImageView imgDetail;
    TextView tvName, tvPrice, tvDesc,tvOldPrice;

    TextView tvSeeMore;
    Button btnBack, btnEdit, btnAddToCart;
    RecyclerView rvFeedbacks;
    TextView tvNoFeedbacks;
    FeedbackPublicAdapter feedbackAdapter;
    List<Feedback> feedbackList = new ArrayList<>();

    private int productId;
    private Product currentProduct;
    private String authToken;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        imgDetail = findViewById(R.id.imgDetail);
        tvName = findViewById(R.id.tvDetailName);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvOldPrice = findViewById(R.id.tvOldPrice);

// gạch ngang giá cũ
        tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tvDesc = findViewById(R.id.tvDetailDesc);
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        rvFeedbacks = findViewById(R.id.rvFeedbacks);
        tvNoFeedbacks = findViewById(R.id.tvNoFeedbacks);
        tvSeeMore = findViewById(R.id.tvSeeMore);
        feedbackAdapter = new FeedbackPublicAdapter(feedbackList);
        rvFeedbacks.setLayoutManager(new LinearLayoutManager(this));
        rvFeedbacks.setAdapter(feedbackAdapter);
        rvFeedbacks.setNestedScrollingEnabled(false);

        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        isAdmin = SessionManager.isAdmin(this);
        btnEdit.setVisibility(isAdmin ? android.view.View.VISIBLE : android.view.View.GONE);

        Intent intent = getIntent();
        if (intent != null) {
            productId = intent.getIntExtra("id", -1);
        }

        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct == null) {
                return;
            }

            ApiClient.addToCart(this, authToken, currentProduct.getId(), 1, new ApiClient.DataCallback<>() {
                @Override
                public void onSuccess(Void data, String message) {
                    Toast.makeText(DetailActivity.this,
                            message.isEmpty() ? "Đã thêm vào giỏ hàng" : message,
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(DetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            if (!isAdmin) {
                Toast.makeText(this, "Bạn không có quyền sửa sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent editIntent = new Intent(DetailActivity.this, AddEditActivity.class);
            editIntent.putExtra("PRODUCT_ID", productId);
            startActivity(editIntent);
        });

        loadProduct();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (productId > 0) {
            loadProduct();
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatVND(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private void loadProduct() {
        if (productId <= 0) {
            Toast.makeText(this, "Không có mã sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiClient.getProductById(this, authToken, productId, new ApiClient.DataCallback<>() {
            @Override
            public void onSuccess(Product data, String message) {
                currentProduct = data;
                tvName.setText(data.getName());
                tvDesc.setText(data.getDescription() == null ? "" : data.getDescription());
                double price = data.getPrice();
                double discount = 0.2; // 20%

                double oldPrice = price / (1 - discount);

                tvPrice.setText(formatVND(price));
                tvOldPrice.setText(formatVND(oldPrice));

                if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
                    Glide.with(DetailActivity.this)
                            .load(data.getImageUrl())
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(imgDetail);
                } else {
                    imgDetail.setImageResource(R.mipmap.ic_launcher);
                }

                loadFeedbacks();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadFeedbacks() {
        ApiClient.getFeedbacksByProduct(this, productId, new ApiClient.DataCallback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(List<Feedback> data, String message) {

                // Nếu không có feedback
                if (data == null || data.isEmpty()) {
                    tvNoFeedbacks.setVisibility(View.VISIBLE);
                    rvFeedbacks.setVisibility(View.GONE);
                    tvSeeMore.setVisibility(View.GONE);
                    return;
                }

                tvNoFeedbacks.setVisibility(View.GONE);
                rvFeedbacks.setVisibility(View.VISIBLE);

                // ⭐ Hiện see more nếu > 2 feedback
                if (data.size() > 2) {
                    tvSeeMore.setVisibility(View.VISIBLE);

                    // Chỉ show 2 cái đầu
                    feedbackList.clear();
                    feedbackList.add(data.get(0));
                    feedbackList.add(data.get(1));
                } else {
                    tvSeeMore.setVisibility(View.GONE);
                    feedbackList.clear();
                    feedbackList.addAll(data);
                }

                feedbackAdapter.notifyDataSetChanged();

                // ⭐ Chuyển sang màn hình Reviews
                tvSeeMore.setOnClickListener(v -> {
                    Intent intent = new Intent(DetailActivity.this, ReviewsActivity.class);
                    intent.putExtra("PRODUCT_ID", productId);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String error) {}
        });
    }
}