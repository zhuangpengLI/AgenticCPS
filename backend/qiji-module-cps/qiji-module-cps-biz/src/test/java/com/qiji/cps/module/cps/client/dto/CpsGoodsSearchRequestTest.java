package com.qiji.cps.module.cps.client.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpsGoodsSearchRequestTest {

    @Test
    void copyForPage_copiesEverySearchFieldAndOverridesOnlyPagination() {
        CpsGoodsSearchRequest original = fullyPopulatedRequest();
        CpsGoodsSearchRequest expected = fullyPopulatedRequest();
        expected.setPageNo(2);
        expected.setPageSize(10);

        CpsGoodsSearchRequest copy = original.copyForPage(2, 10);

        assertEquals(expected, copy);
        assertEquals(7, original.getPageNo());
        assertEquals(70, original.getPageSize());
    }

    private CpsGoodsSearchRequest fullyPopulatedRequest() {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("coffee");
        request.setSearchMode("dataoke_image");
        request.setSearchField("title_content");
        request.setImageBase64("image-base64");
        request.setPageNo(7);
        request.setPageSize(70);
        request.setPriceLowerLimit(new BigDecimal("10.01"));
        request.setPriceUpperLimit(new BigDecimal("99.99"));
        request.setSortType(4);
        request.setHasCoupon(1);
        request.setAdzoneId("adzone-1");
        request.setExternalId("external-1");
        request.setChannelCode("channel-1");
        request.setCategoryId("category-1");
        request.setMinCommissionRate(new BigDecimal("12.34"));
        request.setMinCommissionAmount(new BigDecimal("5.67"));
        request.setMinMonthSales(1234L);
        request.setCouponAmountMin(new BigDecimal("3.21"));
        request.setCouponPriceUpperLimit(new BigDecimal("88.88"));
        request.setHotRankMin(9L);
        request.setCouponExpireDays(6);
        request.setTmallOnly(true);
        request.setBrandOnly(true);
        request.setHaitaoOnly(true);
        request.setGoldSellerOnly(true);
        request.setTchaoshiOnly(true);
        request.setJuhuasuanOnly(true);
        request.setTaoqianggouOnly(true);
        request.setInspectedGoodsOnly(true);
        request.setFreeshipRemoteDistrict(true);
        request.setShopType("tmall");
        request.setGoodsPerformance("hot");
        request.setCommercialOnly(true);
        request.setPreSaleOnly(true);
        request.setActivityTag("618");
        return request;
    }
}
