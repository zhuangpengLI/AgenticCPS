package com.qiji.cps.module.cps.controller.admin.cpx.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CpxTaskRespVO {

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
