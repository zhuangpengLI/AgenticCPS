package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CPS选品主题统计 Response VO")
@Data
@Builder
public class CpsSelectionThemeStatsRespVO {

    @Schema(description = "主题总数")
    private Long total;

    @Schema(description = "草稿数量")
    private Long draft;

    @Schema(description = "已发布数量")
    private Long published;

    @Schema(description = "已下线数量")
    private Long offline;
}
