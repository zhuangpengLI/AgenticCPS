package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - CPS选品大促模板 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsSelectionThemeTemplateRespVO {

    private String templateCode;
    private String themeName;
    private String promotionEvent;
    private String description;
    private String tags;
    private String ruleJson;
    private String aiPrompt;
}
