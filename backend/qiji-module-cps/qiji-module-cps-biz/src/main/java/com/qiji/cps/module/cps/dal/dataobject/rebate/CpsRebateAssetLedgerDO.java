package com.qiji.cps.module.cps.dal.dataobject.rebate;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * CPS 返利资产不可变流水。业务层仅允许追加。
 */
@TableName("cps_rebate_asset_ledger")
@KeySequence("cps_rebate_asset_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateAssetLedgerDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long memberId;
    private String sourceSystem;
    private String businessType;
    private String businessId;
    private Long orderId;
    private String platformOrderId;
    private String idempotencyKey;
    private Long availableChangeCent;
    private Long frozenChangeCent;
    private Long debtChangeCent;
    private Long availableBeforeCent;
    private Long availableAfterCent;
    private Long frozenBeforeCent;
    private Long frozenAfterCent;
    private Long debtBeforeCent;
    private Long debtAfterCent;
    private String operatorType;
    private String operatorId;
    private String reason;
}
