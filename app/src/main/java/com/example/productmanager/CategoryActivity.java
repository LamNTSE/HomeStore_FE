package com.example.productmanager;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    ListView lvCategory;

    ArrayList<Category> categoryList;
    CategoryAdapter adapter;

    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        lvCategory = findViewById(R.id.lvCategory);

        token = SessionManager.getToken(this);

        if (token.isEmpty()) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        categoryList = new ArrayList<>();

        adapter = new CategoryAdapter(this, categoryList);

        lvCategory.setAdapter(adapter);

        loadCategories();
    }

    // =============================
    // LOAD CATEGORY
    // =============================

    private void loadCategories() {

        ApiClient.getCategories(
                this,
                token,
                new ApiClient.DataCallback<List<Category>>() {

                    @Override
                    public void onSuccess(List<Category> data, String message) {

                        categoryList.clear();

                        if (data != null) {
                            categoryList.addAll(data);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(
                                CategoryActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
