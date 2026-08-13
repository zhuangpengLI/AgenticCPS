package com.qiji.cps.module.cps.client.jutuike;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JutuikeUnionVendorClientTest {

    @Test
    @DisplayName("聚推客配置只要求 apikey")
    void configSchemaShouldAcceptApiKeyWithoutAppSecret() {
        JutuikeUnionVendorClient client = new JutuikeUnionVendorClient();

        assertTrue(client.getConfigSchema()
                .validate(CpsVendorConfig.builder().appKey("jtk-key").build())
                .isValid());
        assertFalse(client.getConfigSchema().getFields().stream()
                .anyMatch(field -> field.isRequired() && "appSecret".equals(field.getName())));
    }

    @Test
    @DisplayName("fetchActivities maps Jutuike act_list to standard activity page")
    void fetchActivities_mapsJutuikeActivityList() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":[{"act_id":7,"act_name":"美团外卖","desc":"外卖红包","img":"https://img.example/a.jpg","icon":"https://img.example/icon.png","start_date":"2026-01-01","end_date":"2026-12-31","settlement_time":"T+1","cate_name":"美团"}],"total":1}
                """);

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("meituan")
                        .keyword("美团")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                config());

        assertEquals("/union/act_list", client.lastPath);
        assertEquals("美团", client.lastParams.get("cate_name"));
        assertEquals(1L, page.getTotal());
        CpsThirdPartyActivity activity = page.getList().get(0);
        assertEquals("jtk:7", activity.getExternalActivityId());
        assertEquals("美团外卖", activity.getActivityName());
        assertEquals("meituan", activity.getPlatformCode());
        assertEquals("CPS", activity.getBillingType());
        assertEquals("T+1", activity.getExtraFields().get("settlement_time"));
        assertTrue(activity.getSupportsList());
        assertTrue(activity.getSupportsPromotionLink());
        assertTrue(activity.getSupportsOrders());
        assertTrue(activity.getSupportsLocalLife());
        assertTrue(Boolean.TRUE.equals(activity.getExtraFields().get("supportsPromotionLink")));
    }

    @Test
    @DisplayName("fetchActivities classifies online ordering as local life")
    void fetchActivities_classifiesOnlineOrderingAsLocalLife() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":[{"act_id":8,"act_name":"喜茶在线点餐","cate_name":"在线点餐"}],"total":1}
                """);

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(
                CpsThirdPartyActivityRequest.builder().pageNo(1).pageSize(20).build(), config());

        CpsThirdPartyActivity activity = page.getList().get(0);
        assertEquals("local_life", activity.getPlatformCode());
        assertTrue(activity.getSupportsLocalLife());
        assertTrue(Boolean.TRUE.equals(activity.getExtraFields().get("supportsLocalLife")));
    }

    @Test
    @DisplayName("fetchActivities skips Jutuike items without activity id")
    void fetchActivities_skipsItemsWithoutActivityId() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":[{"act_name":"无ID活动","cate_name":"美团"}],"total":1}
                """);

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("meituan")
                        .keyword("美团")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                config());

        assertEquals(0, page.getList().size());
        assertEquals(1L, page.getTotal());
    }

    @Test
    @DisplayName("fetchActivities maps official Jutuike paged act_list response")
    void fetchActivities_mapsOfficialPagedActivityList() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":{"total":63,"per_page":20,"current_page":1,"last_page":4,"data":[{"act_id":45,"act_name":"美团外卖品质商家活动","desc":"美团外卖品质商家红包","img":"https://img.example/meituan.jpg","icon":"https://img.example/meituan.png","start_date":"2026-01-01","end_date":"2026-12-31","settlement_time":"T+1","cate_name":"美团"}]}}
                """);

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("meituan")
                        .keyword("美团")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                config());

        assertEquals(63L, page.getTotal());
        assertEquals(1, page.getList().size());
        CpsThirdPartyActivity activity = page.getList().get(0);
        assertEquals("jtk:45", activity.getExternalActivityId());
        assertEquals("美团外卖品质商家活动", activity.getActivityName());
        assertEquals("meituan", activity.getPlatformCode());
        assertEquals("T+1", activity.getExtraFields().get("settlement_time"));
    }

    @Test
    @DisplayName("generatePromotionLink maps Jutuike union act response to standard promotion link")
    void generatePromotionLink_mapsActivityLink() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":{"url":"https://union.example/act?s=member-1","short_url":"https://s.example/a","sid":"member-1"}}
                """);
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("7");
        request.setExternalId("member-1");

        CpsPromotionLinkResult result = client.generatePromotionLink(request, config());

        assertEquals("/union/act", client.lastPath);
        assertEquals("7", client.lastParams.get("act_id"));
        assertEquals("member-1", client.lastParams.get("sid"));
        assertEquals("https://s.example/a", result.getShortUrl());
        assertEquals("https://union.example/act?s=member-1", result.getLongUrl());
        assertEquals("member-1", result.getExtraFields().get("sid"));
    }

    @Test
    @DisplayName("generatePromotionLink maps official Jutuike h5 and long_h5 fields")
    void generatePromotionLink_mapsOfficialActivityLinkFields() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":{"h5":"https://s.example/h5","long_h5":"https://union.example/long","act_name":"美团外卖","we_app_info":{"app_id":"wx123","page_path":"pages/index"}}}
                """);
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("45");
        request.setExternalId("member-1");

        CpsPromotionLinkResult result = client.generatePromotionLink(request, config());

        assertEquals("https://s.example/h5", result.getShortUrl());
        assertEquals("https://union.example/long", result.getLongUrl());
        assertEquals("https://s.example/h5", result.getMobileUrl());
        assertEquals("美团外卖", result.getExtraFields().get("act_name"));
        assertEquals("{\"app_id\":\"wx123\",\"page_path\":\"pages/index\"}",
                result.getExtraFields().get("we_app_info"));
    }

    @Test
    @DisplayName("queryOrders maps Jutuike union orders to standard order DTO with extension fields")
    void queryOrders_mapsUnionOrders() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":[{"act_name":"美团外卖","act_id":7,"sid":"member-1","jtk_share_rate":"0.1","jtk_share_fee":"1.20","order_sn":"O-1","order_title":"北京烤鸭","create_time":"2026-05-26 10:00:00","pay_time":"2026-05-26 10:01:00","pay_price":"12.30","status":1,"status_desc":"已付款","brand_id":1,"invalid_reason":"","icon":"https://img.example/icon.png"}],"total":1}
                """);
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setStartTime("2026-05-26 10:00:00");
        request.setEndTime("2026-05-26 10:30:00");
        request.setPageNo(1);
        request.setPageSize(20);

        List<CpsOrderDTO> orders = client.queryOrders(request, config());

        assertEquals("/union/orders", client.lastPath);
        assertEquals("2026-05-26 10:00:00", client.lastParams.get("start_time"));
        CpsOrderDTO order = orders.get(0);
        assertEquals("jutuike", order.getVendorCode());
        assertEquals("O-1", order.getPlatformOrderId());
        assertEquals("meituan", order.getPlatformCode());
        assertEquals("7", order.getItemId());
        assertEquals("北京烤鸭", order.getItemTitle());
        assertEquals("10.0", order.getCommissionRate().toPlainString());
        assertEquals("1.20", order.getCommissionAmount().toPlainString());
        assertEquals("member-1", order.getExternalId());
        assertEquals("已付款", order.getExtraFields().get("status_desc"));
    }

    @Test
    @DisplayName("queryOrders maps official Jutuike paged order response")
    void queryOrders_mapsOfficialPagedUnionOrders() throws Exception {
        StubJutuikeUnionVendorClient client = new StubJutuikeUnionVendorClient("""
                {"code":1,"data":{"total":4,"per_page":20,"current_page":1,"last_page":1,"data":[{"act_name":"美团外卖","act_id":7,"sid":"member-1","jtk_share_rate":"0.03","jtk_share_fee":"0.56","order_sn":"85669950292410503","order_title":"正宗安徽板面","create_time":"2026-05-26 10:00:00","pay_time":"2026-05-26 10:01:00","modified_time":"2026-05-26 10:20:00","order_price":"18.80","pay_price":"18.80","status":3,"status_desc":"已结算","brand_id":1}]}}
                """);
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setStartTime("2026-05-26 10:00:00");
        request.setEndTime("2026-05-26 10:30:00");
        request.setPageNo(1);
        request.setPageSize(20);

        List<CpsOrderDTO> orders = client.queryOrders(request, config());

        assertEquals(1, orders.size());
        CpsOrderDTO order = orders.get(0);
        assertEquals("85669950292410503", order.getPlatformOrderId());
        assertEquals("meituan", order.getPlatformCode());
        assertEquals("7", order.getItemId());
        assertEquals("3.00", order.getCommissionRate().toPlainString());
        assertEquals("0.56", order.getCommissionAmount().toPlainString());
        assertEquals("member-1", order.getExternalId());
    }

    private CpsVendorConfig config() {
        return CpsVendorConfig.builder()
                .vendorCode("jutuike")
                .platformCode("meituan")
                .appKey("test-key")
                .apiBaseUrl("http://api.jutuike.com")
                .build();
    }

    private static class StubJutuikeUnionVendorClient extends JutuikeUnionVendorClient {
        private final String responseJson;
        private String lastPath;
        private Map<String, Object> lastParams;

        private StubJutuikeUnionVendorClient(String responseJson) {
            this.responseJson = responseJson;
        }

        @Override
        protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
            try {
                this.lastPath = path;
                this.lastParams = params;
                return objectMapper.readTree(responseJson);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
