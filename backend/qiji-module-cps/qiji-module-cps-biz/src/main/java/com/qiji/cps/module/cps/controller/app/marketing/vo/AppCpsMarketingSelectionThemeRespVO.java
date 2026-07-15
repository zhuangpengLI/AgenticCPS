package com.qiji.cps.module.cps.controller.app.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - CPS选品主题 Response VO")
@Data
public class AppCpsMarketingSelectionThemeRespVO {

    @Schema(description = "主题ID")
    private Long id;

    @Schema(description = "主题编码")
    private String themeCode;

    @Schema(description = "主题名称")
    private String themeName;

    @Schema(description = "主题类型")
    private String themeType;

    @Schema(description = "促销事件")
    private String promotionEvent;

    @Schema(description = "平台编码集合")
    private String platformCodes;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "封面图")
    private String coverPic;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "AI摘要")
    private String aiSummary;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
