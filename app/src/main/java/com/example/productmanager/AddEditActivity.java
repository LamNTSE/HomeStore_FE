package com.example.productmanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class AddEditActivity extends AppCompatActivity {

    EditText edtName, edtDesc, edtPrice;
    Button btnSave, btnUpload, btnCancel;
    ImageView imgPreview;
    TextView tvHeaderTitle;

    int productId = -1;
    final int REQUEST_CODE_FOLDER = 123;
    String authToken;
    String currentImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        initViews();

        authToken = SessionManager.getToken(this);
        if (authToken.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!SessionManager.isAdmin(this)) {
            Toast.makeText(this, "Bạn không có quyền thao tác sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handleIntentData();

        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_FOLDER);
        });

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void initViews() {
        edtName = findViewById(R.id.edtName);
        edtDesc = findViewById(R.id.edtDesc);
        edtPrice = findViewById(R.id.edtPrice);
        btnSave = findViewById(R.id.btnSave);
        btnUpload = findViewById(R.id.btnUpload);
        btnCancel = findViewById(R.id.btnCancel);
        imgPreview = findViewById(R.id.imgPreview);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra("PRODUCT_ID") || intent.hasExtra("id")) {
            productId = intent.getIntExtra("PRODUCT_ID", -1);
            if (productId == -1) {
                productId = intent.getIntExtra("id", -1);
            }

            tvHeaderTitle.setText("CẬP NHẬT SẢN PHẨM");
            loadProductData(productId);

        } else {
            tvHeaderTitle.setText("THÊM SẢN PHẨM");
        }
    }

    private void loadProductData(int id) {
        ApiClient.getProductById(this, authToken, id, new ApiClient.DataCallback<Product>() {
            @Override
            public void onSuccess(Product data, String message) {
                edtName.setText(data.getName());
                edtDesc.setText(data.getDescription() == null ? "" : data.getDescription());
                edtPrice.setText(String.valueOf(data.getPrice()));
                currentImageUrl = data.getImageUrl();
                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(AddEditActivity.this)
                            .load(currentImageUrl)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(imgPreview);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AddEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void saveProduct() {
        try {
            String name = edtName.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Tên và giá không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            btnSave.setEnabled(false);

            if (productId == -1) {
                ApiClient.createProduct(this, authToken, name, desc, price, currentImageUrl,
                        new ApiClient.DataCallback<Void>() {
                            @Override
                            public void onSuccess(Void data, String message) {
                                btnSave.setEnabled(true);
                                Toast.makeText(AddEditActivity.this,
                                        message.isEmpty() ? "Thêm thành công!" : message,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                btnSave.setEnabled(true);
                                Toast.makeText(AddEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                ApiClient.updateProduct(this, authToken, productId, name, desc, price, currentImageUrl,
                        new ApiClient.DataCallback<Void>() {
                            @Override
                            public void onSuccess(Void data, String message) {
                                btnSave.setEnabled(true);
                                Toast.makeText(AddEditActivity.this,
                                        message.isEmpty() ? "Cập nhật thành công!" : message,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                btnSave.setEnabled(true);
                                Toast.makeText(AddEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá tiền phải là số!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imgPreview.setImageBitmap(bitmap);
                currentImageUrl = null;
                Toast.makeText(this,
                        "Ảnh local chỉ dùng preview. API hiện nhận imageUrl từ server.",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi đọc ảnh!", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}