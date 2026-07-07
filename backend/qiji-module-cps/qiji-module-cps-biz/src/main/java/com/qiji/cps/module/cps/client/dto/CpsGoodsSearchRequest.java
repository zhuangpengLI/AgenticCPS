package com.qiji.cps.module.cps.client.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * CPS 商品搜索请求 DTO（平台无关）
 *
 * @author CPS System
 */
@Data
public class CpsGoodsSearchRequest {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 搜索模式：keyword/dataoke_image
     */
    private String searchMode;

    /**
     * 搜索字段模式：title/title_content/goods_id
     */
    private String searchField;

    /**
     * 图片搜索 BASE64，不包含 data:image 前缀
     */
    private String imageBase64;

    /**
     * 页码（从1开始）
     */
    private Integer pageNo = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 最低价格（券后价，元）
     */
    private BigDecimal priceLowerLimit;

    /**
     * 最高价格（券后价，元）
     */
    private BigDecimal priceUpperLimit;

    /**
     * 排序方式（0-综合，1-销量，2-价格升序，3-价格降序，4-佣金比例）
     */
    private Integer sortType = 0;

    /**
     * 是否只返回有券商品（1-是，0-全部）
     */
    private Integer hasCoupon;

    /**
     * 推广位ID（用于生成追踪链接，可选）
     */
    private String adzoneId;

    /**
     * 渠道/用户外部标识（用于订单归因，可选）
     */
    private String externalId;

    /**
     * 选品频道编码（淘系选品库专用）
     */
    private String channelCode;

    /**
     * 供应商类目 ID（淘系选品库专用）
     */
    private String categoryId;

    /**
     * 最低佣金比例（百分比）
     */
    private BigDecimal minCommissionRate;

    /**
     * 最低预估佣金金额（元）
     */
    private BigDecimal minCommissionAmount;

    /**
     * 最低月销量
     */
    private Long minMonthSales;

    /**
     * 最低优惠券金额（元）
     */
    private BigDecimal couponAmountMin;

    /**
     * 最高优惠券金额（元）
     */
    private BigDecimal couponPriceUpperLimit;

    /**
     * 热销排名最小值，用于页面筛选透传
     */
    private Long hotRankMin;

    /**
     * 券到期天数上限
     */
    private Integer couponExpireDays;

    /**
     * 是否只看天猫
     */
    private Boolean tmallOnly;

    /**
     * 是否只看品牌商品
     */
    private Boolean brandOnly;

    /**
     * 是否只看海淘商品
     */
    private Boolean haitaoOnly;

    /**
     * 是否只看金牌卖家
     */
    private Boolean goldSellerOnly;

    /**
     * 是否只看天猫超市
     */
    private Boolean tchaoshiOnly;

    /**
     * 是否只看聚划算
     */
    private Boolean juhuasuanOnly;

    /**
     * 是否只看淘抢购
     */
    private Boolean taoqianggouOnly;

    /**
     * 是否只看验货商品
     */
    private Boolean inspectedGoodsOnly;

    /**
     * 是否偏远地区包邮
     */
    private Boolean freeshipRemoteDistrict;

    /**
     * 店铺类型
     */
    private String shopType;

    /**
     * 商品表现筛选：hot/daily/two_hours/coupon/new
     */
    private String goodsPerformance;

    /**
     * 是否只看商单
     */
    private Boolean commercialOnly;

    /**
     * 是否只看预告
     */
    private Boolean preSaleOnly;

    /**
     * 活动标签
     */
    private String activityTag;

}
