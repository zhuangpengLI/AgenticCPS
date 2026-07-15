package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsRebateTokenExchangeOrderMapper;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeOrderRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateTokenExchangeCompensationServiceTest {

    @InjectMocks
    private CpsRebateTokenExchangeCompensationServiceImpl service;
    @Mock
    private CpsRebateTokenExchangeOrderMapper orderMapper;
    @Mock
    private CpsRebateTokenExchangeStepExecutor stepExecutor;
    @Mock
    private CpsAitokenExchangeClient aitokenExchangeClient;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        when(stepExecutor.claimCompensation(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void processingRemoteCreditedConfirmsLocalDeductAndRemoteSuccess() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.PROCESSING, "AT-1");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(aitokenExchangeClient.getOrder("AT-1", 1L)).thenReturn(remote("credited"));

        service.compensate(1L);

        verify(stepExecutor).markCredited(1L, "AT-1");
        verify(stepExecutor).confirmLocalDeduct(1L);
        verify(aitokenExchangeClient).confirmSourceDeduct(eq("AT-1"), any(), eq(1L));
        verify(stepExecutor).markSuccess(1L);
        verify(stepExecutor, never()).unfreezeAndFail(any(), any());
    }

    @Test
    void processingRemoteFailedUnfreezes() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.PROCESSING, "AT-2");
        when(orderMapper.selectById(1L)).thenReturn(order);
        CpsAitokenExchangeOrderRespDTO failed = remote("failed");
        failed.setFailureReason("rejected");
        when(aitokenExchangeClient.getOrder("AT-2", 1L)).thenReturn(failed);

        service.compensate(1L);

        verify(stepExecutor).unfreezeAndFail(1L, "rejected");
        verify(stepExecutor, never()).confirmLocalDeduct(any());
    }

    @Test
    void queryTimeoutKeepsFundsFrozenAndSchedulesRetry() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.PROCESSING, "AT-3");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(aitokenExchangeClient.getOrder("AT-3", 1L)).thenThrow(new IllegalStateException("timeout"));

        service.compensate(1L);

        verify(stepExecutor).scheduleRetry(1L, "timeout");
        verify(stepExecutor, never()).unfreezeAndFail(any(), any());
    }

    @Test
    void processingWithoutRemoteOrderIdResubmitsWithOriginalIdempotencyKey() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.PROCESSING, null);
        order.setSourceSystem("AgenticCPS");
        order.setSourceAsset("REBATE");
        order.setTargetAsset("TOKEN");
        order.setSourceAmount(new java.math.BigDecimal("10.00"));
        order.setTargetTokens(1000L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        CpsAitokenExchangeOrderRespDTO processing = remote("processing");
        processing.setExchangeOrderId("AT-new");
        when(aitokenExchangeClient.submit(any(), eq(1L))).thenReturn(processing);

        service.compensate(1L);

        verify(aitokenExchangeClient).submit(org.mockito.ArgumentMatchers.argThat(request ->
                "idem-1".equals(request.getIdempotencyKey())
                        && "CPSX1".equals(request.getSourceOrderId())), eq(1L));
        verify(stepExecutor).markProcessing(1L, "aitoken status pending: processing", "AT-new");
        verify(stepExecutor, never()).unfreezeAndFail(any(), any());
    }

    @Test
    void rollbackRequiredWaitsUntilRemoteExplicitlyRolledBack() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED, "AT-4");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(aitokenExchangeClient.rollback(eq("AT-4"), any(), eq(1L))).thenReturn(remote("processing"));

        service.compensate(1L);

        verify(stepExecutor).scheduleRetry(eq(1L), any());
        verify(stepExecutor, never()).unfreezeAndFail(any(), any());
    }

    @Test
    void rollbackRequiredUnfreezesOnlyAfterRemoteRolledBack() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED, "AT-5");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(aitokenExchangeClient.rollback(eq("AT-5"), any(), eq(1L))).thenReturn(remote("rolled_back"));

        service.compensate(1L);

        verify(stepExecutor).unfreezeAndFail(1L, "aitoken rolled back");
    }

    @Test
    void compensationSkipsRemoteCallsWhenAnotherWorkerWonClaim() {
        CpsRebateTokenExchangeOrderDO order = order(CpsRebateExchangeStatusEnum.PROCESSING, "AT-6");
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(stepExecutor.claimCompensation(order)).thenReturn(null);

        service.compensate(1L);

        verifyNoInteractions(aitokenExchangeClient);
        verify(stepExecutor, never()).scheduleRetry(any(), any());
    }

    private CpsRebateTokenExchangeOrderDO order(CpsRebateExchangeStatusEnum status, String remoteId) {
        return CpsRebateTokenExchangeOrderDO.builder().id(1L).exchangeOrderNo("CPSX1").memberId(100L)
                .freezeRecordId(9L).aitokenExchangeOrderId(remoteId).idempotencyKey("idem-1")
                .status(status.getStatus()).build();
    }

    private CpsAitokenExchangeOrderRespDTO remote(String status) {
        CpsAitokenExchangeOrderRespDTO response = new CpsAitokenExchangeOrderRespDTO();
        response.setExchangeOrderId("AT-X");
        response.setStatus(status);
        return response;
    }
}
