package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CPS 商品信息 DTO（平台无关，搜索结果的单条商品）
 *
 * @author CPS System
 */
@Data
@Builder
public class CpsGoodsItem {

    /**
     * 平台商品ID
     */
    private String goodsId;

    /**
     * 平台标识（taobao/jd/pdd/douyin）
     */
    private String platformCode;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品主图
     */
    private String mainPic;

    /**
     * 商品原价（元）
     */
    private BigDecimal originalPrice;

    /**
     * 券后价（元）
     */
    private BigDecimal actualPrice;

    /**
     * 优惠券金额（元）
     */
    private BigDecimal couponPrice;

    /**
     * 佣金比例（百分比，如10.5表示10.5%）
     */
    private BigDecimal commissionRate;

    /**
     * 预估佣金金额（元）
     */
    private BigDecimal commissionAmount;

    /**
     * 30天销量
     */
    private Long monthSales;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺类型（1-天猫/自营，0-普通商家）
     */
    private Integer shopType;

    /**
     * 商品详情页链接（原始链接）
     */
    private String itemLink;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 商品goodsSign（拼多多专用）
     */
    private String goodsSign;

}
