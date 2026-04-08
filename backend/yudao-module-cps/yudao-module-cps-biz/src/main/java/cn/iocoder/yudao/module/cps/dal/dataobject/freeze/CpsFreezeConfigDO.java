package cn.iocoder.yudao.module.cps.dal.dataobject.freeze;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * CPS冻结解冻配置 DO
 *
 * @author CPS System
 */
@TableName("cps_freeze_config")
@KeySequence("cps_freeze_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsFreezeConfigDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 平台编码（NULL表示全平台）
     */
    private String platformCode;
    /**
     * 解冻天数（确认收货后天数）
     */
    private Integer unfreezeDays;
    /**
     * 状态（0禁用 1启用）
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
