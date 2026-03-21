package com.example.productmanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddEditActivity extends AppCompatActivity {

    EditText edtName, edtDesc, edtPrice, edtStockQuantity;
    Button btnSave, btnUpload, btnCancel;
    ImageView imgPreview;
    TextView tvHeaderTitle;
    Spinner spCategory;

    int productId = -1;
    int selectedCategoryId = -1;

    final int REQUEST_CODE_FOLDER = 123;

    String authToken;
    String currentImageUrl;

    List<Category> categoryList = new ArrayList<>();
    List<String> categoryNames = new ArrayList<>();


    // ================= FORMAT VND =================
    private String formatVND(double amount) {
        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        initViews();
        setupPriceFormat();

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

        loadCategories();
        handleIntentData();

        btnUpload.setOnClickListener(v -> openImagePicker());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProduct());
    }


    @SuppressLint("WrongViewCast")
    private void initViews() {

        edtName = findViewById(R.id.edtName);
        edtDesc = findViewById(R.id.edtDesc);
        edtPrice = findViewById(R.id.edtPrice);
        edtStockQuantity = findViewById(R.id.edtStock);

        btnSave = findViewById(R.id.btnSave);
        btnUpload = findViewById(R.id.btnUpload);
        btnCancel = findViewById(R.id.btnCancel);

        imgPreview = findViewById(R.id.imgPreview);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        spCategory = findViewById(R.id.spCategory);
    }


    // ================= FORMAT PRICE =================
    private void setupPriceFormat() {

        edtPrice.addTextChangedListener(new TextWatcher() {

            private String current = "";

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!s.toString().equals(current)) {

                    edtPrice.removeTextChangedListener(this);

                    String clean = s.toString().replaceAll("[^0-9]", "");

                    if (!clean.isEmpty()) {

                        double parsed = Double.parseDouble(clean);
                        String formatted = formatVND(parsed);

                        current = formatted;

                        edtPrice.setText(formatted);
                        edtPrice.setSelection(formatted.length());
                    }

                    edtPrice.addTextChangedListener(this);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }


    // ================= LOAD CATEGORY =================
    private void loadCategories() {

        ApiClient.getCategories(this, authToken, new ApiClient.DataCallback<List<Category>>() {

            @Override
            public void onSuccess(List<Category> data, String message) {

                categoryList.clear();
                categoryNames.clear();

                categoryList.addAll(data);

                for (Category c : data) {
                    categoryNames.add(c.getCategoryName());
                }

                ArrayAdapter<String> adapter =
                        new ArrayAdapter<>(
                                AddEditActivity.this,
                                android.R.layout.simple_spinner_item,
                                categoryNames
                        );

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCategory.setAdapter(adapter);

                spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                        if (position >= 0 && position < categoryList.size()) {
                            selectedCategoryId = categoryList.get(position).getCategoryId();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AddEditActivity.this, "Lỗi tải category!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // ================= HANDLE EDIT MODE =================
    private void handleIntentData() {

        Intent intent = getIntent();

        if (intent.hasExtra("PRODUCT_ID") || intent.hasExtra("id")) {

            productId = intent.getIntExtra("PRODUCT_ID", -1);

            if (productId == -1)
                productId = intent.getIntExtra("id", -1);

            tvHeaderTitle.setText("CẬP NHẬT SẢN PHẨM");

            loadProductData(productId);
        }
        else {

            tvHeaderTitle.setText("THÊM SẢN PHẨM");
        }
    }


    // ================= LOAD PRODUCT =================
    private void loadProductData(int id) {

        ApiClient.getProductById(this, authToken, id, new ApiClient.DataCallback<Product>() {

            @Override
            public void onSuccess(Product p, String message) {

                edtName.setText(p.getName());
                edtDesc.setText(p.getDescription());
                edtPrice.setText(formatVND(p.getPrice()));
                edtStockQuantity.setText(String.valueOf(p.getStockQuantity()));

                currentImageUrl = p.getImageUrl();

                if (currentImageUrl != null && !currentImageUrl.isEmpty()) {

                    com.bumptech.glide.Glide
                            .with(AddEditActivity.this)
                            .load(currentImageUrl)
                            .placeholder(R.mipmap.ic_launcher)
                            .into(imgPreview);
                }

                for (int i = 0; i < categoryList.size(); i++) {

                    if (categoryList.get(i).getCategoryId() == p.getCategoryId()) {

                        spCategory.setSelection(i);
                        selectedCategoryId = p.getCategoryId();
                        break;
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {

                Toast.makeText(AddEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    // ================= SAVE PRODUCT =================
    private void saveProduct() {

        String name = edtName.getText().toString().trim();
        String desc = edtDesc.getText().toString().trim();

        String priceStr = edtPrice.getText().toString().replaceAll("[^0-9]", "");
        String stockStr = edtStockQuantity.getText().toString().replaceAll("[^0-9]", "");

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {

            Toast.makeText(this, "Vui lòng nhập đầy đủ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔥 CHECK ẢNH (QUAN TRỌNG)
        if (currentImageUrl == null || currentImageUrl.isEmpty()) {
            Toast.makeText(this, "Vui lòng upload ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int stockQuantity = Integer.parseInt(stockStr);

        Category selected = categoryList.get(spCategory.getSelectedItemPosition());
        selectedCategoryId = selected.getCategoryId();

        if (selectedCategoryId <= 0) {

            Toast.makeText(this, "Vui lòng chọn Category", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        if (productId == -1) {

            ApiClient.createProduct(
                    this, authToken, name, desc, price,
                    currentImageUrl, selectedCategoryId, stockQuantity,

                    new ApiClient.DataCallback<Void>() {

                        @Override
                        public void onSuccess(Void data, String message) {

                            btnSave.setEnabled(true);

                            Toast.makeText(
                                    AddEditActivity.this,
                                    message.isEmpty() ? "Thêm thành công!" : message,
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {

                            btnSave.setEnabled(true);

                            Toast.makeText(AddEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }

        else {

            ApiClient.updateProduct(
                    this, authToken, productId, name, desc, price,
                    currentImageUrl, selectedCategoryId, stockQuantity,

                    new ApiClient.DataCallback<Void>() {

                        @Override
                        public void onSuccess(Void data, String message) {

                            btnSave.setEnabled(true);

                            Toast.makeText(
                                    AddEditActivity.this,
                                    message.isEmpty() ? "Cập nhật thành công!" : message,
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {

                            btnSave.setEnabled(true);

                            Toast.makeText(AddEditActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }


    // ================= IMAGE PICKER =================
    private void openImagePicker() {

        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");

        startActivityForResult(pick, REQUEST_CODE_FOLDER);
    }


    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {

        if (req == REQUEST_CODE_FOLDER && res == RESULT_OK && data != null) {

            Uri uri = data.getData();

            try {

                // Preview ảnh
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bm = BitmapFactory.decodeStream(is);
                imgPreview.setImageBitmap(bm);

                // 🔥 UPLOAD ẢNH LÊN SERVER
                ApiClient.uploadImage(this, authToken, uri, new ApiClient.DataCallback<String>() {

                    @Override
                    public void onSuccess(String filePath, String message) {
                        // Ghép domain + filePath

                        currentImageUrl = filePath;

                        // Load bằng Glide
                        Glide.with(AddEditActivity.this)
                                .load(currentImageUrl)
                                .into(imgPreview);

                        Toast.makeText(AddEditActivity.this, "Upload thành công!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                AddEditActivity.this,
                                "Upload lỗi: " + errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            }
            catch (Exception e) {

                Toast.makeText(this, "Lỗi ảnh!", Toast.LENGTH_SHORT).show();
            }
        }

        super.onActivityResult(req, res, data);
    }
}