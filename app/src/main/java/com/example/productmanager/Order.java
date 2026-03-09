package com.example.productmanager;

public class Order {

    private int orderId;
    private int userId;
    private double totalAmount;
    private String status;
    private String shippingAddress;
    private String phone;
    private String receiverName;
    private String createdAt;
    private String paymentMethod;
    private String paymentStatus;

    public Order(int orderId, int userId, double totalAmount, String status,
                 String shippingAddress, String phone, String receiverName,
                 String createdAt, String paymentMethod, String paymentStatus) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.shippingAddress = shippingAddress;
        this.phone = phone;
        this.receiverName = receiverName;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    public int getOrderId() { return orderId; }
    public int getUserId() { return userId; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getShippingAddress() { return shippingAddress; }
    public String getPhone() { return phone; }
    public String getReceiverName() { return receiverName; }
    public String getCreatedAt() { return createdAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
}
