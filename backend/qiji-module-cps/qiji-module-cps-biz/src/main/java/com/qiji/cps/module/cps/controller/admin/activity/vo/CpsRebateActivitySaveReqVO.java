package com.qiji.cps.module.cps.controller.admin.activity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS返利活动创建/修改 Request VO")
@Data
public class CpsRebateActivitySaveReqVO {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "饿了么外卖红包")
    @NotBlank(message = "活动名称不能为空")
    private String activityName;

    @Schema(description = "专题类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "外卖")
    @NotBlank(message = "专题类型不能为空")
    private String activityType;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "eleme")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "活动主图")
    private String mainPic;

    @Schema(description = "短描述")
    private String shortDesc;

    @Schema(description = "返利文案")
    private String rebateDesc;

    @Schema(description = "计费类型", example = "CPS")
    private String billingType;

    @Schema(description = "推广数", example = "1405")
    private Integer promotionCount;

    @Schema(description = "来源类型", example = "configured")
    private String sourceType;

    @Schema(description = "外部活动ID")
    private String externalActivityId;

    @Schema(description = "标签文案", example = "热")
    private String tagText;

    @Schema(description = "跳转类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "search")
    @NotBlank(message = "跳转类型不能为空")
    private String jumpType;

    @Schema(description = "跳转地址")
    private String jumpUrl;

    @Schema(description = "搜索关键词")
    private String searchKeyword;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态（0禁用 1启用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "上线时间")
    private LocalDateTime startTime;

    @Schema(description = "下线时间")
    private LocalDateTime endTime;

    @Schema(description = "备注")
    private String remark;

}
