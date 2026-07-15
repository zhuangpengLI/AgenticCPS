package com.qiji.cps.module.cps.service.exchange;

import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsRebateTokenExchangeOrderMapper;
import com.qiji.cps.module.cps.enums.CpsRebateExchangeStatusEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateTokenExchangeStepExecutorTest {

    @InjectMocks
    private CpsRebateTokenExchangeStepExecutor executor;
    @Mock
    private CpsRebateTokenExchangeOrderMapper orderMapper;
    @Mock
    private CpsRebateAssetService rebateAssetService;
    @Mock
    private CpsMoneyConverter moneyConverter;

    @Test
    void staleVersionCannotOverwriteNewerExchangeState() {
        CpsRebateTokenExchangeOrderDO credited = order(CpsRebateExchangeStatusEnum.CREDITED, 5);
        when(orderMapper.selectById(1L)).thenReturn(credited);
        when(orderMapper.updateByIdAndStatusVersion(any(), eq(5),
                eq(List.of(CpsRebateExchangeStatusEnum.CREDITED.getStatus())))).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> executor.markSuccess(1L));
    }

    @Test
    void terminalSuccessCannotBeDowngradedToProcessing() {
        CpsRebateTokenExchangeOrderDO success = order(CpsRebateExchangeStatusEnum.SUCCESS, 6);
        when(orderMapper.selectById(1L)).thenReturn(success);

        assertThrows(IllegalStateException.class,
                () -> executor.markProcessing(1L, "late timeout", "AT-1"));

        verify(orderMapper, never()).updateByIdAndStatusVersion(any(), any(), any());
    }

    @Test
    void compensationClaimSkipsWhenAnotherWorkerWonCas() {
        CpsRebateTokenExchangeOrderDO processing = order(CpsRebateExchangeStatusEnum.PROCESSING, 3);
        when(orderMapper.updateByIdAndStatusVersion(any(), eq(3),
                eq(List.of(CpsRebateExchangeStatusEnum.PROCESSING.getStatus())))).thenReturn(0);

        assertNull(executor.claimCompensation(processing));
    }

    private CpsRebateTokenExchangeOrderDO order(CpsRebateExchangeStatusEnum status, int version) {
        return CpsRebateTokenExchangeOrderDO.builder()
                .id(1L).status(status.getStatus()).statusVersion(version).retryCount(0).build();
    }
}
