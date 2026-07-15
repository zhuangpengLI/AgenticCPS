package com.qiji.cps.module.cps.service.withdraw;

import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.enums.CpsWithdrawStatusEnum;
import com.qiji.cps.module.pay.api.transfer.PayTransferApi;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferCreateRespDTO;
import com.qiji.cps.module.pay.api.transfer.dto.PayTransferRespDTO;
import com.qiji.cps.module.pay.enums.transfer.PayTransferStatusEnum;
import com.qiji.cps.framework.common.enums.UserTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsWithdrawTransferExecutorTest {

    @InjectMocks private CpsWithdrawTransferExecutor executor;
    @Mock private CpsWithdrawStepExecutor stepExecutor;
    @Mock private PayTransferApi payTransferApi;

    @Test
    void successConfirmsDeductAndClosedUnfreezes() {
        CpsWithdrawDO successOrder = order(8L, null);
        when(stepExecutor.claimTransfer(8L)).thenReturn(successOrder);
        when(payTransferApi.getTransfer("cps", "8")).thenReturn(null);
        when(payTransferApi.createTransfer(any())).thenReturn(new PayTransferCreateRespDTO().setId(88L));
        when(stepExecutor.attachPayTransfer(8L, 88L)).thenReturn(order(8L, 88L));
        when(payTransferApi.getTransfer(88L)).thenReturn(transfer(88L, PayTransferStatusEnum.SUCCESS.getStatus()));

        executor.startTransfer(8L);

        verify(stepExecutor).completeSuccess(8L, 88L);
        verify(stepExecutor, never()).completeFailure(any(), any(), any());
    }

    @Test
    void timeoutKeepsFrozenAndSchedulesCompensation() {
        when(stepExecutor.claimTransfer(8L)).thenReturn(order(8L, null));
        when(payTransferApi.getTransfer("cps", "8")).thenReturn(null);
        when(payTransferApi.createTransfer(any())).thenThrow(new IllegalStateException("timeout"));

        executor.startTransfer(8L);

        verify(stepExecutor).scheduleRetry(8L, "timeout");
        verify(stepExecutor, never()).completeFailure(any(), any(), any());
        verify(stepExecutor, never()).completeSuccess(any(), any());
    }

    @Test
    void existingProcessingTransferIsQueriedAndClosedIsUnfrozen() {
        when(stepExecutor.claimTransfer(8L)).thenReturn(order(8L, 88L));
        when(payTransferApi.getTransfer(88L)).thenReturn(transfer(88L, PayTransferStatusEnum.CLOSED.getStatus()));

        executor.startTransfer(8L);

        verify(payTransferApi, never()).createTransfer(any());
        verify(stepExecutor).completeFailure(8L, 88L, "closed");
    }

    @Test
    void existingPayTransferIsRecoveredBeforeCreatingAnotherOne() {
        when(stepExecutor.claimTransfer(8L)).thenReturn(order(8L, null));
        when(payTransferApi.getTransfer("cps", "8"))
                .thenReturn(transfer(88L, PayTransferStatusEnum.SUCCESS.getStatus()));
        when(stepExecutor.attachPayTransfer(8L, 88L)).thenReturn(order(8L, 88L));

        executor.startTransfer(8L);

        verify(payTransferApi, never()).createTransfer(any());
        verify(stepExecutor).completeSuccess(8L, 88L);
    }

    @Test
    void mismatchedPayIdentityNeverDeductsFrozenAsset() {
        when(stepExecutor.claimTransfer(8L)).thenReturn(order(8L, 88L));
        PayTransferRespDTO transfer = transfer(88L, PayTransferStatusEnum.SUCCESS.getStatus())
                .setUserAccount("attacker@example.com");
        when(payTransferApi.getTransfer(88L)).thenReturn(transfer);

        executor.startTransfer(8L);

        verify(stepExecutor, never()).completeSuccess(any(), any());
        verify(stepExecutor).scheduleRetry(8L, "Pay 转账单与提现申请不匹配");
    }

    private CpsWithdrawDO order(Long id, Long payTransferId) {
        return CpsWithdrawDO.builder().id(id).memberId(1001L).amountCent(1200L)
                .withdrawType("alipay").withdrawAccount("member@example.com")
                .withdrawAccountName("Member").status(CpsWithdrawStatusEnum.REVIEWING.getStatus())
                .payTransferId(payTransferId).transferChannelCode("alipay_pc").build();
    }

    private PayTransferRespDTO transfer(Long id, Integer status) {
        return new PayTransferRespDTO().setId(id).setStatus(status).setPrice(1200)
                .setMerchantTransferId("8").setChannelCode("alipay_pc")
                .setUserId(1001L).setUserType(UserTypeEnum.MEMBER.getValue())
                .setUserAccount("member@example.com").setUserName("Member");
    }
}
