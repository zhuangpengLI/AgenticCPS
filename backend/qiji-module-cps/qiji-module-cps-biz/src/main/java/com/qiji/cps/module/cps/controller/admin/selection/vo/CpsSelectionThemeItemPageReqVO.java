package com.qiji.cps.module.cps.controller.admin.selection.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CPS选品主题商品快照分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CpsSelectionThemeItemPageReqVO extends PageParam {

    @Schema(description = "主题ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主题ID不能为空")
    private Long themeId;
}
