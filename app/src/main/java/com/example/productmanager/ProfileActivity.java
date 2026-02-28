package com.example.productmanager;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtName, txtEmail, txtPhone, txtAddress;
    private ImageView imgAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        loadProfile();
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtPhone = findViewById(R.id.txtPhone);
        txtAddress = findViewById(R.id.txtAddress);
    }

    private void loadProfile() {

        String token = SessionManager.getToken(this);

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getProfile(this, token, new ApiClient.DataCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject data, String message) {

                if (data == null) {
                    Toast.makeText(ProfileActivity.this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fullName = data.optString("fullName", "");
                String email = data.optString("email", "");
                String phone = data.optString("phone", "");
                String address = data.optString("address", "");
                String avatarUrl = data.optString("avatarUrl", null);

                txtName.setText(fullName);
                txtEmail.setText(email);
                txtPhone.setText(phone);
                txtAddress.setText(address);

                if (avatarUrl != null && !avatarUrl.equals("null") && !avatarUrl.isEmpty()) {
                    Glide.with(ProfileActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.ic_avatar)
                            .into(imgAvatar);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}