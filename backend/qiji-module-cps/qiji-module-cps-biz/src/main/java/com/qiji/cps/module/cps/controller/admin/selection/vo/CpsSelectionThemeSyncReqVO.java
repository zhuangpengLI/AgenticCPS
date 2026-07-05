package com.qiji.cps.module.cps.controller.admin.selection.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Schema(description = "管理后台 - CPS选品主题供应商同步 Request VO")
@Data
public class CpsSelectionThemeSyncReqVO {

    @Schema(description = "供应商编码，默认 dataoke", example = "haodanku")
    private String vendorCode;

    @Schema(description = "选品库同步源编码，默认 SCENE_PALLET", example = "SCENE_PALLET")
    private String sourceCode;

    @Schema(description = "主题名前缀，最终主题名为 前缀_列表主题名", example = "爆品商品")
    private String themeNamePrefix;

    @Schema(description = "主题列表请求 URL 或路径", example = "/open-api/scene-pallet")
    private String themeListUrl;

    @Schema(description = "主题列表请求可选参数 JSON", example = "{\"version\":\"v1.0.0\"}")
    private String themeListParamsJson;

    @Schema(description = "主题商品列表请求 URL 或路径", example = "/open-api/goods/scene-pallet")
    private String goodsListUrl;

    @Schema(description = "主题商品列表请求可选参数 JSON", example = "{\"sortType\":4}")
    private String goodsListParamsJson;

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
