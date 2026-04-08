package cn.iocoder.yudao.module.cps.dal.dataobject.rebate;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * CPS返利配置 DO
 *
 * @author CPS System
 */
@TableName("cps_rebate_config")
@KeySequence("cps_rebate_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateConfigDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 会员等级ID（NULL表示无等级限制）
     */
    private Long memberLevelId;
    /**
     * 平台编码（NULL表示全平台）
     */
    private String platformCode;
    /**
     * 返利比例（百分比）
     */
    private BigDecimal rebateRate;
    /**
     * 单笔最大返利金额（0表示不限）
     */
    private BigDecimal maxRebateAmount;
    /**
     * 单笔最小返利金额（0表示不限）
     */
    private BigDecimal minRebateAmount;
    /**
     * 状态（0禁用 1启用）
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority;

}
