package cn.didi.union.models;

import com.google.gson.annotations.SerializedName;

public class OrderCallbackData {
    private String title;
    @SerializedName("order_id") private String orderId;
    @SerializedName("product_id") private String productId;
    @SerializedName("pay_price") private long payPrice;
    @SerializedName("pay_time") private long payTime;
    @SerializedName("refund_price") private long refundPrice;
    @SerializedName("refund_time") private long refundTime;
    @SerializedName("cpa_profit") private long cpaProfit;
    @SerializedName("cps_profit") private long cpsProfit;
    @SerializedName("cpa_type") private String cpaType;
    private int status;
    @SerializedName("promotion_id") private long promotionId;
    @SerializedName("source_id") private String sourceId;
    @SerializedName("is_risk") private int isRisk;
    public String getTitle() { return title; }
    public String getOrderId() { return orderId; }
    public String getProductId() { return productId; }
    public long getPayPrice() { return payPrice; }
    public long getPayTime() { return payTime; }
    public long getRefundPrice() { return refundPrice; }
    public long getRefundTime() { return refundTime; }
    public long getCpaProfit() { return cpaProfit; }
    public long getCpsProfit() { return cpsProfit; }
    public String getCpaType() { return cpaType; }
    public int getStatus() { return status; }
    public long getPromotionId() { return promotionId; }
    public String getSourceId() { return sourceId; }
    public int getIsRisk() { return isRisk; }
}
