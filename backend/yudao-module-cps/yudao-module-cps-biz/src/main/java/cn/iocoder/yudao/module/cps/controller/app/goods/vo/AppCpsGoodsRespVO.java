package cn.iocoder.yudao.module.cps.controller.app.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户 APP - 商品信息 Response VO
 *
 * @author CPS System
 */
@Schema(description = "用户 APP - 商品信息 Response VO")
@Data
public class AppCpsGoodsRespVO {

    @Schema(description = "商品ID（平台原始ID）")
    private String goodsId;

    @Schema(description = "商品goodsSign（拼多多转链需要）")
    private String goodsSign;

    @Schema(description = "平台编码")
    private String platformCode;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "商品主图")
    private String mainPic;

    @Schema(description = "商品原价（元）")
    private BigDecimal originalPrice;

    @Schema(description = "券后价（元）")
    private BigDecimal actualPrice;

    @Schema(description = "优惠券金额（元）")
    private BigDecimal couponPrice;

    @Schema(description = "预估返利金额（元，已计算用户返利比例）")
    private BigDecimal estimateRebateAmount;

    @Schema(description = "佣金比例（百分比，如10.5%）")
    private BigDecimal commissionRate;

    @Schema(description = "30天销量")
    private Long monthSales;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺类型（1-天猫/自营，0-普通）")
    private Integer shopType;

    @Schema(description = "商品原始链接")
    private String itemLink;

    @Schema(description = "品牌名称")
    private String brandName;

}
