package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.service.withdraw.CpsWithdrawCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("cpsWithdrawCompensationJob")
@RequiredArgsConstructor
public class CpsWithdrawCompensationJob implements JobHandler {
    private final CpsWithdrawCompensationService compensationService;

    @Override
    @TenantJob
    public String execute(String param) {
        int success = 0;
        int failed = 0;
        for (Long id : compensationService.getDueWithdrawIds(200)) {
            try {
                compensationService.compensate(id);
                success++;
            } catch (Exception ex) {
                log.warn("[execute][withdraw compensation failed, id={}]", id, ex);
                failed++;
            }
        }
        return "withdraw compensation completed: success=" + success + ", failed=" + failed;
    }
}
