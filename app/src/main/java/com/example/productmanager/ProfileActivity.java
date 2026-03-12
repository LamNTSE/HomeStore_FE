package com.example.productmanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends BaseCustomerActivity {

    private TextView txtName, txtEmail, txtPhone, txtAddress;
    private ImageView imgAvatar, btnChangeAvatar;
    private String token;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap photo = (Bitmap) extras.get("data");
                    if (photo != null) {
                        imgAvatar.setImageBitmap(photo);
                        uploadAvatar(photo);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap photo = BitmapFactory.decodeStream(inputStream);
                            if (inputStream != null) inputStream.close();
                            if (photo != null) {
                                imgAvatar.setImageBitmap(photo);
                                uploadAvatar(photo);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        token = SessionManager.getToken(this);
        loadProfile();

        btnChangeAvatar.setOnClickListener(v -> showAvatarPickerDialog());
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.imgAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtPhone = findViewById(R.id.txtPhone);
        txtAddress = findViewById(R.id.txtAddress);
    }

    private void showAvatarPickerDialog() {
        String[] options = {"📷 Chụp ảnh", "🖼️ Chọn từ thư viện"};
        new AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        // Gallery
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void uploadAvatar(Bitmap bitmap) {
        // Resize to max 256x256 to keep Base64 string manageable
        int maxSize = 256;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale < 1) {
            width = Math.round(width * scale);
            height = Math.round(height * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        ApiClient.updateMyProfile(this, token, base64Image, new ApiClient.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data, String message) {
                Toast.makeText(ProfileActivity.this,
                        "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfile() {
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