package com.qiji.cps.module.cps.service.exchange.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CpsAitokenExchangeOrderRespDTO {
    private String exchangeOrderId;
    private Long userId;
    private String tenantId;
    private String sourceSystem;
    private String sourceOrderId;
    private String sourceAsset;
    private BigDecimal sourceAmount;
    private String targetAsset;
    private Long targetTokens;
    private BigDecimal exchangeRate;
    private BigDecimal bonusRate;
    private Long bonusTokens;
    private String status;
    private String failureReason;
    private String idempotencyKey;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
