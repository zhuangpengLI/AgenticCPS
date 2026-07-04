package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Schema(description = "管理后台 - CPS选品主题供应商同步 Request VO")
@Data
public class CpsSelectionThemeSyncReqVO {

    @Schema(description = "关键词过滤")
    private String keyword;

    @Schema(description = "同步页数", example = "1")
    @Min(value = 1, message = "同步页数最小值为 1")
    @Max(value = 20, message = "同步页数最大值为 20")
    private Integer maxPages;

    @Schema(description = "每页主题数", example = "20")
    @Min(value = 1, message = "每页主题数最小值为 1")
    @Max(value = 100, message = "每页主题数最大值为 100")
    private Integer pageSize;

    @Schema(description = "是否同步主题下商品", example = "true")
    private Boolean syncGoods;

    @Schema(description = "每个主题同步商品数", example = "20")
    @Min(value = 1, message = "每个主题同步商品数最小值为 1")
    @Max(value = 100, message = "每个主题同步商品数最大值为 100")
    private Integer goodsPullCount;

}
