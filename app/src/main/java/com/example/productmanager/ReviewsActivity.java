package com.example.productmanager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {

    private int productId;

    RecyclerView rvAllFeedbacks;
    TextView tvAverageRating, tvReviewTitle;
    LinearLayout layoutRatingBars;

    FeedbackPublicAdapter adapter;
    List<Feedback> feedbackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        productId = getIntent().getIntExtra("PRODUCT_ID", -1);

        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvReviewTitle = findViewById(R.id.tvReviewTitle);
        rvAllFeedbacks = findViewById(R.id.rvAllFeedbacks);
        layoutRatingBars = findViewById(R.id.layoutRatingBars);

        adapter = new FeedbackPublicAdapter(feedbackList);
        rvAllFeedbacks.setLayoutManager(new LinearLayoutManager(this));
        rvAllFeedbacks.setAdapter(adapter);

        loadReviews();
    }

    private void loadReviews() {

        ApiClient.getFeedbacksByProduct(this, productId, new ApiClient.DataCallback<List<Feedback>>() {

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(List<Feedback> data, String message) {

                if (data == null || data.isEmpty()) return;

                feedbackList.clear();
                feedbackList.addAll(data);
                adapter.notifyDataSetChanged();

                // ⭐ Tính rating trung bình
                double avg = 0;
                for (Feedback f : data) {
                    avg += f.getRating();
                }
                avg = avg / data.size();

                tvAverageRating.setText(String.format("%.1f (%d)", avg, data.size()));

                // ⭐ Hiển thị rating breakdown
                showRatingBreakdown(data);
            }

            @Override
            public void onError(String errorMessage) {

            }
        });
    }

    // ⭐ Hiển thị thanh rating 5⭐ -> 1⭐
    private void showRatingBreakdown(List<Feedback> feedbacks) {

        int[] stars = new int[6]; // index 1-5
        int total = feedbacks.size();

        for (Feedback f : feedbacks) {

            int rating = f.getRating();

            if (rating >= 1 && rating <= 5) {
                stars[rating]++;
            }
        }

        layoutRatingBars.removeAllViews();

        for (int i = 5; i >= 1; i--) {

            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_rating_bar, layoutRatingBars, false);

            TextView tvStar = row.findViewById(R.id.tvStar);
            TextView tvCount = row.findViewById(R.id.tvCount);
            ProgressBar progress = row.findViewById(R.id.progressRating);

            tvStar.setText(i + "★");
            tvCount.setText(String.valueOf(stars[i]));

            int percent = total == 0 ? 0 : (stars[i] * 100 / total);
            progress.setProgress(percent);

            layoutRatingBars.addView(row);
        }
    }
}