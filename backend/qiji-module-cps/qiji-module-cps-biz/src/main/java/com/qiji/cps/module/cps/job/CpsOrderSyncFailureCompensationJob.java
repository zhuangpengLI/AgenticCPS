package com.qiji.cps.module.cps.job;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecoveryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("cpsOrderSyncFailureCompensationJob")
public class CpsOrderSyncFailureCompensationJob implements JobHandler {

    @Resource
    private CpsOrderSyncFailureRecoveryService failureRecoveryService;

    @Override
    @TenantJob
    public String execute(String param) {
        int limit = parseLimit(param);
        int count = failureRecoveryService.compensateDueFailures(limit);
        String result = "订单同步失败恢复队列扫描完成，处理" + count + "条";
        log.info("[CpsOrderSyncFailureCompensationJob] {}", result);
        return result;
    }

    private int parseLimit(String param) {
        if (StrUtil.isBlank(param) || !param.contains("limit")) {
            return 50;
        }
        try {
            String limitText = param.replaceAll(".*\"limit\"\\s*:\\s*(\\d+).*", "$1");
            return limitText.equals(param) ? 50 : Math.max(1, Math.min(Integer.parseInt(limitText), 200));
        } catch (Exception ignored) {
            return 50;
        }
    }
}
