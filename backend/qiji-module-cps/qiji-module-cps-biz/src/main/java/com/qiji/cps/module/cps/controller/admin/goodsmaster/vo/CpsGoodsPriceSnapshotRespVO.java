package com.qiji.cps.module.cps.controller.admin.goodsmaster.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CpsGoodsPriceSnapshotRespVO {

    private Long id;
    private Long masterId;
    private Long sourceMappingId;
    private String platformCode;
    private String vendorCode;
    private String externalGoodsId;
    private String goodsSign;
    private Integer originalPrice;
    private Integer actualPrice;
    private Integer couponPrice;
    private BigDecimal commissionRate;
    private Integer commissionAmount;
    private Long monthSales;
    private String shopName;
    private String activityTag;
    private LocalDateTime snapshotTime;
    private LocalDateTime createTime;
}
