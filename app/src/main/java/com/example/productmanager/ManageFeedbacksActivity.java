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

public class ManageFeedbacksActivity extends AppCompatActivity {

    private RecyclerView rvFeedbacks;
    private TextView tvEmpty;
    private FeedbackAdminAdapter adapter;
    private List<Feedback> feedbackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_feedbacks);

        ImageView btnBack = findViewById(R.id.btnBackFeedbacks);
        rvFeedbacks = findViewById(R.id.rvFeedbacks);
        tvEmpty = findViewById(R.id.tvEmptyFeedbacks);

        btnBack.setOnClickListener(v -> finish());

        adapter = new FeedbackAdminAdapter(feedbackList, (feedbackId, reply, position) -> {
            String token = SessionManager.getToken(this);
            ApiClient.adminReplyFeedback(this, token, feedbackId, reply,
                    new ApiClient.DataCallback<Feedback>() {
                        @Override
                        public void onSuccess(Feedback data, String message) {
                            adapter.updateReply(position, reply);
                            Toast.makeText(ManageFeedbacksActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(ManageFeedbacksActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        rvFeedbacks.setLayoutManager(new LinearLayoutManager(this));
        rvFeedbacks.setAdapter(adapter);

        loadFeedbacks();
    }

    private void loadFeedbacks() {
        String token = SessionManager.getToken(this);
        ApiClient.getAllFeedbacks(this, token, new ApiClient.DataCallback<List<Feedback>>() {
            @Override
            public void onSuccess(List<Feedback> data, String message) {
                feedbackList.clear();
                feedbackList.addAll(data);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(feedbackList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ManageFeedbacksActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
