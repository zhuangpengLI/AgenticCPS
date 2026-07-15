package com.qiji.cps.module.cps.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 APP - CPS订单 Response VO")
@Data
public class AppCpsOrderRespVO {

    @Schema(description = "订单ID", example = "1")
    private Long id;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "平台订单号", example = "1234567890")
    private String platformOrderId;

    @Schema(description = "商品ID")
    private String itemId;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "商品主图")
    private String itemPic;

    @Schema(description = "商品原价（元）")
    private BigDecimal itemPrice;

    @Schema(description = "券后价（元）")
    private BigDecimal finalPrice;

    @Schema(description = "优惠券金额（元）")
    private BigDecimal couponAmount;

    @Schema(description = "预估佣金金额（元）")
    private BigDecimal commissionAmount;

    @Schema(description = "预估返利金额（元）")
    private BigDecimal estimateRebate;

    @Schema(description = "实际返利金额（元）")
    private BigDecimal realRebate;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "返利冻结状态")
    private String rebateFreezeStatus;

    @Schema(description = "同步时间")
    private LocalDateTime syncTime;

    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "返利入账时间")
    private LocalDateTime rebateTime;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "确认收货时间")
    private LocalDateTime confirmReceiptTime;

    @Schema(description = "计划解冻时间")
    private LocalDateTime planUnfreezeTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
