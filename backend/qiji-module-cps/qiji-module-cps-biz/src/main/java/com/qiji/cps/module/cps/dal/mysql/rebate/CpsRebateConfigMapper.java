package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CPS返利配置 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsRebateConfigMapper extends BaseMapperX<CpsRebateConfigDO> {

    default List<CpsRebateConfigDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<CpsRebateConfigDO>()
                .eq(CpsRebateConfigDO::getStatus, status)
                .orderByDesc(CpsRebateConfigDO::getPriority));
    }

    default List<CpsRebateConfigDO> selectListByPlatformCode(String platformCode) {
        return selectList(new LambdaQueryWrapperX<CpsRebateConfigDO>()
                .eq(CpsRebateConfigDO::getPlatformCode, platformCode)
                .orderByDesc(CpsRebateConfigDO::getPriority)
                .orderByAsc(CpsRebateConfigDO::getId));
    }

    default List<CpsRebateConfigDO> selectManagedRulesByPlatformCode(String platformCode) {
        return selectList(new LambdaQueryWrapperX<CpsRebateConfigDO>()
                .eq(CpsRebateConfigDO::getPlatformCode, platformCode)
                .isNull(CpsRebateConfigDO::getMemberId)
                .orderByDesc(CpsRebateConfigDO::getPriority)
                .orderByAsc(CpsRebateConfigDO::getId));
    }

    default List<CpsRebateConfigDO> selectListByScope(Long memberId, Long memberLevelId,
                                                       String platformCode, Integer priority) {
        LambdaQueryWrapperX<CpsRebateConfigDO> query = new LambdaQueryWrapperX<>();
        if (memberId == null) {
            query.isNull(CpsRebateConfigDO::getMemberId);
        } else {
            query.eq(CpsRebateConfigDO::getMemberId, memberId);
        }
        if (memberLevelId == null) {
            query.isNull(CpsRebateConfigDO::getMemberLevelId);
        } else {
            query.eq(CpsRebateConfigDO::getMemberLevelId, memberLevelId);
        }
        if (platformCode == null) {
            query.isNull(CpsRebateConfigDO::getPlatformCode);
        } else {
            query.eq(CpsRebateConfigDO::getPlatformCode, platformCode);
        }
        query.eq(CpsRebateConfigDO::getPriority, priority);
        return selectList(query);
    }

}
