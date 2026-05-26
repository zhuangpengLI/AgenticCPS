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

@TableName("cpx_offer")
@KeySequence("cpx_offer_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxOfferDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long taskId;
    private String offerCode;
    private String offerName;
    private String promotionMethod;
    private Integer rewardAmount;
    private Integer status;
    private String ruleJson;
    private String remark;
}
