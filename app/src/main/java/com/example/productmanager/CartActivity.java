package com.example.productmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.productmanager.ApiClient;
import com.example.productmanager.CartItem;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity
        implements CartAdapter.CartActionListener {

    private ImageView btnBackCart;
    private TextView tvEmptyCart, tvTotalPrice;
    private ListView lvCart;
    private Button btnClearCart;
    private LinearLayout layoutFooter;

    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        initViews();

        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            finish();
            return;
        }

        cartItems = new ArrayList<>();
        adapter = new CartAdapter(this, R.layout.item_cart, cartItems, this);
        lvCart.setAdapter(adapter);

        loadCart();

        btnBackCart.setOnClickListener(v -> finish());

        btnClearCart.setOnClickListener(v -> clearCart());
    }

    private void initViews() {
        btnBackCart = findViewById(R.id.btnBackCart);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        lvCart = findViewById(R.id.lvCart);
        btnClearCart = findViewById(R.id.btnClearCart);
        layoutFooter = findViewById(R.id.layoutFooter);
    }

    // =========================
    // LOAD CART
    // =========================
    private void loadCart() {
        ApiClient.getCart(this, authToken,
                new ApiClient.DataCallback<List<CartItem>>() {
                    @Override
                    public void onSuccess(List<CartItem> data, String message) {
                        cartItems.clear();
                        cartItems.addAll(data);
                        updateUI();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(CartActivity.this,
                                errorMessage, Toast.LENGTH_SHORT).show();
                        updateUI();
                    }
                });
    }

    // =========================
    // UPDATE UI
    // =========================
    private void updateUI() {

        if (cartItems.isEmpty()) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            lvCart.setVisibility(View.GONE);
            layoutFooter.setVisibility(View.GONE);
        } else {
            tvEmptyCart.setVisibility(View.GONE);
            lvCart.setVisibility(View.VISIBLE);
            layoutFooter.setVisibility(View.VISIBLE);

            adapter.notifyDataSetChanged();
            updateTotal();
        }
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubTotal();
        }
        tvTotalPrice.setText(String.format("%.2f USD", total));
    }

    // =========================
    // CLEAR CART
    // =========================
    private void clearCart() {

        if (cartItems.isEmpty()) {
            Toast.makeText(this,
                    "Giỏ hàng đang trống!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.clearCart(this, authToken,
                new ApiClient.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data, String message) {
                        cartItems.clear();
                        updateUI();
                        Toast.makeText(CartActivity.this,
                                message.isEmpty() ?
                                        "Đã xóa sạch giỏ hàng!" : message,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(CartActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================
    // TĂNG SỐ LƯỢNG
    // =========================
    @Override
    public void onIncreaseQuantity(CartItem item) {

        int newQuantity = item.getQuantity() + 1;

        ApiClient.updateCartQuantity(
                this,
                authToken,
                item.getCartItemId(),
                newQuantity,
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {
                        item.setQuantity(newQuantity);
                        adapter.notifyDataSetChanged();
                        updateTotal();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(CartActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // =========================
    // GIẢM SỐ LƯỢNG
    // =========================
    @Override
    public void onDecreaseQuantity(CartItem item) {

        if (item.getQuantity() <= 1) return;

        int newQuantity = item.getQuantity() - 1;

        ApiClient.updateCartQuantity(
                this,
                authToken,
                item.getCartItemId(),
                newQuantity,
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {
                        item.setQuantity(newQuantity);
                        adapter.notifyDataSetChanged();
                        updateTotal();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(CartActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================
    // XÓA 1 SẢN PHẨM
    // =========================
    @Override
    public void onRemoveCartItem(CartItem item) {

        ApiClient.removeCartItem(
                this,
                authToken,
                item.getCartItemId(),
                new ApiClient.DataCallback<Void>() {

                    @Override
                    public void onSuccess(Void data, String message) {
                        cartItems.remove(item);
                        updateUI();
                        Toast.makeText(CartActivity.this,
                                message.isEmpty() ?
                                        "Đã xóa sản phẩm khỏi giỏ" : message,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(CartActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}