package com.qiji.cps.module.cps.dal.mysql.order;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncBatchDO;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface CpsOrderSyncBatchMapper extends BaseMapperX<CpsOrderSyncBatchDO> {
    default PageResult<CpsOrderSyncBatchDO> selectPage(int pageNo, int pageSize, String platform, String status, String batchType, Integer queryType) {
        return selectPage(new com.qiji.cps.framework.common.pojo.PageParam(){ { setPageNo(pageNo); setPageSize(pageSize); } },
                new LambdaQueryWrapperX<CpsOrderSyncBatchDO>().eqIfPresent(CpsOrderSyncBatchDO::getPlatformCode, platform)
                        .eqIfPresent(CpsOrderSyncBatchDO::getStatus, status).eqIfPresent(CpsOrderSyncBatchDO::getBatchType, batchType)
                        .eqIfPresent(CpsOrderSyncBatchDO::getQueryType, queryType).orderByDesc(CpsOrderSyncBatchDO::getId));
    }
}
