package com.qiji.cps.module.cps.job;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.quartz.core.handler.JobHandler;
import com.qiji.cps.framework.tenant.core.job.TenantJob;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderPaginationMode;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncCheckpointMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecordCommand;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecoveryService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncPageService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncWindowPlanner;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CPS 订单同步定时任务
 *
 * <p>定时拉取各已启用平台的订单，并进行幂等保存/状态更新。</p>
 *
 * <h3>Quartz 注册方式</h3>
 * 在管理后台【基础设施 - 定时任务】手动添加：
 * 处理器名字：cpsOrderSyncJob
 * 处理器参数示例：{"hours":2,"queryType":4}（或留空使用默认值）
 * CRON 表达式：0 0/30 * * * ?（每 30 分钟执行一次）
 *
 * <h3>参数说明（JSON 格式）</h3>
 * hours：向前追溯的小时数，默认 2
 * queryType：查询时间维度（1=下单时间，2=付款时间，3=结算时间，4=更新时间），默认 4
 * platformCode：指定平台编码，留空则同步所有已启用平台
 *
 * @author CPS System
 */
@Slf4j
@Component("cpsOrderSyncJob")
public class CpsOrderSyncJob implements JobHandler {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private CpsPlatformService platformService;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsOrderSyncPageService pageService;

    @Resource
    private CpsOrderSyncLogMapper syncLogMapper;

    @Resource
    private CpsOrderSyncCheckpointMapper checkpointMapper;

    @Resource
    private CpsOrderSyncFailureRecoveryService failureRecoveryService;

    @Override
    @TenantJob
    public String execute(String param) throws Exception {
        // 解析参数
        int hours = 2;
        int queryType = 4;
        String targetPlatformCode = null;

        if (StrUtil.isNotBlank(param)) {
            try {
                if (param.contains("hours")) {
                    String hoursStr = param.replaceAll(".*\"hours\"\\s*:\\s*(\\d+).*", "$1");
                    if (!hoursStr.equals(param)) {
                        hours = Integer.parseInt(hoursStr);
                    }
                }
                if (param.contains("queryType")) {
                    String qtStr = param.replaceAll(".*\"queryType\"\\s*:\\s*(\\d+).*", "$1");
                    if (!qtStr.equals(param)) {
                        queryType = Integer.parseInt(qtStr);
                    }
                }
                if (param.contains("platformCode")) {
                    String pcStr = param.replaceAll(".*\"platformCode\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                    if (!pcStr.equals(param)) {
                        targetPlatformCode = pcStr;
                    }
                }
            } catch (Exception e) {
                log.warn("[CpsOrderSyncJob] 参数解析失败，使用默认值: param={}", param);
            }
        }

        // 确定同步平台范围
        List<CpsPlatformDO> platforms;
        if (StrUtil.isNotBlank(targetPlatformCode)) {
            CpsPlatformDO platform = platformService.getPlatformByCode(targetPlatformCode);
            platforms = (platform != null) ? List.of(platform) : List.of();
        } else {
            platforms = platformService.getEnabledPlatformList();
        }

        if (platforms.isEmpty()) {
            log.info("[CpsOrderSyncJob] 没有已启用的平台，跳过本次同步");
            return "没有已启用的平台";
        }

        // 时间窗口：大淘客普通日期最多 3 小时，任务参数 hours 可为 30 天，必须拆分后逐段请求。
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);
        // Existing checkpoints are keyed by platform/scene/queryType (not window),
        // therefore execute non-overlapping windows here. The planner supports
        // overlap for the forthcoming persisted batch/window orchestration.
        List<CpsOrderSyncWindowPlanner.Window> windows = CpsOrderSyncWindowPlanner.plan(
                startTime, endTime, Duration.ZERO, date -> false);
        log.info("[CpsOrderSyncJob] 开始同步，时间窗口: {} ~ {}，拆分为 {} 段，queryType={}，平台数={}",
                startTime.format(DTF), endTime.format(DTF), windows.size(), queryType, platforms.size());

