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

import java.time.LocalDateTime;

/**
 * CPS order sync failure recovery queue and audit record.
 */
@TableName("cps_order_sync_failure")
@KeySequence("cps_order_sync_failure_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOrderSyncFailureDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String platformCode;
    private String vendorCode;
    private Integer orderScene;
    private String queryType;
    private String paginationMode;
    private Integer pageNo;
    private String nextCursor;
    private String syncBatchNo;
    private String failureStage;
    private String requestSnapshot;
    private String rawSummary;
    private String failureReason;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime lastReplayTime;
    private Long replayOperatorId;
    private String replayAuditNote;
    private String idempotencyKey;
    @Version
    private Integer version;
}
