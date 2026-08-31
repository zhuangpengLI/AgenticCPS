package com.qiji.cps.module.cps.service.order;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.dal.dataobject.order.*;
import com.qiji.cps.module.cps.dal.mysql.order.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;

@Service public class CpsOrderSyncBatchServiceImpl implements CpsOrderSyncBatchService {
    @Resource private CpsOrderSyncBatchMapper batchMapper; @Resource private CpsOrderSyncWindowMapper windowMapper;
    @Override @Transactional(rollbackFor = Exception.class)
    public CpsOrderSyncBatchDO create(String platform, String vendor, String type, Integer queryType, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || start.isAfter(end)) throw new IllegalArgumentException("同步时间范围无效");
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
    public void updateStatus(Long id,String status){batchMapper.updateById(CpsOrderSyncBatchDO.builder().id(id).status(status).build());}
    public void replayWindow(Long id){windowMapper.updateById(CpsOrderSyncWindowDO.builder().id(id).status("PENDING").nextRetryTime(null).lastErrorCode(null).lastErrorMessage(null).build());}
    public long countByStatus(String status){return batchMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CpsOrderSyncBatchDO>().eq(CpsOrderSyncBatchDO::getStatus,status));}
}
