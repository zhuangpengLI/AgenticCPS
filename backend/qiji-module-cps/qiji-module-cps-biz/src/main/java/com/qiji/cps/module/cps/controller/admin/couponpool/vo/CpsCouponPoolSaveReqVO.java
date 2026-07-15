package com.qiji.cps.module.cps.controller.admin.couponpool.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CpsCouponPoolSaveReqVO {

    private Long id;
    private Long masterId;
    private Long sourceMappingId;

    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    private String vendorCode;

    @NotBlank(message = "外部商品ID不能为空")
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
    private String rawData;
}
