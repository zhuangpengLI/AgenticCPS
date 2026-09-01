package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateTypeEnum;
import com.qiji.cps.module.cps.service.freeze.CpsFreezeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 返利账户唯一写服务。所有余额变更、业务记录与资产流水在同一事务提交。
 */
@Service
public class CpsRebateAssetServiceImpl implements CpsRebateAssetService {

    static final String ORDER_REBATE = "ORDER_REBATE";
    static final String ORDER_REBATE_RELEASE = "ORDER_REBATE_RELEASE";
    static final String ORDER_REFUND = "ORDER_REFUND";
    static final String REBATE_INCOMING = "REBATE_INCOMING";
    static final String TOKEN_EXCHANGE = "TOKEN_EXCHANGE";
    static final String TOKEN_EXCHANGE_UNFREEZE = "TOKEN_EXCHANGE_UNFREEZE";
    static final String TOKEN_EXCHANGE_DEDUCT = "TOKEN_EXCHANGE_DEDUCT";
    static final String WITHDRAWAL = "WITHDRAWAL";
    static final String WITHDRAWAL_UNFREEZE = "WITHDRAWAL_UNFREEZE";
    static final String WITHDRAWAL_DEDUCT = "WITHDRAWAL_DEDUCT";
    static final String DEBT_ADJUST = "DEBT_ADJUST";

