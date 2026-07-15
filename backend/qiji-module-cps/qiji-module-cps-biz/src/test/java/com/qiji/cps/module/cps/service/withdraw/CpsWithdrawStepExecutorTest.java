package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.dal.mysql.withdraw.CpsWithdrawMapper;
import com.qiji.cps.module.cps.enums.CpsWithdrawStatusEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsWithdrawStepExecutorTest {
    @InjectMocks private CpsWithdrawStepExecutor executor;
    @Mock private CpsWithdrawMapper withdrawMapper;
    @Mock private CpsRebateAssetService assetService;
    @Spy private CpsMoneyConverter moneyConverter = new CpsMoneyConverter();

    @Test
    void createAndFreezePersistsOneWithdrawalBoundToDedicatedFreeze() {
        CpsWithdrawCreateCommand command = new CpsWithdrawCreateCommand(
                1001L, 1200L, "alipay", "member@example.com", "Member", "withdraw-1");
        doAnswer(invocation -> {
            CpsWithdrawDO inserted = invocation.getArgument(0);
            inserted.setId(8L);
            return 1;
        }).when(withdrawMapper).insert(any(CpsWithdrawDO.class));
        when(assetService.freezeAvailableForWithdrawal(eq(1001L), eq(1200L), any(), eq("withdraw-1"), any()))
                .thenReturn(CpsFreezeRecordDO.builder().id(18L).memberId(1001L).businessType("WITHDRAWAL")
                        .amountCent(1200L).build());
        when(withdrawMapper.updateById(any(CpsWithdrawDO.class))).thenReturn(1);

        CpsWithdrawDO result = executor.createAndFreeze(command);

        assertEquals(8L, result.getId());
        assertEquals(18L, result.getFreezeRecordId());
        assertEquals(new BigDecimal("12.00"), result.getAmount());
        verify(withdrawMapper).insert(any(CpsWithdrawDO.class));
        verify(assetService).freezeAvailableForWithdrawal(eq(1001L), eq(1200L), any(), eq("withdraw-1"), any());
    }

    @Test
    void terminalStepsDeductOnSuccessAndUnfreezeOnClosed() {
        CpsWithdrawDO success = reviewing(8L, 88L);
        when(withdrawMapper.selectById(8L)).thenReturn(success);
        when(withdrawMapper.updateByIdAndStatusVersion(any(), eq(0), anyList())).thenReturn(1);

        executor.completeSuccess(8L, 88L);

        verify(assetService).confirmWithdrawalDeduct(eq(18L), eq("withdraw-deduct:8"), any());
        verify(withdrawMapper).updateByIdAndStatusVersion(
                org.mockito.ArgumentMatchers.argThat(update -> CpsWithdrawStatusEnum.SUCCESS.getStatus().equals(update.getStatus())),
                eq(0), eq(List.of(CpsWithdrawStatusEnum.REVIEWING.getStatus())));

        CpsWithdrawDO failed = reviewing(9L, 99L);
        when(withdrawMapper.selectById(9L)).thenReturn(failed);
        executor.completeFailure(9L, 99L, "closed");
        verify(assetService).unfreezeWithdrawalAsset(eq(18L), eq("withdraw-unfreeze:9"), any());
    }

    @Test
    void attachCasConflictMustNotAcceptAnotherTransfer() {
        CpsWithdrawDO unbound = reviewing(8L, null);
        CpsWithdrawDO concurrentlyBound = reviewing(8L, 99L);
        when(withdrawMapper.selectById(8L)).thenReturn(unbound, concurrentlyBound);
        when(withdrawMapper.updateByIdAndStatusVersion(any(), eq(0), anyList())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> executor.attachPayTransfer(8L, 88L));
    }

    @Test
    void idempotencyReplayChecksEveryImmutableField() {
        CpsWithdrawDO existing = reviewing(8L, null).setWithdrawType("alipay")
                .setWithdrawAccount("member@example.com").setWithdrawAccountName("Member")
                .setIdempotencyKey("same-key");
        when(withdrawMapper.selectByIdempotencyKey("same-key")).thenReturn(existing);

        CpsWithdrawCreateCommand changedAccount = new CpsWithdrawCreateCommand(
                1001L, 1200L, "alipay", "other@example.com", "Member", "same-key");
        assertThrows(IllegalStateException.class, () -> executor.createAndFreeze(changedAccount));
    }

    @Test
    void localMoneyStepsUseIndependentTransactions() throws Exception {
        for (String name : List.of("createAndFreeze", "markReviewing", "rejectAndUnfreeze", "claimTransfer",
                "attachPayTransfer", "scheduleRetry", "completeSuccess", "completeFailure")) {
            Method method = switch (name) {
                case "createAndFreeze" -> CpsWithdrawStepExecutor.class.getMethod(name, CpsWithdrawCreateCommand.class);
                case "markReviewing" -> CpsWithdrawStepExecutor.class.getMethod(name, Long.class, String.class, Long.class);
                case "rejectAndUnfreeze" -> CpsWithdrawStepExecutor.class.getMethod(name, Long.class, String.class, String.class);
                case "attachPayTransfer", "completeSuccess" -> CpsWithdrawStepExecutor.class.getMethod(name, Long.class, Long.class);
                case "completeFailure" -> CpsWithdrawStepExecutor.class.getMethod(name, Long.class, Long.class, String.class);
                case "scheduleRetry" -> CpsWithdrawStepExecutor.class.getMethod(name, Long.class, String.class);
                default -> CpsWithdrawStepExecutor.class.getMethod(name, Long.class);
            };
            Transactional annotation = method.getAnnotation(Transactional.class);
            assertNotNull(annotation, name);
            assertEquals(Propagation.REQUIRES_NEW, annotation.propagation(), name);
        }
    }

    private CpsWithdrawDO reviewing(Long id, Long payTransferId) {
        return CpsWithdrawDO.builder().id(id).memberId(1001L).freezeRecordId(18L).amountCent(1200L)
                .payTransferId(payTransferId).statusVersion(0)
                .status(CpsWithdrawStatusEnum.REVIEWING.getStatus()).build();
    }
}
