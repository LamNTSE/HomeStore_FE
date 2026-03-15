package com.example.productmanager;

import androidx.annotation.NonNull;

public class Category {

    private int categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;

    public Category(int categoryId, String categoryName, String description, String imageUrl) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Spinner sẽ hiển thị label này
    @NonNull
    @Override
    public String toString() {
        return categoryName;
    }
}
