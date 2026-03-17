package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserOrdersActivity extends BaseCustomerActivity implements OrderAdapter.OrderActionListener {

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

        String token = SessionManager.getToken(this);

        SignalRManager.getInstance().connectOrders(token);

        SignalRManager.getInstance().setOrderUpdateListener(orderJson -> {
            try {

                JSONObject updated = new JSONObject(orderJson);

                int updatedId = updated.optInt("orderId", -1);

                for (Order o : orderList) {

                    if (o.getOrderId() == updatedId) {

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
        SignalRManager.getInstance().setOrderUpdateListener(null);
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

                adapter.updateFeedbackStatus(feedbacks);

            }

            @Override
            public void onError(String error) {
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
