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
 * Imported platform bill row snapshot for order reconciliation.
 */
@TableName("cps_platform_bill_row")
@KeySequence("cps_platform_bill_row_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformBillRowDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String platformCode;
    private String vendorCode;
    private String billBatchNo;
    private String platformOrderId;
    private String parentOrderId;
    private String billStatus;
    private BigDecimal commissionAmount;
    private BigDecimal refundAmount;
    private LocalDateTime orderTime;
    private LocalDateTime settleTime;
    private LocalDateTime refundTime;
    private String sourceFileName;
    private String rawSummary;
    private String idempotencyKey;
    @Version
    private Integer version;
}
