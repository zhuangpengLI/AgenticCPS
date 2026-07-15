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
 * CPS order synchronization checkpoint.
 */
@TableName("cps_order_sync_checkpoint")
@KeySequence("cps_order_sync_checkpoint_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOrderSyncCheckpointDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String platformCode;
    private String vendorCode;
    private Integer orderScene;
    private String queryType;
    private String paginationMode;
    private String nextCursor;
    private Integer nextPageNo;
    private LocalDateTime watermarkTime;
    private LocalDateTime queryEndTime;
    private String lastSyncStatus;
    private Integer lastSuccessCount;
    private Integer lastFailureCount;
    private String failureSummary;
    @Version
    private Integer version;
}
