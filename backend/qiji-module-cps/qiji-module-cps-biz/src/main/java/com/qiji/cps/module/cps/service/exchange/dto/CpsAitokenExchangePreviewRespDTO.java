package com.qiji.cps.module.cps.service.exchange.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CpsAitokenExchangePreviewRespDTO {
    private Long userId;
    private String tenantId;
    private String sourceSystem;
    private String sourceAsset;
    private BigDecimal sourceAmount;
    private String targetAsset;
    private BigDecimal exchangeRate;
    private Long targetTokens;
    private BigDecimal fee;
    private Long actualTokens;
    private BigDecimal bonusRate;
    private Long bonusTokens;
}
