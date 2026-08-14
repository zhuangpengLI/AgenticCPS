package com.qiji.cps.module.cps.dal.dataobject.didi;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiji.cps.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("cps_didi_callback_event")
@KeySequence("cps_didi_callback_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsDidiCallbackEventDO extends TenantBaseDO {
    @TableId private Long id;
    private String eventType;
    private String idempotencyKey;
    private String appKey;
    private String traceId;
    private String platformOrderId;
    private String activityId;
    private String sourceId;
    private Boolean rewardSent;
    private Integer retryTimes;
    private String processStatus;
    private String failureReason;
    private String requestBody;
}
