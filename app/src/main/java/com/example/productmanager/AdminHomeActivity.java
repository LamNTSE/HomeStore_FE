package com.example.productmanager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class AdminHomeActivity extends AppCompatActivity {

    Button btnManageProducts, btnManageOrders, btnManageUsers, btnAddProduct, btnManageFeedbacks, btnLogout, btnChat;
    ImageView btnStoreLocation;
        FrameLayout layoutChatBadge, layoutOrderBadge;
        private BadgeDrawable chatBadge, orderBadge;
        private String token;
        private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        btnManageProducts = findViewById(R.id.btnManageProducts);
        btnManageOrders = findViewById(R.id.btnManageOrders);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnAddProduct = findViewById(R.id.btnManageVouchers);
        btnManageFeedbacks = findViewById(R.id.btnManageFeedbacks);
        btnLogout = findViewById(R.id.btnLogout);
        btnChat = findViewById(R.id.btnChat);
        btnStoreLocation = findViewById(R.id.btnStoreLocation);
        layoutChatBadge = findViewById(R.id.layoutChatBadge);
        layoutOrderBadge = findViewById(R.id.layoutOrderBadge);

        token = SessionManager.getToken(this);
        currentUserId = SessionManager.getUserId(this);
        initChatBadge();
        initOrderBadge();

        btnManageProducts.setOnClickListener(v ->
                startActivity(new Intent(this, ManageProductsActivity.class)));

        btnManageOrders.setOnClickListener(v ->
                startActivity(new Intent(this, ManageOrdersActivity.class)));

        btnManageUsers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        btnAddProduct.setOnClickListener(v ->
                startActivity(new Intent(this, ManageVouchersActivity.class)));

        btnManageFeedbacks.setOnClickListener(v ->
                startActivity(new Intent(this, ManageFeedbacksActivity.class)));

        btnChat.setOnClickListener(v ->
                startActivity(new Intent(this, AdminChatListActivity.class)));

        btnStoreLocation.setOnClickListener(v ->
                startActivity(new Intent(this, AdminStoreLocationActivity.class)));

        btnLogout.setOnClickListener(v -> {
            SignalRManager.getInstance().disconnectAll();
            SessionManager.clear(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

        @Override
        protected void onResume() {
                super.onResume();
                refreshUnreadChatBadge();
                refreshPendingOrdersCount();

                SignalRManager.getInstance().connectChat(token);
                SignalRManager.getInstance().setChatMessageListener(messageJson -> refreshUnreadChatBadge());
        }

        @Override
        protected void onPause() {
                super.onPause();
                SignalRManager.getInstance().setChatMessageListener(null);
        }

        private void initChatBadge() {
                chatBadge = BadgeDrawable.create(this);
                chatBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
                chatBadge.setBadgeTextColor(Color.WHITE);
                chatBadge.setBadgeGravity(BadgeDrawable.TOP_END);
                chatBadge.setVerticalOffset(dpToPx(8));
                chatBadge.setHorizontalOffset(dpToPx(8));
                chatBadge.setVisible(false);
        }

        private void initOrderBadge() {
                orderBadge = BadgeDrawable.create(this);
                orderBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
                orderBadge.setBadgeTextColor(Color.WHITE);
                orderBadge.setBadgeGravity(BadgeDrawable.TOP_END);
                orderBadge.setVerticalOffset(dpToPx(8));
                orderBadge.setHorizontalOffset(dpToPx(8));
                orderBadge.setVisible(false);
        }

        private void refreshUnreadChatBadge() {
                ApiClient.getUnreadMessages(this, token, new ApiClient.DataCallback<JSONArray>() {
                        @Override
                        public void onSuccess(JSONArray data, String message) {
                                int unreadCount = data.length();
                                updateChatBadge(unreadCount);
                        }

                        @Override
                        public void onError(String errorMessage) {
                                updateChatBadge(0);
                        }
                });
        }

        private void updateChatBadge(int unreadCount) {
                if (unreadCount > 0) {
                        chatBadge.setVisible(true);
                        chatBadge.setNumber(unreadCount);
                        BadgeUtils.attachBadgeDrawable(chatBadge, btnChat, layoutChatBadge);
                } else {
                        chatBadge.clearNumber();
                        chatBadge.setVisible(false);
                        BadgeUtils.detachBadgeDrawable(chatBadge, btnChat);
                }
        }

        private void refreshPendingOrdersCount() {
                if (token == null || token.isEmpty()) {
                        updateOrderBadge(0);
                        return;
                }

                ApiClient.getAllOrders(this, token, new ApiClient.DataCallback<List<Order>>() {
                        @Override
                        public void onSuccess(List<Order> orders, String message) {
                                int pendingCount = 0;
                                for (Order order : orders) {
                                        if ("Pending".equalsIgnoreCase(order.getStatus())) {
                                                pendingCount++;
                                        }
                                }
                                updateOrderBadge(pendingCount);
                        }

                        @Override
                        public void onError(String errorMessage) {
                                updateOrderBadge(0);
                        }
                });
        }

        private void updateOrderBadge(int count) {
                if (orderBadge == null || btnManageOrders == null || layoutOrderBadge == null) return;

                if (count > 0) {
                        orderBadge.setVisible(true);
                        orderBadge.setNumber(count);
                        BadgeUtils.attachBadgeDrawable(orderBadge, btnManageOrders, layoutOrderBadge);
                } else {
                        orderBadge.clearNumber();
                        orderBadge.setVisible(false);
                        BadgeUtils.detachBadgeDrawable(orderBadge, btnManageOrders);
                }
        }

        private int dpToPx(int dp) {
                return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
        }
}