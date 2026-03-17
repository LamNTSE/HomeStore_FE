package com.example.productmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class UserDetailActivity extends AppCompatActivity {

    String authToken;
    int userId;
    JSONObject currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        authToken = SessionManager.getToken(this);
        userId = getIntent().getIntExtra("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "Không tìm thấy user", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnEditUser).setOnClickListener(v -> {
            if (currentUser != null) showEditDialog();
        });

        loadUserDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDetail();
    }

    private void loadUserDetail() {
        ApiClient.getUserById(this, authToken, userId, new ApiClient.DataCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                currentUser = data;
                displayUser(data);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(UserDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUser(JSONObject user) {
        setDetailRow(findViewById(R.id.rowFullName), "Họ và tên", user.optString("fullName", "N/A"));
        setDetailRow(findViewById(R.id.rowEmail), "Email", user.optString("email", "N/A"));
        setDetailRow(findViewById(R.id.rowPhone), "Số điện thoại", user.optString("phone", "N/A"));
        setDetailRow(findViewById(R.id.rowAddress), "Địa chỉ", user.optString("address", "N/A"));
        setDetailRow(findViewById(R.id.rowRole), "Vai trò", user.optString("role", "N/A"));
        setDetailRow(findViewById(R.id.rowProvider), "Đăng ký qua", user.optString("provider", "N/A"));
        setDetailRow(findViewById(R.id.rowCreatedAt), "Ngày tạo", user.optString("createdAt", "N/A"));
    }

    private void setDetailRow(View row, String label, String value) {
        if (row == null) return;
        TextView tvLabel = row.findViewById(R.id.tvLabel);
        TextView tvValue = row.findViewById(R.id.tvValue);
        if (tvLabel != null) tvLabel.setText(label);
        if (tvValue != null) tvValue.setText(value != null && !value.equals("null") ? value : "N/A");
    }

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_form, null);

        TextInputEditText edtFullName = dialogView.findViewById(R.id.edtFullName);
        TextInputEditText edtEmail = dialogView.findViewById(R.id.edtEmail);
        TextInputEditText edtPassword = dialogView.findViewById(R.id.edtPassword);
        TextInputEditText edtPhone = dialogView.findViewById(R.id.edtPhone);
        TextInputEditText edtAddress = dialogView.findViewById(R.id.edtAddress);
        TextInputLayout layoutPassword = dialogView.findViewById(R.id.layoutPassword);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinnerRole);

        String[] roles = {"Customer", "Admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRole.setAdapter(roleAdapter);

        edtFullName.setText(currentUser.optString("fullName", ""));
        edtEmail.setText(currentUser.optString("email", ""));
        edtEmail.setEnabled(false);
        edtPhone.setText(currentUser.optString("phone", ""));
        edtAddress.setText(currentUser.optString("address", ""));
        layoutPassword.setHint("Mật khẩu mới (để trống nếu giữ nguyên)");

        String role = currentUser.optString("role", "Customer");
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(role)) {
                spinnerRole.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa người dùng")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String fullName = edtFullName.getText() != null ? edtFullName.getText().toString().trim() : "";
                    String password = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
                    String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
                    String address = edtAddress.getText() != null ? edtAddress.getText().toString().trim() : "";
                    String selectedRole = spinnerRole.getSelectedItem().toString();

                    if (fullName.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject body = new JSONObject();
                    try {
                        body.put("fullName", fullName);
                        if (!phone.isEmpty()) body.put("phone", phone);
                        if (!address.isEmpty()) body.put("address", address);
                        body.put("role", selectedRole);
                        if (!password.isEmpty()) body.put("newPassword", password);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ApiClient.updateUser(this, authToken, userId, body, new ApiClient.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data, String message) {
                            Toast.makeText(UserDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                            loadUserDetail();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(UserDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
