package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BillingSuccessActivity extends BaseCustomerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_success);

        TextView tvOrderId = findViewById(R.id.tvOrderId);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        TextView tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        TextView tvStatus = findViewById(R.id.tvStatus);
        Button btnBackHome = findViewById(R.id.btnBackHome);

        int orderId = getIntent().getIntExtra("orderId", 0);
        double totalAmount = getIntent().getDoubleExtra("totalAmount", 0);
        String paymentMethod = getIntent().getStringExtra("paymentMethod");
        String status = getIntent().getStringExtra("status");

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
