package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - 商品返利查询 Response VO.
 *
 * @author CPS System
 */
@Schema(description = "管理后台 - 商品返利查询 Response VO")
@Data
public class CpsGoodsRebateQueryRespVO {

    @Schema(description = "解析状态：SUCCESS/PARSE_FAILED/LINK_FAILED")
    private String parseStatus;

    @Schema(description = "解析或转链提示")
    private String parseMessage;

    @Schema(description = "商品信息")
    private Goods goods;

    @Schema(description = "返利信息")
    private Rebate rebate;

    @Schema(description = "可复制链接信息")
    private Links links;

    @Schema(description = "转链记录ID")
    private Long transferRecordId;

    @Schema(description = "商品信息")
    @Data
    public static class Goods {

        @Schema(description = "平台编码")
        private String platformCode;

        @Schema(description = "商品ID")
        private String goodsId;

        @Schema(description = "商品goodsSign")
        private String goodsSign;

        @Schema(description = "商品原始链接")
        private String itemLink;

        @Schema(description = "商品标题")
        private String title;

        @Schema(description = "商品主图")
        private String mainPic;

        @Schema(description = "店铺名称")
        private String shopName;

        @Schema(description = "券后价")
        private BigDecimal actualPrice;

        @Schema(description = "优惠券信息")
        private String couponInfo;

    }

    @Schema(description = "返利信息")
    @Data
    public static class Rebate {

        @Schema(description = "佣金比例")
        private BigDecimal commissionRate;

        @Schema(description = "预估佣金")
        private BigDecimal commissionAmount;

        @Schema(description = "会员预估返利金额")
        private BigDecimal estimateRebateAmount;

        @Schema(description = "实际使用推广位ID")
        private String usedAdzoneId;

        @Schema(description = "实际使用API供应商编码")
        private String usedVendorCode;

    }

    @Schema(description = "可复制链接信息")
    @Data
    public static class Links {

        @Schema(description = "推广短链接")
        private String shortUrl;

        @Schema(description = "推广长链接")
        private String longUrl;

        @Schema(description = "淘口令")
        private String tpwd;

        @Schema(description = "移动端链接")
        private String mobileUrl;

    }

}
