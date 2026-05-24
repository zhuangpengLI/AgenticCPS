package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS商品广场转链 Request VO")
@Data
public class CpsGoodsSquareLinkReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品ID不能为空")
    private String goodsId;

    @Schema(description = "拼多多 goodsSign")
    private String goodsSign;

    @Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    @Schema(description = "推广位ID")
    private String adzoneId;

    @Schema(description = "供应商编码")
    private String vendorCode;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "原始链接或口令")
    private String originalContent;

}
