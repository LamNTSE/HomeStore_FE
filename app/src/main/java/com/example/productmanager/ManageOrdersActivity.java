package com.example.productmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        ImageView btnBack = findViewById(R.id.btnBackOrders);
        rvOrders = findViewById(R.id.rvOrders);
        tvEmpty = findViewById(R.id.tvEmptyOrders);

        btnBack.setOnClickListener(v -> finish());

        adapter = new OrderAdapter(this, orderList);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        String token = SessionManager.getToken(this);
        ApiClient.getAllOrders(this, token, new ApiClient.DataCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> data, String message) {
                orderList.clear();
                orderList.addAll(data);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ManageOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
