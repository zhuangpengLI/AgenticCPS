package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateAssetQueryServiceImplTest {

    @InjectMocks
    private CpsRebateAssetQueryServiceImpl service;

    @Mock
    private CpsRebateDebtMapper debtMapper;
    @Mock
    private CpsRebateAssetLedgerMapper ledgerMapper;

    @Test
    void getDebtSummary_aggregatesAllDebtDimensionsNullSafely() {
        when(debtMapper.selectListByMemberId(1001L)).thenReturn(List.of(
                CpsRebateDebtDO.builder()
                        .memberId(1001L)
                        .originalDebtCent(3000L)
                        .repaidDebtCent(500L)
                        .waivedDebtCent(null)
                        .outstandingDebtCent(2500L)
                        .build(),
                CpsRebateDebtDO.builder()
                        .memberId(1001L)
                        .originalDebtCent(2000L)
                        .repaidDebtCent(700L)
                        .waivedDebtCent(300L)
                        .outstandingDebtCent(1000L)
                        .build()));

        var summary = service.getDebtSummary(1001L);

        assertEquals(1001L, summary.getMemberId());
        assertEquals(2L, summary.getDebtCount());
        assertEquals(5000L, summary.getOriginalDebtCent());
        assertEquals(1200L, summary.getRepaidDebtCent());
        assertEquals(300L, summary.getWaivedDebtCent());
        assertEquals(3500L, summary.getOutstandingDebtCent());
        verify(debtMapper).selectListByMemberId(1001L);
    }
}
