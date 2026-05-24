package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - CPS选品主题操作 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsSelectionThemeOperationRespVO {

    private Long themeId;
    private String status;
    private Integer pulledCount;
    private Integer importedCount;
    private String message;
}
