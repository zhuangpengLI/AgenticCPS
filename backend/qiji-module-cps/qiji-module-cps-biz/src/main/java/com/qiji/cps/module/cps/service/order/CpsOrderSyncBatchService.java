package com.qiji.cps.module.cps.service.order;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.dal.dataobject.order.*;
import java.time.LocalDateTime;
import java.util.Map;

public interface CpsOrderSyncBatchService {
    CpsOrderSyncBatchDO create(String platform, String vendor, String type, Integer queryType, LocalDateTime start, LocalDateTime end);
    PageResult<CpsOrderSyncBatchDO> page(int pageNo, int pageSize, String platform, String status, String type, Integer queryType);
    PageResult<CpsOrderSyncWindowDO> windows(Long batchId, int pageNo, int pageSize);
    void updateStatus(Long id, String status);
    void replayWindow(Long windowId);
    long countByStatus(String status);
    int executeDueWindows(int limit);
    void delete(Long batchId);
    Map<String, Object> metrics();
}
