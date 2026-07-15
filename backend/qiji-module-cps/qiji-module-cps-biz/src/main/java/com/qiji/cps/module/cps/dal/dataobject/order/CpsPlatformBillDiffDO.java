package com.qiji.cps.module.cps.dal.dataobject.order;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Platform bill reconciliation difference and manual audit record.
 */
@TableName("cps_platform_bill_diff")
@KeySequence("cps_platform_bill_diff_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformBillDiffDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long billRowId;
    private Long orderId;
    private String platformCode;
    private String vendorCode;
    private String billBatchNo;
    private String platformOrderId;
    private String diffType;
    private String diffStatus;
    private String diffSummary;
    private BigDecimal orderCommissionAmount;
    private BigDecimal billCommissionAmount;
    private BigDecimal billRefundAmount;
    private String orderStatus;
    private String billStatus;
    private LocalDateTime orderSettleTime;
    private LocalDateTime billSettleTime;
    private String handleConclusion;
    private String handleAuditNote;
    private Long handleOperatorId;
    private LocalDateTime handleTime;
    private String idempotencyKey;
    @Version
    private Integer version;
}
