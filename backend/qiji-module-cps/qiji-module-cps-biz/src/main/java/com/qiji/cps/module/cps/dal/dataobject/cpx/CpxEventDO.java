package com.qiji.cps.module.cps.dal.dataobject.cpx;

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

@TableName("cpx_event")
@KeySequence("cpx_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxEventDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String eventId;
    private String trackingId;
    private Long taskId;
    private String platformCode;
    private String promotionMethod;
    private String eventType;
    private String sourceEventId;
    private String idempotencyKey;
    private Long memberId;
    private String clientIp;
    private String userAgent;
    private LocalDateTime eventTime;
    private String rawPayload;
    private Boolean validFlag;
    private Integer status;
}
