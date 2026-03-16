package com.example.productmanager;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Base class for all customer-facing Activities.
 *
 * Attaches a circular, draggable floating chat bubble to the bottom-right corner
 * of every customer screen. Tapping the bubble opens the customer support chat
 * (ChatActivity) with the admin user. Dragging repositions the bubble anywhere
 * on screen.
 *
 * Admin Activities must NOT extend this class.
 */
public abstract class BaseCustomerActivity extends AppCompatActivity {

    private static final int BUBBLE_SIZE_DP = 56;
    private static final int BUBBLE_MARGIN_DP = 16;

    private ImageButton bubbleBtn;
    private BadgeDrawable chatBadge;
    private String token;
    private int currentUserId;
    private int adminUserId = -1;
    private float dX, dY;
    private long touchStartTime;
    private static final long TAP_THRESHOLD_MS = 200;
    private static final float TAP_SLOP_DP = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        token = SessionManager.getToken(this);
        currentUserId = SessionManager.getUserId(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        token = SessionManager.getToken(this);
        currentUserId = SessionManager.getUserId(this);

        refreshUnreadBadge();

        if (token != null && !token.isEmpty()) {
            SignalRManager.getInstance().connectChat(token);
            SignalRManager.getInstance().setChatMessageListener(messageJson -> refreshUnreadBadge());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SignalRManager.getInstance().setChatMessageListener(null);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        addFloatingChatBubble();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        addFloatingChatBubble();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        addFloatingChatBubble();
    }

    // ─── Bubble setup ─────────────────────────────────────────────────────────

    private void addFloatingChatBubble() {
        int sizePx = dpToPx(BUBBLE_SIZE_DP);
        int marginPx = dpToPx(BUBBLE_MARGIN_DP);

        bubbleBtn = new ImageButton(this);
        bubbleBtn.setImageResource(R.drawable.ic_chat);
        bubbleBtn.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        bubbleBtn.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        bubbleBtn.setBackground(buildCircleBackground());
        bubbleBtn.setElevation(dpToPx(8));
        bubbleBtn.setContentDescription("Chat hỗ trợ");

        // Position: bottom-right corner (resolved after first layout)
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        lp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        lp.setMargins(0, 0, marginPx, marginPx);

        bubbleBtn.setOnTouchListener(new View.OnTouchListener() {
            private float startRawX, startRawY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        startRawX = event.getRawX();
                        startRawY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float distX = Math.abs(event.getRawX() - startRawX);
                        float distY = Math.abs(event.getRawY() - startRawY);
                        long elapsed = System.currentTimeMillis() - touchStartTime;
                        float slopPx = dpToPx(TAP_SLOP_DP);

                        if (elapsed < TAP_THRESHOLD_MS && distX < slopPx && distY < slopPx) {
                            // Treat as tap → open chat
                            openCustomerChat();
                        }
                        return true;
                }
                return false;
            }
        });

        addContentView(bubbleBtn, lp);
        initChatBadge();
        refreshUnreadBadge();
    }

    private void initChatBadge() {
        chatBadge = BadgeDrawable.create(this);
        chatBadge.setBackgroundColor(0xFFD32F2F);
        chatBadge.setBadgeTextColor(0xFFFFFFFF);
        chatBadge.setBadgeGravity(BadgeDrawable.TOP_END);
        chatBadge.setHorizontalOffset(dpToPx(2));
        chatBadge.setVerticalOffset(dpToPx(2));
        chatBadge.setVisible(false);
    }

    private void refreshUnreadBadge() {
        if (bubbleBtn == null || token == null || token.isEmpty()) {
            updateBubbleBadge(0);
            return;
        }

        if (adminUserId > 0) {
            loadUnreadFromAdmin(adminUserId);
            return;
        }

        ApiClient.getAdminUser(this, token, new ApiClient.DataCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                adminUserId = data.optInt("userId", -1);
                if (adminUserId > 0) {
                    loadUnreadFromAdmin(adminUserId);
                } else {
                    updateBubbleBadge(0);
                }
            }

            @Override
            public void onError(String errorMessage) {
                updateBubbleBadge(0);
            }
        });
    }

    private void loadUnreadFromAdmin(int adminId) {
        ApiClient.getUnreadMessages(this, token, new ApiClient.DataCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data, String message) {
                int unreadCount = 0;
                for (int i = 0; i < data.length(); i++) {
                    JSONObject msg = data.optJSONObject(i);
                    if (msg == null) {
                        continue;
                    }

                    int senderId = msg.optInt("senderId", -1);
                    int receiverId = msg.optInt("receiverId", -1);
                    if (senderId == adminId && receiverId == currentUserId) {
                        unreadCount++;
                    }
                }

                updateBubbleBadge(unreadCount);
            }

            @Override
            public void onError(String errorMessage) {
                updateBubbleBadge(0);
            }
        });
    }

    private void updateBubbleBadge(int unreadCount) {
        if (chatBadge == null || bubbleBtn == null) {
            return;
        }

        FrameLayout root = findViewById(android.R.id.content);
        if (root == null) {
            return;
        }

        if (unreadCount > 0) {
            chatBadge.setVisible(true);
            chatBadge.setNumber(unreadCount);
            BadgeUtils.attachBadgeDrawable(chatBadge, bubbleBtn, root);
        } else {
            chatBadge.clearNumber();
            chatBadge.setVisible(false);
            BadgeUtils.detachBadgeDrawable(chatBadge, bubbleBtn);
        }
    }

    private GradientDrawable buildCircleBackground() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF6C63FF); // brand purple
        circle.setStroke(0, 0);
        return circle;
    }

    // ─── Open chat with admin ─────────────────────────────────────────────────

    private void openCustomerChat() {
        String token = SessionManager.getToken(this);
        if (token.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để chat", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getAdminUser(this, token, new ApiClient.DataCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                int adminId = data.optInt("userId", -1);
                adminUserId = adminId;
                String adminName = data.optString("fullName", "Chủ cửa hàng");

                if (adminId < 0) {
                    Toast.makeText(BaseCustomerActivity.this,
                            "Không tìm thấy cửa hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(BaseCustomerActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, adminId);
                intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, adminName);
                startActivity(intent);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(BaseCustomerActivity.this,
                        "Không thể kết nối hỗ trợ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
