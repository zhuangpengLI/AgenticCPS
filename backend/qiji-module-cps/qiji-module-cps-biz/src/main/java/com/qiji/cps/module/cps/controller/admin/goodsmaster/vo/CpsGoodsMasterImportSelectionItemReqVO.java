package com.qiji.cps.module.cps.controller.admin.goodsmaster.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS商品主档导入选品快照 Request VO")
@Data
public class CpsGoodsMasterImportSelectionItemReqVO {

    @Schema(description = "选品主题商品快照ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "选品主题商品快照ID不能为空")
    private Long selectionItemId;
}
