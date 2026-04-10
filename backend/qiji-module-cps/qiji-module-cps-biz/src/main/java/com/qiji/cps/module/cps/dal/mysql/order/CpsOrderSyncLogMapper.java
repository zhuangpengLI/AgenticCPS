package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CPS 订单同步日志 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsOrderSyncLogMapper extends BaseMapperX<CpsOrderSyncLogDO> {

    /**
     * 查询最近 N 条同步日志（按平台）
     */
    default List<CpsOrderSyncLogDO> selectRecentByPlatform(String platformCode, Integer limit) {
        return selectList(new LambdaQueryWrapperX<CpsOrderSyncLogDO>()
                .eqIfPresent(CpsOrderSyncLogDO::getPlatformCode, platformCode)
                .orderByDesc(CpsOrderSyncLogDO::getId)
                .last("LIMIT " + limit));
    }

}
