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

@TableName("cpx_task")
@KeySequence("cpx_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpxTaskDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String taskNo;
    private String taskName;
    private String platformCode;
    private String promotionMethod;
    private String taskType;
    private String offerType;
    private String title;
    private String shortDesc;
    private String rewardDesc;
    private Integer budgetAmount;
    private Integer dailyBudgetAmount;
    private Integer rewardAmount;
    private Integer rewardRate;
    private Boolean memberRewardEnabled;
    private Integer dedupeWindowSeconds;
    private Integer frequencyLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer priority;
    private String tags;
    private String materialJson;
    private String ruleJson;
    private String landingUrl;
    private String remark;
}
