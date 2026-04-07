package cn.iocoder.yudao.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品ID不能为空")
    private String goodsId;

    @Schema(description = "商品goodsSign（拼多多必填）")
    private String goodsSign;

    @Schema(description = "推广位ID（不传则使用平台/会员默认推广位）")
    private String adzoneId;

}
