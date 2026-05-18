package com.qiji.cps.module.cps.controller.openapi.rebate.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OpenApiCpsRebateFreezeRespVO {
    private String freezeId;
    private Long userId;
    private BigDecimal amount;
    private String businessType;
    private String businessId;
    private String status;
    private String idempotencyKey;
}
