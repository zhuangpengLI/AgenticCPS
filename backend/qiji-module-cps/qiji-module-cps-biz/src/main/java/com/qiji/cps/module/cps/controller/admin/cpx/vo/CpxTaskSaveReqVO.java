package com.qiji.cps.module.cps.controller.admin.cpx.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPX 任务创建/更新 Request VO")
@Data
public class CpxTaskSaveReqVO {

    @Schema(description = "任务编号")
    private Long id;

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "推广方式：CPS/CPA/CPL/CPM/CPC/OCPA/OCPC/MIXED")
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
