package com.qiji.cps.module.cps.service.order;

/**
 * Manual order attribution binding command.
 */
public record CpsOrderManualBindCommand(
        Long orderId,
        Long memberId,
        Long operatorId,
        String idempotencyKey,
        String auditNote
) {
}
