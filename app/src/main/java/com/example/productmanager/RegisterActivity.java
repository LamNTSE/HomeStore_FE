package com.example.productmanager;
import com.example.productmanager.ApiClient.DataCallback;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtEmail, edtPassword, edtPhone, edtAddress;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupEvents();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupEvents() {
        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {

        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        // ===== VALIDATION =====
        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            edtPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            return;
        }

        if (TextUtils.isEmpty(address)) {
            edtAddress.setError("Vui lòng nhập địa chỉ");
            return;
        }

        btnRegister.setEnabled(false);

        // ===== CALL API =====
        ApiClient.register(
                this,
                fullName,
                email,
                password,
                phone,
                address,
                new DataCallback<String>() {
                    @Override
                    public void onSuccess(String data, String message) {

                        runOnUiThread(() -> {
                            btnRegister.setEnabled(true);
                            Toast.makeText(RegisterActivity.this,
                                    message,
                                    Toast.LENGTH_LONG).show();

                            // Chuyển về Login sau khi đăng ký thành công
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnRegister.setEnabled(true);
                            Toast.makeText(RegisterActivity.this,
                                    error,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }
}