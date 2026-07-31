package com.qiji.cps.module.cps.service.order;

/** 管理员订单申领审核命令。 */
public record CpsOrderClaimReviewCommand(Long claimId, boolean approved, Long operatorId, String auditNote) {
}
