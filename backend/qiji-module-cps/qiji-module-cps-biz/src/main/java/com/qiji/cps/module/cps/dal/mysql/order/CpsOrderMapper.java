package com.qiji.cps.module.cps.dal.mysql.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.enums.CpsFreezeStatusEnum;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LambdaQueryWrapperX<CpsOrderDO> wrapper = new LambdaQueryWrapperX<CpsOrderDO>()
                .eqIfPresent(CpsOrderDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsOrderDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(CpsOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .likeIfPresent(CpsOrderDO::getItemTitle, reqVO.getItemTitle())
                .likeIfPresent(CpsOrderDO::getPlatformOrderId, reqVO.getPlatformOrderId())
                .betweenIfPresent(CpsOrderDO::getCreateTime, reqVO.getCreateTime());
        if (reqVO.getMemberName() != null && !reqVO.getMemberName().isBlank()) {
            wrapper.and(w -> {
                w.like(CpsOrderDO::getMemberNickname, reqVO.getMemberName());
                if (reqVO.getMemberIds() != null && !reqVO.getMemberIds().isEmpty()) {
                    w.or().in(CpsOrderDO::getMemberId, reqVO.getMemberIds());
                }
            });
        } else {
            wrapper.inIfPresent(CpsOrderDO::getMemberId, reqVO.getMemberIds());
        }
        return selectPage(reqVO, wrapper.orderByDesc(CpsOrderDO::getId));
    }

    default CpsOrderDO selectByPlatformOrderId(String platformCode, String platformOrderId) {
        return selectOne(new LambdaQueryWrapperX<CpsOrderDO>()
                .eq(CpsOrderDO::getPlatformCode, platformCode)
                .eq(CpsOrderDO::getPlatformOrderId, platformOrderId));
    }

    /**
     * 在结算事务内锁定并重读当前订单，禁止使用批扫描阶段的陈旧状态做资金变更。
     */
    default CpsOrderDO selectForUpdateById(Long id) {
        return selectOne(new LambdaQueryWrapperX<CpsOrderDO>()
                .eq(CpsOrderDO::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 仅在订单尚未归因时绑定会员，避免并发申领覆盖已确认归属。
     */
    default int bindMemberIfUnattributed(Long orderId, Long memberId, String memberNickname,
                                         String attributionSource) {
        return update(null, new LambdaUpdateWrapper<CpsOrderDO>()
                .eq(CpsOrderDO::getId, orderId)
                .isNull(CpsOrderDO::getMemberId)
                .set(CpsOrderDO::getMemberId, memberId)
                .set(CpsOrderDO::getMemberNickname, memberNickname)
                .set(CpsOrderDO::getAttributionSource, attributionSource));
    }

    /**
     * 使用订单状态版本号做条件更新，避免退款与结算同步并发时发生最后写覆盖。
     */
    default int updateByIdAndStatusVersion(CpsOrderDO updateDO, Integer expectedStatusVersion) {
        int currentVersion = expectedStatusVersion == null ? 0 : expectedStatusVersion;
        LambdaUpdateWrapper<CpsOrderDO> wrapper = new LambdaUpdateWrapper<CpsOrderDO>()
                .eq(CpsOrderDO::getId, updateDO.getId())
                .eq(CpsOrderDO::getStatusVersion, currentVersion)
                .set(CpsOrderDO::getStatusVersion, currentVersion + 1);
        return update(updateDO, wrapper);
    }

    /**
     * 结算成功后以订单状态版本做 CAS，任何退款/失效并发更新都会让本事务失败回滚。
     */
    default int updateRebateFreezeByStatusVersion(CpsOrderDO updateDO, Integer expectedStatusVersion) {
        int currentVersion = expectedStatusVersion == null ? 0 : expectedStatusVersion;
        LambdaUpdateWrapper<CpsOrderDO> wrapper = new LambdaUpdateWrapper<CpsOrderDO>()
                .eq(CpsOrderDO::getId, updateDO.getId())
                .eq(CpsOrderDO::getOrderStatus, CpsOrderStatusEnum.SETTLED.getStatus())
                .eq(CpsOrderDO::getStatusVersion, currentVersion)
                .set(CpsOrderDO::getOrderStatus, CpsOrderStatusEnum.SETTLED.getStatus())
                .set(CpsOrderDO::getRealRebate, updateDO.getRealRebate())
                .set(CpsOrderDO::getRebateFreezeStatus, updateDO.getRebateFreezeStatus())
                .set(CpsOrderDO::getPlanUnfreezeTime, updateDO.getPlanUnfreezeTime())
                .set(CpsOrderDO::getRebateSettleRetryCount, 0)
                .set(CpsOrderDO::getRebateSettleNextRetryTime, null)
                .set(CpsOrderDO::getRebateSettleLastError, null)
                .set(CpsOrderDO::getStatusVersion, currentVersion + 1);
        return update(updateDO, wrapper);
    }

    /**
     * 查询待创建返利资产的订单（平台已结算、有会员归因，且尚未创建V2冻结）。
     *
     * @param statusList 订单状态列表（received / settled）
     * @param limit      每批最大数量
     */
    default List<CpsOrderDO> selectPendingSettleOrders(List<String> statusList, int limit) {
        LocalDateTime now = LocalDateTime.now();
        return selectList(new LambdaQueryWrapperX<CpsOrderDO>()
                .in(CpsOrderDO::getOrderStatus, statusList)
                .isNotNull(CpsOrderDO::getMemberId)
                .isNull(CpsOrderDO::getRebateTime)     // 兼容V1：返利尚未到账
                .and(w -> w.isNull(CpsOrderDO::getRebateFreezeStatus)
                        .or().eq(CpsOrderDO::getRebateFreezeStatus, CpsFreezeStatusEnum.PENDING.getStatus()))
                .and(w -> w.isNull(CpsOrderDO::getRebateSettleNextRetryTime)
                        .or().le(CpsOrderDO::getRebateSettleNextRetryTime, now))
                // 新单使用创建时间、重试单使用到期时间，统一按资格时间 FIFO，避免任一队列反向饥饿。
                .last("ORDER BY COALESCE(rebate_settle_next_retry_time, create_time) ASC, id ASC LIMIT " + limit));
    }

    /**
     * 记录一次待处理/失败并延后重试；只写调度元数据，不覆盖订单业务状态。
     */
    default int markSettleRetry(Long orderId, String error, LocalDateTime nextRetryTime) {
        return update(null, new LambdaUpdateWrapper<CpsOrderDO>()
                .eq(CpsOrderDO::getId, orderId)
                .setSql("rebate_settle_retry_count = COALESCE(rebate_settle_retry_count, 0) + 1")
                .set(CpsOrderDO::getRebateSettleNextRetryTime, nextRetryTime)
                .set(CpsOrderDO::getRebateSettleLastError, error));
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
     * 查询可信会员在指定时间窗口内的订单，用于只读成交画像分析。
     *
     * <p>调用方必须传入从登录或签名上下文解析出的 memberId，不能接受请求体身份。</p>
     */
    default List<CpsOrderDO> selectRecentListByMemberId(Long memberId, LocalDateTime startTime,
                                                         LocalDateTime endTime, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return selectList(new LambdaQueryWrapperX<CpsOrderDO>()
                .eq(CpsOrderDO::getMemberId, memberId)
                .betweenIfPresent(CpsOrderDO::getCreateTime, startTime, endTime)
                .orderByDesc(CpsOrderDO::getId)
                .last("LIMIT " + safeLimit));
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

    default List<CpsOrderDO> selectListForMarketingFunnel(CpsMarketingFunnelReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CpsOrderDO>()
                .eqIfPresent(CpsOrderDO::getPlatformCode, reqVO.getPlatformCode())
                .betweenIfPresent(CpsOrderDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
                .orderByDesc(CpsOrderDO::getId));
    }

}
