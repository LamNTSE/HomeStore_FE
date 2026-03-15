package com.example.productmanager;

public class Product {
    private int id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;

    private int categoryId;      // thêm
    private int stockQuantity;   // thêm

    public Product(int id, String name, String description, double price, String imageUrl,
                   int categoryId, int stockQuantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.stockQuantity = stockQuantity;
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public double getPrice() { return price; }

    public String getImageUrl() { return imageUrl; }

    public int getCategoryId() { return categoryId; }      // getter mới

    public int getStockQuantity() { return stockQuantity; } // getter mới
}