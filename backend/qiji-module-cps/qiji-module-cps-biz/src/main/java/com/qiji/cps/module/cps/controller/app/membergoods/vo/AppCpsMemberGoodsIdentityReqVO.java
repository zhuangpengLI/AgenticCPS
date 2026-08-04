package com.qiji.cps.module.cps.controller.app.membergoods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

@Schema(description = "用户 APP - CPS 商品身份 Request VO")
@Data
public class AppCpsMemberGoodsIdentityReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    @Size(max = 32, message = "平台编码长度不能超过 32")
    private String platformCode;

    @Schema(description = "平台商品 ID")
    @Size(max = 128, message = "商品 ID 长度不能超过 128")
    private String goodsId;

    @Schema(description = "平台商品签名")
    @Size(max = 512, message = "商品签名长度不能超过 512")
    private String goodsSign;

    @AssertTrue(message = "商品 ID 和商品签名不能同时为空")
    public boolean isGoodsIdentityPresent() {
        return StringUtils.hasText(goodsId) || StringUtils.hasText(goodsSign);
    }
}
