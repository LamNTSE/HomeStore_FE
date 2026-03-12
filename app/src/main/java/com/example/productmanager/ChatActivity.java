package com.example.productmanager;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_USER_ID = "other_user_id";
    public static final String EXTRA_OTHER_USER_NAME = "other_user_name";

    private RecyclerView recyclerMessages;
    private EditText edtMessage;
    private ImageView btnSend;
    private MaterialToolbar toolbar;

    private ChatAdapter chatAdapter;
    private String token;
    private int currentUserId;
    private int otherUserId;
    private String otherUserName;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        token = SessionManager.getToken(this);
        isAdmin = SessionManager.isAdmin(this);
        currentUserId = SessionManager.getUserId(this);

        otherUserId = getIntent().getIntExtra(EXTRA_OTHER_USER_ID, -1);
        otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);

        initViews();
        setupToolbar();
        setupRecycler();
        setupSend();
        loadMessages();
    }

    private void initViews() {
        recyclerMessages = findViewById(R.id.recyclerMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        String title = otherUserName != null ? otherUserName : "Chat";
        if (!isAdmin) {
            title = "Chủ cửa hàng";
        }
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(this, new JSONArray(), currentUserId);
        recyclerMessages.setAdapter(chatAdapter);
    }

    private void setupSend() {
        btnSend.setOnClickListener(v -> {
            String message = edtMessage.getText().toString().trim();
            if (message.isEmpty()) return;

            edtMessage.setText("");

            ApiClient.sendMessage(this, token, otherUserId, message,
                    new ApiClient.DataCallback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject data, String msg) {
                            // Message saved; SignalR will deliver it to the other side.
                            // Reload to show the sent message in our own list.
                            loadMessages();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void loadMessages() {
        if (otherUserId < 0) return;

        ApiClient.getConversation(this, token, otherUserId,
                new ApiClient.DataCallback<JSONArray>() {
                    @Override
                    public void onSuccess(JSONArray data, String message) {
                        chatAdapter.updateMessages(data);
                        if (data.length() > 0) {
                            recyclerMessages.scrollToPosition(data.length() - 1);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Silent fail
                    }
                });
    }

    // ─── SignalR real-time receive ────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Register to receive incoming messages in real-time
        SignalRManager.getInstance().connectChat(token);
        SignalRManager.getInstance().setChatMessageListener(messageJson -> {
            // A new message arrived – reload the full conversation to keep order/read state correct
            loadMessages();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear listener so messages don't trigger reloads while screen is hidden
        SignalRManager.getInstance().setChatMessageListener(null);
    }
}

