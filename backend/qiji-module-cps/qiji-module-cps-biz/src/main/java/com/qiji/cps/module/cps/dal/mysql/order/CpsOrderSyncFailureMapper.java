package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailurePageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncFailureDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CpsOrderSyncFailureMapper extends BaseMapperX<CpsOrderSyncFailureDO> {

    default PageResult<CpsOrderSyncFailureDO> selectPage(CpsOrderSyncFailurePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsOrderSyncFailureDO>()
                .eqIfPresent(CpsOrderSyncFailureDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsOrderSyncFailureDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsOrderSyncFailureDO::getOrderScene, reqVO.getOrderScene())
                .eqIfPresent(CpsOrderSyncFailureDO::getQueryType, reqVO.getQueryType())
                .eqIfPresent(CpsOrderSyncFailureDO::getFailureStage, reqVO.getFailureStage())
                .eqIfPresent(CpsOrderSyncFailureDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CpsOrderSyncFailureDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsOrderSyncFailureDO::getId));
    }

    default CpsOrderSyncFailureDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<CpsOrderSyncFailureDO>()
                .eq(CpsOrderSyncFailureDO::getIdempotencyKey, idempotencyKey));
    }

    default List<CpsOrderSyncFailureDO> selectDueRetryFailures(LocalDateTime now, Integer limit) {
        return selectList(new LambdaQueryWrapperX<CpsOrderSyncFailureDO>()
                .in(CpsOrderSyncFailureDO::getStatus, List.of("PENDING", "RETRYING"))
                .le(CpsOrderSyncFailureDO::getNextRetryTime, now)
                .orderByAsc(CpsOrderSyncFailureDO::getNextRetryTime)
                .orderByAsc(CpsOrderSyncFailureDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit == null ? 50 : limit, 200))));
    }
}
