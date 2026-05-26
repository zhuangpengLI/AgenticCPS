package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CPS工具箱优惠券查询 Response VO")
@Data
@Builder
public class CpsGoodsCouponQueryRespVO {

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "实际搜索关键词")
    private String keyword;

    @Schema(description = "商品列表")
    private List<CpsGoodsSquareGoodsRespVO> list;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "页码")
    private Integer pageNo;

    @Schema(description = "每页大小")
    private Integer pageSize;

    @Schema(description = "摘要文案")
    private String summary;

}
