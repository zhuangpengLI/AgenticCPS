package com.qiji.cps.module.cps.service.exchange.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CpsAitokenExchangeSubmitReqDTO {
    private Long userId;
    private String tenantId;
    private String sourceSystem;
    private String sourceOrderId;
    private String sourceAsset;
    private BigDecimal sourceAmount;
    private String targetAsset;
    private Long targetTokens;
    private String idempotencyKey;
}
