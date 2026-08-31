package com.qiji.cps.module.cps.dal.dataobject.order;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;
import java.time.LocalDateTime;

@TableName("cps_order_sync_batch") @KeySequence("cps_order_sync_batch_seq")
@Data @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(callSuper = true)
public class CpsOrderSyncBatchDO extends TenantBaseDO {
    @TableId private Long id; private String batchNo; private String batchType; private Integer queryType;
    private String platformCode; private String vendorCode; private LocalDateTime startTime; private LocalDateTime endTime;
    private String status; private Integer totalWindows; private Integer successWindows; private Integer failedWindows;
    private Integer retryWindows; private String failureSummary;
}
