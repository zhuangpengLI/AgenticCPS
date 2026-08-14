package com.qiji.cps.module.cps.controller.openapi.didi.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DidiOrderCallbackReqVO {
    @JsonProperty("cpa_profit") private Long cpaProfit;
    @JsonProperty("cpa_type") private String cpaType;
    @JsonProperty("cps_profit") private Long cpsProfit;
    @JsonProperty("is_risk") private Integer isRisk;
    @JsonProperty("is_mock") private Integer isMock;
    @JsonProperty("open_uid") private String openUid;
    private String callback;
    @JsonProperty("order_id") private String orderId;
    @JsonProperty("order_status") private Integer orderStatus;
    @JsonProperty("pay_price") private Long payPrice;
    @JsonProperty("pay_time") private Long payTime;
    @JsonProperty("product_id") private String productId;
    @JsonProperty("promotion_id") private String promotionId;
    @JsonProperty("refund_price") private Long refundPrice;
    @JsonProperty("refund_time") private Long refundTime;
    @JsonProperty("retry_times") private Integer retryTimes;
    @JsonProperty("source_id") private String sourceId;
    private Integer status;
    @JsonProperty("fail_reason") private String failReason;
    @JsonProperty("activity_id") private String activityId;
    private String title;
}
