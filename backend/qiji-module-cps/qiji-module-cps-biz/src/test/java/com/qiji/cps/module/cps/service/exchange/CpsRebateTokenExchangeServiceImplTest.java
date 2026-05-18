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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("freeze - 可用余额充足时冻结到账户 frozen_balance")
    void freeze_success() {
        when(freezeRecordMapper.selectByBusinessAndIdempotencyKey("TOKEN_EXCHANGE", "idem-1")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(rebateAccountMapper.freezeBalance(100L, new BigDecimal("10.00"))).thenReturn(1);
        doAnswer(invocation -> {
            CpsFreezeRecordDO record = invocation.getArgument(0);
            record.setId(10L);
            return 1;
        }).when(freezeRecordMapper).insert(any(CpsFreezeRecordDO.class));

        OpenApiCpsRebateFreezeReqVO request = new OpenApiCpsRebateFreezeReqVO();
        request.setUserId(100L);
        request.setAmount(new BigDecimal("10.00"));
        request.setBusinessType("TOKEN_EXCHANGE");
        request.setBusinessId("CPSX001");
        request.setIdempotencyKey("idem-1");

        var response = service.freeze(request);

        assertEquals("10", response.getFreezeId());
        assertEquals(CpsFreezeStatusEnum.FROZEN.getStatus(), response.getStatus());
        verify(rebateAccountMapper).freezeBalance(100L, new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("submit - 成功链路冻结返利、发放Token、确认扣减")
    void submit_success() {
        TenantContextHolder.setTenantId(1L);
        when(properties.getSourceSystem()).thenReturn("AgenticCPS");
        when(properties.getSourceAsset()).thenReturn("REBATE");
        when(properties.getTargetAsset()).thenReturn("TOKEN");
        when(exchangeOrderMapper.selectByIdempotencyKey("idem-1")).thenReturn(null);
        when(rebateSettleService.getOrInitAccount(100L)).thenReturn(account(new BigDecimal("20.00")));
        when(aitokenExchangeClient.preview(any(), eq(1L))).thenReturn(preview());
        doAnswer(invocation -> {
            CpsRebateTokenExchangeOrderDO order = invocation.getArgument(0);
            order.setId(11L);
            return 1;
        }).when(exchangeOrderMapper).insert(any(CpsRebateTokenExchangeOrderDO.class));
        when(freezeRecordMapper.selectByBusinessAndIdempotencyKey(eq("TOKEN_EXCHANGE"), eq("idem-1"))).thenReturn(null);
        when(rebateAccountMapper.freezeBalance(100L, new BigDecimal("10.00"))).thenReturn(1);
        doAnswer(invocation -> {
            CpsFreezeRecordDO record = invocation.getArgument(0);
            record.setId(12L);
            return 1;
        }).when(freezeRecordMapper).insert(any(CpsFreezeRecordDO.class));
        CpsAitokenExchangeOrderRespDTO aitokenOrder = new CpsAitokenExchangeOrderRespDTO();
        aitokenOrder.setStatus("approved");
        aitokenOrder.setExchangeOrderId("EX001");
        when(aitokenExchangeClient.submit(any(), eq(1L))).thenReturn(aitokenOrder);
        when(freezeRecordMapper.selectById(12L)).thenReturn(CpsFreezeRecordDO.builder()
                .id(12L).memberId(100L).freezeAmount(new BigDecimal("10.00"))
                .status(CpsFreezeStatusEnum.FROZEN.getStatus()).build());
        when(rebateAccountMapper.deductFrozenBalance(100L, new BigDecimal("10.00"))).thenReturn(1);
        when(exchangeOrderMapper.selectById(11L)).thenReturn(CpsRebateTokenExchangeOrderDO.builder()
                .id(11L).status(CpsRebateExchangeStatusEnum.SUCCESS.getStatus()).build());

        CpsRebateTokenExchangeOrderDO result = service.submit(100L, new BigDecimal("10.00"), "idem-1");

        assertEquals(CpsRebateExchangeStatusEnum.SUCCESS.getStatus(), result.getStatus());
        verify(rebateAccountMapper).freezeBalance(100L, new BigDecimal("10.00"));
        verify(rebateAccountMapper).deductFrozenBalance(100L, new BigDecimal("10.00"));
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
