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

@TableName("cpx_conversion")
@KeySequence("cpx_conversion_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxConversionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String conversionNo;
    private Long taskId;
    private String trackingId;
    private String platformCode;
    private String promotionMethod;
    private String sourceEventId;
    private String targetEventType;
    private Long memberId;
    private Integer amount;
    private Integer rewardAmount;
    private String conversionStatus;
    private String settlementStatus;
    private LocalDateTime confirmedTime;
    private String remark;
}
