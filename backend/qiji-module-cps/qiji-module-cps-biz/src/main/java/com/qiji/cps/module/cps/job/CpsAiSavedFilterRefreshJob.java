package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeOperationRespVO;
import com.qiji.cps.module.cps.service.selection.CpsSelectionThemeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 按租户刷新结构化 AI 保存条件；prompt-only 条件会被安全跳过。 */
@Slf4j
@Component("cpsAiSavedFilterRefreshJob")
public class CpsAiSavedFilterRefreshJob implements JobHandler {

    @Resource
    private CpsSelectionThemeService selectionThemeService;

    @Override
    @TenantJob
    public String execute(String param) {
        CpsSelectionThemeOperationRespVO result = selectionThemeService.refreshAiSavedFilters();
        log.info("[CpsAiSavedFilterRefreshJob] {}", result.getMessage());
        return result.getMessage();
    }
}
