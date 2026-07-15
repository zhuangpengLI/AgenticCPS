package com.qiji.cps.module.cps.service.order;

public record CpsFundsTraceQuery(Long orderId, String platformCode, String platformOrderId,
                                 String businessId, String idempotencyKey) {
}
