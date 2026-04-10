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

}
