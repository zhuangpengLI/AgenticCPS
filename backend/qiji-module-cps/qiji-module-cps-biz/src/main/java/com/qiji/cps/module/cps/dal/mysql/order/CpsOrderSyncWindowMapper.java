package com.qiji.cps.module.cps.dal.mysql.order;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncWindowDO;
import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDateTime;
import java.util.List;
@Mapper public interface CpsOrderSyncWindowMapper extends BaseMapperX<CpsOrderSyncWindowDO> {
    default PageResult<CpsOrderSyncWindowDO> selectPageByBatch(Long batchId, int pageNo, int pageSize) {
        return selectPage(new com.qiji.cps.framework.common.pojo.PageParam(){ { setPageNo(pageNo); setPageSize(pageSize); } },
                new LambdaQueryWrapperX<CpsOrderSyncWindowDO>().eq(CpsOrderSyncWindowDO::getBatchId, batchId)
                        .orderByAsc(CpsOrderSyncWindowDO::getWindowStart));
    }
    default List<CpsOrderSyncWindowDO> selectExecutable(LocalDateTime now, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return selectList(new LambdaQueryWrapperX<CpsOrderSyncWindowDO>()
                // Newly-created windows have no retry timestamp.  Keep them
                // executable; applying `next_retry_time <= now` to PENDING
                // rows would make SQL's NULL comparison filter them forever.
                .and(wrapper -> wrapper.eq(CpsOrderSyncWindowDO::getStatus, "PENDING")
                        .or(retry -> retry.eq(CpsOrderSyncWindowDO::getStatus, "RETRY_WAIT")
                                .and(time -> time.isNull(CpsOrderSyncWindowDO::getNextRetryTime)
                                        .or().le(CpsOrderSyncWindowDO::getNextRetryTime, now))))
                .orderByAsc(CpsOrderSyncWindowDO::getWindowStart).last("LIMIT " + safeLimit));
    }
}
