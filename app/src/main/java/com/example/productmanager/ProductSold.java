package com.example.productmanager;

public class ProductSold {

    private int productId;
    private int sold;

    public ProductSold(int productId, int sold) {
        this.productId = productId;
        this.sold = sold;
    }

    public int getProductId() {
        return productId;
    }

    public int getSold() {
        return sold;
    }
}