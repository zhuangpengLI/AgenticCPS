package com.qiji.cps.module.cps.controller.openapi.rebate.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OpenApiCpsRebateBalanceRespVO {
    private Long userId;
    private Long tenantId;
    private BigDecimal pending;
    private BigDecimal settled;
    private BigDecimal available;
    private BigDecimal frozen;
    private BigDecimal exchanged;
    private BigDecimal withdrawn;
}
