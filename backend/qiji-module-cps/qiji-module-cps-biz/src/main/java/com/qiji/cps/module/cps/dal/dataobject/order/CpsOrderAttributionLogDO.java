package com.qiji.cps.module.cps.dal.dataobject.order;

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

/** 订单归因追加式审计日志。 */
@TableName("cps_order_attribution_log")
@KeySequence("cps_order_attribution_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOrderAttributionLogDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long orderId;
    private String platformCode;
    private String platformOrderId;
    private Long candidateMemberId;
    private Long attributedMemberId;
    private String attributionSource;
    private String bindingType;
    private String bindingId;
    private String action;
    private String result;
    private String rejectReason;
    private String operatorType;
    private String operatorId;
    private String idempotencyKey;
    private String reviewStatus;
    private String reviewAuditNote;
    private Long reviewOperatorId;
    private java.time.LocalDateTime reviewTime;
}
