package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - CPS商品广场选品元数据项 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsGoodsSquareMetaItemRespVO {

    @Schema(description = "选项值")
    private String value;

    @Schema(description = "展示名称")
    private String label;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "图片地址")
    private String imageUrl;

    @Schema(description = "描述")
    private String description;

}
