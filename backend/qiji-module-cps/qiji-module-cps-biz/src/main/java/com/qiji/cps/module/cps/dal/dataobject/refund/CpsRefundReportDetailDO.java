package com.qiji.cps.module.cps.dal.dataobject.refund;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("cps_refund_report_detail")
@KeySequence("cps_refund_report_detail_seq")
@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
@Builder @NoArgsConstructor @AllArgsConstructor
public class CpsRefundReportDetailDO extends TenantBaseDO {
    @TableId private Long id;
    private Long importId;
    private String platformCode;
    private String platformOrderId;
    private String refundType;
    private BigDecimal refundAmount;
    private LocalDateTime refundTime;
    private Long orderId;
    private String matchStatus;
    private String differenceReason;
    private Long assetLedgerId;
}