        // 统计汇总
        int totalNew = 0, totalUpdate = 0, totalSkip = 0, totalFailed = 0;
        List<String> resultLines = new ArrayList<>();

        for (CpsOrderSyncWindowPlanner.Window window : windows) {
            String startTimeStr = window.start().format(DTF);
            String endTimeStr = window.end().format(DTF);
            for (CpsPlatformDO platform : platforms) {
                String platformCode = platform.getPlatformCode();
                CpsOrderSyncLogDO syncLog = CpsOrderSyncLogDO.builder()
                    .platformCode(platformCode)
                    .syncType(1)
                    .queryType(queryType)
                    .queryStartTime(window.start())
                    .queryEndTime(window.end())
                    .syncStartTime(LocalDateTime.now())
                    .totalCount(0)
                    .newCount(0)
                    .updateCount(0)
                    .skipCount(0)
                    .build();

                long t0 = System.currentTimeMillis();
                try {
                CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);
                PlatformSyncResult platformResult = syncPlatform(platform, client, queryType,
                        startTimeStr, endTimeStr, window.end());

                int total = platformResult.totalCount();
                int newCount = platformResult.newCount();
                int updateCount = platformResult.updateCount();
                int skipCount = platformResult.skipCount();

                syncLog.setTotalCount(total);
                syncLog.setNewCount(newCount);
                syncLog.setUpdateCount(updateCount);
                syncLog.setSkipCount(skipCount);
                syncLog.setSyncStatus(platformResult.status());
                if (StrUtil.isNotBlank(platformResult.failureSummary())) {
                    syncLog.setErrorMsg(StrUtil.subWithLength(platformResult.failureSummary(), 0, 500));
                }

                totalNew += newCount;
                totalUpdate += updateCount;
                totalSkip += skipCount;
                if (platformResult.status() != 1) {
                    totalFailed++;
                }
                String line = String.format("[%s] %s，共%d条，新增%d，更新%d，跳过%d%s",
                        platformCode, statusText(platformResult.status()), total, newCount, updateCount, skipCount,
                        StrUtil.isBlank(platformResult.failureSummary()) ? "" : "，原因: " + platformResult.failureSummary());
                resultLines.add(line);
                log.info("[CpsOrderSyncJob] 平台 {} 同步完成: {}", platformCode, line);

                } catch (Exception e) {
                log.error("[CpsOrderSyncJob] 平台 {} 同步失败", platformCode, e);
                syncLog.setSyncStatus(2);
                syncLog.setErrorMsg(StrUtil.subWithLength(e.getMessage(), 0, 500));
                totalFailed++;
                resultLines.add(String.format("[%s] 同步失败: %s", platformCode, e.getMessage()));
                } finally {
                long cost = System.currentTimeMillis() - t0;
                syncLog.setSyncEndTime(LocalDateTime.now());
                syncLog.setCostMs(cost);
                syncLogMapper.insert(syncLog);
                }
            }
        }

        int totalTasks = windows.size() * platforms.size();
        String summary = String.format("同步完成: 成功%d平台，失败%d平台，新增%d，更新%d，跳过%d%n%s",
                totalTasks - totalFailed, totalFailed, totalNew, totalUpdate, totalSkip,
                String.join("\n", resultLines));
        log.info("[CpsOrderSyncJob] {}", summary);
        return summary;
    }

    private PlatformSyncResult syncPlatform(CpsPlatformDO platform, CpsPlatformClient client, int queryType,
                                              String defaultStartTime, String endTime,
                                              LocalDateTime watermarkTime) {
        int total = 0;
        int newCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        int status = 1;
        List<String> failures = new ArrayList<>();
        for (Integer nullableScene : resolveOrderScenes(platform.getPlatformCode())) {
            int orderScene = nullableScene == null ? 0 : nullableScene;
            SceneSyncResult result = syncScene(platform, client, queryType, orderScene,
                    defaultStartTime, endTime, watermarkTime);
            total += result.totalCount();
            newCount += result.newCount();
            updateCount += result.updateCount();
            skipCount += result.skipCount();
            if (result.status() != 1) {
                status = status == 1 ? result.status() : Math.min(status, result.status());
                failures.add("scene=" + orderScene + ": " + result.failureSummary());
            }
        }
        return new PlatformSyncResult(total, newCount, updateCount, skipCount, status,
                String.join("; ", failures));
    }

    private SceneSyncResult syncScene(CpsPlatformDO platform, CpsPlatformClient client, int queryType,
                                      int orderScene, String defaultStartTime, String endTime,
                                      LocalDateTime watermarkTime) {
        String vendorCode = StrUtil.blankToDefault(platform.getActiveVendorCode(), "OFFICIAL");
        String checkpointQueryType = String.valueOf(queryType);
        CpsOrderSyncCheckpointDO checkpoint = checkpointMapper.selectByKey(
                platform.getPlatformCode(), vendorCode, orderScene, checkpointQueryType);
        if (checkpoint == null) {
            checkpoint = CpsOrderSyncCheckpointDO.builder()
                    .platformCode(platform.getPlatformCode())
                    .vendorCode(vendorCode)
                    .orderScene(orderScene)
                    .queryType(checkpointQueryType)
                    .nextPageNo(1)
                    .queryEndTime(watermarkTime)
                    .lastSyncStatus("NEW")
                    .lastSuccessCount(0)
                    .lastFailureCount(0)
                    .version(0)
                    .build();
            if (checkpointMapper.insert(checkpoint) != 1) {
                throw new IllegalStateException("checkpoint insert failed for "
                        + platform.getPlatformCode() + ":" + vendorCode + ":" + orderScene);
            }
        }

        LocalDateTime fixedQueryEndTime = checkpoint.getQueryEndTime();
        boolean inFlightWindow = fixedQueryEndTime != null;
        if (fixedQueryEndTime == null) {
            // queryEndTime is persisted only while a window is in flight. Once
            // the previous window completed it is cleared, and the next run
            // must use the current planner boundary instead of reusing the
            // previous watermark (which would create a zero-width window).
            fixedQueryEndTime = watermarkTime;
            checkpoint.setQueryEndTime(fixedQueryEndTime);
            if (checkpoint.getNextCursor() != null
                    || (checkpoint.getNextPageNo() != null && checkpoint.getNextPageNo() > 1)) {
                checkpoint.setNextCursor(null);
                checkpoint.setNextPageNo(1);
                checkpoint.setPaginationMode(null);
                checkpoint.setLastSyncStatus("RESTARTED");
            }
            saveCheckpoint(checkpoint, "initialize fixed query window");
        }

        // A completed window starts from the planner's requested boundary
        // (including any configured overlap). Only an in-flight window resumes
        // from its checkpoint watermark. Reusing the completed watermark here
        // can collapse the next request into a zero-width time range.
        String queryStartTime;
        if (inFlightWindow && checkpoint.getWatermarkTime() != null) {
            LocalDateTime requestedStart = LocalDateTime.parse(defaultStartTime, DTF);
            LocalDateTime watermark = checkpoint.getWatermarkTime();
            // For an in-flight window, the watermark is the exact resume point;
            // for a completed window, do not move backwards into older windows.
            queryStartTime = (watermark.isAfter(requestedStart) ? watermark : requestedStart).format(DTF);
        } else {
            queryStartTime = defaultStartTime;
        }
        String fixedQueryEndTimeText = fixedQueryEndTime.format(DTF);
        String positionIndex = checkpoint.getNextCursor();
        int pageNo = checkpoint.getNextPageNo() == null ? 1 : checkpoint.getNextPageNo();
        int total = 0;
        int newCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        boolean hasMore = false;

        for (int pageCount = 1; pageCount <= 20; pageCount++) {
            CpsOrderQueryRequest req = new CpsOrderQueryRequest();
            req.setQueryType(queryType);
            req.setOrderScene(orderScene == 0 ? null : orderScene);
            req.setStartTime(queryStartTime);
            req.setEndTime(fixedQueryEndTimeText);
            req.setPageSize(50);
            req.setPageNo(pageNo);
            if (positionIndex != null) {
                req.setPositionIndex(positionIndex);
            }

            CpsOrderPageResult pageResult;
            try {
                pageResult = client.queryOrderPage(req);
            } catch (Exception ex) {
                int failedStatus = total > 0 ? 3 : 2;
                recordSyncFailure(platform, vendorCode, orderScene, checkpointQueryType,
                        null, pageNo, positionIndex, "QUERY_PAGE", req, List.of(), ex.getMessage());
                markCheckpointFailure(checkpoint, failedStatus, total, 1, ex.getMessage());
                return new SceneSyncResult(total, newCount, updateCount, skipCount,
                        failedStatus, ex.getMessage());
            }
            List<CpsOrderDTO> pageOrders = pageResult.getItems();
            if (pageOrders.isEmpty() && pageResult.isHasMore()) {
                String message = "上游返回空页但 hasMore=true";
                int failedStatus = total > 0 ? 3 : 2;
                recordSyncFailure(platform, vendorCode, orderScene, checkpointQueryType,
                        pageResult.getPaginationMode().name(), pageNo, positionIndex, "EMPTY_HAS_MORE",
                        req, pageOrders, message);
                markCheckpointFailure(checkpoint, failedStatus, total, 0, message);
                return new SceneSyncResult(total, newCount, updateCount, skipCount,
                        failedStatus, message);
            }

            int[] stats;
            try {
                attachSyncBatchNo(pageOrders, platform, vendorCode, orderScene, checkpointQueryType,
                        fixedQueryEndTimeText, pageNo, positionIndex);
                stats = pageOrders.isEmpty() ? new int[]{0, 0, 0} : pageService.persistPage(pageOrders);
            } catch (Exception ex) {
                int failedStatus = total > 0 ? 3 : 2;
                recordSyncFailure(platform, vendorCode, orderScene, checkpointQueryType,
                        pageResult.getPaginationMode().name(), pageNo, positionIndex, "PERSIST_PAGE",
                        req, pageOrders, ex.getMessage());
                markCheckpointFailure(checkpoint, failedStatus, total, pageOrders.size(), ex.getMessage());
                return new SceneSyncResult(total, newCount, updateCount, skipCount,
                        failedStatus, ex.getMessage());
            }

            total += pageOrders.size();
            newCount += stats[0];
            updateCount += stats[1];
            skipCount += stats[2];
            hasMore = pageResult.isHasMore();
            checkpoint.setPaginationMode(pageResult.getPaginationMode().name());
            checkpoint.setLastSuccessCount(total);
            checkpoint.setLastFailureCount(0);
            checkpoint.setFailureSummary(null);

            if (!hasMore) {
                checkpoint.setNextCursor(null);
                checkpoint.setNextPageNo(null);
                checkpoint.setWatermarkTime(fixedQueryEndTime);
                checkpoint.setQueryEndTime(null);
                checkpoint.setLastSyncStatus("SUCCESS");
                saveCheckpoint(checkpoint, "complete query window");
                return new SceneSyncResult(total, newCount, updateCount, skipCount, 1, null);
            }
            if (pageResult.getPaginationMode() == CpsOrderPaginationMode.CURSOR) {
                String nextIndex = pageResult.getNextCursor();
                if (nextIndex == null || nextIndex.equals(positionIndex)) {
                    String message = "订单游标分页返回 hasMore=true 但未提供有效 nextCursor";
                    recordSyncFailure(platform, vendorCode, orderScene, checkpointQueryType,
                            pageResult.getPaginationMode().name(), pageNo, positionIndex, "INVALID_CURSOR",
                            req, pageOrders, message);
                    markCheckpointFailure(checkpoint, 3, total, 0, message);
                    return new SceneSyncResult(total, newCount, updateCount, skipCount, 3, message);
                }
                positionIndex = nextIndex;
                checkpoint.setNextCursor(nextIndex);
                checkpoint.setNextPageNo(null);
            } else {
                if (pageResult.getNextPageNo() == null || pageResult.getNextPageNo() <= pageNo) {
                    String message = "订单页码分页返回 hasMore=true 但未提供有效 nextPageNo";
                    recordSyncFailure(platform, vendorCode, orderScene, checkpointQueryType,
                            pageResult.getPaginationMode().name(), pageNo, positionIndex, "INVALID_PAGE",
                            req, pageOrders, message);
                    markCheckpointFailure(checkpoint, 3, total, 0, message);
                    return new SceneSyncResult(total, newCount, updateCount, skipCount, 3, message);
                }
                pageNo = pageResult.getNextPageNo();
                checkpoint.setNextPageNo(pageNo);
                checkpoint.setNextCursor(null);
            }
            checkpoint.setLastSyncStatus("PROCESSING");
            saveCheckpoint(checkpoint, "advance page");
        }

        String message = "达到最大分页数 20 仍有更多订单";
        if (hasMore) {
            recordSyncFailure(platform, vendorCode, orderScene, checkpointQueryType,
                    checkpoint.getPaginationMode(), pageNo, positionIndex, "PAGE_LIMIT", null, List.of(), message);
            markCheckpointFailure(checkpoint, 3, total, 0, message);
            log.warn("[CpsOrderSyncJob] platform={}, vendor={}, scene={} {}",
                    platform.getPlatformCode(), vendorCode, orderScene, message);
            return new SceneSyncResult(total, newCount, updateCount, skipCount, 3, message);
        }
        return new SceneSyncResult(total, newCount, updateCount, skipCount, 1, null);
    }

    private void attachSyncBatchNo(List<CpsOrderDTO> pageOrders, CpsPlatformDO platform, String vendorCode,
                                   int orderScene, String queryType, String fixedQueryEndTimeText,
                                   int pageNo, String positionIndex) {
        if (pageOrders == null || pageOrders.isEmpty()) {
            return;
        }
        String pageMark = positionIndex == null ? "page-" + pageNo : "cursor-" + positionIndex;
        String batchNo = String.join(":",
                "order-sync",
                platform.getPlatformCode(),
                vendorCode,
                String.valueOf(orderScene),
                queryType,
                fixedQueryEndTimeText,
                pageMark);
        for (CpsOrderDTO order : pageOrders) {
            order.setSyncBatchNo(batchNo);
        }
    }

    private void markCheckpointFailure(CpsOrderSyncCheckpointDO checkpoint, int syncStatus,
                                       int successCount, int failureCount, String failureSummary) {
        checkpoint.setLastSyncStatus(syncStatus == 2 ? "FAILED" : "PARTIAL");
        checkpoint.setLastSuccessCount(successCount);
        checkpoint.setLastFailureCount(failureCount);
        checkpoint.setFailureSummary(StrUtil.subWithLength(failureSummary, 0, 1000));
        saveCheckpoint(checkpoint, "record failure");
    }

    private void recordSyncFailure(CpsPlatformDO platform, String vendorCode, int orderScene, String queryType,
                                   String paginationMode, int pageNo, String nextCursor, String failureStage,
                                   CpsOrderQueryRequest request, List<CpsOrderDTO> pageOrders,
                                   String failureReason) {
        if (failureRecoveryService == null) {
            return;
        }
        String syncBatchNo = pageOrders == null || pageOrders.isEmpty() ? null : pageOrders.get(0).getSyncBatchNo();
        String rawSummary = summarizeRawPayload(pageOrders);
        String requestSnapshot = request == null ? "pageNo=" + pageNo + ",cursor=" + nextCursor
                : "queryType=" + request.getQueryType()
                + ",orderScene=" + request.getOrderScene()
                + ",startTime=" + request.getStartTime()
                + ",endTime=" + request.getEndTime()
                + ",pageNo=" + request.getPageNo()
                + ",positionIndex=" + request.getPositionIndex()
                + ",pageSize=" + request.getPageSize();
        String identity = StrUtil.blankToDefault(syncBatchNo, requestSnapshot);
        CpsOrderSyncFailureRecordCommand command = CpsOrderSyncFailureRecordCommand.builder()
                .platformCode(platform.getPlatformCode())
                .vendorCode(vendorCode)
                .orderScene(orderScene)
                .queryType(queryType)
                .paginationMode(paginationMode)
                .pageNo(pageNo)
                .nextCursor(nextCursor)
                .syncBatchNo(syncBatchNo)
                .failureStage(failureStage)
                .requestSnapshot(maskSensitive(requestSnapshot))
                .rawSummary(maskSensitive(rawSummary))
                .failureReason(failureReason)
                .idempotencyKey(StrUtil.subWithLength("order-sync-failure:" + platform.getPlatformCode()
                        + ":" + vendorCode + ":" + orderScene + ":" + queryType + ":" + failureStage
                        + ":" + identity, 0, 128))
                .build();
        failureRecoveryService.recordFailure(command);
    }

    private String summarizeRawPayload(List<CpsOrderDTO> pageOrders) {
        if (pageOrders == null || pageOrders.isEmpty()) {
            return null;
        }
        List<String> summaries = new ArrayList<>();
        for (int i = 0; i < Math.min(3, pageOrders.size()); i++) {
            CpsOrderDTO order = pageOrders.get(i);
            summaries.add("{platformOrderId=" + order.getPlatformOrderId()
                    + ",platformStatus=" + order.getPlatformStatus()
                    + ",refundTag=" + order.getRefundTag()
                    + ",rawPayload=" + order.getRawPayload() + "}");
        }
        return StrUtil.subWithLength(String.join(";", summaries), 0, 2000);
    }

    private String maskSensitive(String text) {
        if (text == null) {
            return null;
        }
        String masked = text.replaceAll("(?<!\\d)1[3-9]\\d{9}(?!\\d)", "***");
        return masked.replaceAll("(?i)(accessToken|token|secret|appSecret)(\\\"?\\s*[:=]\\s*\\\"?)[^,\\\"\\s}]+",
                "$1$2***");
    }

    private void saveCheckpoint(CpsOrderSyncCheckpointDO checkpoint, String action) {
        if (checkpoint.getId() == null || checkpointMapper.updateById(checkpoint) != 1) {
            throw new IllegalStateException("checkpoint update failed while attempting to " + action
                    + " [platform=" + checkpoint.getPlatformCode()
                    + ", vendor=" + checkpoint.getVendorCode()
                    + ", scene=" + checkpoint.getOrderScene()
                    + ", queryType=" + checkpoint.getQueryType() + "]");
        }
    }

    private String statusText(int status) {
        if (status == 1) {
            return "同步成功";
        }
        if (status == 3) {
            return "部分成功";
        }
        return "同步失败";
    }

    private List<Integer> resolveOrderScenes(String platformCode) {
        if ("taobao".equalsIgnoreCase(platformCode)) {
            return List.of(1, 2, 3);
        }
        return java.util.Collections.singletonList(null);
    }

    private record SceneSyncResult(int totalCount, int newCount, int updateCount, int skipCount,
                                   int status, String failureSummary) {
    }

    private record PlatformSyncResult(int totalCount, int newCount, int updateCount, int skipCount,
                                      int status, String failureSummary) {
    }

}
