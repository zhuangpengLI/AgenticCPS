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

@TableName("cpx_settlement_record")
@KeySequence("cpx_settlement_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxSettlementRecordDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String settlementNo;
    private Long conversionId;
    private Long taskId;
    private String platformCode;
    private String promotionMethod;
    private Long memberId;
    private Integer amount;
    private Integer rewardAmount;
    private String settlementStatus;
    private Long freezeRecordId;
    private Long rebateRecordId;
    private String idempotencyKey;
    private String remark;
}
