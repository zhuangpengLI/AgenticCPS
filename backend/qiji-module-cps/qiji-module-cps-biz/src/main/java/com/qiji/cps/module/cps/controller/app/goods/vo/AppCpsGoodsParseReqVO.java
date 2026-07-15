package com.qiji.cps.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户 APP - CPS 商品内容解析 Request VO")
@Data
public class AppCpsGoodsParseReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "原始商品链接、商品ID或口令", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始内容不能为空")
    private String originalContent;

}
