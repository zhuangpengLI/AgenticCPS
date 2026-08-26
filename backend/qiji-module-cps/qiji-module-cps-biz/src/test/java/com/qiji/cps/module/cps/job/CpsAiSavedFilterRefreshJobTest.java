package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeOperationRespVO;
import com.qiji.cps.module.cps.service.selection.CpsSelectionThemeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsAiSavedFilterRefreshJobTest {

    @InjectMocks
    private CpsAiSavedFilterRefreshJob job;

    @Mock
    private CpsSelectionThemeService selectionThemeService;

    @Test
    void executeRunsPerTenantAndReturnsSummary() throws Exception {
        assertNotNull(CpsAiSavedFilterRefreshJob.class.getMethod("execute", String.class)
                .getAnnotation(TenantJob.class));
        when(selectionThemeService.refreshAiSavedFilters()).thenReturn(CpsSelectionThemeOperationRespVO.builder()
                .status("SUCCESS").message("刷新完成：成功1，跳过1，失败0，写入商品3").build());

        assertEquals("刷新完成：成功1，跳过1，失败0，写入商品3", job.execute(null));
    }
}
