package cn.iocoder.yudao.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户 APP - 转链结果 Response VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 转链结果 Response VO")
@Data
public class AppCpsLinkRespVO {

    @Schema(description = "推广短链接（优先使用）")
    private String shortUrl;

    @Schema(description = "推广长链接")
    private String longUrl;

    @Schema(description = "淘口令（淘宝专用）")
    private String tpwd;

    @Schema(description = "移动端链接（拼多多专用）")
    private String mobileUrl;

    @Schema(description = "券后价（元）")
    private BigDecimal actualPrice;

    @Schema(description = "佣金比例（百分比）")
    private BigDecimal commissionRate;

    @Schema(description = "预估返利金额（元，已计算用户返利比例）")
    private BigDecimal estimateRebateAmount;

    @Schema(description = "优惠券信息")
    private String couponInfo;

}
