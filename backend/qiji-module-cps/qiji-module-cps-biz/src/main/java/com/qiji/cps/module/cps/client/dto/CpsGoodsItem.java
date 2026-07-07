package com.qiji.cps.module.cps.client.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

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
     * 优惠券使用门槛（元）
     */
    private BigDecimal couponConditions;

    /**
     * 优惠券总量
     */
    private Long couponTotalNum;

    /**
     * 优惠券剩余量
     */
    private Long couponRemainNum;

    /**
     * 优惠券领取量
     */
    private Long couponReceiveNum;

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

    /**
     * 实际使用的 API 供应商编码
     */
    private String vendorCode;

    /**
     * 商品来源，如好单库精选/联盟超级搜
     */
    private String source;

    /**
     * 活动标签
     */
    private String activityTag;

    /**
     * 类目名称
     */
    private String categoryName;

    /**
     * 优惠券结束时间
     */
    private String couponEndTime;

    /**
     * 优惠券开始时间
     */
    private String couponStartTime;

    /**
     * 榜单或热销标签
     */
    private String rankTag;

    /**
     * 商品卖点文案
     */
    private String sellingPoint;

    /**
     * 供应商特有扩展字段，保留标准对象未覆盖的信息。
     */
    private Map<String, Object> extraFields;

    /**
     * 供应商原始响应片段，便于排查真实接口差异。
     */
    private String rawPayload;

}
