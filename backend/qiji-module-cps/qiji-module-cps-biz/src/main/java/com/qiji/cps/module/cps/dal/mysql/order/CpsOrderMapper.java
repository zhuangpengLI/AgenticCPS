package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * CPS订单 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsOrderMapper extends BaseMapperX<CpsOrderDO> {

    default PageResult<CpsOrderDO> selectPage(CpsOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsOrderDO>()
                .eqIfPresent(CpsOrderDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsOrderDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .likeIfPresent(CpsOrderDO::getItemTitle, reqVO.getItemTitle())
                .likeIfPresent(CpsOrderDO::getPlatformOrderId, reqVO.getPlatformOrderId())
                .betweenIfPresent(CpsOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsOrderDO::getId));
    }

    default CpsOrderDO selectByPlatformOrderId(String platformOrderId) {
        return selectOne(CpsOrderDO::getPlatformOrderId, platformOrderId);
    }

    /**
     * 查询待结算订单（已收货 or 已结算状态，且有会员归因，且返利未入账）
     *
     * @param statusList 订单状态列表（received / settled）
     * @param limit      每批最大数量
     */
    default List<CpsOrderDO> selectPendingSettleOrders(List<String> statusList, int limit) {
        return selectList(new LambdaQueryWrapperX<CpsOrderDO>()
                .in(CpsOrderDO::getOrderStatus, statusList)
                .isNotNull(CpsOrderDO::getMemberId)
                .isNull(CpsOrderDO::getRebateTime)     // 返利未入账
                .orderByAsc(CpsOrderDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 查询指定会员的订单分页（App端「我的订单」）
     */
    default PageResult<CpsOrderDO> selectPageByMemberId(CpsOrderPageReqVO reqVO, Long memberId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsOrderDO>()
                .eq(CpsOrderDO::getMemberId, memberId)
                .eqIfPresent(CpsOrderDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .orderByDesc(CpsOrderDO::getId));
    }

    /**
     * 按日期统计各平台订单聊合数据（给定日期、租户）
     */
    List<Map<String, Object>> selectDailyStatsByDate(@Param("statDate") LocalDate statDate,
                                                     @Param("tenantId") Long tenantId);

    /**
     * 实时看板：返回指定日期全平台汇总数据
     */
    Map<String, Object> selectRealtimeDashboard(@Param("statDate") LocalDate statDate,
                                                @Param("tenantId") Long tenantId);

}
