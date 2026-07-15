package com.qiji.cps.module.cps.controller.admin.couponpool.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CpsCouponPoolRespVO {

    private Long id;
    private Long masterId;
    private Long sourceMappingId;
    private String platformCode;
    private String vendorCode;
    private String externalGoodsId;
    private String goodsSign;
    private String couponId;
    private String couponName;
    private Integer couponAmount;
    private Integer thresholdAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer stockTotal;
    private Integer stockRemain;
    private String status;
    private String sourceType;
    private Long activityId;
    private Long themeId;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
}
