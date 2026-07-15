package com.qiji.cps.module.cps.service.withdraw;

public record CpsWithdrawCreateCommand(Long memberId, long amountCent, String withdrawType,
                                       String withdrawAccount, String withdrawAccountName,
                                       String idempotencyKey) {
}
