package com.example.productmanager;

public class Feedback {
    private int feedbackId;
    private int userId;
    private String userFullName;
    private int productId;
    private String productName;
    private int rating;
    private String comment;
    private String adminReply;
    private String adminReplyAt;
    private String createdAt;
    private String updatedAt;

    public Feedback(int feedbackId, int userId, String userFullName,
                    int productId, String productName, int rating,
                    String comment, String adminReply, String adminReplyAt,
                    String createdAt, String updatedAt) {
        this.feedbackId = feedbackId;
        this.userId = userId;
        this.userFullName = userFullName;
        this.productId = productId;
        this.productName = productName;
        this.rating = rating;
        this.comment = comment;
        this.adminReply = adminReply;
        this.adminReplyAt = adminReplyAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getFeedbackId() { return feedbackId; }
    public int getUserId() { return userId; }
    public String getUserFullName() { return userFullName; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getAdminReply() { return adminReply; }
    public String getAdminReplyAt() { return adminReplyAt; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }
}
