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

@TableName("cpx_lead_detail")
@KeySequence("cpx_lead_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxLeadDetailDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long conversionId;
    private Long taskId;
    private String trackingId;
    private String contactHash;
    private String encryptedContact;
    private Boolean consentFlag;
    private String reviewStatus;
    private String reviewReason;
}
