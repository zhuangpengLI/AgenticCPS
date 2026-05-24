package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS选品主题保存 Request VO")
@Data
public class CpsSelectionThemeSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "主题编码")
    @NotBlank(message = "主题编码不能为空")
    private String themeCode;

    @Schema(description = "主题名称")
    @NotBlank(message = "主题名称不能为空")
    private String themeName;

    @Schema(description = "主题类型")
    private String themeType;

    @Schema(description = "大促标识")
    private String promotionEvent;

    @Schema(description = "平台范围，逗号分隔")
    private String platformCodes;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "封面图")
    private String coverPic;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "主题规则 JSON")
    private String ruleJson;

    @Schema(description = "AI Prompt")
    private String aiPrompt;

    @Schema(description = "AI 摘要")
    private String aiSummary;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "上线时间")
    private LocalDateTime startTime;

    @Schema(description = "下线时间")
    private LocalDateTime endTime;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
