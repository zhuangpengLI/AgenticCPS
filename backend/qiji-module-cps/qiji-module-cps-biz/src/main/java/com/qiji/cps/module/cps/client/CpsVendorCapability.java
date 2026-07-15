package com.qiji.cps.module.cps.client;

import lombok.Getter;

@Getter
public enum CpsVendorCapability {

    GOODS_SEARCH("goods_search"),
    CONTENT_PARSE("content_parse"),
    PROMOTION_LINK("promotion_link"),
    COUPON_QUERY("coupon_query"),
    ORDER_QUERY("order_query"),
    ACTIVITY_PULL("activity_pull"),
    IMAGE_SEARCH("image_search"),
    SELECTION_LIBRARY("selection_library"),
    CONNECTION_TEST("connection_test");

    private final String code;

    CpsVendorCapability(String code) {
        this.code = code;
    }
}
