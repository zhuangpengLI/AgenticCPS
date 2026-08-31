package com.qiji.cps.module.cps.job;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncBatchService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/** Creates durable compensation batches; workers execute the persisted windows. */
@Component("cpsOrderSyncCompensationJob")
public class CpsOrderSyncCompensationJob implements JobHandler {
    @Resource private CpsPlatformService platformService;
    @Resource private CpsOrderSyncBatchService batchService;

    @Override @TenantJob
    public String execute(String param) {
        String type = value(param, "batchType", "ROLLING");
        int queryType = intValue(param, "queryType", "4");
        int days = intValue(param, "days", type.equalsIgnoreCase("NIGHTLY") ? "10" : "30");
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(Math.max(1, Math.min(days, 90)));
        String target = value(param, "platformCode", null);
        List<CpsPlatformDO> platforms = StrUtil.isBlank(target)
                ? platformService.getEnabledPlatformList() : List.of(platformService.getPlatformByCode(target));
        int created = 0;
        for (CpsPlatformDO platform : platforms) {
            if (platform == null) continue;
            batchService.create(platform.getPlatformCode(), platform.getActiveVendorCode(), type, queryType, start, end);
            created++;
        }
        return "创建订单同步补偿批次 " + created + " 个，范围 " + start + " ~ " + end;
    }

    private String value(String json, String key, String fallback) {
        if (StrUtil.isBlank(json)) return fallback;
        String marker = "\"" + key + "\"";
        int p = json.indexOf(marker); if (p < 0) return fallback;
        int colon = json.indexOf(':', p); if (colon < 0) return fallback;
        String tail = json.substring(colon + 1).trim();
        if (tail.startsWith("\"")) { int end = tail.indexOf('"', 1); return end > 0 ? tail.substring(1, end) : fallback; }
        int comma = tail.indexOf(','); return (comma < 0 ? tail : tail.substring(0, comma)).replace("}", "").trim();
    }
    private int intValue(String json, String key, String fallback) {
        try { return Integer.parseInt(value(json, key, fallback)); } catch (Exception e) { return Integer.parseInt(fallback); }
    }
}
