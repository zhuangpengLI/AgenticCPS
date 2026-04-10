package com.qiji.cps.module.cps.dal.mysql.risk;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.mybatis.core.mapper.BaseMapperX;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.controller.admin.risk.vo.CpsRiskRulePageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.risk.CpsRiskRuleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CPS 风控规则 Mapper
 *
 * @author CPS System
 */
@Mapper
public interface CpsRiskRuleMapper extends BaseMapperX<CpsRiskRuleDO> {

    /**
     * 分页查询风控规则
     */
    default PageResult<CpsRiskRuleDO> selectPage(CpsRiskRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CpsRiskRuleDO>()
                .eqIfPresent(CpsRiskRuleDO::getRuleType, reqVO.getRuleType())
                .eqIfPresent(CpsRiskRuleDO::getStatus, reqVO.getStatus())
                .orderByDesc(CpsRiskRuleDO::getId));
    }

    /**
     * 查询启用的全局频率限制规则（targetValue 为 null 表示全量规则）
     */
    default CpsRiskRuleDO selectActiveRateLimit() {
        return selectOne(new LambdaQueryWrapperX<CpsRiskRuleDO>()
                .eq(CpsRiskRuleDO::getRuleType, "rate_limit")
                .eq(CpsRiskRuleDO::getStatus, 1)
                .isNull(CpsRiskRuleDO::getTargetValue)
                .last("LIMIT 1"));
    }

    /**
     * 检查指定目标是否在黑名单中
     *
     * @param targetType  目标类型（member / ip）
     * @param targetValue 目标值（会员ID字符串 / IP地址）
     * @return true=在黑名单中，false=不在
     */
    default boolean existsBlacklist(String targetType, String targetValue) {
        return selectCount(new LambdaQueryWrapperX<CpsRiskRuleDO>()
                .eq(CpsRiskRuleDO::getRuleType, "blacklist")
                .eq(CpsRiskRuleDO::getTargetType, targetType)
                .eq(CpsRiskRuleDO::getTargetValue, targetValue)
                .eq(CpsRiskRuleDO::getStatus, 1)) > 0;
    }

}
