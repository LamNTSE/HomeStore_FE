package com.example.productmanager;

public class OrderItem {
    private int orderItemId;
    private int productId;
    private String productName;
    private String imageUrl;
    private int quantity;
    private double unitPrice;

    public OrderItem(int orderItemId, int productId, String productName,
                     String imageUrl, int quantity, double unitPrice) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getOrderItemId() { return orderItemId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getImageUrl() { return imageUrl; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
}
