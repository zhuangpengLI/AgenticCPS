package com.qiji.cps.module.cps.dal.mysql.activity;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CpsRebateActivityMapper extends BaseMapperX<CpsRebateActivityDO> {

    Integer CPS_ENABLE_STATUS = 1;

    default PageResult<CpsRebateActivityDO> selectPage(CpsRebateActivityPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsRebateActivityDO>()
                .likeIfPresent(CpsRebateActivityDO::getActivityName, reqVO.getActivityName())
                .eqIfPresent(CpsRebateActivityDO::getActivityType, reqVO.getActivityType())
                .eqIfPresent(CpsRebateActivityDO::getPlatformCode, reqVO.getPlatformCode())
                .eqIfPresent(CpsRebateActivityDO::getBillingType, reqVO.getBillingType())
                .eqIfPresent(CpsRebateActivityDO::getStatus, reqVO.getStatus())
                .orderByAsc(CpsRebateActivityDO::getSort)
                .orderByDesc(CpsRebateActivityDO::getId));
    }

    default List<CpsRebateActivityDO> selectEnabledList(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<CpsRebateActivityDO>()
                .eq(CpsRebateActivityDO::getStatus, CPS_ENABLE_STATUS)
                .and(wrapper -> wrapper.isNull(CpsRebateActivityDO::getStartTime)
                        .or().le(CpsRebateActivityDO::getStartTime, now))
                .and(wrapper -> wrapper.isNull(CpsRebateActivityDO::getEndTime)
                        .or().ge(CpsRebateActivityDO::getEndTime, now))
                .orderByAsc(CpsRebateActivityDO::getSort)
                .orderByDesc(CpsRebateActivityDO::getId));
    }

    default CpsRebateActivityDO selectBySourceTypeAndExternalActivityId(String sourceType, String externalActivityId) {
        return selectOne(new LambdaQueryWrapperX<CpsRebateActivityDO>()
                .eq(CpsRebateActivityDO::getSourceType, sourceType)
                .eq(CpsRebateActivityDO::getExternalActivityId, externalActivityId));
    }

}
