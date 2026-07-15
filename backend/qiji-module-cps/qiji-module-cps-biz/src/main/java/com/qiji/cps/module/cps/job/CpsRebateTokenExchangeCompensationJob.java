package com.qiji.cps.module.cps.job;

import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeCompensationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("cpsRebateTokenExchangeCompensationJob")
public class CpsRebateTokenExchangeCompensationJob implements JobHandler {

    @Resource
    private CpsRebateTokenExchangeCompensationService compensationService;

    @Override
    @TenantJob
    public String execute(String param) {
        int success = 0;
        int failed = 0;
        for (Long orderId : compensationService.getDueOrderIds(200)) {
            try {
                compensationService.compensate(orderId);
                success++;
            } catch (Exception ex) {
                log.warn("[execute][exchange compensation failed, orderId={}]", orderId, ex);
                failed++;
            }
        }
        return "Token exchange compensation completed: success=" + success + ", failed=" + failed;
    }
}
