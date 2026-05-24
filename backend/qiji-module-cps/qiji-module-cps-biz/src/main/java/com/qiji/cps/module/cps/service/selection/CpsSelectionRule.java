package com.qiji.cps.module.cps.service.selection;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 选品主题规则 JSON 的固定结构。
 */
@Data
public class CpsSelectionRule {

    private List<String> keywords;
    private List<String> platforms;
    private String vendorCode;
    private BigDecimal priceLowerLimit;
    private BigDecimal priceUpperLimit;
    private BigDecimal minCommissionRate;
    private BigDecimal minCommissionAmount;
    private Long minMonthSales;
    private BigDecimal couponAmountMin;
    private Boolean onlyCoupon;
    private String categoryId;
    private String channelCode;
    private List<String> activityTags;
    private Integer sortType;
    private String sortBy;
    private Integer pullCount;
    private Map<String, BigDecimal> platformWeights;
}
