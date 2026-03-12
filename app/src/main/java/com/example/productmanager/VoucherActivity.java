package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class VoucherActivity extends BaseCustomerActivity {

    private RecyclerView rvVoucher;
    private List<Voucher> voucherList;
    private VoucherAdapter adapter;
    private String token;

    // thêm mới
    private EditText edtVoucherCode;
    private Button btnCheckVoucher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher);

        rvVoucher = findViewById(R.id.rvVoucher);

        // thêm mới
        edtVoucherCode = findViewById(R.id.edtVoucherCode);
        btnCheckVoucher = findViewById(R.id.btnCheckVoucher);

        // lấy token
        token = SessionManager.getToken(this);

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        voucherList = new ArrayList<>();

        // adapter chọn voucher
        adapter = new VoucherAdapter(
                this,
                voucherList,
                voucher -> {

                    Intent intent = new Intent();
                    intent.putExtra("voucherCode", voucher.getCode());

                    setResult(RESULT_OK, intent);
                    finish();
                }
        );

        rvVoucher.setLayoutManager(new LinearLayoutManager(this));
        rvVoucher.setAdapter(adapter);

        loadVouchers();

        // check voucher theo code
        btnCheckVoucher.setOnClickListener(v -> {

            String code = edtVoucherCode.getText().toString().trim();

            if (code.isEmpty()) {
                Toast.makeText(this, "Nhập mã voucher", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiClient.getVoucherByCode(
                    this,
                    token,
                    code,
                    new ApiClient.DataCallback<Voucher>() {

                        @Override
                        public void onSuccess(Voucher voucher, String message) {

                            if (!voucher.isActive()) {
                                Toast.makeText(
                                        VoucherActivity.this,
                                        "Voucher đã hết hiệu lực",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            Intent intent = new Intent();
                            intent.putExtra("voucherCode", voucher.getCode());

                            setResult(RESULT_OK, intent);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {

                            Toast.makeText(
                                    VoucherActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        });
    }

    private void loadVouchers() {

        ApiClient.getAvailableVouchers(
                this,
                token,
                new ApiClient.DataCallback<List<Voucher>>() {

                    @Override
                    public void onSuccess(List<Voucher> data, String message) {

                        voucherList.clear();

                        if (data != null) {
                            voucherList.addAll(data);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                VoucherActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}