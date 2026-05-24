package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS选品主题第三方拉取 Request VO")
@Data
public class CpsSelectionThemeVendorPullReqVO {

    @NotNull(message = "主题ID不能为空")
    private Long themeId;

    @Schema(description = "临时覆盖规则 JSON")
    private String ruleJson;
}
