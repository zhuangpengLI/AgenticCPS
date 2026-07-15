package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateFreezeReqVO;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsRebateTokenExchangeOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangeOrderRespDTO;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewRespDTO;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsRebateTokenExchangeServiceImplTest {

    @InjectMocks
    private CpsRebateTokenExchangeServiceImpl service;

    @Mock
    private CpsRebateSettleService rebateSettleService;
    @Mock
    private CpsRebateAccountMapper rebateAccountMapper;
    @Mock
    private CpsFreezeRecordMapper freezeRecordMapper;
    @Mock
    private CpsRebateTokenExchangeOrderMapper exchangeOrderMapper;
    @Mock
    private CpsAitokenExchangeClient aitokenExchangeClient;
    @Mock
    private CpsAitokenExchangeProperties properties;
    @Mock
    private CpsRebateAssetService rebateAssetService;
    @Mock
    private CpsMoneyConverter moneyConverter;
    @Mock
    private CpsRebateTokenExchangeStepExecutor stepExecutor;

    @Test
    void submitDoesNotHoldDatabaseTransactionAcrossRemoteCalls() throws Exception {
        Method submit = CpsRebateTokenExchangeServiceImpl.class.getMethod(
                "submit", Long.class, BigDecimal.class, String.class);

        assertNull(submit.getAnnotation(Transactional.class));
    }

    @Test
    void localPersistenceStepsAlwaysUseIndependentTransactions() throws Exception {
        assertRequiresNew("createOrder", CpsRebateTokenExchangeOrderDO.class);
        assertRequiresNew("freezeOrder", Long.class);
        assertRequiresNew("markCredited", Long.class, String.class);
        assertRequiresNew("confirmLocalDeduct", Long.class);
        assertRequiresNew("unfreezeAndFail", Long.class, String.class);
        assertRequiresNew("scheduleRetry", Long.class, String.class);
        assertRequiresNew("claimCompensation", CpsRebateTokenExchangeOrderDO.class);
    }

    private void assertRequiresNew(String methodName, Class<?>... parameterTypes) throws Exception {
        Transactional transactional = CpsRebateTokenExchangeStepExecutor.class
                .getMethod(methodName, parameterTypes).getAnnotation(Transactional.class);
        org.junit.jupiter.api.Assertions.assertNotNull(transactional, methodName);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation(), methodName);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("freeze - 可用余额充足时冻结到账户 frozen_balance")
    void freeze_success() {
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(moneyConverter.yuanToCent(new BigDecimal("10.00"))).thenReturn(1000L);
        when(rebateAssetService.freezeAvailableForExchange(eq(100L), eq(1000L), eq("CPSX001"),
                eq("idem-1"), any())).thenReturn(CpsFreezeRecordDO.builder().id(10L).memberId(100L)
                .businessType("TOKEN_EXCHANGE").businessId("CPSX001").idempotencyKey("idem-1")
                .freezeAmount(new BigDecimal("10.00")).status(CpsFreezeStatusEnum.FROZEN.getStatus()).build());

        OpenApiCpsRebateFreezeReqVO request = new OpenApiCpsRebateFreezeReqVO();
        request.setUserId(100L);
        request.setAmount(new BigDecimal("10.00"));
        request.setBusinessType("TOKEN_EXCHANGE");
        request.setBusinessId("CPSX001");
        request.setIdempotencyKey("idem-1");

        var response = service.freeze(request);

        assertEquals("10", response.getFreezeId());
        assertEquals(CpsFreezeStatusEnum.FROZEN.getStatus(), response.getStatus());
        verify(rebateAssetService).freezeAvailableForExchange(eq(100L), eq(1000L), eq("CPSX001"),
                eq("idem-1"), any());
    }

    @Test
    @DisplayName("submit - aitoken credited 后确认扣减并回调 aitoken 完成闭环")
    void submit_confirmsSourceDeductAfterAitokenCredited() {
        TenantContextHolder.setTenantId(1L);
        when(properties.getSourceSystem()).thenReturn("AgenticCPS");
        when(properties.getSourceAsset()).thenReturn("REBATE");
        when(properties.getTargetAsset()).thenReturn("TOKEN");
        when(exchangeOrderMapper.selectByIdempotencyKey("idem-1")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        when(stepExecutor.createOrder(any())).thenAnswer(invocation -> {
            CpsRebateTokenExchangeOrderDO order = invocation.getArgument(0);
            order.setId(11L);
            return order;
        });
        when(stepExecutor.freezeOrder(11L)).thenAnswer(invocation -> CpsRebateTokenExchangeOrderDO.builder()
                .id(11L).memberId(100L).sourceAmount(new BigDecimal("10.00")).targetTokens(1_000_000L)
                .freezeRecordId(12L).exchangeOrderNo("CPSX1").idempotencyKey("idem-1").build());
        CpsAitokenExchangeOrderRespDTO aitokenOrder = new CpsAitokenExchangeOrderRespDTO();
        aitokenOrder.setStatus("credited");
        aitokenOrder.setExchangeOrderId("EX001");
        when(aitokenExchangeClient.submit(any(), eq(1L))).thenReturn(aitokenOrder);
        when(aitokenExchangeClient.confirmSourceDeduct(eq("EX001"), any(), eq(1L))).thenReturn(aitokenOrder);
        when(exchangeOrderMapper.selectById(11L)).thenReturn(CpsRebateTokenExchangeOrderDO.builder()
                .id(11L).status(CpsRebateExchangeStatusEnum.SUCCESS.getStatus()).build());

        CpsRebateTokenExchangeOrderDO result = service.submit(100L, new BigDecimal("10.00"), "idem-1");

        assertEquals(CpsRebateExchangeStatusEnum.SUCCESS.getStatus(), result.getStatus());
        verify(stepExecutor).confirmLocalDeduct(11L);
        verify(aitokenExchangeClient).confirmSourceDeduct(eq("EX001"), any(), eq(1L));
        verify(aitokenExchangeClient, never()).rollback(any(), any(), any());
    }

    @Test
    @DisplayName("submit - aitoken credited 但 CPS 扣减失败时请求 aitoken rollback 并标记待补偿")
    void submit_rollsBackAitokenWhenCpsDeductFails() {
        TenantContextHolder.setTenantId(1L);
        when(properties.getSourceSystem()).thenReturn("AgenticCPS");
        when(properties.getSourceAsset()).thenReturn("REBATE");
        when(properties.getTargetAsset()).thenReturn("TOKEN");
        when(exchangeOrderMapper.selectByIdempotencyKey("idem-rollback")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        when(stepExecutor.createOrder(any())).thenAnswer(invocation -> {
            CpsRebateTokenExchangeOrderDO order = invocation.getArgument(0);
            order.setId(21L);
            return order;
        });
        when(stepExecutor.freezeOrder(21L)).thenAnswer(invocation -> CpsRebateTokenExchangeOrderDO.builder()
                .id(21L).memberId(100L).sourceAmount(new BigDecimal("10.00")).targetTokens(1_000_000L)
                .freezeRecordId(22L).exchangeOrderNo("CPSX2").idempotencyKey("idem-rollback").build());
        CpsAitokenExchangeOrderRespDTO aitokenOrder = new CpsAitokenExchangeOrderRespDTO();
        aitokenOrder.setStatus("credited");
        aitokenOrder.setExchangeOrderId("EX002");
        when(aitokenExchangeClient.submit(any(), eq(1L))).thenReturn(aitokenOrder);
        doThrow(new IllegalStateException("deduct failed")).when(stepExecutor).confirmLocalDeduct(21L);
        CpsAitokenExchangeOrderRespDTO rollback = new CpsAitokenExchangeOrderRespDTO();
        rollback.setStatus("processing");
        when(aitokenExchangeClient.rollback(eq("EX002"), any(), eq(1L))).thenReturn(rollback);
        when(exchangeOrderMapper.selectById(21L)).thenReturn(CpsRebateTokenExchangeOrderDO.builder()
                .id(21L).status(CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED.getStatus()).build());

        CpsRebateTokenExchangeOrderDO result = service.submit(100L, new BigDecimal("10.00"), "idem-rollback");

        assertEquals(CpsRebateExchangeStatusEnum.ROLLBACK_REQUIRED.getStatus(), result.getStatus());
        verify(aitokenExchangeClient).rollback(eq("EX002"), any(), eq(1L));
        verify(aitokenExchangeClient, never()).confirmSourceDeduct(any(), any(), any());
    }

    @Test
    void submitKeepsCreditedWhenRemoteConfirmTimesOutAfterLocalDeduct() {
        TenantContextHolder.setTenantId(1L);
        when(properties.getSourceSystem()).thenReturn("AgenticCPS");
        when(properties.getSourceAsset()).thenReturn("REBATE");
        when(properties.getTargetAsset()).thenReturn("TOKEN");
        when(exchangeOrderMapper.selectByIdempotencyKey("idem-confirm-timeout")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        when(stepExecutor.createOrder(any())).thenAnswer(invocation -> {
            CpsRebateTokenExchangeOrderDO order = invocation.getArgument(0);
            order.setId(41L);
            return order;
        });
        when(stepExecutor.freezeOrder(41L)).thenReturn(CpsRebateTokenExchangeOrderDO.builder()
                .id(41L).memberId(100L).sourceAmount(new BigDecimal("10.00")).targetTokens(1_000_000L)
                .freezeRecordId(42L).exchangeOrderNo("CPSX4").idempotencyKey("idem-confirm-timeout").build());
        CpsAitokenExchangeOrderRespDTO credited = new CpsAitokenExchangeOrderRespDTO();
        credited.setStatus("credited");
        credited.setExchangeOrderId("EX004");
        when(aitokenExchangeClient.submit(any(), eq(1L))).thenReturn(credited);
        when(aitokenExchangeClient.confirmSourceDeduct(eq("EX004"), any(), eq(1L)))
                .thenThrow(new IllegalStateException("confirm timeout"));
        when(exchangeOrderMapper.selectById(41L)).thenReturn(CpsRebateTokenExchangeOrderDO.builder()
                .id(41L).status(CpsRebateExchangeStatusEnum.CREDITED.getStatus()).build());

        CpsRebateTokenExchangeOrderDO result = service.submit(
                100L, new BigDecimal("10.00"), "idem-confirm-timeout");

        assertEquals(CpsRebateExchangeStatusEnum.CREDITED.getStatus(), result.getStatus());
        verify(stepExecutor).confirmLocalDeduct(41L);
        verify(stepExecutor).scheduleRetry(41L, "confirm timeout");
        verify(aitokenExchangeClient, never()).rollback(any(), any(), any());
        verify(stepExecutor, never()).unfreezeAndFail(any(), any());
    }

    @Test
    void submit_replayRejectsDifferentMember() {
        when(exchangeOrderMapper.selectByIdempotencyKey("same-key")).thenReturn(
                CpsRebateTokenExchangeOrderDO.builder().id(31L).memberId(200L)
                        .sourceAmount(new BigDecimal("10.00")).idempotencyKey("same-key").build());

        assertThrows(IllegalStateException.class,
                () -> service.submit(100L, new BigDecimal("10.00"), "same-key"));

        verifyNoInteractions(aitokenExchangeClient, rebateAssetService);
    }

    @Test
    void submit_replayRejectsDifferentAmount() {
        when(exchangeOrderMapper.selectByIdempotencyKey("same-key")).thenReturn(
                CpsRebateTokenExchangeOrderDO.builder().id(32L).memberId(100L)
                        .sourceAmount(new BigDecimal("20.00")).idempotencyKey("same-key").build());

        assertThrows(IllegalStateException.class,
                () -> service.submit(100L, new BigDecimal("10.00"), "same-key"));

        verifyNoInteractions(aitokenExchangeClient, rebateAssetService);
    }

    @Test
    void submit_concurrentDuplicateRejectsExistingOrderOwnedByDifferentMember() {
        TenantContextHolder.setTenantId(1L);
        when(exchangeOrderMapper.selectByIdempotencyKey("race-key")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        CpsRebateTokenExchangeOrderDO winner = CpsRebateTokenExchangeOrderDO.builder()
                .id(51L).memberId(200L).sourceAmount(new BigDecimal("10.00"))
                .exchangeOrderNo("CPSX-WINNER").idempotencyKey("race-key")
                .status(CpsRebateExchangeStatusEnum.INIT.getStatus()).build();
        when(stepExecutor.createOrder(any())).thenReturn(winner);

        assertThrows(IllegalStateException.class,
                () -> service.submit(100L, new BigDecimal("10.00"), "race-key"));

        verify(stepExecutor, never()).freezeOrder(anyLong());
        verify(aitokenExchangeClient, never()).submit(any(), any());
    }

    @Test
    void submit_concurrentDuplicateRejectsExistingOrderWithDifferentAmount() {
        TenantContextHolder.setTenantId(1L);
        when(exchangeOrderMapper.selectByIdempotencyKey("race-key")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        CpsRebateTokenExchangeOrderDO winner = CpsRebateTokenExchangeOrderDO.builder()
                .id(52L).memberId(100L).sourceAmount(new BigDecimal("20.00"))
                .exchangeOrderNo("CPSX-WINNER").idempotencyKey("race-key")
                .status(CpsRebateExchangeStatusEnum.INIT.getStatus()).build();
        when(stepExecutor.createOrder(any())).thenReturn(winner);

        assertThrows(IllegalStateException.class,
                () -> service.submit(100L, new BigDecimal("10.00"), "race-key"));

        verify(stepExecutor, never()).freezeOrder(anyLong());
        verify(aitokenExchangeClient, never()).submit(any(), any());
    }

    @Test
    void submit_concurrentDuplicateWithSameParametersReturnsWinningOrder() {
        TenantContextHolder.setTenantId(1L);
        when(exchangeOrderMapper.selectByIdempotencyKey("race-key")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        CpsRebateTokenExchangeOrderDO winner = CpsRebateTokenExchangeOrderDO.builder()
                .id(53L).memberId(100L).sourceAmount(new BigDecimal("10.00"))
                .exchangeOrderNo("CPSX-WINNER").idempotencyKey("race-key")
                .status(CpsRebateExchangeStatusEnum.FROZEN.getStatus()).freezeRecordId(54L).build();
        when(stepExecutor.createOrder(any())).thenReturn(winner);

        CpsRebateTokenExchangeOrderDO result = service.submit(
                100L, new BigDecimal("10.00"), "race-key");

        assertEquals(winner, result);
        verify(stepExecutor, never()).freezeOrder(anyLong());
        verify(aitokenExchangeClient, never()).submit(any(), any());
    }

    private CpsRebateAccountDO account(BigDecimal available) {
        return CpsRebateAccountDO.builder()
                .memberId(100L)
                .availableBalance(available)
                .frozenBalance(BigDecimal.ZERO)
                .totalRebate(available)
                .withdrawnAmount(BigDecimal.ZERO)
                .status(1)
                .build();
    }

    private CpsAitokenExchangePreviewRespDTO preview() {
        CpsAitokenExchangePreviewRespDTO response = new CpsAitokenExchangePreviewRespDTO();
        response.setExchangeRate(new BigDecimal("100000.0000"));
        response.setActualTokens(1_000_000L);
        return response;
    }
}
