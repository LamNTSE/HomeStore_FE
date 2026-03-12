package com.example.productmanager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends BaseCustomerActivity
        implements CartAdapter.CartActionListener {

    private ImageView btnBackCart;
    private TextView tvEmptyCart, tvTotalPrice;
    private ListView lvCart;
    private Button btnClearCart;
    private Button btnCheckout;
    private LinearLayout layoutFooter;

    private LinearLayout layoutVoucher;
    private TextView tvVoucherCode;

    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private String authToken;

    private Voucher appliedVoucher = null;

    private static final int REQUEST_VOUCHER = 1001;

    private double discountAmount = 0;
    private String voucherCode = "";

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

        // CLICK thanh toán
        btnCheckout.setOnClickListener(v -> openCheckout());

        // CLICK mở trang chọn voucher
        layoutVoucher.setOnClickListener(v -> openVoucherPage());
    }



    private void initViews() {
        btnBackCart = findViewById(R.id.btnBackCart);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        lvCart = findViewById(R.id.lvCart);
        btnClearCart = findViewById(R.id.btnClearCart);

        layoutFooter = findViewById(R.id.layoutFooter);

        layoutVoucher = findViewById(R.id.layoutVoucher);
        tvVoucherCode = findViewById(R.id.tvVoucherCode);
        btnCheckout = findViewById(R.id.btnCheckout);
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
    // RESULT VOUCHER
    // =========================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VOUCHER && resultCode == RESULT_OK) {

            voucherCode = data.getStringExtra("voucherCode");

            if (voucherCode != null && !voucherCode.isEmpty()) {

                tvVoucherCode.setText(voucherCode);

                applyVoucher(voucherCode);
            }
        }
    }

    private void openVoucherPage() {

        Intent intent = new Intent(this, VoucherActivity.class);
        startActivityForResult(intent, REQUEST_VOUCHER);
    }

    private double calculateTotal() {

        double total = 0;

        for (CartItem item : cartItems) {
            total += item.getSubTotal();
        }

        return total;
    }

    // =========================
    // APPLY VOUCHER
    // =========================
    private void applyVoucher(String code) {

        ApiClient.getVoucherByCode(
                this,
                authToken,
                code,
                new ApiClient.DataCallback<Voucher>() {

                    @Override
                    public void onSuccess(Voucher voucher, String message) {

                        if (!voucher.isActive()) {
                            Toast.makeText(CartActivity.this,
                                    "Voucher đã hết hiệu lực",
                                    Toast.LENGTH_SHORT).show();
                            appliedVoucher = null;
                            discountAmount = 0;
                            tvVoucherCode.setText("Chọn voucher");
                            updateTotal();
                            return;
                        }

                        double total = calculateTotal();

                        if (total < voucher.getMinOrderValue()) {

                            Toast.makeText(CartActivity.this,
                                    "Đơn hàng phải từ " + formatVND(voucher.getMinOrderValue()),
                                    Toast.LENGTH_SHORT).show();

                            appliedVoucher = null;
                            discountAmount = 0;
                            tvVoucherCode.setText("Chọn voucher");

                            updateTotal();
                            return;
                        }

                        appliedVoucher = voucher;

                        Toast.makeText(CartActivity.this,
                                "Áp dụng voucher thành công",
                                Toast.LENGTH_SHORT).show();

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

    @SuppressLint("DefaultLocale")
    private String formatVND(double amount) {
        return String.format("%,.0f ₫", amount);
    }

    private void updateTotal() {

        double total = calculateTotal();

        discountAmount = 0;

        if (appliedVoucher != null) {

            if (total >= appliedVoucher.getMinOrderValue()) {

                if ("FIXED".equalsIgnoreCase(appliedVoucher.getDiscountType())) {

                    discountAmount = appliedVoucher.getDiscountValue();

                } else if ("PERCENT".equalsIgnoreCase(appliedVoucher.getDiscountType())) {

                    discountAmount = total * appliedVoucher.getDiscountValue() / 100;
                }

                if (discountAmount > total) {
                    discountAmount = total;
                }

            } else {
                // total không đủ điều kiện -> hủy voucher
                appliedVoucher = null;
                discountAmount = 0;

                tvVoucherCode.setText("Chọn voucher");

                Toast.makeText(this,
                        "Đơn hàng không còn đủ điều kiện áp dụng voucher",
                        Toast.LENGTH_SHORT).show();
            }
        }

        double finalTotal = total - discountAmount;

        if (finalTotal < 0) finalTotal = 0;

        tvTotalPrice.setText(formatVND(finalTotal));
    }

    // =========================
    // CHECKOUT
    // =========================
    private void openCheckout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = calculateTotal();

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("subtotal", subtotal);
        intent.putExtra("discount", discountAmount);
        startActivity(intent);
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

        new AlertDialog.Builder(this)
                .setTitle("Xóa giỏ hàng")
                .setMessage("Bạn có chắc muốn xóa toàn bộ sản phẩm trong giỏ hàng?")
                .setPositiveButton("Xóa", (dialog, which) -> doClearCart())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void doClearCart() {

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