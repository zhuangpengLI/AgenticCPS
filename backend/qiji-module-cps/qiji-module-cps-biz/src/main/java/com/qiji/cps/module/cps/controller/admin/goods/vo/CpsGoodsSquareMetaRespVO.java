package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - CPS商品广场选品元数据 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsSquareMetaRespVO {

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "元数据来源")
    private String metaSource;

    @Schema(description = "是否使用供应商真实元数据")
    private Boolean usingVendorMeta;

    @Schema(description = "是否支持淘宝选品模式")
    private Boolean taobaoSelectionSupported;

    @Schema(description = "能力说明")
    private String capabilityDesc;

    @Schema(description = "活动入口")
    private List<CpsGoodsSquareMetaItemRespVO> activities;

    @Schema(description = "推荐热词")
    private List<CpsGoodsSquareMetaItemRespVO> hotKeywords;

    @Schema(description = "热门类目")
    private List<CpsGoodsSquareMetaItemRespVO> categories;

    @Schema(description = "排序项")
    private List<CpsGoodsSquareMetaItemRespVO> sortOptions;

    @Schema(description = "筛选项")
    private List<CpsGoodsSquareMetaItemRespVO> filterOptions;

}
