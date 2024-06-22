package com.example.besafe.activities;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class QueryPurchaseResponse {
    @SerializedName("orderId")
    @Expose
    private String orderId;
    @SerializedName("packageName")
    @Expose
    private String packageName;
    @SerializedName("productId")
    @Expose
    private String productId;
    @SerializedName("purchaseTime")
    @Expose
    private Long purchaseTime;
    @SerializedName("purchaseState")
    @Expose
    private Integer purchaseState;
    @SerializedName("purchaseToken")
    @Expose
    private String purchaseToken;
    @SerializedName("quantity")
    @Expose
    private Integer quantity;
    @SerializedName("autoRenewing")
    @Expose
    private Boolean autoRenewing;
    @SerializedName("acknowledged")
    @Expose
    private Boolean acknowledged;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(Long purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public Integer getPurchaseState() {
        return purchaseState;
    }

    public void setPurchaseState(Integer purchaseState) {
        this.purchaseState = purchaseState;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getAutoRenewing() {
        return autoRenewing;
    }

    public void setAutoRenewing(Boolean autoRenewing) {
        this.autoRenewing = autoRenewing;
    }

    public Boolean getAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
}

