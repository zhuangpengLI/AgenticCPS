package com.qiji.cps.module.cps.dal.mysql.transfer;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.transfer.vo.CpsTransferRecordPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

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

}
