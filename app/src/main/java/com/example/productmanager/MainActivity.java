package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("UnsafeOptInUsageError")
public class MainActivity extends BaseCustomerActivity
        implements ProductAdapter.ProductActionListener {

    ListView lvProduct;
    ArrayList<Product> productList;
    ProductAdapter adapter;

    FloatingActionButton fabAdd;
    EditText edtSearch;

    ImageView btnCartMain;
    ImageView btnFilter;
    ImageView btnStoreLocationMain;

    FrameLayout frameCart;
    BadgeDrawable cartBadge;

    String authToken;
    boolean isAdmin;

    Handler handler = new Handler();
    Runnable searchRunnable;

    // =============================
    // CATEGORY
    // =============================

    List<Category> categoryList = new ArrayList<>();
    int selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvProduct = findViewById(R.id.lvProduct);
        fabAdd = findViewById(R.id.fabAdd);
        edtSearch = findViewById(R.id.edtSearch);
        frameCart = findViewById(R.id.frameCart);
        btnCartMain = findViewById(R.id.btnCartMain);
        btnFilter = findViewById(R.id.btnFilter);
        btnStoreLocationMain = findViewById(R.id.btnStoreLocationMain);
        ImageView avatar = findViewById(R.id.btnAvatar);

        // =============================
        // TOKEN
        // =============================

        authToken = SessionManager.getToken(this);

        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        isAdmin = SessionManager.isAdmin(this);

        // =============================
        // PRODUCT LIST
        // =============================

        productList = new ArrayList<>();

        adapter = new ProductAdapter(
                this,
                R.layout.item_product,
                productList,
                this,
                isAdmin
        );

        lvProduct.setAdapter(adapter);

        // =============================
        // ROLE UI
        // =============================

        fabAdd.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnCartMain.setVisibility(isAdmin ? View.GONE : View.VISIBLE);

        // =============================
        // LOAD DATA
        // =============================

        loadProducts("");
        loadCategories();

        if (!isAdmin) {
            SignalRManager.getInstance().connectCart(authToken);
            SignalRManager.getInstance().setCartUpdateListener(msgJson -> {
                NotificationHelper.showNotification(
                        MainActivity.this, 
                        "Cập nhật Giỏ Hàng", 
                        "Một sản phẩm trong giỏ hàng của bạn vừa bị ngừng kinh doanh và đã tự động gỡ bỏ."
                );
                updateCartBadge();
            });
        }

        // =============================
        // CART CLICK
        // =============================

        btnCartMain.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });

        btnStoreLocationMain.setOnClickListener(v ->
            startActivity(new Intent(MainActivity.this, StoreLocationActivity.class))
        );

        // =============================
        // FILTER CLICK
        // =============================

        btnFilter.setOnClickListener(v -> showCategoryFilter(v));

        // =============================
        // CART BADGE
        // =============================

        if (!isAdmin) {

            cartBadge = BadgeDrawable.create(this);
            cartBadge.setMaxCharacterCount(3);
            cartBadge.setVisible(false);
            cartBadge.setBackgroundColor(0xFFE53935);

            frameCart.post(() ->
                    BadgeUtils.attachBadgeDrawable(cartBadge, btnCartMain, frameCart)
            );
        }

        // =============================
        // ADD PRODUCT (ADMIN)
        // =============================

        fabAdd.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            startActivity(intent);
        });

        // =============================
        // SEARCH DELAY
        // =============================

        edtSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {

                    if (selectedCategoryId == -1) {
                        loadProducts(s.toString().trim());
                    } else {
                        loadProductsByCategory(selectedCategoryId);
                    }
                };

                handler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // =============================
        // AVATAR MENU
        // =============================

        avatar.setOnClickListener(v -> {

            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_profile, popupMenu.getMenu());

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

                if (item.getItemId() == R.id.menuMyOrders) {
                    startActivity(new Intent(MainActivity.this, UserOrdersActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.menuLogout) {

                    SignalRManager.getInstance().disconnectAll();
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
                new ApiClient.DataCallback<List<Product>>() {

                    @Override
                    public void onSuccess(List<Product> data, String message) {

                        productList.clear();
                        productList.addAll(data);
                        adapter.notifyDataSetChanged();
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
    // LOAD PRODUCTS BY CATEGORY
    // =============================

    public void loadProductsByCategory(int categoryId) {

        ApiClient.getProductsByCategory(this,
                authToken,
                categoryId,
                new ApiClient.DataCallback<List<Product>>() {

                    @Override
                    public void onSuccess(List<Product> data, String message) {

                        productList.clear();
                        productList.addAll(data);
                        adapter.notifyDataSetChanged();
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
    // LOAD CATEGORY
    // =============================

    private void loadCategories() {

        ApiClient.getCategories(this,
                authToken, new ApiClient.DataCallback<List<Category>>() {

                    @Override
                    public void onSuccess(List<Category> data, String message) {

                        categoryList.clear();

                        if (data != null) {
                            categoryList.addAll(data);
                        }
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
    // SHOW CATEGORY FILTER
    // =============================

    private void showCategoryFilter(View anchor) {

        PopupMenu popup = new PopupMenu(this, anchor);

        popup.getMenu().add("Tất cả");

        for (Category c : categoryList) {
            popup.getMenu().add(c.getCategoryName());
        }

        popup.setOnMenuItemClickListener(item -> {

            String selected = item.getTitle().toString();

            if (selected.equals("Tất cả")) {

                selectedCategoryId = -1;
                loadProducts(edtSearch.getText().toString().trim());
                return true;
            }

            for (Category c : categoryList) {

                if (c.getCategoryName().equals(selected)) {

                    selectedCategoryId = c.getCategoryId();
                    loadProductsByCategory(selectedCategoryId);
                    break;
                }
            }

            return true;
        });

        popup.show();
    }

    // =============================
    // CART BADGE
    // =============================

    @Override
    protected void onResume() {
        super.onResume();

        loadProducts(edtSearch.getText().toString().trim());

        if (!isAdmin) {
            updateCartBadge();
        }
    }

    private void updateCartBadge() {

        ApiClient.getCart(this, authToken,
                new ApiClient.DataCallback<List<CartItem>>() {

                    @Override
                    public void onSuccess(List<CartItem> data, String message) {

                        int count = 0;

                        if (data != null) {
                            for (CartItem item : data) {
                                count += item.getQuantity();
                            }
                        }

                        if (count > 0) {

                            cartBadge.setVisible(true);
                            cartBadge.setNumber(count);

                        } else {

                            cartBadge.setVisible(false);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {}
                });
    }

    // =============================
    // DELETE PRODUCT
    // =============================

    @Override
    public void onProductClick(Product product) {

    }

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
                                "Đã xóa sản phẩm",
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
    // ADD TO CART
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
                                "Đã thêm vào giỏ hàng",
                                Toast.LENGTH_SHORT).show();

                        updateCartBadge();
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