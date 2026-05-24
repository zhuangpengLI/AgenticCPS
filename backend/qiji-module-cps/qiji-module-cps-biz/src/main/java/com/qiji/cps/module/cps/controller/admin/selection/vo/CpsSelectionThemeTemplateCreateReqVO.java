package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - CPS选品大促模板创建 Request VO")
@Data
public class CpsSelectionThemeTemplateCreateReqVO {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "主题编码，不填自动生成")
    private String themeCode;
}
