package com.qiji.cps.module.cps.controller.admin.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CPS商品广场转链 Response VO")
@Data
@Builder
public class CpsGoodsSquareLinkRespVO {

    @Schema(description = "转链状态 SUCCESS/FAILED")
    private String linkStatus;

    @Schema(description = "转链消息")
    private String linkMessage;

    @Schema(description = "转链记录ID")
    private Long transferRecordId;

    @Schema(description = "实际推广位ID")
    private String adzoneId;

    @Schema(description = "推广短链")
    private String shortUrl;

    @Schema(description = "推广长链")
    private String longUrl;

    @Schema(description = "淘口令")
    private String tpwd;

    @Schema(description = "移动端链接")
    private String mobileUrl;

    @Schema(description = "券后价")
    private BigDecimal actualPrice;

    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;

    @Schema(description = "预估佣金")
    private BigDecimal commissionAmount;

    @Schema(description = "优惠券信息")
    private String couponInfo;

    @Schema(description = "可复制推广内容")
    private String promotionContent;

}
