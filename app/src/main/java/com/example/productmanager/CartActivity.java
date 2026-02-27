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

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartActionListener {

    ImageView btnBackCart;
    TextView tvEmptyCart, tvTotalPrice;
    ListView lvCart;
    Button btnClearCart;
    LinearLayout layoutFooter;
    CartAdapter adapter;
    List<CartItem> cartItems;
    String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        btnBackCart = findViewById(R.id.btnBackCart);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        lvCart = findViewById(R.id.lvCart);
        btnClearCart = findViewById(R.id.btnClearCart);
        layoutFooter = findViewById(R.id.layoutFooter);

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

        btnClearCart.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiClient.clearCart(this, authToken, new ApiClient.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data, String message) {
                    cartItems.clear();
                    updateUI();
                    Toast.makeText(CartActivity.this,
                            message.isEmpty() ? "Đã xóa sạch giỏ hàng!" : message,
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        ApiClient.getCart(this, authToken, new ApiClient.DataCallback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> data, String message) {
                cartItems.clear();
                cartItems.addAll(data);
                updateUI();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }

    public void updateUI() {
        if (cartItems.isEmpty()) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            lvCart.setVisibility(View.GONE);
            layoutFooter.setVisibility(View.GONE);
        } else {
            tvEmptyCart.setVisibility(View.GONE);
            lvCart.setVisibility(View.VISIBLE);
            layoutFooter.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();

            double total = 0;
            for (CartItem item : cartItems) {
                total += item.getSubTotal();
            }
            tvTotalPrice.setText(String.format("%.2f USD", total));
        }
    }

    @Override
    public void onRemoveCartItem(CartItem item) {
        ApiClient.removeCartItem(this, authToken, item.getCartItemId(), new ApiClient.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data, String message) {
                loadCart();
                Toast.makeText(CartActivity.this,
                        message.isEmpty() ? "Đã xóa sản phẩm khỏi giỏ" : message,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}