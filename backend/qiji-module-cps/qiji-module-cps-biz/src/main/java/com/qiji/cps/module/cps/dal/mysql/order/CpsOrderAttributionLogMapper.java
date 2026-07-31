package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.LocalDateTime;
import java.util.List;

/** 订单归因审计日志 Mapper。 */
@Mapper
public interface CpsOrderAttributionLogMapper extends BaseMapperX<CpsOrderAttributionLogDO> {

    default CpsOrderAttributionLogDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(CpsOrderAttributionLogDO::getIdempotencyKey, idempotencyKey);
    }

    default CpsOrderAttributionLogDO selectForUpdateById(Long id) {
        return selectOne(new LambdaQueryWrapperX<CpsOrderAttributionLogDO>()
                .eq(CpsOrderAttributionLogDO::getId, id)
                .last("FOR UPDATE"));
    }

    default CpsOrderAttributionLogDO selectLatestClaimByMemberAndOrder(Long memberId, String platformCode,
                                                                       String platformOrderId) {
        return selectOne(new LambdaQueryWrapperX<CpsOrderAttributionLogDO>()
                .eq(CpsOrderAttributionLogDO::getCandidateMemberId, memberId)
                .eq(CpsOrderAttributionLogDO::getPlatformCode, platformCode)
                .eq(CpsOrderAttributionLogDO::getPlatformOrderId, platformOrderId)
                .eq(CpsOrderAttributionLogDO::getAction, "CLAIM")
                .orderByDesc(CpsOrderAttributionLogDO::getId)
                .last("LIMIT 1"));
    }

    default List<CpsOrderAttributionLogDO> selectClaimsByMemberId(Long memberId, int limit) {
        return selectList(new LambdaQueryWrapperX<CpsOrderAttributionLogDO>()
                .eq(CpsOrderAttributionLogDO::getCandidateMemberId, memberId)
                .eq(CpsOrderAttributionLogDO::getAction, "CLAIM")
                .orderByDesc(CpsOrderAttributionLogDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    default int updateClaimReview(Long id, String reviewStatus, String auditNote, Long operatorId) {
        return update(null, new LambdaUpdateWrapper<CpsOrderAttributionLogDO>()
                .eq(CpsOrderAttributionLogDO::getId, id)
                .eq(CpsOrderAttributionLogDO::getAction, "CLAIM")
                .eq(CpsOrderAttributionLogDO::getReviewStatus, "PENDING_REVIEW")
                .set(CpsOrderAttributionLogDO::getReviewStatus, reviewStatus)
                .set(CpsOrderAttributionLogDO::getReviewAuditNote, auditNote)
                .set(CpsOrderAttributionLogDO::getReviewOperatorId, operatorId)
                .set(CpsOrderAttributionLogDO::getReviewTime, LocalDateTime.now()));
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
                .eqIfPresent(CpsOrderAttributionLogDO::getReviewStatus, reqVO.getReviewStatus())
                .betweenIfPresent(CpsOrderAttributionLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsOrderAttributionLogDO::getId));
    }
}
