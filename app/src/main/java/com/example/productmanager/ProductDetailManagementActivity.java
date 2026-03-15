package com.example.productmanager;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductDetailManagementActivity extends AppCompatActivity {

    ImageView imgProduct;
    TextView txtName, txtPrice, txtDescription, txtStock, txtCategory;

    String authToken;
    int productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        imgProduct = findViewById(R.id.imgProduct);
        txtName = findViewById(R.id.txtName);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        txtStock = findViewById(R.id.txtStock);
        txtCategory = findViewById(R.id.txtCategory);

        authToken = SessionManager.getToken(this);

        productId = getIntent().getIntExtra("productId", -1);

        if (productId == -1) {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProductDetail();
    }

    private void loadProductDetail() {

        ApiClient.getProductById(
                this,
                authToken,
                productId,
                new ApiClient.DataCallback<Product>() {

                    @Override
                    public void onSuccess(Product product, String message) {

                        txtName.setText(product.getName());

                        txtPrice.setText(
                                String.format("%,.0f đ", product.getPrice())
                        );

                        txtDescription.setText(product.getDescription());

                        txtStock.setText(
                                "Stock: " + product.getStockQuantity()
                        );

                        // LOAD CATEGORY NAME
                        loadCategory(product.getCategoryId());

                        Glide.with(ProductDetailManagementActivity.this)
                                .load(product.getImageUrl())
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(imgProduct);
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                ProductDetailManagementActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void loadCategory(int categoryId) {

        ApiClient.getCategories(this,
                authToken, new ApiClient.DataCallback<List<Category>>() {

                    @Override
                    public void onSuccess(List<Category> data, String message) {

                        if (data == null || data.isEmpty()) {
                            txtCategory.setText("Category: Unknown");
                            return;
                        }

                        for (Category c : data) {

                            if (c.getCategoryId() == categoryId) {

                                txtCategory.setText(
                                        "Category: " + c.getCategoryName()
                                );
                                return;
                            }
                        }

                        txtCategory.setText("Category: Unknown");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        txtCategory.setText("Category: Unknown");
                    }
                });
    }
}