    @Resource private CpsOrderMapper orderMapper;
    @Resource private CpsRebateRecordMapper rebateRecordMapper;
    @Resource private CpsRebateAccountMapper accountMapper;
    @Resource private CpsFreezeRecordMapper freezeRecordMapper;
    @Resource private CpsRebateAssetLedgerMapper ledgerMapper;
    @Resource private CpsRebateDebtMapper debtMapper;
    @Resource private CpsFreezeService freezeService;
    @Resource private CpsMoneyConverter moneyConverter;
    @Resource private CpsRebateAssetPolicyService policyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsFreezeRecordDO createOrderRebateFreeze(Long orderId, String idempotencyKey) {
        requirePositive(orderId, "订单ID");
        requireText(idempotencyKey, "幂等键");
        CpsRebateAssetLedgerDO replay = ledgerMapper.selectByBusinessAndIdempotencyKey(ORDER_REBATE, idempotencyKey);
        if (replay != null) {
            return freezeRecordMapper.selectById(replay.getBusinessId());
        }
        policyService.assertWritable();

        CpsOrderDO order = requireNonNull(orderMapper.selectById(orderId), "订单不存在: " + orderId);
        if (order.getMemberId() == null) {
            throw new IllegalStateException("未归因订单不能创建返利冻结: " + orderId);
        }
        if (order.getSettleTime() == null
                || (!CpsOrderStatusEnum.SETTLED.getStatus().equals(order.getOrderStatus())
                && order.getConfirmReceiptTime() == null)) {
            throw new IllegalStateException("订单必须具备平台结算时间，非已结算状态还必须具备确认收货时间: " + orderId);
        }
        CpsRebateRecordDO rebate = requireNonNull(rebateRecordMapper.selectByOrderIdAndType(
                orderId, CpsRebateTypeEnum.REBATE.getType()), "订单返利记录不存在: " + orderId);
        long amountCent = moneyConverter.yuanToCent(rebate.getRebateAmount());
        requirePositive(amountCent, "返利金额");
        CpsFreezeConfigDO config = requireNonNull(freezeService.getActiveConfig(order.getPlatformCode(), amountCent),
                "没有可匹配的冻结规则: " + order.getPlatformCode());
        if (config.getUnfreezeDays() == null || config.getUnfreezeDays() < 0) {
            throw new IllegalStateException("冻结规则天数非法: " + config.getId());
        }
        LocalDateTime eligibleTime = later(order.getConfirmReceiptTime(), order.getSettleTime());

        CpsRebateAccountDO account = lockOrCreateAccount(order.getMemberId());
        replay = ledgerMapper.selectByBusinessAndIdempotencyKey(ORDER_REBATE, idempotencyKey);
        if (replay != null) {
            return freezeRecordMapper.selectById(replay.getBusinessId());
        }
        if (freezeRecordMapper.selectByBusinessId(ORDER_REBATE, String.valueOf(orderId)) != null) {
            throw new IllegalStateException("订单已有返利冻结记录: " + orderId);
        }
        Balance before = balance(account);
        Balance after = new Balance(before.available, Math.addExact(before.frozen, amountCent), before.debt);
        persistAccount(account, after, 0L);

        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .memberId(order.getMemberId()).orderId(orderId).platformOrderId(order.getPlatformOrderId())
                .businessType(ORDER_REBATE).businessId(String.valueOf(orderId)).idempotencyKey(idempotencyKey)
                .freezeAmount(moneyConverter.centToYuan(amountCent)).amountCent(amountCent)
                .freezeConfigId(config.getId()).freezeDaysSnapshot(config.getUnfreezeDays())
                .eligibleTime(eligibleTime).unfreezeTime(eligibleTime.plusDays(config.getUnfreezeDays()))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        freezeRecordMapper.insert(freeze);
        CpsRebateRecordDO rebateUpdate = new CpsRebateRecordDO();
        rebateUpdate.setId(rebate.getId());
        rebateUpdate.setFreezeRecordId(freeze.getId());
        rebateUpdate.setRebateStatus(CpsRebateStatusEnum.PENDING.getStatus());
        rebateRecordMapper.updateById(rebateUpdate);
        appendLedger(order.getMemberId(), ORDER_REBATE, String.valueOf(freeze.getId()), orderId,
                order.getPlatformOrderId(), idempotencyKey, before, after,
                CpsAssetOperatorContext.system(idempotencyKey, "平台结算后创建订单返利冻结"));
        return freeze;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult releaseOrderRebate(Long freezeRecordId, CpsAssetOperatorContext context) {
        return releaseOrderRebate(freezeRecordId, context, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult manualReleaseOrderRebate(Long freezeRecordId, CpsAssetOperatorContext context) {
        return releaseOrderRebate(freezeRecordId, context, true);
    }

    private CpsRebateAssetResult releaseOrderRebate(Long freezeRecordId, CpsAssetOperatorContext context,
                                                     boolean manual) {
        CpsRebateAssetResult replay = replay(ORDER_REBATE_RELEASE, context.idempotencyKey());
        if (replay != null) return replay;
        policyService.assertWritable();
        CpsFreezeRecordDO freeze = requireNonNull(freezeRecordMapper.selectForUpdateById(freezeRecordId),
                "冻结记录不存在: " + freezeRecordId);
        requireBusinessAndStatus(freeze, ORDER_REBATE, CpsFreezeStatusEnum.FROZEN.getStatus());
        if (!manual && (freeze.getUnfreezeTime() == null || freeze.getUnfreezeTime().isAfter(LocalDateTime.now()))) {
            throw new IllegalStateException("订单返利尚未到自动解冻时间: " + freezeRecordId);
        }
        if (manual) {
            CpsOrderDO order = requireNonNull(orderMapper.selectById(freeze.getOrderId()),
                    "订单不存在: " + freeze.getOrderId());
            if (order.getSettleTime() == null
                    || (!CpsOrderStatusEnum.SETTLED.getStatus().equals(order.getOrderStatus())
                    && order.getConfirmReceiptTime() == null)) {
                throw new IllegalStateException("手动解冻不能绕过平台结算条件");
            }
        }
        long amountCent = freezeAmountCent(freeze);
        CpsRebateAccountDO account = lockOrCreateAccount(freeze.getMemberId());
        replay = replay(ORDER_REBATE_RELEASE, context.idempotencyKey());
        if (replay != null) return replay;
        Balance before = balance(account);
        if (before.frozen < amountCent) throw new IllegalStateException("账户冻结余额不足");
        long repaid = repayOutstandingDebt(freeze.getMemberId(), Math.min(amountCent, before.debt));
        Balance after = new Balance(Math.addExact(before.available, amountCent - repaid),
                before.frozen - amountCent, before.debt - repaid);
        persistAccount(account, after, amountCent);
        LocalDateTime releasedAt = LocalDateTime.now();
        markFreeze(freeze, CpsFreezeStatusEnum.UNFREEZED.getStatus(), releasedAt,
                manual ? context : null);
        markOriginalRebateReceived(freeze.getOrderId(), releasedAt);
        CpsRebateAssetLedgerDO ledger = appendLedger(freeze.getMemberId(), ORDER_REBATE_RELEASE,
                String.valueOf(freezeRecordId), freeze.getOrderId(), freeze.getPlatformOrderId(),
                context.idempotencyKey(), before, after, context);
        return result(ledger, freezeRecordId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult reverseOrderRebate(Long orderId, String idempotencyKey) {
        CpsRebateAssetResult replay = replay(ORDER_REFUND, idempotencyKey);
        if (replay != null) return replay;
        policyService.assertWritable();
        CpsRebateRecordDO rebate = requireNonNull(rebateRecordMapper.selectByOrderIdAndType(
                orderId, CpsRebateTypeEnum.REBATE.getType()), "订单返利记录不存在: " + orderId);
        long amountCent = moneyConverter.yuanToCent(rebate.getRebateAmount());
        CpsFreezeRecordDO freeze = freezeRecordMapper.selectForUpdateByBusinessId(ORDER_REBATE, String.valueOf(orderId));
        CpsRebateAccountDO account = lockOrCreateAccount(rebate.getMemberId());
        replay = replay(ORDER_REFUND, idempotencyKey);
        if (replay != null) return replay;
        Balance before = balance(account);
        boolean reversingFrozen = freeze != null && CpsFreezeStatusEnum.FROZEN.getStatus().equals(freeze.getStatus());
        if (reversingFrozen && before.frozen < amountCent) {
            throw new IllegalStateException("对应订单冻结余额账实不一致: orderId=" + orderId);
        }
        long frozenDeduct = reversingFrozen ? amountCent : 0L;
        long remaining = amountCent - frozenDeduct;
        long availableDeduct = Math.min(remaining, before.available);
        long debtIncrease = remaining - availableDeduct;
        Balance after = new Balance(before.available - availableDeduct, before.frozen - frozenDeduct,
                Math.addExact(before.debt, debtIncrease));
        persistAccount(account, after, reversingFrozen ? 0L : -amountCent);
        if (frozenDeduct > 0L) markFreeze(freeze, CpsFreezeStatusEnum.DEDUCTED.getStatus(), null, null);
        if (debtIncrease > 0L) {
            createDebt(rebate.getMemberId(), orderId, rebate.getPlatformOrderId(), String.valueOf(orderId),
                    idempotencyKey, debtIncrease);
        }
        createRefundRecord(rebate, idempotencyKey);
        CpsRebateAssetLedgerDO ledger = appendLedger(rebate.getMemberId(), ORDER_REFUND, String.valueOf(orderId),
                orderId, rebate.getPlatformOrderId(), idempotencyKey, before, after,
                CpsAssetOperatorContext.system(idempotencyKey, "订单退款返利冲正"));
        return result(ledger, orderId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult repayDebt(Long memberId, long incomingAmountCent, String sourceBusinessId) {
        requirePositive(incomingAmountCent, "入账金额");
        requireText(sourceBusinessId, "来源业务单号");
        String key = "repay:" + sourceBusinessId;
        CpsRebateAssetResult replay = replay(REBATE_INCOMING, key);
        if (replay != null) return replay;
        policyService.assertWritable();
        CpsRebateAccountDO account = lockOrCreateAccount(memberId);
        replay = replay(REBATE_INCOMING, key);
        if (replay != null) return replay;
        Balance before = balance(account);
        long repaid = repayOutstandingDebt(memberId, Math.min(incomingAmountCent, before.debt));
        Balance after = new Balance(Math.addExact(before.available, incomingAmountCent - repaid),
                before.frozen, before.debt - repaid);
        persistAccount(account, after, incomingAmountCent);
        CpsRebateAssetLedgerDO ledger = appendLedger(memberId, REBATE_INCOMING, sourceBusinessId, null,
                null, key, before, after, CpsAssetOperatorContext.system(key, "新增返利优先偿还欠款"));
        return result(ledger, null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsFreezeRecordDO freezeAvailableForExchange(Long memberId, long amountCent, String businessId,
                                                         String idempotencyKey, CpsAssetOperatorContext context) {
        requirePositive(amountCent, "兑换冻结金额");
        CpsRebateAssetLedgerDO existing = ledgerMapper.selectByBusinessAndIdempotencyKey(TOKEN_EXCHANGE, idempotencyKey);
        if (existing != null) return validateExchangeFreezeReplay(existing, memberId, amountCent);
        policyService.assertWritable();
        CpsRebateAccountDO account = lockOrCreateAccount(memberId);
        existing = ledgerMapper.selectByBusinessAndIdempotencyKey(TOKEN_EXCHANGE, idempotencyKey);
        if (existing != null) return validateExchangeFreezeReplay(existing, memberId, amountCent);
        Balance before = balance(account);
        if (before.debt > 0L || before.available < amountCent) {
            throw new IllegalStateException("欠款未清零或可用余额不足，不能兑换");
        }
        Balance after = new Balance(before.available - amountCent, before.frozen + amountCent, before.debt);
        persistAccount(account, after, 0L);
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder().memberId(memberId).businessType(TOKEN_EXCHANGE)
                .businessId(businessId).idempotencyKey(idempotencyKey).amountCent(amountCent)
                .freezeAmount(moneyConverter.centToYuan(amountCent)).status(CpsFreezeStatusEnum.FROZEN.getStatus())
                .unfreezeTime(LocalDateTime.now().plusDays(30))
                .build();
        freezeRecordMapper.insert(freeze);
        appendLedger(memberId, TOKEN_EXCHANGE, String.valueOf(freeze.getId()), null, null,
                idempotencyKey, before, after, context);
        return freeze;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult unfreezeExchangeAsset(Long freezeRecordId, String idempotencyKey,
                                                       CpsAssetOperatorContext context) {
        return changeExchangeFreeze(freezeRecordId, idempotencyKey, context, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult confirmExchangeDeduct(Long freezeRecordId, String idempotencyKey,
                                                       CpsAssetOperatorContext context) {
        return changeExchangeFreeze(freezeRecordId, idempotencyKey, context, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsFreezeRecordDO freezeAvailableForWithdrawal(Long memberId, long amountCent, String businessId,
                                                           String idempotencyKey, CpsAssetOperatorContext context) {
        requirePositive(amountCent, "提现冻结金额");
        requireText(businessId, "提现业务单号");
        requireText(idempotencyKey, "幂等键");
        CpsRebateAssetLedgerDO existing = ledgerMapper.selectByBusinessAndIdempotencyKey(WITHDRAWAL, idempotencyKey);
        if (existing != null) return validateWithdrawalFreezeReplay(existing, memberId, amountCent, businessId);
        policyService.assertWritable();
        CpsRebateAccountDO account = lockOrCreateAccount(memberId);
        existing = ledgerMapper.selectByBusinessAndIdempotencyKey(WITHDRAWAL, idempotencyKey);
        if (existing != null) return validateWithdrawalFreezeReplay(existing, memberId, amountCent, businessId);
        Balance before = balance(account);
        if (before.debt > 0L) throw new IllegalStateException("欠款未清零，不能提现");
        if (before.available < amountCent) throw new IllegalStateException("可用余额不足，不能提现");
        Balance after = new Balance(before.available - amountCent, before.frozen + amountCent, before.debt);
        persistAccount(account, after, 0L);
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder().memberId(memberId).businessType(WITHDRAWAL)
                .businessId(businessId).idempotencyKey(idempotencyKey).amountCent(amountCent)
                .freezeAmount(moneyConverter.centToYuan(amountCent)).status(CpsFreezeStatusEnum.FROZEN.getStatus())
                .build();
        freezeRecordMapper.insert(freeze);
        appendLedger(memberId, WITHDRAWAL, String.valueOf(freeze.getId()), null, null,
                idempotencyKey, before, after, context);
        return freeze;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult unfreezeWithdrawalAsset(Long freezeRecordId, String idempotencyKey,
                                                         CpsAssetOperatorContext context) {
        return changeWithdrawalFreeze(freezeRecordId, idempotencyKey, context, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult confirmWithdrawalDeduct(Long freezeRecordId, String idempotencyKey,
                                                         CpsAssetOperatorContext context) {
        return changeWithdrawalFreeze(freezeRecordId, idempotencyKey, context, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetResult manualAdjustDebt(Long memberId, CpsDebtAdjustAction action, long amountCent,
                                                 String sourceBusinessId, CpsAssetOperatorContext context) {
        requirePositive(amountCent, "调整金额");
        CpsRebateAssetResult replay = replay(DEBT_ADJUST, context.idempotencyKey());
        if (replay != null) return replay;
        policyService.assertWritable();
        CpsRebateAccountDO account = lockOrCreateAccount(memberId);
        replay = replay(DEBT_ADJUST, context.idempotencyKey());
        if (replay != null) return replay;
        Balance before = balance(account);
        long debtChange;
        if (action == CpsDebtAdjustAction.INCREASE) {
            createDebt(memberId, null, null, sourceBusinessId,
                    context.idempotencyKey(), amountCent);
            debtChange = amountCent;
        } else {
            long waived = Math.min(amountCent, before.debt);
            if (waived != amountCent) throw new IllegalStateException("减免金额超过当前欠款");
            waiveOutstandingDebt(memberId, waived);
            debtChange = -waived;
        }
        Balance after = new Balance(before.available, before.frozen, before.debt + debtChange);
        persistAccount(account, after, 0L);
        CpsRebateAssetLedgerDO ledger = appendLedger(memberId, DEBT_ADJUST, sourceBusinessId, null, null,
                context.idempotencyKey(), before, after, context);
        // 调整来源业务号可能不是数字；首次与幂等重放均返回同一可重建结果。
        return result(ledger, parseLong(ledger.getBusinessId()), false);
    }

    private CpsRebateAssetResult changeExchangeFreeze(Long freezeRecordId, String idempotencyKey,
                                                       CpsAssetOperatorContext context, boolean deduct) {
        String businessType = deduct ? TOKEN_EXCHANGE_DEDUCT : TOKEN_EXCHANGE_UNFREEZE;
        CpsRebateAssetResult replay = replay(businessType, idempotencyKey);
        if (replay != null) return replay;
        policyService.assertWritable();
        CpsFreezeRecordDO freeze = requireNonNull(freezeRecordMapper.selectForUpdateById(freezeRecordId),
                "冻结记录不存在: " + freezeRecordId);
        requireBusinessAndStatus(freeze, TOKEN_EXCHANGE, CpsFreezeStatusEnum.FROZEN.getStatus());
        CpsRebateAccountDO account = lockOrCreateAccount(freeze.getMemberId());
        replay = replay(businessType, idempotencyKey);
        if (replay != null) return replay;
        long amountCent = freezeAmountCent(freeze);
        Balance before = balance(account);
        if (before.frozen < amountCent) throw new IllegalStateException("账户冻结余额不足");
        if (before.available > 0L && before.debt > 0L) {
            throw new IllegalStateException("账户可用余额与欠款同时存在，需先完成资产对账");
        }
        if (deduct && before.debt > 0L) {
            throw new IllegalStateException("兑换冻结期间产生欠款，不能确认扣减");
        }
        long repaid = deduct ? 0L : repayOutstandingDebt(freeze.getMemberId(), Math.min(amountCent, before.debt));
        Balance after = new Balance(before.available + (deduct ? 0L : amountCent - repaid),
                before.frozen - amountCent, before.debt - repaid);
        persistAccount(account, after, 0L);
        markFreeze(freeze, deduct ? CpsFreezeStatusEnum.DEDUCTED.getStatus()
                : CpsFreezeStatusEnum.UNFREEZED.getStatus(), deduct ? null : LocalDateTime.now(), null);
        CpsRebateAssetLedgerDO ledger = appendLedger(freeze.getMemberId(), businessType,
                String.valueOf(freezeRecordId), null, null, idempotencyKey, before, after, context);
        return result(ledger, freezeRecordId, false);
    }

    private CpsRebateAssetResult changeWithdrawalFreeze(Long freezeRecordId, String idempotencyKey,
                                                          CpsAssetOperatorContext context, boolean deduct) {
        String businessType = deduct ? WITHDRAWAL_DEDUCT : WITHDRAWAL_UNFREEZE;
        CpsRebateAssetResult replay = replay(businessType, idempotencyKey);
        if (replay != null) return replay;
        policyService.assertWritable();
        CpsFreezeRecordDO freeze = requireNonNull(freezeRecordMapper.selectForUpdateById(freezeRecordId),
                "提现冻结记录不存在: " + freezeRecordId);
        requireBusinessAndStatus(freeze, WITHDRAWAL, CpsFreezeStatusEnum.FROZEN.getStatus());
        CpsRebateAccountDO account = lockOrCreateAccount(freeze.getMemberId());
        replay = replay(businessType, idempotencyKey);
        if (replay != null) return replay;
        long amountCent = freezeAmountCent(freeze);
        Balance before = balance(account);
        if (before.frozen < amountCent) throw new IllegalStateException("账户冻结余额不足");
        if (before.available > 0L && before.debt > 0L) {
            throw new IllegalStateException("账户可用余额与欠款同时存在，需先完成资产对账");
        }
        long repaid = deduct ? 0L : repayOutstandingDebt(freeze.getMemberId(), Math.min(amountCent, before.debt));
        Balance after = new Balance(before.available + (deduct ? 0L : amountCent - repaid),
                before.frozen - amountCent, before.debt - repaid);
        persistAccount(account, after, 0L, deduct ? amountCent : 0L);
        markFreeze(freeze, deduct ? CpsFreezeStatusEnum.DEDUCTED.getStatus()
                : CpsFreezeStatusEnum.UNFREEZED.getStatus(), deduct ? null : LocalDateTime.now(), null);
        CpsRebateAssetLedgerDO ledger = appendLedger(freeze.getMemberId(), businessType,
                String.valueOf(freezeRecordId), null, null, idempotencyKey, before, after, context);
        return result(ledger, freezeRecordId, false);
    }

    private CpsFreezeRecordDO validateExchangeFreezeReplay(CpsRebateAssetLedgerDO ledger,
                                                            Long memberId, long amountCent) {
        CpsFreezeRecordDO freeze = requireNonNull(freezeRecordMapper.selectById(ledger.getBusinessId()),
                "兑换幂等流水对应的冻结记录不存在: " + ledger.getBusinessId());
        if (!memberId.equals(freeze.getMemberId()) || freezeAmountCent(freeze) != amountCent) {
            throw new IllegalStateException("幂等键已用于其他会员或金额的兑换请求");
        }
        return freeze;
    }

    private CpsFreezeRecordDO validateWithdrawalFreezeReplay(CpsRebateAssetLedgerDO ledger,
                                                               Long memberId, long amountCent, String businessId) {
        CpsFreezeRecordDO freeze = requireNonNull(freezeRecordMapper.selectById(ledger.getBusinessId()),
                "提现幂等流水对应的冻结记录不存在: " + ledger.getBusinessId());
        if (!memberId.equals(freeze.getMemberId()) || freezeAmountCent(freeze) != amountCent
                || !businessId.equals(freeze.getBusinessId())) {
            throw new IllegalStateException("幂等键已用于其他提现请求");
        }
        return freeze;
    }

    private CpsRebateAccountDO lockOrCreateAccount(Long memberId) {
        requirePositive(memberId, "会员ID");
        CpsRebateAccountDO account = accountMapper.selectForUpdateByMemberId(memberId);
        if (account != null) return account;
        CpsRebateAccountDO created = CpsRebateAccountDO.builder().memberId(memberId)
                .totalRebate(BigDecimal.ZERO).availableBalance(BigDecimal.ZERO).frozenBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO).withdrawnAmount(BigDecimal.ZERO).status(1).version(0).build();
        try {
            accountMapper.insert(created);
            return created;
        } catch (DuplicateKeyException duplicate) {
            CpsRebateAccountDO concurrent = accountMapper.selectForUpdateByMemberId(memberId);
            if (concurrent == null) throw duplicate;
            return concurrent;
        }
    }

    private Balance balance(CpsRebateAccountDO account) {
        return new Balance(moneyConverter.yuanToCent(zero(account.getAvailableBalance())),
                moneyConverter.yuanToCent(zero(account.getFrozenBalance())),
                moneyConverter.yuanToCent(zero(account.getDebtBalance())));
    }

    private void persistAccount(CpsRebateAccountDO account, Balance after, long totalChangeCent) {
        persistAccount(account, after, totalChangeCent, 0L);
    }

    private void persistAccount(CpsRebateAccountDO account, Balance after, long totalChangeCent,
                                long withdrawnChangeCent) {
        CpsRebateAccountDO update = CpsRebateAccountDO.builder().id(account.getId())
                .availableBalance(moneyConverter.centToYuan(after.available))
                .frozenBalance(moneyConverter.centToYuan(after.frozen))
                .debtBalance(moneyConverter.centToYuan(after.debt))
                .totalRebate(zero(account.getTotalRebate()).add(moneyConverter.centToYuan(totalChangeCent))
                        .max(BigDecimal.ZERO))
                .withdrawnAmount(zero(account.getWithdrawnAmount())
                        .add(moneyConverter.centToYuan(withdrawnChangeCent)))
                .version(account.getVersion()).build();
        if (accountMapper.updateById(update) != 1) {
            throw new IllegalStateException("返利账户并发更新失败: " + account.getMemberId());
        }
    }

    private long repayOutstandingDebt(Long memberId, long repaymentCent) {
        long remaining = repaymentCent;
        List<CpsRebateDebtDO> debts = debtMapper.selectOutstandingForUpdateByMemberId(memberId);
        for (CpsRebateDebtDO debt : debts) {
            if (remaining == 0L) break;
            long outstanding = value(debt.getOutstandingDebtCent());
            long paid = Math.min(remaining, outstanding);
            CpsRebateDebtDO update = CpsRebateDebtDO.builder().id(debt.getId())
                    .repaidDebtCent(Math.addExact(value(debt.getRepaidDebtCent()), paid))
                    .outstandingDebtCent(outstanding - paid)
                    .status(outstanding == paid ? "PAID" : "PARTIAL").build();
            if (debtMapper.updateById(update) != 1) {
                throw new IllegalStateException("欠款并发偿还失败: " + debt.getId());
            }
            remaining -= paid;
        }
        if (remaining != 0L) {
            throw new IllegalStateException("账户欠款余额与欠款明细不一致: memberId=" + memberId);
        }
        return repaymentCent;
    }

    private CpsRebateDebtDO createDebt(Long memberId, Long orderId, String platformOrderId,
                                        String sourceBusinessId, String idempotencyKey, long amountCent) {
        CpsRebateDebtDO debt = CpsRebateDebtDO.builder().memberId(memberId).orderId(orderId)
                .platformOrderId(platformOrderId).sourceBusinessId(sourceBusinessId).idempotencyKey(idempotencyKey)
                .originalDebtCent(amountCent).repaidDebtCent(0L).waivedDebtCent(0L).outstandingDebtCent(amountCent)
                .status("OPEN").nextReminderTime(LocalDateTime.now()).build();
        debtMapper.insert(debt);
        return debt;
    }

    private void waiveOutstandingDebt(Long memberId, long waiverCent) {
        long remaining = waiverCent;
        for (CpsRebateDebtDO debt : debtMapper.selectOutstandingForUpdateByMemberId(memberId)) {
            if (remaining == 0L) break;
            long outstanding = value(debt.getOutstandingDebtCent());
            long waived = Math.min(remaining, outstanding);
            CpsRebateDebtDO update = CpsRebateDebtDO.builder().id(debt.getId())
                    .waivedDebtCent(Math.addExact(value(debt.getWaivedDebtCent()), waived))
                    .outstandingDebtCent(outstanding - waived)
                    .status(outstanding == waived ? "WAIVED" : "PARTIAL").build();
            if (debtMapper.updateById(update) != 1) {
                throw new IllegalStateException("欠款并发减免失败: " + debt.getId());
            }
            remaining -= waived;
        }
        if (remaining != 0L) {
            throw new IllegalStateException("账户欠款余额与欠款明细不一致: memberId=" + memberId);
        }
    }

    private void createRefundRecord(CpsRebateRecordDO rebate, String idempotencyKey) {
        CpsRebateRecordDO refund = CpsRebateRecordDO.builder().memberId(rebate.getMemberId())
                .orderId(rebate.getOrderId()).platformCode(rebate.getPlatformCode())
                .platformOrderId(rebate.getPlatformOrderId()).itemId(rebate.getItemId()).itemTitle(rebate.getItemTitle())
                .orderAmount(rebate.getOrderAmount()).commissionAmount(rebate.getCommissionAmount())
                .rebateRate(rebate.getRebateRate()).rebateAmount(rebate.getRebateAmount().negate())
                .rebateType(CpsRebateTypeEnum.REFUND.getType()).rebateStatus(CpsRebateStatusEnum.REFUNDED.getStatus())
                .precedingRebateId(rebate.getId()).remark("订单退款由统一资产服务冲正，幂等键=" + idempotencyKey).build();
        rebateRecordMapper.insert(refund);
        CpsRebateRecordDO original = new CpsRebateRecordDO();
        original.setId(rebate.getId());
        original.setRebateStatus(CpsRebateStatusEnum.REFUNDED.getStatus());
        rebateRecordMapper.updateById(original);
    }

    private void markOriginalRebateReceived(Long orderId, LocalDateTime receivedAt) {
        if (orderId == null) return;
        CpsRebateRecordDO rebate = rebateRecordMapper.selectByOrderIdAndType(orderId, CpsRebateTypeEnum.REBATE.getType());
        if (rebate != null) {
            CpsRebateRecordDO update = new CpsRebateRecordDO();
            update.setId(rebate.getId());
            update.setRebateStatus(CpsRebateStatusEnum.RECEIVED.getStatus());
            rebateRecordMapper.updateById(update);
        }
        int updated = orderMapper.markRebateReceived(orderId, receivedAt);
        if (updated == 0) {
            CpsOrderDO current = orderMapper.selectById(orderId);
            if (current == null || !CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(current.getOrderStatus())) {
                throw new IllegalStateException("订单返利到账状态回写失败: " + orderId);
            }
        }
    }

    private void markFreeze(CpsFreezeRecordDO freeze, String status, LocalDateTime actualUnfreezeTime,
                            CpsAssetOperatorContext manualContext) {
        CpsFreezeRecordDO update = CpsFreezeRecordDO.builder().id(freeze.getId()).status(status)
                .actualUnfreezeTime(actualUnfreezeTime)
                .manualUnfreezeReason(manualContext == null ? null : manualContext.reason())
                .manualUnfreezeOperatorId(manualContext == null ? null : manualContext.operatorId())
                .build();
        if (freezeRecordMapper.updateById(update) != 1) {
            throw new IllegalStateException("冻结记录并发更新失败: " + freeze.getId());
        }
    }

    private CpsRebateAssetLedgerDO appendLedger(Long memberId, String businessType, String businessId,
                                                 Long orderId, String platformOrderId, String idempotencyKey,
                                                 Balance before, Balance after, CpsAssetOperatorContext context) {
        CpsRebateAssetLedgerDO ledger = CpsRebateAssetLedgerDO.builder().memberId(memberId)
                .sourceSystem("AgenticCPS").businessType(businessType).businessId(businessId)
                .orderId(orderId).platformOrderId(platformOrderId).idempotencyKey(idempotencyKey)
                .availableChangeCent(after.available - before.available)
                .frozenChangeCent(after.frozen - before.frozen).debtChangeCent(after.debt - before.debt)
                .availableBeforeCent(before.available).availableAfterCent(after.available)
                .frozenBeforeCent(before.frozen).frozenAfterCent(after.frozen)
                .debtBeforeCent(before.debt).debtAfterCent(after.debt)
                .operatorType(context.operatorType()).operatorId(context.operatorId()).reason(context.reason()).build();
        ledgerMapper.insert(ledger);
        return ledger;
    }

    private CpsRebateAssetResult replay(String businessType, String idempotencyKey) {
        requireText(idempotencyKey, "幂等键");
        CpsRebateAssetLedgerDO ledger = ledgerMapper.selectByBusinessAndIdempotencyKey(businessType, idempotencyKey);
        return ledger == null ? null : result(ledger, parseLong(ledger.getBusinessId()), true);
    }

    private CpsRebateAssetResult result(CpsRebateAssetLedgerDO ledger, Long businessRecordId, boolean replay) {
        return new CpsRebateAssetResult(ledger.getId(), businessRecordId,
                value(ledger.getAvailableAfterCent()), value(ledger.getFrozenAfterCent()),
                value(ledger.getDebtAfterCent()), replay);
    }

    private long freezeAmountCent(CpsFreezeRecordDO freeze) {
        long amountCent = freeze.getAmountCent() != null && freeze.getAmountCent() > 0
                ? freeze.getAmountCent() : moneyConverter.yuanToCent(freeze.getFreezeAmount());
        requirePositive(amountCent, "冻结金额");
        return amountCent;
    }

    private void requireBusinessAndStatus(CpsFreezeRecordDO freeze, String businessType, String status) {
        if (!businessType.equals(freeze.getBusinessType()) || !status.equals(freeze.getStatus())) {
            throw new IllegalStateException("冻结记录业务类型或状态不允许当前操作: " + freeze.getId());
        }
    }

    private static LocalDateTime later(LocalDateTime first, LocalDateTime second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }

    private static Long parseLong(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) throw new IllegalStateException(message);
        return value;
    }

    private static void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(name + "不能为空");
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0L) throw new IllegalArgumentException(name + "必须大于0");
    }

    private record Balance(long available, long frozen, long debt) {
        private Balance {
            if (available < 0L || frozen < 0L || debt < 0L) {
                throw new IllegalStateException("资产余额不能为负数");
            }
        }
    }
}
