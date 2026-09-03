package com.qiji.cps.module.cps.job;

import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateSettleJobTest {

    @Mock
    private CpsRebateSettleService settleService;

    private CpsRebateSettleJob job;

    @BeforeEach
    void setUp() {
        job = new CpsRebateSettleJob();
        ReflectionTestUtils.setField(job, "rebateSettleService", settleService);
    }

    @Test
    void usesDefaultBatchSizeAndReportsDirectCreditStats() throws Exception {
        when(settleService.batchSettle(200)).thenReturn(new int[]{2, 1, 0});

        assertEquals("返利结算完成: 成功处理=2（含冻结/直接入账），跳过=1，失败=0", job.execute(null));

        verify(settleService).batchSettle(200);
    }

    @Test
    void clampsConfiguredBatchSizeToSafeRange() throws Exception {
        when(settleService.batchSettle(1000)).thenReturn(new int[]{0, 0, 0});
        job.execute("{\"batchSize\":99999}");
        verify(settleService).batchSettle(1000);

        when(settleService.batchSettle(1)).thenReturn(new int[]{0, 0, 0});
        job.execute("{\"batchSize\":0}");
        verify(settleService).batchSettle(1);
    }
}
