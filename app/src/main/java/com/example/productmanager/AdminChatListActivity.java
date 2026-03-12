package com.example.productmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;

public class AdminChatListActivity extends AppCompatActivity
        implements ChatUserAdapter.OnUserClickListener {

    private RecyclerView recyclerChatUsers;
    private ChatUserAdapter adapter;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat_list);

        token = SessionManager.getToken(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerChatUsers = findViewById(R.id.recyclerChatUsers);
        recyclerChatUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatUserAdapter(this, new JSONArray(), this);
        recyclerChatUsers.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPartners();

        // Refresh conversation list whenever a message is received in real-time
        SignalRManager.getInstance().connectChat(token);
        SignalRManager.getInstance().setChatMessageListener(messageJson -> loadPartners());
    }

    @Override
    protected void onPause() {
        super.onPause();
        SignalRManager.getInstance().setChatMessageListener(null);
    }

    private void loadPartners() {
        ApiClient.getConversationPartners(this, token, new ApiClient.DataCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray data, String message) {
                adapter.updateUsers(data);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AdminChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUserClick(int userId, String fullName, String avatarUrl) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, userId);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, fullName);
        startActivity(intent);
    }
}
