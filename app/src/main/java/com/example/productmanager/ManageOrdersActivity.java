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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ManageOrdersActivity extends AppCompatActivity implements OrderAdapter.OrderActionListener {

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

        adapter = new OrderAdapter(this, orderList, true, this);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();

        // Real-time: new order from any customer
        String token = SessionManager.getToken(this);
        SignalRManager.getInstance().connectOrders(token);
        SignalRManager.getInstance().setNewOrderListener(orderJson -> {
            // Reload full list so new order appears at correct position
            loadOrders();
        });
        // Real-time: a customer cancelled or confirmed delivery
        SignalRManager.getInstance().setOrderUpdateListener(orderJson -> {
            try {
                JSONObject updated = new JSONObject(orderJson);
                int updatedId = updated.optInt("orderId", -1);
                for (int i = 0; i < orderList.size(); i++) {
                    if (orderList.get(i).getOrderId() == updatedId) {
                        loadOrders();
                        break;
                    }
                }
            } catch (JSONException ignored) {}
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SignalRManager.getInstance().setNewOrderListener(null);
        SignalRManager.getInstance().setOrderUpdateListener(null);
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

    @Override
    public void onConfirm(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đơn hàng")
                .setMessage("Chuyển đơn hàng #" + order.getOrderId() + " sang trạng thái Shipping?")
                .setPositiveButton("Xác nhận", (d, w) -> {
                    String token = SessionManager.getToken(this);
                    ApiClient.updateOrderStatus(this, token, order.getOrderId(), "Shipping",
                            new ApiClient.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data, String message) {
                                    Toast.makeText(ManageOrdersActivity.this, message, Toast.LENGTH_SHORT).show();
                                    loadOrders();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(ManageOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
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
                    ApiClient.updateOrderStatus(this, token, order.getOrderId(), "Cancelled",
                            new ApiClient.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data, String message) {
                                    Toast.makeText(ManageOrdersActivity.this, message, Toast.LENGTH_SHORT).show();
                                    loadOrders();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(ManageOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Không", null)
                .show();
    }

    @Override
    public void onFeedbackClick(Order order) {
        // Admin does not use feedback click
    }
}
