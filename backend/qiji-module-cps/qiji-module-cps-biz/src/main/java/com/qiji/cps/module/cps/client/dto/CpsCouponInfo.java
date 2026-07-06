package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CpsCouponInfo {

    private String couponId;

    private String couponLink;

    private BigDecimal couponAmount;

    private BigDecimal couponConditions;

    private Long couponTotalNum;

    private Long couponRemainNum;

    private Long couponReceiveNum;

    private String couponStartTime;

    private String couponEndTime;

}
