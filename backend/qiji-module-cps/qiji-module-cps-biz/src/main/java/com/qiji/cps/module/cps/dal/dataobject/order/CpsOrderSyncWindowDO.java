package com.qiji.cps.module.cps.dal.dataobject.order;

import com.baomidou.mybatisplus.annotation.*;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;
import java.time.LocalDateTime;

@TableName("cps_order_sync_window") @KeySequence("cps_order_sync_window_seq")
@Data @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(callSuper = true)
public class CpsOrderSyncWindowDO extends TenantBaseDO {
    @TableId private Long id; private Long batchId; private String platformCode; private String vendorCode;
    private Integer orderScene; private Integer queryType; private LocalDateTime windowStart; private LocalDateTime windowEnd;
    private String status; private String paginationMode; private String nextCursor; private Integer nextPageNo;
    private Integer retryCount; private Integer maxRetryCount; private LocalDateTime nextRetryTime;
    private String leaseOwner; private LocalDateTime leaseUntil; private String lastErrorCode; private String lastErrorMessage;
}
