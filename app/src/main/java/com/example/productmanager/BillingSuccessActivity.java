package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class BillingSuccessActivity extends BaseCustomerActivity {

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_success);

        TextView tvOrderId = findViewById(R.id.tvOrderId);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        TextView tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        TextView tvStatus = findViewById(R.id.tvStatus);
        Button btnBackHome = findViewById(R.id.btnBackHome);

        android.net.Uri data = getIntent().getData();

        int orderId = 0;
        double totalAmount = 0;
        String paymentMethod = "COD";
        String status = "Pending";

        if (data != null) {
            try {
                orderId = Integer.parseInt(Objects.requireNonNull(data.getQueryParameter("orderId")));
                totalAmount = Double.parseDouble(Objects.requireNonNull(data.getQueryParameter("amount")));
                paymentMethod = data.getQueryParameter("method");
                status = data.getQueryParameter("status");
            } catch (Exception ignored) {}
        } else {
            // trường hợp COD (đi từ CheckoutActivity)
            orderId = getIntent().getIntExtra("orderId", 0);
            totalAmount = getIntent().getDoubleExtra("totalAmount", 0);
            paymentMethod = getIntent().getStringExtra("paymentMethod");
            status = getIntent().getStringExtra("status");
        }

        tvOrderId.setText("#" + orderId);
        tvTotalAmount.setText(String.format("%,.0f ₫", totalAmount));
        tvPaymentMethod.setText("COD".equals(paymentMethod)
                ? "Thanh toán khi nhận hàng" : "VNPay");
        tvStatus.setText(status != null ? status : "Pending");

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
