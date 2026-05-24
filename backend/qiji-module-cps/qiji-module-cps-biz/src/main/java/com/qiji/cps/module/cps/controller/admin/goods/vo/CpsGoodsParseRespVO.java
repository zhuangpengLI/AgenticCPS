package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CPS商品内容解析 Response VO")
@Data
@Builder
public class CpsGoodsParseRespVO {

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "是否解析成功或内容是否支持")
    private Boolean supported;

    @Schema(description = "平台商品ID")
    private String goodsId;

    @Schema(description = "平台商品goodsSign")
    private String goodsSign;

    @Schema(description = "商品原始链接")
    private String itemLink;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "解析来源 local/platform")
    private String parseSource;

    @Schema(description = "失败编码")
    private String failureCode;

    @Schema(description = "失败原因")
    private String failureReason;

}
