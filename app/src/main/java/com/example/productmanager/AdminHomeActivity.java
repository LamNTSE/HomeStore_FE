package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AdminHomeActivity extends AppCompatActivity {

    Button btnManageProducts, btnManageOrders, btnManageUsers, btnAddProduct, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        btnManageProducts = findViewById(R.id.btnManageProducts);
        btnManageOrders = findViewById(R.id.btnManageOrders);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnAddProduct = findViewById(R.id.btnManageVouchers);
        btnLogout = findViewById(R.id.btnLogout);

        btnManageProducts.setOnClickListener(v ->
                startActivity(new Intent(this, ManageProductsActivity.class)));

        btnManageOrders.setOnClickListener(v ->
                startActivity(new Intent(this, ManageOrdersActivity.class)));

//        btnManageUsers.setOnClickListener(v ->
//                startActivity(new Intent(this, ManageUsersActivity.class)));

        btnAddProduct.setOnClickListener(v ->
                startActivity(new Intent(this, ManageVouchersActivity.class)));

        btnLogout.setOnClickListener(v -> {
            SessionManager.clear(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}