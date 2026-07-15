package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderStatusEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * CPS order status event mapper.
 */
@Mapper
public interface CpsOrderStatusEventMapper extends BaseMapperX<CpsOrderStatusEventDO> {

    default List<CpsOrderStatusEventDO> selectListByTrace(Long orderId, String platformOrderId) {
        if (orderId == null && (platformOrderId == null || platformOrderId.isBlank())) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CpsOrderStatusEventDO>()
                .eqIfPresent(CpsOrderStatusEventDO::getOrderId, orderId)
                .eqIfPresent(CpsOrderStatusEventDO::getPlatformOrderId, platformOrderId)
                .orderByDesc(CpsOrderStatusEventDO::getId));
    }
}
