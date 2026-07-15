package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS order synchronization checkpoint mapper.
 */
@Mapper
public interface CpsOrderSyncCheckpointMapper extends BaseMapperX<CpsOrderSyncCheckpointDO> {

    default PageResult<CpsOrderSyncCheckpointDO> selectPage(CpsOrderSyncCheckpointPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsOrderSyncCheckpointDO>()
                .eqIfPresent(CpsOrderSyncCheckpointDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsOrderSyncCheckpointDO::getVendorCode, reqVO.getVendorCode())
                .eqIfPresent(CpsOrderSyncCheckpointDO::getOrderScene, reqVO.getOrderScene())
                .eqIfPresent(CpsOrderSyncCheckpointDO::getQueryType, reqVO.getQueryType())
                .eqIfPresent(CpsOrderSyncCheckpointDO::getPaginationMode, reqVO.getPaginationMode())
                .eqIfPresent(CpsOrderSyncCheckpointDO::getLastSyncStatus, reqVO.getLastSyncStatus())
                .betweenIfPresent(CpsOrderSyncCheckpointDO::getUpdateTime, reqVO.getUpdateTime())
                .orderByDesc(CpsOrderSyncCheckpointDO::getId));
    }

    default CpsOrderSyncCheckpointDO selectByKey(String platformCode, String vendorCode,
                                                  Integer orderScene, String queryType) {
        return selectOne(new LambdaQueryWrapperX<CpsOrderSyncCheckpointDO>()
                .eq(CpsOrderSyncCheckpointDO::getPlatformCode, platformCode)
                .eq(CpsOrderSyncCheckpointDO::getVendorCode, vendorCode)
                .eq(CpsOrderSyncCheckpointDO::getOrderScene, orderScene)
                .eq(CpsOrderSyncCheckpointDO::getQueryType, queryType));
    }
}
