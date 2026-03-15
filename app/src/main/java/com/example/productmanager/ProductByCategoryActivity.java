package com.example.productmanager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ProductByCategoryActivity extends AppCompatActivity {

    ListView lvProduct;

    ArrayList<Product> productList;

    ProductAdapter adapter;

    String token;

    int categoryId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_category);

        lvProduct = findViewById(R.id.lvProduct);

        token = SessionManager.getToken(this);

        categoryId = getIntent().getIntExtra("categoryId",-1);

        productList = new ArrayList<>();

        adapter = new ProductAdapter(
                this,
                R.layout.item_product,
                productList,
                null,
                false
        );

        lvProduct.setAdapter(adapter);

        loadProducts();
    }

    private void loadProducts(){

        ApiClient.getProductsByCategory(this,
                token,
                categoryId,
                new ApiClient.DataCallback<List<Product>>() {

                    @Override
                    public void onSuccess(List<Product> data, String message) {

                        productList.clear();

                        if(data != null){
                            productList.addAll(data);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String errorMessage) {

                        Toast.makeText(ProductByCategoryActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}