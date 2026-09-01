package com.qiji.cps.module.cps.job;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncBatchService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/** Consumes queued order-sync compensation windows. */
@Component("cpsOrderSyncBatchExecutionJob")
public class CpsOrderSyncBatchExecutionJob implements JobHandler {
    @Resource private CpsOrderSyncBatchService batchService;

    @Override @TenantJob
    public String execute(String param) {
        int processed = batchService.executeDueWindows(parseLimit(param));
        return "订单同步补偿窗口执行完成，处理 " + processed + " 个";
    }

    private int parseLimit(String param) {
        if (StrUtil.isBlank(param) || !param.contains("limit")) return 10;
        try {
            String value = param.replaceAll(".*\\\"limit\\\"\\s*:\\s*(\\d+).*", "$1");
            return value.equals(param) ? 10 : Math.max(1, Math.min(Integer.parseInt(value), 100));
        } catch (Exception ignored) {
            return 10;
        }
    }
}
