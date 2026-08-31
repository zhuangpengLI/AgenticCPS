package com.qiji.cps.module.cps.dal.mysql.order;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncWindowDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface CpsOrderSyncWindowMapper extends BaseMapperX<CpsOrderSyncWindowDO> {
    default PageResult<CpsOrderSyncWindowDO> selectPageByBatch(Long batchId, int pageNo, int pageSize) {
        return selectPage(new com.qiji.cps.framework.common.pojo.PageParam(){ { setPageNo(pageNo); setPageSize(pageSize); } },
                new LambdaQueryWrapperX<CpsOrderSyncWindowDO>().eq(CpsOrderSyncWindowDO::getBatchId, batchId)
                        .orderByAsc(CpsOrderSyncWindowDO::getWindowStart));
    }
}
