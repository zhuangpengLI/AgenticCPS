package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CPS商品广场商品 Response VO")
@Data
public class CpsGoodsSquareGoodsRespVO {

    @Schema(description = "商品ID")
    private String goodsId;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "商品主图")
    private String mainPic;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "券后价")
    private BigDecimal actualPrice;

    @Schema(description = "优惠券金额")
    private BigDecimal couponPrice;

    @Schema(description = "优惠券使用门槛")
    private BigDecimal couponConditions;

    @Schema(description = "优惠券总量")
    private Long couponTotalNum;

    @Schema(description = "优惠券剩余数量")
    private Long couponRemainNum;

    @Schema(description = "优惠券已领取数量")
    private Long couponReceiveNum;

    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;

    @Schema(description = "预估佣金")
    private BigDecimal commissionAmount;

    @Schema(description = "30天销量")
    private Long monthSales;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "商品原始链接")
    private String itemLink;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "拼多多 goodsSign")
    private String goodsSign;

    @Schema(description = "商品来源")
    private String source;

    @Schema(description = "活动标签")
    private String activityTag;

    @Schema(description = "类目名称")
    private String categoryName;

    @Schema(description = "优惠券结束时间")
    private String couponEndTime;

    @Schema(description = "优惠券开始时间")
    private String couponStartTime;

    @Schema(description = "榜单或热销标签")
    private String rankTag;

    @Schema(description = "商品卖点文案")
    private String sellingPoint;

}
