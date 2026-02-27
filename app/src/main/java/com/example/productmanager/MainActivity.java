package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ProductAdapter.ProductActionListener {

    ListView lvProduct;
    ArrayList<Product> productList;
    ProductAdapter adapter;
    FloatingActionButton fabAdd;
    EditText edtSearch;
    ImageView btnCartMain;
    String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvProduct = findViewById(R.id.lvProduct);
        fabAdd = findViewById(R.id.fabAdd);
        edtSearch = findViewById(R.id.edtSearch);
        btnCartMain = findViewById(R.id.btnCartMain);

        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, R.layout.item_product, productList, this);
        lvProduct.setAdapter(adapter);

        loadProducts("");

        btnCartMain.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            startActivity(intent);
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void loadProducts(String keyword) {
        ApiClient.getProducts(this, authToken, keyword, new ApiClient.DataCallback<java.util.List<Product>>() {
            @Override
            public void onSuccess(java.util.List<Product> data, String message) {
                productList.clear();
                productList.addAll(data);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                if (errorMessage.contains("401") || errorMessage.contains("403")) {
                    SessionManager.clear(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    return;
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(edtSearch.getText().toString().trim());
    }

    @Override
    public void onDeleteProduct(Product product) {
        ApiClient.deleteProduct(this, authToken, product.getId(), new ApiClient.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data, String message) {
                Toast.makeText(MainActivity.this,
                        message.isEmpty() ? "Đã xóa sản phẩm" : message,
                        Toast.LENGTH_SHORT).show();
                loadProducts(edtSearch.getText().toString().trim());
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAddToCart(Product product) {
        ApiClient.addToCart(this, authToken, product.getId(), 1, new ApiClient.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data, String message) {
                Toast.makeText(MainActivity.this,
                        message.isEmpty() ? "Đã thêm vào giỏ hàng" : message,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}