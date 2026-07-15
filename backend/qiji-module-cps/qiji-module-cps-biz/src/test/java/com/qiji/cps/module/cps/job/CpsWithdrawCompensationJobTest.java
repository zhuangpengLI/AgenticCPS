package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.service.withdraw.CpsWithdrawCompensationService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsWithdrawCompensationJobTest {

    @Test
    void tenantJobRetriesEachDueWithdrawalIndependently() throws Exception {
        CpsWithdrawCompensationService service = mock(CpsWithdrawCompensationService.class);
        when(service.getDueWithdrawIds(200)).thenReturn(List.of(1L, 2L));
        CpsWithdrawCompensationJob job = new CpsWithdrawCompensationJob(service);

        job.execute("");

        Method execute = CpsWithdrawCompensationJob.class.getMethod("execute", String.class);
        assertNotNull(execute.getAnnotation(TenantJob.class));
        verify(service).compensate(1L);
        verify(service).compensate(2L);
    }
}
