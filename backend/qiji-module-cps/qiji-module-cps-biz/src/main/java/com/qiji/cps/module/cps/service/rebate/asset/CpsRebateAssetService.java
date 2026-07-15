package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;

public interface CpsRebateAssetService {

    CpsFreezeRecordDO createOrderRebateFreeze(Long orderId, String idempotencyKey);

    CpsRebateAssetResult releaseOrderRebate(Long freezeRecordId, CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult manualReleaseOrderRebate(Long freezeRecordId, CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult reverseOrderRebate(Long orderId, String idempotencyKey);

    CpsRebateAssetResult repayDebt(Long memberId, long incomingAmountCent, String sourceBusinessId);

    CpsFreezeRecordDO freezeAvailableForExchange(Long memberId, long amountCent, String businessId,
                                                  String idempotencyKey, CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult unfreezeExchangeAsset(Long freezeRecordId, String idempotencyKey,
                                                CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult confirmExchangeDeduct(Long freezeRecordId, String idempotencyKey,
                                                CpsAssetOperatorContext operatorContext);

    CpsFreezeRecordDO freezeAvailableForWithdrawal(Long memberId, long amountCent, String businessId,
                                                    String idempotencyKey, CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult unfreezeWithdrawalAsset(Long freezeRecordId, String idempotencyKey,
                                                  CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult confirmWithdrawalDeduct(Long freezeRecordId, String idempotencyKey,
                                                  CpsAssetOperatorContext operatorContext);

    CpsRebateAssetResult manualAdjustDebt(Long memberId, CpsDebtAdjustAction action, long amountCent,
                                          String sourceBusinessId, CpsAssetOperatorContext operatorContext);
}
