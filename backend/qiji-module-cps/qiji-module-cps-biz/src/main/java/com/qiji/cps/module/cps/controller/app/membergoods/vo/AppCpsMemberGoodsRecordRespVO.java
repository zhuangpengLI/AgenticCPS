package com.qiji.cps.module.cps.controller.app.membergoods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "用户 APP - CPS 商品记录 Response VO")
@Data
public class AppCpsMemberGoodsRecordRespVO {

    private Long id;
    private String platformCode;
    private String goodsId;
    private String goodsSign;
    private String title;
    private String mainPic;
    @Schema(description = "商品原价（元）")
    private BigDecimal originalPrice;
    @Schema(description = "券后价（元）")
    private BigDecimal actualPrice;
    @Schema(description = "优惠券金额（元）")
    private BigDecimal couponPrice;
    @Schema(description = "预估返利金额（元）")
    private BigDecimal estimateRebateAmount;
    private Long monthSales;
    private String shopName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
