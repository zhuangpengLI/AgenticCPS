package com.qiji.cps.module.cps.dal.dataobject.activity;

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

/**
 * CPS 返利活动 DO.
 */
@TableName("cps_rebate_activity")
@KeySequence("cps_rebate_activity_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateActivityDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String activityName;

    private String activityType;

    private String platformCode;

    private String mainPic;

    private String shortDesc;

    private String rebateDesc;

    private String billingType;

    private Integer promotionCount;

    private String sourceType;

    private String externalActivityId;

    private String tagText;

    private String jumpType;

    private String jumpUrl;

    private String searchKeyword;

    private Integer sort;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String remark;

}
