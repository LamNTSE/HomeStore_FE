package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements ProductAdapter.ProductActionListener {

    ListView lvProduct;
    ArrayList<Product> productList;
    ProductAdapter adapter;

    FloatingActionButton fabAdd;
    EditText edtSearch;
    ImageView btnCartMain;

    String authToken;
    boolean isAdmin;

    Handler handler = new Handler();
    Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvProduct = findViewById(R.id.lvProduct);
        fabAdd = findViewById(R.id.fabAdd);
        edtSearch = findViewById(R.id.edtSearch);
        btnCartMain = findViewById(R.id.btnCartMain);
        ImageView avatar = findViewById(R.id.btnAvatar);

        // Lấy token
        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        isAdmin = SessionManager.isAdmin(this);

        productList = new ArrayList<>();
        adapter = new ProductAdapter(
                this,
                R.layout.item_product,
                productList,
                this,
                isAdmin
        );

        lvProduct.setAdapter(adapter);

        // 👑 Phân quyền hiển thị
        fabAdd.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnCartMain.setVisibility(isAdmin ? View.GONE : View.VISIBLE);

        loadProducts("");

        // Giỏ hàng
        btnCartMain.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Thêm sản phẩm (Admin)
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            startActivity(intent);
        });

        // Search có delay
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () ->
                        loadProducts(s.toString().trim());

                handler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Popup menu avatar
        avatar.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_profile, popupMenu.getMenu());

            // Ép hiện icon
            try {
                Field field = popupMenu.getClass().getDeclaredField("mPopup");
                field.setAccessible(true);
                Object menuPopupHelper = field.get(popupMenu);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                Method setForceIcons =
                        classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                setForceIcons.invoke(menuPopupHelper, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            popupMenu.setOnMenuItemClickListener(item -> {

                if (item.getItemId() == R.id.menuProfile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.menuLogout) {

                    SessionManager.clear(MainActivity.this);

                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    return true;
                }

                return false;
            });

            popupMenu.show();
        });
    }

    // =============================
    // LOAD PRODUCTS
    // =============================
    public void loadProducts(String keyword) {

        ApiClient.getProducts(this, authToken, keyword,
                new ApiClient.DataCallback<java.util.List<Product>>() {

                    @Override
                    public void onSuccess(java.util.List<Product> data, String message) {
                        productList.clear();
                        productList.addAll(data);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        if (errorMessage.contains("401")
                                || errorMessage.contains("403")) {

                            SessionManager.clear(MainActivity.this);
                            startActivity(new Intent(MainActivity.this,
                                    LoginActivity.class));
                            finish();
                            return;
                        }

                        Toast.makeText(MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(edtSearch.getText().toString().trim());
    }

    // =============================
    // DELETE (Admin)
    // =============================
    @Override
    public void onDeleteProduct(Product product) {

        if (!isAdmin) {
            Toast.makeText(this,
                    "Bạn không có quyền xóa sản phẩm",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.deleteProduct(this, authToken,
                product.getId(),
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {
                        Toast.makeText(MainActivity.this,
                                message.isEmpty()
                                        ? "Đã xóa sản phẩm"
                                        : message,
                                Toast.LENGTH_SHORT).show();

                        loadProducts(edtSearch.getText().toString().trim());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =============================
    // ADD TO CART (Customer)
    // =============================
    @Override
    public void onAddToCart(Product product) {

        if (isAdmin) {
            Toast.makeText(this,
                    "Admin không thể thêm vào giỏ hàng",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.addToCart(this,
                authToken,
                product.getId(),
                1,
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {
                        Toast.makeText(MainActivity.this,
                                message.isEmpty()
                                        ? "Đã thêm vào giỏ hàng"
                                        : message,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}