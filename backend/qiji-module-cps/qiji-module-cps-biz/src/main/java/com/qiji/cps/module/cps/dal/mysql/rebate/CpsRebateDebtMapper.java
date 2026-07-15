package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CpsRebateDebtMapper extends BaseMapperX<CpsRebateDebtDO> {

    default List<CpsRebateDebtDO> selectDueReminderList(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<CpsRebateDebtDO>()
                .gt(CpsRebateDebtDO::getOutstandingDebtCent, 0L)
                .le(CpsRebateDebtDO::getNextReminderTime, now)
                .orderByAsc(CpsRebateDebtDO::getNextReminderTime)
                .last("LIMIT " + limit));
    }

    default PageResult<CpsRebateDebtDO> selectPage(CpsRebateDebtPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsRebateDebtDO>()
                .eqIfPresent(CpsRebateDebtDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsRebateDebtDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(CpsRebateDebtDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(CpsRebateDebtDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsRebateDebtDO::getId));
    }

    default List<CpsRebateDebtDO> selectListByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapperX<CpsRebateDebtDO>()
                .eq(CpsRebateDebtDO::getMemberId, memberId)
                .orderByAsc(CpsRebateDebtDO::getId));
    }

    default List<CpsRebateDebtDO> selectOutstandingForUpdateByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapperX<CpsRebateDebtDO>()
                .eq(CpsRebateDebtDO::getMemberId, memberId)
                .gt(CpsRebateDebtDO::getOutstandingDebtCent, 0L)
                .orderByAsc(CpsRebateDebtDO::getId)
                .last("FOR UPDATE"));
    }

    default List<CpsRebateDebtDO> selectListByTrace(Long orderId, String platformOrderId,
                                                    String sourceBusinessId, String idempotencyKey) {
        if (orderId == null && isBlank(platformOrderId) && isBlank(sourceBusinessId) && isBlank(idempotencyKey)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CpsRebateDebtDO>()
                .eqIfPresent(CpsRebateDebtDO::getOrderId, orderId)
                .eqIfPresent(CpsRebateDebtDO::getPlatformOrderId, platformOrderId)
                .eqIfPresent(CpsRebateDebtDO::getSourceBusinessId, sourceBusinessId)
                .eqIfPresent(CpsRebateDebtDO::getIdempotencyKey, idempotencyKey)
                .orderByDesc(CpsRebateDebtDO::getId));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
