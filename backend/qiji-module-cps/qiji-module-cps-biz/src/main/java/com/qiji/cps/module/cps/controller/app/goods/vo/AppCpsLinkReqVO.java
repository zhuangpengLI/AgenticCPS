package com.qiji.cps.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 用户 APP - 转链 Request VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 转链 Request VO")
@Data
public class AppCpsLinkReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    @Schema(description = "商品ID（淘宝/京东必填，拼多多传 goodsSign 时可不填）")
    private String goodsId;

    @Schema(description = "商品goodsSign（拼多多必填，传此字段时 goodsId 可不填）")
    private String goodsSign;

    @Schema(description = "推广位ID（不传则使用平台/会员默认推广位）")
    private String adzoneId;

    /**
     * goodsId 和 goodsSign 至少一个非空
     */
    @AssertTrue(message = "商品ID和goodsSign至少填写一个")
    @Schema(hidden = true)
    public boolean isGoodsIdentifierValid() {
        return StringUtils.hasText(goodsId) || StringUtils.hasText(goodsSign);
    }

}
