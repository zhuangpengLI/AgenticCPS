package com.qiji.cps.module.cps.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员订单申领命令，memberId 必须来自登录上下文。 */
public record CpsOrderClaimCommand(Long memberId, String platformCode, String platformOrderId,
                                   LocalDateTime orderTime, BigDecimal payAmount, String itemTitle,
                                   String idempotencyKey) {
}
