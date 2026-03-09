package com.example.productmanager;

public class CartItem {
    private int cartItemId;
    private int productId;
    private String productName;
    private String imageUrl;
    private double price;
    private int quantity;

    public CartItem(int cartItemId, int productId, String productName, String imageUrl, double price, int quantity) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
    }

    public int getCartItemId() {
        return cartItemId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSubTotal() {
        return price * quantity;
    }

    // 🔥 QUAN TRỌNG: THÊM CÁI NÀY
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
