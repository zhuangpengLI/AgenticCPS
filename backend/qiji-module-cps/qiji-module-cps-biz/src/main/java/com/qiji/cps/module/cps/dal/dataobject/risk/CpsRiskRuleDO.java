package com.qiji.cps.module.cps.dal.dataobject.risk;

import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import com.qiji.cps.module.cps.enums.CpsRiskRuleTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * CPS 风控规则 DO
 *
 * <p>支持两种规则类型：
 * <ul>
 *   <li>rate_limit：频率限制，通过 {@code limitCount} 指定每日最大转链次数</li>
 *   <li>blacklist：黑名单，通过 {@code targetType} + {@code targetValue} 指定被拦截的会员或IP</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 * @see CpsRiskRuleTypeEnum
 */
@TableName("cps_risk_rule")
@KeySequence("cps_risk_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRiskRuleDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 规则类型：rate_limit（频率限制）/ blacklist（黑名单）
     *
     * @see CpsRiskRuleTypeEnum
     */
    private String ruleType;

    /**
     * 目标类型：member（会员）/ ip（IP地址）
     */
    private String targetType;

    /**
     * 目标值（blacklist 类型使用：会员ID 或 IP 地址）
     * <p>rate_limit 类型此字段为 null，表示对全部会员生效</p>
     */
    private String targetValue;

    /**
     * 频率限制次数（rate_limit 类型：每日最大转链次数）
     */
    private Integer limitCount;

    /**
     * 状态（0禁用 1启用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
