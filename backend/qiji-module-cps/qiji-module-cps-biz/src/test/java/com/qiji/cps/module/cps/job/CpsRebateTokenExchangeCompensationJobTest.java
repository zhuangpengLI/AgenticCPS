package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeCompensationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateTokenExchangeCompensationJobTest {

    @InjectMocks
    private CpsRebateTokenExchangeCompensationJob job;
    @Mock
    private CpsRebateTokenExchangeCompensationService compensationService;

    @Test
    void jobIsTenantAwareAndContinuesAfterSingleOrderFailure() throws Exception {
        assertNotNull(CpsRebateTokenExchangeCompensationJob.class.getMethod("execute", String.class)
                .getAnnotation(TenantJob.class));
        when(compensationService.getDueOrderIds(200)).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("one failed")).when(compensationService).compensate(1L);

        String result = job.execute(null);

        verify(compensationService).compensate(1L);
        verify(compensationService).compensate(2L);
        assertTrue(result.contains("success=1"));
        assertTrue(result.contains("failed=1"));
    }
}
