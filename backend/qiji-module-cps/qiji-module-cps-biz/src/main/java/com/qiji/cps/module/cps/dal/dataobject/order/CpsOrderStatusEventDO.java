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

import java.time.LocalDateTime;

/**
 * Immutable CPS order status event.
 */
@TableName("cps_order_status_event")
@KeySequence("cps_order_status_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOrderStatusEventDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long orderId;
    private String platformCode;
    private String platformOrderId;
    private String sourceType;
    private String sourceBatchNo;
    private String rawStatus;
    private String rawStatusSummary;
    private String previousStatus;
    private String mappedStatus;
    private String currentStatus;
    private LocalDateTime eventTime;
    private Integer statusVersion;
    private Boolean downgradeRejected;
    private String rejectReason;
}
