package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import org.apache.ibatis.annotations.Mapper;

/** 订单归因审计日志 Mapper。 */
@Mapper
public interface CpsOrderAttributionLogMapper extends BaseMapperX<CpsOrderAttributionLogDO> {

    default CpsOrderAttributionLogDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(CpsOrderAttributionLogDO::getIdempotencyKey, idempotencyKey);
    }

    default PageResult<CpsOrderAttributionLogDO> selectPage(CpsOrderAttributionLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsOrderAttributionLogDO>()
                .eqIfPresent(CpsOrderAttributionLogDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(CpsOrderAttributionLogDO::getPlatformCode, reqVO.getPlatformCode())
                .likeIfPresent(CpsOrderAttributionLogDO::getPlatformOrderId, reqVO.getPlatformOrderId())
                .eqIfPresent(CpsOrderAttributionLogDO::getCandidateMemberId, reqVO.getCandidateMemberId())
                .eqIfPresent(CpsOrderAttributionLogDO::getAttributedMemberId, reqVO.getAttributedMemberId())
                .eqIfPresent(CpsOrderAttributionLogDO::getAttributionSource, reqVO.getAttributionSource())
                .eqIfPresent(CpsOrderAttributionLogDO::getAction, reqVO.getAction())
                .eqIfPresent(CpsOrderAttributionLogDO::getResult, reqVO.getResult())
                .betweenIfPresent(CpsOrderAttributionLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsOrderAttributionLogDO::getId));
    }
}
