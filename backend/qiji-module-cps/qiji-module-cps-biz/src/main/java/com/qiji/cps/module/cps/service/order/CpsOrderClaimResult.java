package com.qiji.cps.module.cps.service.order;

/** 订单申领结果，不包含其他会员身份或资产信息。 */
public record CpsOrderClaimResult(Long claimId, Long orderId, String platformCode, String platformOrderId,
                                  String status, String message) {
}
