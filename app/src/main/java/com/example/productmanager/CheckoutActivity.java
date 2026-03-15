package com.example.productmanager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class CheckoutActivity extends BaseCustomerActivity {

    private TextInputEditText edtReceiverName, edtPhone, edtAddress;
    private RadioGroup rgPaymentMethod;
    private TextView tvSubtotal, tvDiscount, tvTotal;
    private LinearLayout layoutDiscount;
    private Button btnPlaceOrder;

    private String authToken;

    private double subtotal;
    private double discount;

    private Integer voucherId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initViews();
        setupToolbar();

        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            finish();
            return;
        }

        // nhận dữ liệu từ CartActivity
        subtotal = getIntent().getDoubleExtra("subtotal", 0);
        discount = getIntent().getDoubleExtra("discount", 0);

        if (getIntent().hasExtra("voucherId")) {
            voucherId = getIntent().getIntExtra("voucherId", 0);
        }

        displaySummary();
        prefillShippingInfo();

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void initViews() {
        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);

        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvTotal = findViewById(R.id.tvTotal);

        layoutDiscount = findViewById(R.id.layoutDiscount);

        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void prefillShippingInfo() {

        ApiClient.getProfile(this, authToken, new ApiClient.DataCallback<JSONObject>() {

            @Override
            public void onSuccess(JSONObject data, String message) {

                if (data == null) return;

                edtReceiverName.setText(data.optString("fullName", ""));
                edtPhone.setText(data.optString("phone", ""));
                edtAddress.setText(data.optString("address", ""));
            }

            @Override
            public void onError(String errorMessage) {
                // user tự nhập
            }
        });
    }

    private void displaySummary() {

        tvSubtotal.setText(formatVND(subtotal));

        if (discount > 0) {
            layoutDiscount.setVisibility(View.VISIBLE);
            tvDiscount.setText("-" + formatVND(discount));
        } else {
            layoutDiscount.setVisibility(View.GONE);
        }

        double total = subtotal - discount;
        if (total < 0) total = 0;

        tvTotal.setText(formatVND(total));
    }

    private void placeOrder() {

        String name = getText(edtReceiverName);
        String phone = getText(edtPhone);
        String address = getText(edtAddress);

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {

            Toast.makeText(this,
                    "Vui lòng điền đầy đủ thông tin giao hàng",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod =
                rgPaymentMethod.getCheckedRadioButtonId() == R.id.rbCOD
                        ? "COD"
                        : "VNPay";

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang xử lý...");

        ApiClient.createOrder(
                this,
                authToken,
                address,
                phone,
                name,
                paymentMethod,
                voucherId,
                new ApiClient.DataCallback<JSONObject>() {

                    @Override
                    public void onSuccess(JSONObject data, String message) {

                        int orderId = data.optInt("orderId", 0);
                        double finalTotal = data.optDouble("totalAmount", 0);

                        if (paymentMethod.equals("VNPay")) {

                            ApiClient.createPayment(
                                    CheckoutActivity.this,
                                    authToken,
                                    orderId,
                                    "VNPay",
                                    new ApiClient.DataCallback<JSONObject>() {

                                        @Override
                                        public void onSuccess(JSONObject paymentData, String message) {

                                            String paymentUrl = paymentData.optString("paymentUrl");

                                            if (paymentUrl == null || paymentUrl.isEmpty()) {

                                                Toast.makeText(
                                                        CheckoutActivity.this,
                                                        "Không lấy được link thanh toán",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                                btnPlaceOrder.setEnabled(true);
                                                btnPlaceOrder.setText("Đặt hàng");
                                                return;
                                            }

                                            Intent intent = new Intent(
                                                    CheckoutActivity.this,
                                                    VnpayWebViewActivity.class
                                            );

                                            intent.putExtra("paymentUrl", paymentUrl);
                                            startActivity(intent);

                                        }

                                        @Override
                                        public void onError(String errorMessage) {

                                            btnPlaceOrder.setEnabled(true);
                                            btnPlaceOrder.setText("Đặt hàng");

                                            Toast.makeText(
                                                    CheckoutActivity.this,
                                                    errorMessage,
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            );

                        } else {

                            Intent intent = new Intent(
                                    CheckoutActivity.this,
                                    BillingSuccessActivity.class
                            );

                            intent.putExtra("orderId", orderId);
                            intent.putExtra("totalAmount", finalTotal);
                            intent.putExtra("paymentMethod", paymentMethod);
                            intent.putExtra("status", "Pending");

                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {

                        btnPlaceOrder.setEnabled(true);
                        btnPlaceOrder.setText("Đặt hàng");

                        Toast.makeText(
                                CheckoutActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private String getText(TextInputEditText edt) {
        return edt.getText() == null
                ? ""
                : edt.getText().toString().trim();
    }

    @SuppressLint("DefaultLocale")
    private String formatVND(double amount) {
        return String.format("%,.0f ₫", amount);
    }
}