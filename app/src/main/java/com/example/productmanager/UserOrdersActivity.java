package com.example.productmanager;

import android.content.Intent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                loadFeedbackStatuses(token);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(UserOrdersActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFeedbackStatuses(String token) {
        ApiClient.getMyFeedbacks(this, token, new ApiClient.DataCallback<List<Feedback>>() {
            @Override
            public void onSuccess(List<Feedback> feedbacks, String message) {
                // Collect productIds that were reviewed. We need per-order status:
                // an order is "reviewed" if any of its items' productIds has a feedback.
                // Since we don't have order->productId map here, we track by productId.
                // The WriteFeedbackActivity will handle exact per-item state.
                // For the link text, we need to know if THIS order has ANY feedback.
                // We'll do a quick check: fetch order items for each delivered order.
                Map<Integer, Boolean> statusMap = new HashMap<>();
                // Build set of reviewed productIds
                java.util.Set<Integer> reviewedProducts = new java.util.HashSet<>();
                for (Feedback fb : feedbacks) reviewedProducts.add(fb.getProductId());

                // For each delivered order, check if any product in order was reviewed
                // We don't have order items locally, so we check per order via API
                List<Order> deliveredOrders = new ArrayList<>();
                for (Order o : orderList) {
                    if ("Delivered".equals(o.getStatus())) deliveredOrders.add(o);
                }
                if (deliveredOrders.isEmpty()) return;

                // Counter for async calls
                int[] remaining = {deliveredOrders.size()};
                for (Order order : deliveredOrders) {
                    ApiClient.getOrderById(UserOrdersActivity.this, token, order.getOrderId(),
                            new ApiClient.DataCallback<List<OrderItem>>() {
                                @Override
                                public void onSuccess(List<OrderItem> items, String msg) {
                                    boolean hasReview = false;
                                    for (OrderItem item : items) {
                                        if (reviewedProducts.contains(item.getProductId())) {
                                            hasReview = true;
                                            break;
                                        }
                                    }
                                    statusMap.put(order.getOrderId(), hasReview);
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        adapter.updateFeedbackStatus(statusMap);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        adapter.updateFeedbackStatus(statusMap);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onError(String error) {
                // ignore feedback status failure
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

    @Override
    public void onFeedbackClick(Order order) {
        Intent intent = new Intent(this, WriteFeedbackActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        startActivity(intent);
    }
}
