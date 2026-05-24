package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - CPS商品广场搜索 Response VO")
@Data
@Builder
public class CpsGoodsSquareSearchRespVO {

    @Schema(description = "商品列表")
    private List<CpsGoodsSquareGoodsRespVO> list;

    @Schema(description = "总数；平台不支持时返回当前页数量")
    private Long total;

    @Schema(description = "下一页游标")
    private String nextPageId;

    @Schema(description = "页码")
    private Integer pageNo;

    @Schema(description = "每页大小")
    private Integer pageSize;

}
