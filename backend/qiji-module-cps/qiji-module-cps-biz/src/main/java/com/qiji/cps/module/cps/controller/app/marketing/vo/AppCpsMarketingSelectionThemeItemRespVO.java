package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - CPS选品主题商品 Response VO")
@Data
public class AppCpsMarketingSelectionThemeItemRespVO {

    @Schema(description = "商品快照ID")
    private Long id;

    @Schema(description = "主题ID")
    private Long themeId;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "商品ID")
    private String goodsId;

    @Schema(description = "商品签名")
    private String goodsSign;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "主图")
    private String mainPic;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "到手价")
    private BigDecimal actualPrice;

    @Schema(description = "优惠券金额")
    private BigDecimal couponPrice;

    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;

    @Schema(description = "佣金金额")
    private BigDecimal commissionAmount;

    @Schema(description = "月销量")
    private Long monthSales;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "类目名称")
    private String categoryName;

    @Schema(description = "活动标签")
    private String activityTag;

    @Schema(description = "榜单标签")
    private String rankTag;

    @Schema(description = "卖点")
    private String sellingPoint;

    @Schema(description = "推荐分")
    private BigDecimal recommendScore;

    @Schema(description = "推荐理由")
    private String recommendReason;

    @Schema(description = "商品链接")
    private String itemLink;
}
