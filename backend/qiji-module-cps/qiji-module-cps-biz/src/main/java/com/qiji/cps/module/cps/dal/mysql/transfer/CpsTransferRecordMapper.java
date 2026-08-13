package com.qiji.cps.module.cps.dal.mysql.transfer;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.marketing.vo.CpsMarketingFunnelReqVO;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CPS转链记录 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsTransferRecordMapper extends BaseMapperX<CpsTransferRecordDO> {

    /**
     * 分页查询转链记录
     */
    default PageResult<CpsTransferRecordDO> selectPage(CpsTransferRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eqIfPresent(CpsTransferRecordDO::getMemberId, reqVO.getMemberId())
                .inIfPresent(CpsTransferRecordDO::getMemberId, reqVO.getMemberIds())
                .eqIfPresent(CpsTransferRecordDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsTransferRecordDO::getStatus, reqVO.getStatus())
                .likeIfPresent(CpsTransferRecordDO::getItemTitle, reqVO.getItemTitle())
                .betweenIfPresent(CpsTransferRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CpsTransferRecordDO::getId));
    }

    /**
     * 统计会员指定日期的转链次数（风控用）
     */
    default long countTodayByMember(Long memberId, LocalDate date) {
        return selectCount(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eq(CpsTransferRecordDO::getMemberId, memberId)
                .between(CpsTransferRecordDO::getCreateTime,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
    }

    /**
     * 查找订单兜底归因候选，最多返回 2 条用于识别歧义。
     */
    default List<CpsTransferRecordDO> selectAttributionCandidates(String platformCode, String itemId,
                                                                  String adzoneId, LocalDateTime startTime,
                                                                  LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eq(CpsTransferRecordDO::getPlatformCode, platformCode)
                .eq(CpsTransferRecordDO::getItemId, itemId)
                .eqIfPresent(CpsTransferRecordDO::getAdzoneId, adzoneId)
                .eq(CpsTransferRecordDO::getStatus, 1)
                .isNull(CpsTransferRecordDO::getPlatformOrderId)
                .between(CpsTransferRecordDO::getCreateTime, startTime, endTime)
                .orderByDesc(CpsTransferRecordDO::getId)
                .last("LIMIT 2"));
    }

    /**
     * 订单归因成功后回写平台订单号，形成转链记录到订单的闭环。
     */
    default int updatePlatformOrderId(Long id, String platformOrderId) {
        return updateById(CpsTransferRecordDO.builder()
                .id(id)
                .platformOrderId(platformOrderId)
                .build());
    }

    default List<CpsTransferRecordDO> selectValidAttributionTokenCandidates(String vendorCode,
                                                                            String platformCode,
                                                                            String attributionType,
                                                                            String attributionToken,
                                                                            LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eq(CpsTransferRecordDO::getVendorCode, vendorCode)
                .eq(CpsTransferRecordDO::getPlatformCode, platformCode)
                .eq(CpsTransferRecordDO::getAttributionType, attributionType)
                .eq(CpsTransferRecordDO::getAttributionToken, attributionToken)
                .eq(CpsTransferRecordDO::getStatus, 1)
                .and(wrapper -> wrapper.isNull(CpsTransferRecordDO::getExpireTime)
                        .or().gt(CpsTransferRecordDO::getExpireTime, now))
                .orderByDesc(CpsTransferRecordDO::getId)
                .last("LIMIT 2"));
    }

    default CpsTransferRecordDO selectReusableMemberSid(Long memberId, String vendorCode,
                                                         String platformCode, LocalDateTime now) {
        return selectOne(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eq(CpsTransferRecordDO::getMemberId, memberId)
                .eq(CpsTransferRecordDO::getVendorCode, vendorCode)
                .eq(CpsTransferRecordDO::getPlatformCode, platformCode)
                .eq(CpsTransferRecordDO::getAttributionType, "SID")
                .eq(CpsTransferRecordDO::getStatus, 1)
                .and(wrapper -> wrapper.isNull(CpsTransferRecordDO::getExpireTime)
                        .or().gt(CpsTransferRecordDO::getExpireTime, now))
                .orderByDesc(CpsTransferRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<CpsTransferRecordDO> selectListForMarketingFunnel(CpsMarketingFunnelReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<CpsTransferRecordDO>()
                .eqIfPresent(CpsTransferRecordDO::getPlatformCode, reqVO.getPlatformCode())
                .eq(CpsTransferRecordDO::getStatus, 1)
                .betweenIfPresent(CpsTransferRecordDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
                .orderByDesc(CpsTransferRecordDO::getId));
    }

}
