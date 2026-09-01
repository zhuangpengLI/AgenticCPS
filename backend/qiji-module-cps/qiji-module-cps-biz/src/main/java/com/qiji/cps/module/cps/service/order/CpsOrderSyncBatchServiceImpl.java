package com.qiji.cps.module.cps.service.order;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.dal.dataobject.order.*;
import com.qiji.cps.module.cps.dal.mysql.order.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;

@Service public class CpsOrderSyncBatchServiceImpl implements CpsOrderSyncBatchService {
    private static final LocalDateTime MIN_SYNC_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Resource private CpsOrderSyncBatchMapper batchMapper; @Resource private CpsOrderSyncWindowMapper windowMapper;
    @Resource private CpsOrderService orderService;
    @Override @Transactional(rollbackFor = Exception.class)
    public CpsOrderSyncBatchDO create(String platform, String vendor, String type, Integer queryType, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || start.isAfter(end) || start.isBefore(MIN_SYNC_TIME) || end.isBefore(MIN_SYNC_TIME)) {
            throw new IllegalArgumentException("同步时间范围无效");
        }
        String no = "sync-" + UUID.randomUUID().toString().replace("-", "");
        CpsOrderSyncBatchDO batch = CpsOrderSyncBatchDO.builder().batchNo(no).batchType(type == null ? "MANUAL" : type)
                .queryType(queryType == null ? 4 : queryType).platformCode(platform).vendorCode(vendor == null ? "OFFICIAL" : vendor)
                .startTime(start).endTime(end).status("PENDING").totalWindows(0).successWindows(0).failedWindows(0).retryWindows(0).build();
        batchMapper.insert(batch);
        List<CpsOrderSyncWindowPlanner.Window> windows = CpsOrderSyncWindowPlanner.plan(start, end);
        for (CpsOrderSyncWindowPlanner.Window w : windows) windowMapper.insert(CpsOrderSyncWindowDO.builder().batchId(batch.getId())
                .platformCode(platform).vendorCode(batch.getVendorCode()).orderScene(0).queryType(batch.getQueryType())
                .windowStart(w.start()).windowEnd(w.end()).status("PENDING").nextPageNo(1).retryCount(0).maxRetryCount(5).build());
        batch.setTotalWindows(windows.size()); batchMapper.updateById(batch); return batch;
    }
    public PageResult<CpsOrderSyncBatchDO> page(int pageNo,int pageSize,String p,String s,String t,Integer q){return batchMapper.selectPage(pageNo,pageSize,p,s,t,q);}
    public PageResult<CpsOrderSyncWindowDO> windows(Long id,int pageNo,int pageSize){return windowMapper.selectPageByBatch(id,pageNo,pageSize);}
    public void updateStatus(Long id,String status){
        batchMapper.updateById(CpsOrderSyncBatchDO.builder().id(id).status(status).build());
        if ("PAUSED".equals(status)) windowMapper.update(CpsOrderSyncWindowDO.builder().status("PAUSED").build(),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncWindowDO>()
                        .eq(CpsOrderSyncWindowDO::getBatchId, id)
                        .in(CpsOrderSyncWindowDO::getStatus, List.of("PENDING", "RETRY_WAIT")));
        if ("RUNNING".equals(status)) windowMapper.update(CpsOrderSyncWindowDO.builder().status("PENDING").nextRetryTime(null).build(),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncWindowDO>()
                        .eq(CpsOrderSyncWindowDO::getBatchId, id)
                        .eq(CpsOrderSyncWindowDO::getStatus, "PAUSED"));
        if ("CANCELLED".equals(status)) windowMapper.update(CpsOrderSyncWindowDO.builder().status("CANCELLED").build(),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncWindowDO>()
                        .eq(CpsOrderSyncWindowDO::getBatchId, id)
                        .in(CpsOrderSyncWindowDO::getStatus, List.of("PENDING", "RETRY_WAIT", "PAUSED")));
    }
    public void replayWindow(Long id){windowMapper.updateById(CpsOrderSyncWindowDO.builder().id(id).status("PENDING").nextRetryTime(null).lastErrorCode(null).lastErrorMessage(null).build());}
    public long countByStatus(String status){return batchMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncBatchDO>().eq(CpsOrderSyncBatchDO::getStatus,status));}
    @Override
    public int executeDueWindows(int limit) {
        int processed = 0;
        for (CpsOrderSyncWindowDO window : windowMapper.selectExecutable(LocalDateTime.now(), limit)) {
            CpsOrderSyncBatchDO batch = batchMapper.selectById(window.getBatchId());
            if (batch == null || List.of("PAUSED", "CANCELLED", "SUCCESS", "DEAD").contains(batch.getStatus())) continue;
            batchMapper.updateById(CpsOrderSyncBatchDO.builder().id(batch.getId()).status("RUNNING").build());
            windowMapper.updateById(CpsOrderSyncWindowDO.builder().id(window.getId()).status("RUNNING")
                    .leaseOwner("cpsOrderSyncBatchExecutionJob").leaseUntil(LocalDateTime.now().plusMinutes(15)).build());
            try {
                orderService.manualSync(window.getPlatformCode(), window.getVendorCode(), 3, window.getQueryType(),
                        null, window.getWindowStart(), window.getWindowEnd());
                windowMapper.updateById(CpsOrderSyncWindowDO.builder().id(window.getId()).status("SUCCESS")
                        .leaseOwner(null).leaseUntil(null).lastErrorCode(null).lastErrorMessage(null).build());
            } catch (RuntimeException ex) {
                int retry = Optional.ofNullable(window.getRetryCount()).orElse(0) + 1;
                int maxRetry = Optional.ofNullable(window.getMaxRetryCount()).orElse(5);
                boolean dead = retry >= maxRetry;
                windowMapper.updateById(CpsOrderSyncWindowDO.builder().id(window.getId())
                        .status(dead ? "DEAD" : "RETRY_WAIT").retryCount(retry)
                        .nextRetryTime(dead ? null : LocalDateTime.now().plusMinutes(5L * retry))
                        .leaseOwner(null).leaseUntil(null).lastErrorCode(ex.getClass().getSimpleName())
                        .lastErrorMessage(shortMessage(ex)).build());
            }
            refreshBatch(window.getBatchId());
            processed++;
        }
        return processed;
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void delete(Long batchId) {
        CpsOrderSyncBatchDO batch = batchMapper.selectById(batchId);
        if (batch == null) return;
        if ("RUNNING".equals(batch.getStatus())) throw new IllegalStateException("运行中的同步批次不能删除，请先暂停或取消");
        windowMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncWindowDO>()
                .eq(CpsOrderSyncWindowDO::getBatchId, batchId));
        batchMapper.deleteById(batchId);
    }
    @Override
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        long success = countWindows("SUCCESS"), dead = countWindows("DEAD"), pending = countWindows("PENDING"), retry = countWindows("RETRY_WAIT");
        metrics.put("runningBatches", countByStatus("RUNNING"));
        metrics.put("pendingWindows", pending); metrics.put("retryWindows", retry); metrics.put("deadWindows", dead);
        metrics.put("successRate", success + dead == 0 ? 0D : (double) success / (success + dead));
        return metrics;
    }
    private void refreshBatch(Long batchId) {
        List<CpsOrderSyncWindowDO> windows = windowMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncWindowDO>()
                .eq(CpsOrderSyncWindowDO::getBatchId, batchId));
        long success = windows.stream().filter(window -> "SUCCESS".equals(window.getStatus())).count();
        long dead = windows.stream().filter(window -> "DEAD".equals(window.getStatus())).count();
        long retry = windows.stream().filter(window -> "RETRY_WAIT".equals(window.getStatus())).count();
        String status = success == windows.size() ? "SUCCESS" : success + dead == windows.size() && dead > 0 ? "DEAD" : "RUNNING";
        batchMapper.updateById(CpsOrderSyncBatchDO.builder().id(batchId).status(status).successWindows((int) success)
                .failedWindows((int) dead).retryWindows((int) retry).build());
    }
    private long countWindows(String status) {
        return windowMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncWindowDO>()
                .eq(CpsOrderSyncWindowDO::getStatus, status));
    }
    private String shortMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message.substring(0, Math.min(1000, message.length()));
    }
}
