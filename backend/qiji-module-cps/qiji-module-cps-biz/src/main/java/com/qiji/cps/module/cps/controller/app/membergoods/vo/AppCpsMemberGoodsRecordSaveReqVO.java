package com.qiji.cps.module.cps.controller.app.membergoods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Schema(description = "用户 APP - CPS 商品展示快照保存 Request VO")
@Data
public class AppCpsMemberGoodsRecordSaveReqVO {

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

    @Schema(description = "商品标题")
    @Size(max = 512, message = "商品标题长度不能超过 512")
    private String title;

    @Schema(description = "商品主图")
    @Size(max = 1024, message = "商品主图长度不能超过 1024")
    private String mainPic;

    @Schema(description = "商品原价（元）")
    @DecimalMin(value = "0", message = "商品原价不能小于 0")
    private BigDecimal originalPrice;

    @Schema(description = "券后价（元）")
    @DecimalMin(value = "0", message = "券后价不能小于 0")
    private BigDecimal actualPrice;

    @Schema(description = "优惠券金额（元）")
    @DecimalMin(value = "0", message = "优惠券金额不能小于 0")
    private BigDecimal couponPrice;

    @Schema(description = "预估返利金额（元）")
    @DecimalMin(value = "0", message = "预估返利金额不能小于 0")
    private BigDecimal estimateRebateAmount;

    @Schema(description = "近 30 天销量")
    private Long monthSales;

    @Schema(description = "店铺名称")
    @Size(max = 255, message = "店铺名称长度不能超过 255")
    private String shopName;

    @AssertTrue(message = "商品 ID 和商品签名不能同时为空")
    public boolean isGoodsIdentityPresent() {
        return StringUtils.hasText(goodsId) || StringUtils.hasText(goodsSign);
    }
}
