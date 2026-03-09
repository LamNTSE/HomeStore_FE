package com.example.productmanager;

import com.google.gson.annotations.SerializedName;

public class Voucher {

    @SerializedName("VoucherId")
    private int voucherId;

    @SerializedName("Code")
    private String code;

    @SerializedName("DiscountType")
    private String discountType;

    @SerializedName("DiscountValue")
    private double discountValue;

    @SerializedName("MinOrderValue")
    private double minOrderValue;

    @SerializedName("MaxUsageCount")
    private int maxUsageCount;

    @SerializedName("StartDate")
    private String startDate;

    @SerializedName("ExpiryDate")
    private String expiryDate;

    @SerializedName("IsActive")
    private boolean isActive;

    // Constructor dùng khi parse thủ công
    public Voucher(int voucherId,
                   String code,
                   String discountType,
                   double discountValue,
                   double minOrderValue,
                   int maxUsageCount,
                   String startDate,
                   String expiryDate,
                   boolean isActive) {

        this.voucherId = voucherId;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxUsageCount = maxUsageCount;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
    }

    public int getVoucherId() {
        return voucherId;
    }

    public String getCode() {
        return code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public int getMaxUsageCount() {
        return maxUsageCount;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public boolean isActive() {
        return isActive;
    }


    // ===============================
    // THÊM MỚI – hỗ trợ UI app bán hàng
    // ===============================

    // hiển thị giảm giá đẹp hơn
    public String getDiscountText() {
        if ("Percent".equalsIgnoreCase(discountType)) {
            return "Giảm " + discountValue + "%";
        } else {
            return "Giảm " + (int)discountValue + "đ";
        }
    }

    // hiển thị điều kiện đơn tối thiểu
    public String getMinOrderText() {
        return "Đơn tối thiểu: " + (int)minOrderValue + "đ";
    }

    // hiển thị ngày hết hạn
    public String getExpiryText() {
        return "HSD: " + expiryDate;
    }

    // trạng thái voucher
    public String getStatusText() {
        return isActive ? "Còn hiệu lực" : "Hết hiệu lực";
    }

    // kiểm tra voucher còn dùng được
    public boolean canUse() {
        return isActive;
    }
}