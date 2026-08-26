package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS选品主题 Response VO")
@Data
public class CpsSelectionThemeRespVO {

    private Long id;
    private String themeCode;
    private String themeName;
    private String themeType;
    private String promotionEvent;
    private String platformCodes;
    private String vendorCode;
    private String coverPic;
    private String description;
    private String tags;
    private String ruleJson;
    private String aiPrompt;
    private String aiSummary;
    private String status;
    private Integer goodsSquareVisible;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String refreshStatus;
    private LocalDateTime lastRefreshTime;
    private String refreshMessage;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
