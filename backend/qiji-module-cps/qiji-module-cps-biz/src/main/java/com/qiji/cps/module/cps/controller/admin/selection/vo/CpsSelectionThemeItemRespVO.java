package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS选品主题商品快照 Response VO")
@Data
public class CpsSelectionThemeItemRespVO {

    private Long id;
    private Long themeId;
    private String platformCode;
    private String vendorCode;
    private String goodsId;
    private String goodsSign;
    private String title;
    private String mainPic;
    private BigDecimal originalPrice;
    private BigDecimal actualPrice;
    private BigDecimal couponPrice;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private Long monthSales;
    private String shopName;
    private String brandName;
    private String categoryName;
    private String activityTag;
    private String rankTag;
    private String sellingPoint;
    private BigDecimal recommendScore;
    private String recommendReason;
    private Integer topFlag;
    private Integer manualAdjusted;
    private String status;
    private String sourceType;
    private String itemLink;
    private LocalDateTime snapshotTime;
    private Integer sort;
    private LocalDateTime createTime;
}
