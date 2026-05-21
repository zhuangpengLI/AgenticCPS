package com.qiji.cps.module.cps.service.exchange.dto;

import lombok.Data;

@Data
public class CpsAitokenExchangeStatusUpdateReqDTO {

    private String sourceOrderId;

    private String idempotencyKey;

    private String reason;
}
