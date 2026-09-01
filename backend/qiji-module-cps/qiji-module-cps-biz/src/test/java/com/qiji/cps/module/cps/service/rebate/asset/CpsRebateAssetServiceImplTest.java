package com.qiji.cps.module.cps.service.rebate.asset;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsRebateAssetServiceImplTest {

    @InjectMocks
    private CpsRebateAssetServiceImpl service;

    @Mock private CpsOrderMapper orderMapper;
    @Mock private CpsRebateRecordMapper rebateRecordMapper;
    @Mock private CpsRebateAccountMapper accountMapper;
    @Mock private CpsFreezeRecordMapper freezeRecordMapper;
    @Mock private CpsRebateAssetLedgerMapper ledgerMapper;
    @Mock private CpsRebateDebtMapper debtMapper;
    @Mock private CpsFreezeService freezeService;
    @Mock private CpsRebateAssetPolicyService policyService;
    @Spy private CpsMoneyConverter moneyConverter = new CpsMoneyConverter();

    @Test
    void reverseOrderRebateStopsBeforeAnyMoneyReadWhenTenantPolicyIsReadOnly() {
        doThrow(new IllegalStateException("只读模式")).when(policyService).assertWritable();

        assertThrows(IllegalStateException.class, () -> service.reverseOrderRebate(22L, "refund-read-only"));

        verifyNoInteractions(rebateRecordMapper, accountMapper, freezeRecordMapper, debtMapper);
    }

    @Test
    void reverseOrderRebateCreatesDebtWhenAvailableIsInsufficient() {
        CpsRebateRecordDO rebate = CpsRebateRecordDO.builder()
                .id(11L).orderId(22L).memberId(33L).platformCode("taobao")
                .platformOrderId("P22").rebateAmount(new BigDecimal("10.00"))
                .rebateType(CpsRebateTypeEnum.REBATE.getType())
                .rebateStatus(CpsRebateStatusEnum.RECEIVED.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("ORDER_REFUND", "refund-22")).thenReturn(null);
        when(rebateRecordMapper.selectByOrderIdAndType(22L, CpsRebateTypeEnum.REBATE.getType())).thenReturn(rebate);
        when(freezeRecordMapper.selectForUpdateByBusinessId("ORDER_REBATE", "22")).thenReturn(null);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 300, 0, 0));
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);

        CpsRebateAssetResult result = service.reverseOrderRebate(22L, "refund-22");

        assertEquals(0, result.availableBalanceCent());
        assertEquals(700, result.debtBalanceCent());
        ArgumentCaptor<CpsRebateDebtDO> debtCaptor = ArgumentCaptor.forClass(CpsRebateDebtDO.class);
        verify(debtMapper).insert((CpsRebateDebtDO) debtCaptor.capture());
        assertEquals(700L, debtCaptor.getValue().getOutstandingDebtCent());
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) -> ledger.getAvailableChangeCent() == -300L
                && ledger.getDebtChangeCent() == 700L));
    }

    @Test
    void createOrderRebateFreezeAllowsSettledOrderWithoutReceiptTime() {
        CpsOrderDO order = CpsOrderDO.builder().id(24L).memberId(33L).platformCode("taobao")
                .platformOrderId("P24").orderStatus(CpsOrderStatusEnum.SETTLED.getStatus())
                .settleTime(java.time.LocalDateTime.now()).build();
        CpsRebateRecordDO rebate = CpsRebateRecordDO.builder().id(25L).orderId(24L)
                .rebateAmount(new BigDecimal("10.00")).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("ORDER_REBATE", "order-rebate:24"))
                .thenReturn(null);
        when(orderMapper.selectById(24L)).thenReturn(order);
        when(rebateRecordMapper.selectByOrderIdAndType(24L, CpsRebateTypeEnum.REBATE.getType()))
                .thenReturn(rebate);
        when(moneyConverter.yuanToCent(new BigDecimal("10.00"))).thenReturn(1000L);
        when(freezeService.getActiveConfig("taobao", 1000L))
                .thenReturn(com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO.builder()
                        .id(26L).unfreezeDays(15).build());
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 0, 0));
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.selectByBusinessId("ORDER_REBATE", "24")).thenReturn(null);

        CpsFreezeRecordDO freeze = service.createOrderRebateFreeze(24L, "order-rebate:24");

        assertEquals(order.getSettleTime(), freeze.getEligibleTime());
        verify(freezeRecordMapper).insert(any(CpsFreezeRecordDO.class));
    }

    @Test
    void releaseOrderRebateRepaysOldestDebtBeforeCreditingAvailable() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .id(40L).memberId(33L).orderId(22L).businessType("ORDER_REBATE")
                .businessId("22").amountCent(1000L).freezeAmount(new BigDecimal("10.00"))
                .unfreezeTime(java.time.LocalDateTime.now().minusMinutes(1))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        CpsRebateDebtDO debt = CpsRebateDebtDO.builder()
                .id(50L).memberId(33L).originalDebtCent(700L).repaidDebtCent(0L)
                .outstandingDebtCent(700L).status("OPEN").build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("ORDER_REBATE_RELEASE", "release-40")).thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(40L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 700));
        when(debtMapper.selectOutstandingForUpdateByMemberId(33L)).thenReturn(List.of(debt));
        when(debtMapper.updateById(any(CpsRebateDebtDO.class))).thenReturn(1);
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.updateById(any(CpsFreezeRecordDO.class))).thenReturn(1);
        when(orderMapper.markRebateReceived(eq(22L), any())).thenReturn(1);
        when(rebateRecordMapper.selectByOrderIdAndType(22L, CpsRebateTypeEnum.REBATE.getType()))
                .thenReturn(CpsRebateRecordDO.builder().id(44L).orderId(22L).rebateStatus("pending").build());

        CpsRebateAssetResult result = service.releaseOrderRebate(40L,
                CpsAssetOperatorContext.system("release-40", "scheduled release"));

        assertEquals(300, result.availableBalanceCent());
        assertEquals(0, result.frozenBalanceCent());
        assertEquals(0, result.debtBalanceCent());
        verify(debtMapper).updateById(argThat((CpsRebateDebtDO updated) -> updated.getOutstandingDebtCent() == 0L
                && "PAID".equals(updated.getStatus())));
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) -> ledger.getAvailableChangeCent() == 300L
                && ledger.getFrozenChangeCent() == -1000L
                && ledger.getDebtChangeCent() == -700L));
        verify(orderMapper).markRebateReceived(eq(22L), any());
    }

    @Test
    void releaseOrderRebateFallsBackToLegacyYuanAmountWhenCentColumnIsZero() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .id(41L).memberId(33L).orderId(23L).businessType("ORDER_REBATE")
                .businessId("23").amountCent(0L).freezeAmount(new BigDecimal("10.00"))
                .unfreezeTime(java.time.LocalDateTime.now().minusMinutes(1))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("ORDER_REBATE_RELEASE", "release-legacy-41"))
                .thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(41L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 0));
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.updateById(any(CpsFreezeRecordDO.class))).thenReturn(1);
        when(orderMapper.markRebateReceived(eq(23L), any())).thenReturn(1);

        CpsRebateAssetResult result = service.releaseOrderRebate(41L,
                CpsAssetOperatorContext.system("release-legacy-41", "legacy release"));

        assertEquals(1000, result.availableBalanceCent());
        assertEquals(0, result.frozenBalanceCent());
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) ->
                ledger.getAvailableChangeCent() == 1000L && ledger.getFrozenChangeCent() == -1000L));
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalBalancesWithoutMutatingAccount() {
        CpsRebateAssetLedgerDO prior = CpsRebateAssetLedgerDO.builder()
                .businessType("ORDER_REFUND").idempotencyKey("same-key")
                .availableAfterCent(125L).frozenAfterCent(0L).debtAfterCent(75L).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("ORDER_REFUND", "same-key")).thenReturn(prior);

        CpsRebateAssetResult result = service.reverseOrderRebate(22L, "same-key");

        assertEquals(125, result.availableBalanceCent());
        assertEquals(75, result.debtBalanceCent());
        verifyNoInteractions(accountMapper, rebateRecordMapper, freezeRecordMapper, debtMapper);
        verify(ledgerMapper, never()).insert(any(CpsRebateAssetLedgerDO.class));
    }

    @Test
    void unfreezeExchangeAssetRepaysNewDebtBeforeRestoringAvailableBalance() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .id(60L).memberId(33L).businessType("TOKEN_EXCHANGE").businessId("CPSX60")
                .amountCent(1000L).freezeAmount(new BigDecimal("10.00"))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        CpsRebateDebtDO debt = CpsRebateDebtDO.builder()
                .id(61L).memberId(33L).originalDebtCent(400L).repaidDebtCent(0L)
                .outstandingDebtCent(400L).status("OPEN").build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("TOKEN_EXCHANGE_UNFREEZE", "unfreeze-60"))
                .thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(60L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 400));
        when(debtMapper.selectOutstandingForUpdateByMemberId(33L)).thenReturn(List.of(debt));
        when(debtMapper.updateById(any(CpsRebateDebtDO.class))).thenReturn(1);
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.updateById(any(CpsFreezeRecordDO.class))).thenReturn(1);

        CpsRebateAssetResult result = service.unfreezeExchangeAsset(60L, "unfreeze-60",
                CpsAssetOperatorContext.system("unfreeze-60", "exchange failed"));

        assertEquals(600L, result.availableBalanceCent());
        assertEquals(0L, result.frozenBalanceCent());
        assertEquals(0L, result.debtBalanceCent());
        verify(debtMapper).updateById(argThat((CpsRebateDebtDO updated) ->
                updated.getOutstandingDebtCent() == 0L && "PAID".equals(updated.getStatus())));
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) ->
                ledger.getAvailableChangeCent() == 600L
                        && ledger.getFrozenChangeCent() == -1000L
                        && ledger.getDebtChangeCent() == -400L));
    }

    @Test
    void confirmExchangeDeductRejectsWhenDebtAppearsAfterFreeze() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .id(70L).memberId(33L).businessType("TOKEN_EXCHANGE").businessId("CPSX70")
                .amountCent(1000L).freezeAmount(new BigDecimal("10.00"))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("TOKEN_EXCHANGE_DEDUCT", "deduct-70"))
                .thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(70L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 400));

        assertThrows(IllegalStateException.class, () -> service.confirmExchangeDeduct(70L, "deduct-70",
                CpsAssetOperatorContext.system("deduct-70", "confirm exchange")));

        verify(accountMapper, never()).updateById(any(CpsRebateAccountDO.class));
        verify(freezeRecordMapper, never()).updateById(any(CpsFreezeRecordDO.class));
        verify(ledgerMapper, never()).insert(any(CpsRebateAssetLedgerDO.class));
    }

    @Test
    void freezeExchangeReplayRejectsDifferentMember() {
        CpsRebateAssetLedgerDO prior = CpsRebateAssetLedgerDO.builder()
                .businessType("TOKEN_EXCHANGE").businessId("80").idempotencyKey("exchange-replay")
                .memberId(33L).frozenChangeCent(1000L).build();
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .id(80L).memberId(33L).businessType("TOKEN_EXCHANGE").businessId("CPSX80")
                .amountCent(1000L).freezeAmount(new BigDecimal("10.00"))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("TOKEN_EXCHANGE", "exchange-replay"))
                .thenReturn(prior);
        when(freezeRecordMapper.selectById("80")).thenReturn(freeze);

        assertThrows(IllegalStateException.class, () -> service.freezeAvailableForExchange(
                34L, 1000L, "CPSX80", "exchange-replay",
                CpsAssetOperatorContext.system("exchange-replay", "replay")));
    }

    @Test
    void freezeExchangeReplayRejectsDifferentAmount() {
        CpsRebateAssetLedgerDO prior = CpsRebateAssetLedgerDO.builder()
                .businessType("TOKEN_EXCHANGE").businessId("81").idempotencyKey("exchange-replay-amount")
                .memberId(33L).frozenChangeCent(1000L).build();
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder()
                .id(81L).memberId(33L).businessType("TOKEN_EXCHANGE").businessId("CPSX81")
                .amountCent(1000L).freezeAmount(new BigDecimal("10.00"))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("TOKEN_EXCHANGE", "exchange-replay-amount"))
                .thenReturn(prior);
        when(freezeRecordMapper.selectById("81")).thenReturn(freeze);

        assertThrows(IllegalStateException.class, () -> service.freezeAvailableForExchange(
                33L, 900L, "CPSX81", "exchange-replay-amount",
                CpsAssetOperatorContext.system("exchange-replay-amount", "replay")));
    }

    @Test
    void freezeWithdrawalRejectsDebtAndDoesNotMutateAccount() {
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("WITHDRAWAL", "withdraw-1")).thenReturn(null);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 1000, 0, 200));

        assertThrows(IllegalStateException.class, () -> service.freezeAvailableForWithdrawal(
                33L, 1000L, "W1", "withdraw-1",
                CpsAssetOperatorContext.member(33L, "withdraw-1", "申请提现")));

        verify(accountMapper, never()).updateById(any(CpsRebateAccountDO.class));
        verify(freezeRecordMapper, never()).insert(any(CpsFreezeRecordDO.class));
        verify(ledgerMapper, never()).insert(any(CpsRebateAssetLedgerDO.class));
    }

    @Test
    void withdrawalFreezeUnfreezeAndDeductUseDedicatedLedgerTypes() {
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("WITHDRAWAL", "withdraw-2")).thenReturn(null);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 2000, 0, 0));
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);

        CpsFreezeRecordDO freeze = service.freezeAvailableForWithdrawal(33L, 1000L, "W2", "withdraw-2",
                CpsAssetOperatorContext.member(33L, "withdraw-2", "申请提现"));
        assertEquals("WITHDRAWAL", freeze.getBusinessType());
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) ->
                "WITHDRAWAL".equals(ledger.getBusinessType())
                        && ledger.getAvailableChangeCent() == -1000L
                        && ledger.getFrozenChangeCent() == 1000L));
    }

    @Test
    void confirmWithdrawalDeductRemovesFrozenAndIncrementsWithdrawnTotal() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder().id(90L).memberId(33L)
                .businessType("WITHDRAWAL").businessId("W90").amountCent(1000L)
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("WITHDRAWAL_DEDUCT", "deduct-w90"))
                .thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(90L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 0));
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.updateById(any(CpsFreezeRecordDO.class))).thenReturn(1);

        CpsRebateAssetResult result = service.confirmWithdrawalDeduct(90L, "deduct-w90",
                CpsAssetOperatorContext.system("deduct-w90", "withdraw success"));

        assertEquals(0L, result.frozenBalanceCent());
        verify(accountMapper).updateById(argThat((CpsRebateAccountDO update) ->
                new BigDecimal("10.00").equals(update.getWithdrawnAmount())));
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) ->
                "WITHDRAWAL_DEDUCT".equals(ledger.getBusinessType())
                        && ledger.getFrozenChangeCent() == -1000L));
    }

    @Test
    void confirmWithdrawalDeductKeepsDebtCreatedAfterExternalPayment() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder().id(93L).memberId(33L)
                .businessType("WITHDRAWAL").businessId("W93").amountCent(1000L)
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("WITHDRAWAL_DEDUCT", "deduct-w93"))
                .thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(93L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 400));
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.updateById(any(CpsFreezeRecordDO.class))).thenReturn(1);

        CpsRebateAssetResult result = service.confirmWithdrawalDeduct(93L, "deduct-w93",
                CpsAssetOperatorContext.system("deduct-w93", "Pay already succeeded"));

        assertEquals(0L, result.frozenBalanceCent());
        assertEquals(400L, result.debtBalanceCent());
    }

    @Test
    void failedWithdrawalUnfreezeRepaysDebtBeforeRestoringAvailable() {
        CpsFreezeRecordDO freeze = CpsFreezeRecordDO.builder().id(91L).memberId(33L)
                .businessType("WITHDRAWAL").businessId("W91").amountCent(1000L)
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build();
        CpsRebateDebtDO debt = CpsRebateDebtDO.builder().id(92L).memberId(33L)
                .originalDebtCent(400L).repaidDebtCent(0L).outstandingDebtCent(400L).status("OPEN").build();
        when(ledgerMapper.selectByBusinessAndIdempotencyKey("WITHDRAWAL_UNFREEZE", "unfreeze-w91"))
                .thenReturn(null);
        when(freezeRecordMapper.selectForUpdateById(91L)).thenReturn(freeze);
        when(accountMapper.selectForUpdateByMemberId(33L)).thenReturn(account(33L, 0, 1000, 400));
        when(debtMapper.selectOutstandingForUpdateByMemberId(33L)).thenReturn(List.of(debt));
        when(debtMapper.updateById(any(CpsRebateDebtDO.class))).thenReturn(1);
        when(accountMapper.updateById(any(CpsRebateAccountDO.class))).thenReturn(1);
        when(freezeRecordMapper.updateById(any(CpsFreezeRecordDO.class))).thenReturn(1);

        CpsRebateAssetResult result = service.unfreezeWithdrawalAsset(91L, "unfreeze-w91",
                CpsAssetOperatorContext.system("unfreeze-w91", "withdraw closed"));

        assertEquals(600L, result.availableBalanceCent());
        assertEquals(0L, result.debtBalanceCent());
        verify(ledgerMapper).insert(argThat((CpsRebateAssetLedgerDO ledger) ->
                "WITHDRAWAL_UNFREEZE".equals(ledger.getBusinessType())
                        && ledger.getAvailableChangeCent() == 600L
                        && ledger.getDebtChangeCent() == -400L));
    }

    private CpsRebateAccountDO account(long memberId, long availableCent, long frozenCent, long debtCent) {
        return CpsRebateAccountDO.builder()
                .id(1L).memberId(memberId)
                .availableBalance(BigDecimal.valueOf(availableCent, 2))
                .frozenBalance(BigDecimal.valueOf(frozenCent, 2))
                .debtBalance(BigDecimal.valueOf(debtCent, 2))
                .totalRebate(BigDecimal.ZERO).withdrawnAmount(BigDecimal.ZERO)
                .status(1).version(0).build();
    }
}
