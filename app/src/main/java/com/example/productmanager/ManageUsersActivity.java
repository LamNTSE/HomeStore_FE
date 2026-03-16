package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ManageUsersActivity extends AppCompatActivity implements UserAdapter.UserActionListener {

    ListView listView;
    FloatingActionButton fabAdd;
    EditText edtSearch;

    JSONArray allUsers = new JSONArray();
    JSONArray filteredUsers = new JSONArray();
    UserAdapter adapter;
    String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        listView = findViewById(R.id.listViewUsers);
        fabAdd = findViewById(R.id.fabAddUser);
        edtSearch = findViewById(R.id.edtSearchUser);

        authToken = SessionManager.getToken(this);

        if (authToken.isEmpty() || !SessionManager.isAdmin(this)) {
            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new UserAdapter(this, filteredUsers, this);
        listView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showUserFormDialog(null));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        ApiClient.getUsers(this, authToken, new ApiClient.DataCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data, String message) {
                allUsers = data;
                filterUsers(edtSearch.getText().toString().trim());
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ManageUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String keyword) {
        filteredUsers = new JSONArray();

        if (keyword.isEmpty()) {
            filteredUsers = allUsers;
        } else {
            String lower = keyword.toLowerCase();
            for (int i = 0; i < allUsers.length(); i++) {
                JSONObject u = allUsers.optJSONObject(i);
                if (u == null) continue;
                String name = u.optString("fullName", "").toLowerCase();
                String email = u.optString("email", "").toLowerCase();
                if (name.contains(lower) || email.contains(lower)) {
                    filteredUsers.put(u);
                }
            }
        }

        adapter = new UserAdapter(this, filteredUsers, this);
        listView.setAdapter(adapter);
    }

    // ── USER FORM DIALOG ────────────────────────────────────────────────────

    private void showUserFormDialog(JSONObject existingUser) {
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

        boolean isEdit = existingUser != null;

        if (isEdit) {
            edtFullName.setText(existingUser.optString("fullName", ""));
            edtEmail.setText(existingUser.optString("email", ""));
            edtEmail.setEnabled(false);
            edtPhone.setText(existingUser.optString("phone", ""));
            edtAddress.setText(existingUser.optString("address", ""));

            layoutPassword.setHint("Mật khẩu mới (để trống nếu giữ nguyên)");

            String role = existingUser.optString("role", "Customer");
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equalsIgnoreCase(role)) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Chỉnh sửa người dùng" : "Thêm người dùng mới")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String fullName = edtFullName.getText() != null ? edtFullName.getText().toString().trim() : "";
                    String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
                    String password = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
                    String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
                    String address = edtAddress.getText() != null ? edtAddress.getText().toString().trim() : "";
                    String role = spinnerRole.getSelectedItem().toString();

                    if (fullName.isEmpty() || email.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập họ tên và email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isEdit && password.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEdit) {
                        updateUser(existingUser.optInt("userId"), fullName, phone, address, role, password);
                    } else {
                        createUser(fullName, email, password, phone, address, role);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createUser(String fullName, String email, String password,
                            String phone, String address, String role) {
        JSONObject body = new JSONObject();
        try {
            body.put("fullName", fullName);
            body.put("email", email);
            body.put("password", password);
            if (!phone.isEmpty()) body.put("phone", phone);
            if (!address.isEmpty()) body.put("address", address);
            body.put("role", role);
        } catch (JSONException e) {
            Toast.makeText(this, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.createUser(this, authToken, body, new ApiClient.DataCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                Toast.makeText(ManageUsersActivity.this, message, Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ManageUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUser(int userId, String fullName, String phone,
                            String address, String role, String newPassword) {
        JSONObject body = new JSONObject();
        try {
            body.put("fullName", fullName);
            if (!phone.isEmpty()) body.put("phone", phone);
            if (!address.isEmpty()) body.put("address", address);
            body.put("role", role);
            if (!newPassword.isEmpty()) body.put("newPassword", newPassword);
        } catch (JSONException e) {
            Toast.makeText(this, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.updateUser(this, authToken, userId, body, new ApiClient.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data, String message) {
                Toast.makeText(ManageUsersActivity.this, message, Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ManageUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── LISTENER CALLBACKS ──────────────────────────────────────────────────

    @Override
    public void onEditUser(JSONObject user) {
        showUserFormDialog(user);
    }

    @Override
    public void onDeleteUser(JSONObject user) {
        int userId = user.optInt("userId");
        String name = user.optString("fullName", "");

        new AlertDialog.Builder(this)
                .setTitle("Xóa người dùng")
                .setMessage("Bạn có chắc chắn muốn xóa \"" + name + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    ApiClient.deleteUser(this, authToken, userId, new ApiClient.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data, String message) {
                            Toast.makeText(ManageUsersActivity.this, message, Toast.LENGTH_SHORT).show();
                            loadUsers();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(ManageUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onViewUser(JSONObject user) {
        int userId = user.optInt("userId");
        Intent intent = new Intent(this, UserDetailActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}
