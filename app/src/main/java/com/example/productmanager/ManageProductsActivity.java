package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ManageProductsActivity extends AppCompatActivity
        implements ProductAdapter.ProductActionListener {

    ListView listView;
    FloatingActionButton fabAdd;

    ArrayList<Product> productList;
    ProductAdapter adapter;

    String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        listView = findViewById(R.id.listViewProducts);
        fabAdd = findViewById(R.id.fabAdd);

        authToken = SessionManager.getToken(this);

        // Nếu chưa login → về Login
        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Nếu không phải admin → không cho vào màn này
        if (!SessionManager.isAdmin(this)) {
            Toast.makeText(this,
                    "Bạn không có quyền truy cập",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productList = new ArrayList<>();

        adapter = new ProductAdapter(
                this,
                R.layout.item_product,
                productList,
                this,
                true // 👑 ADMIN MODE
        );

        listView.setAdapter(adapter);

        fabAdd.setVisibility(View.VISIBLE);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ManageProductsActivity.this,
                    AddEditActivity.class
            );
            startActivity(intent);
        });

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    // =============================
    // LOAD PRODUCTS
    // =============================
    private void loadProducts() {

        ApiClient.getProducts(this,
                authToken,
                "",
                new ApiClient.DataCallback<java.util.List<Product>>() {

                    @Override
                    public void onSuccess(java.util.List<Product> data,
                                          String message) {

                        productList.clear();
                        productList.addAll(data);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        if (errorMessage.contains("401")
                                || errorMessage.contains("403")) {

                            SessionManager.clear(ManageProductsActivity.this);
                            startActivity(new Intent(
                                    ManageProductsActivity.this,
                                    LoginActivity.class));
                            finish();
                            return;
                        }

                        Toast.makeText(
                                ManageProductsActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // =============================
    // DELETE (Admin Only)
    // =============================
    @Override
    public void onDeleteProduct(Product product) {

        ApiClient.deleteProduct(this,
                authToken,
                product.getId(),
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {

                        Toast.makeText(
                                ManageProductsActivity.this,
                                message.isEmpty()
                                        ? "Đã xóa sản phẩm"
                                        : message,
                                Toast.LENGTH_SHORT
                        ).show();

                        loadProducts();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                ManageProductsActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // Không dùng trong admin
    @Override
    public void onAddToCart(Product product) {
        // Không làm gì
    }
}
