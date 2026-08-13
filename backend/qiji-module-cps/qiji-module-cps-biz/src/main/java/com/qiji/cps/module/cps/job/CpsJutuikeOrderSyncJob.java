package com.qiji.cps.module.cps.job;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.jutuike.JutuikeUnionVendorClient;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncPageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Pulls Jutuike union orders in overlapping one-hour windows. */
@Slf4j
@Component("cpsJutuikeOrderSyncJob")
public class CpsJutuikeOrderSyncJob implements JobHandler {

    private static final String VENDOR_JUTUIKE = "jutuike";
    private static final String PLATFORM_UNION = "union";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 100;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;
    @Resource
    private JutuikeUnionVendorClient jutuikeUnionVendorClient;
    @Resource
    private CpsOrderSyncPageService pageService;
    @Resource
    private CpsOrderSyncLogMapper syncLogMapper;

    @Override
    @TenantJob
    public String execute(String param) {
        int hours = parsePositiveInt(param, "hours", 2);
        int queryType = parsePositiveInt(param, "queryType", 4);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);
        CpsOrderSyncLogDO syncLog = newSyncLog(startTime, endTime, queryType);
        long startedAt = System.currentTimeMillis();
        int total = 0;
        int newCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        try {
            CpsVendorConfig config = platformClientFactory.getVendorConfig(VENDOR_JUTUIKE, PLATFORM_UNION);
            if (config == null) {
                throw new IllegalStateException("Jutuike union vendor is not configured");
            }
            LocalDateTime windowStart = startTime;
            while (windowStart.isBefore(endTime)) {
                LocalDateTime windowEnd = windowStart.plusHours(1).isBefore(endTime)
                        ? windowStart.plusHours(1) : endTime;
                int[] stats = syncWindow(config, queryType, windowStart.minusMinutes(5), windowEnd);
                total += stats[0] + stats[1] + stats[2];
                newCount += stats[0];
                updateCount += stats[1];
                skipCount += stats[2];
                windowStart = windowEnd;
            }
            syncLog.setSyncStatus(1);
            return String.format("Jutuike order sync completed: total=%d, new=%d, updated=%d, skipped=%d",
                    total, newCount, updateCount, skipCount);
        } catch (Exception exception) {
            log.error("[CpsJutuikeOrderSyncJob] order sync failed", exception);
            syncLog.setSyncStatus(2);
            syncLog.setErrorMsg(StrUtil.subWithLength(exception.getMessage(), 0, 500));
            throw new IllegalStateException("Jutuike order sync failed", exception);
        } finally {
            syncLog.setTotalCount(total);
            syncLog.setNewCount(newCount);
            syncLog.setUpdateCount(updateCount);
            syncLog.setSkipCount(skipCount);
            syncLog.setSyncEndTime(LocalDateTime.now());
            syncLog.setCostMs(System.currentTimeMillis() - startedAt);
            syncLogMapper.insert(syncLog);
        }
    }

    private int[] syncWindow(CpsVendorConfig config, int queryType,
                             LocalDateTime startTime, LocalDateTime endTime) {
        int[] totals = new int[3];
        int pageNo = 1;
        for (int pageCount = 0; pageCount < MAX_PAGES; pageCount++) {
            CpsOrderQueryRequest request = new CpsOrderQueryRequest();
            request.setStartTime(startTime.format(DTF));
            request.setEndTime(endTime.format(DTF));
            request.setQueryType(queryType);
            request.setPageNo(pageNo);
            request.setPageSize(PAGE_SIZE);
            CpsOrderPageResult page = jutuikeUnionVendorClient.queryOrderPage(request, config);
            List<CpsOrderDTO> orders = page.getItems();
            int currentPageNo = pageNo;
            orders.forEach(order -> {
                order.setVendorCode(VENDOR_JUTUIKE);
                order.setSyncBatchNo("order-sync:jutuike:" + endTime.format(DTF) + ":page-" + currentPageNo);
            });
            int[] stats = orders.isEmpty() ? new int[]{0, 0, 0} : pageService.persistPage(orders);
            totals[0] += stats[0];
            totals[1] += stats[1];
            totals[2] += stats[2];
            if (!page.isHasMore()) {
                return totals;
            }
            pageNo = page.getNextPageNo();
        }
        throw new IllegalStateException("Jutuike order pagination exceeded " + MAX_PAGES + " pages");
    }

    private CpsOrderSyncLogDO newSyncLog(LocalDateTime startTime, LocalDateTime endTime, int queryType) {
        return CpsOrderSyncLogDO.builder()
                .platformCode(PLATFORM_UNION)
                .syncType(1)
                .queryType(queryType)
                .queryStartTime(startTime)
                .queryEndTime(endTime)
                .syncStartTime(LocalDateTime.now())
                .totalCount(0)
                .newCount(0)
                .updateCount(0)
                .skipCount(0)
                .build();
    }

    private int parsePositiveInt(String param, String field, int fallback) {
        if (!StrUtil.isNotBlank(param) || !param.contains(field)) {
            return fallback;
        }
        String value = param.replaceAll(".*\\\"" + field + "\\\"\\s*:\\s*(\\d+).*", "$1");
        if (value.equals(param)) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
