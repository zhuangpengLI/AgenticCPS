package com.qiji.cps.module.cps.dal.dataobject.rebate;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("cps_rebate_asset_policy")
@KeySequence("cps_rebate_asset_policy_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateAssetPolicyDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Boolean v2Enabled;
    /** 仅由发布B变更单在唯一键、期初流水和冻结对账全部完成后置为 true。 */
    private Boolean migrationReady;
    /** 发布B审批所绑定的最新 ready 预检批次。 */
    private String latestReadyCheckBatchNo;
    /** 上述预检批次的执行时间。 */
    private LocalDateTime readyCheckTime;
    private Boolean readOnly;
    private Long largeDebtThresholdCent;
    private Integer reminderIntervalDays;
    private Integer normalReminderDays;
    private Integer largeReminderDays;
    private Integer smsIntervalDays;
}
