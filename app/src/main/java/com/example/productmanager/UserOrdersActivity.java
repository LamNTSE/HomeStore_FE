package com.example.productmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserOrdersActivity extends AppCompatActivity implements OrderAdapter.OrderActionListener {

    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_orders);

        ImageView btnBack = findViewById(R.id.btnBackUserOrders);
        rvOrders = findViewById(R.id.rvUserOrders);
        tvEmpty = findViewById(R.id.tvEmptyUserOrders);

        btnBack.setOnClickListener(v -> finish());

        adapter = new OrderAdapter(this, orderList, false, this);
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
        ApiClient.getMyOrders(this, token, new ApiClient.DataCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> data, String message) {
                orderList.clear();
                orderList.addAll(data);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(UserOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConfirm(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận nhận hàng")
                .setMessage("Bạn đã nhận được đơn hàng #" + order.getOrderId() + "?")
                .setPositiveButton("Đã nhận", (d, w) -> {
                    String token = SessionManager.getToken(this);
                    ApiClient.confirmDelivery(this, token, order.getOrderId(),
                            new ApiClient.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data, String message) {
                                    Toast.makeText(UserOrdersActivity.this, message, Toast.LENGTH_SHORT).show();
                                    loadOrders();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(UserOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    @Override
    public void onCancel(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Huỷ đơn hàng")
                .setMessage("Bạn có chắc muốn huỷ đơn hàng #" + order.getOrderId() + "?")
                .setPositiveButton("Huỷ đơn", (d, w) -> {
                    String token = SessionManager.getToken(this);
                    ApiClient.cancelOrder(this, token, order.getOrderId(),
                            new ApiClient.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data, String message) {
                                    Toast.makeText(UserOrdersActivity.this, message, Toast.LENGTH_SHORT).show();
                                    loadOrders();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(UserOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Không", null)
                .show();
    }
}
