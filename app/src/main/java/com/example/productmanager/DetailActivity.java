package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class DetailActivity extends AppCompatActivity {

    ImageView imgDetail;
    TextView tvName, tvPrice, tvDesc;
    Button btnBack, btnEdit, btnAddToCart;

    private int productId;
    private Product currentProduct;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        imgDetail = findViewById(R.id.imgDetail);
        tvName = findViewById(R.id.tvDetailName);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvDesc = findViewById(R.id.tvDetailDesc);
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Intent intent = getIntent();
        if (intent != null) {
            productId = intent.getIntExtra("id", -1);
        }

        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct == null) {
                return;
            }

            ApiClient.addToCart(this, authToken, currentProduct.getId(), 1, new ApiClient.DataCallback<Void>() {
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

    private void loadProduct() {
        if (productId <= 0) {
            Toast.makeText(this, "Không có mã sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiClient.getProductById(this, authToken, productId, new ApiClient.DataCallback<Product>() {
            @Override
            public void onSuccess(Product data, String message) {
                currentProduct = data;
                tvName.setText(data.getName());
                tvDesc.setText(data.getDescription() == null ? "" : data.getDescription());
                tvPrice.setText(String.format("%.2f USD", data.getPrice()));

                if (data.getImageUrl() != null && !data.getImageUrl().isEmpty()) {
                    Glide.with(DetailActivity.this)
                            .load(data.getImageUrl())
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(imgDetail);
                } else {
                    imgDetail.setImageResource(R.mipmap.ic_launcher);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}