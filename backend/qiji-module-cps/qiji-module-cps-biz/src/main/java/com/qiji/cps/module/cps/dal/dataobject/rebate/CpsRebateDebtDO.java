package com.qiji.cps.module.cps.dal.dataobject.rebate;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CPS 会员返利欠款。
 */
@TableName("cps_rebate_debt")
@KeySequence("cps_rebate_debt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateDebtDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long memberId;
    private Long orderId;
    private String platformOrderId;
    private String sourceBusinessId;
    private String idempotencyKey;
    private Long originalDebtCent;
    private Long repaidDebtCent;
    private Long waivedDebtCent;
    private Long outstandingDebtCent;
    private String status;
    private LocalDateTime lastReminderTime;
    private LocalDateTime lastSmsTime;
    private LocalDateTime nextReminderTime;
}
