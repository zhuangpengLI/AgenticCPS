package com.qiji.cps.module.cps.service.rebate.asset;

/**
 * 一次资产操作提交后的账户余额快照（分）。
 */
public record CpsRebateAssetResult(Long ledgerId, Long businessRecordId,
                                   long availableBalanceCent, long frozenBalanceCent,
                                   long debtBalanceCent, boolean idempotentReplay) {
}
