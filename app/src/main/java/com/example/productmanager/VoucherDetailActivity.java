package com.example.productmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.NumberFormat;
import java.util.Locale;

public class VoucherDetailActivity extends AppCompatActivity {

    private TextView tvCode, tvTypeValue, tvValueValue, tvMinOrderValue,
            tvMaxUsageValue, tvStartValue, tvEndValue, tvStatus;

    private int voucherId;
    private LinearLayout layoutStatus;
    private ImageView imgStatus;
    private String token;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_detail);

        initViews();
        setupToolbar();

        voucherId = getIntent().getIntExtra("voucherId", -1);
        if (voucherId == -1) {
            finish();
            return;
        }

        token = SessionManager.getToken(this);

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadVoucher();
    }

    private void initViews() {

        View rowType = findViewById(R.id.rowType);
        View rowValue = findViewById(R.id.rowValue);
        View rowMinOrder = findViewById(R.id.rowMinOrder);
        View rowMaxUsage = findViewById(R.id.rowMaxUsage);
        View rowStart = findViewById(R.id.rowStart);
        View rowEnd = findViewById(R.id.rowEnd);

        tvCode = findViewById(R.id.tvCode);
        tvStatus = findViewById(R.id.tvStatus);

        layoutStatus = findViewById(R.id.layoutStatus);
        imgStatus = findViewById(R.id.imgStatus);

        tvTypeValue = rowType.findViewById(R.id.tvValue);
        tvValueValue = rowValue.findViewById(R.id.tvValue);
        tvMinOrderValue = rowMinOrder.findViewById(R.id.tvValue);
        tvMaxUsageValue = rowMaxUsage.findViewById(R.id.tvValue);
        tvStartValue = rowStart.findViewById(R.id.tvValue);
        tvEndValue = rowEnd.findViewById(R.id.tvValue);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadVoucher() {

        ApiClient.getVoucherById(
                this,
                token,
                voucherId,
                new ApiClient.DataCallback<>() {
                    @Override
                    public void onSuccess(Voucher voucher, String message) {

                        if (voucher == null) {
                            Toast.makeText(VoucherDetailActivity.this,
                                    "Voucher không tồn tại",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        bindData(voucher);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(VoucherDetailActivity.this,
                                error,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
    }

    private void bindData(Voucher voucher) {

        NumberFormat format =
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        tvCode.setText(voucher.getCode());
        tvTypeValue.setText(voucher.getDiscountType());

        if ("Percent".equalsIgnoreCase(voucher.getDiscountType())) {
            tvValueValue.setText(voucher.getDiscountValue() + "%");
        } else {
            tvValueValue.setText(format.format(voucher.getDiscountValue()));
        }

        tvMinOrderValue.setText(format.format(voucher.getMinOrderValue()));
        tvMaxUsageValue.setText(String.valueOf(voucher.getMaxUsageCount()));
        tvStartValue.setText(voucher.getStartDate());
        tvEndValue.setText(voucher.getExpiryDate());
        if (voucher.isActive()) {
            tvStatus.setText("Active");
            layoutStatus.setBackgroundResource(R.drawable.status_bg);
            imgStatus.setImageResource(R.drawable.ic_check);
        } else {
            tvStatus.setText("Inactive");
            layoutStatus.setBackgroundResource(R.drawable.status_bg_inactive);
            imgStatus.setImageResource(R.drawable.ic_close);
        }
    }
}