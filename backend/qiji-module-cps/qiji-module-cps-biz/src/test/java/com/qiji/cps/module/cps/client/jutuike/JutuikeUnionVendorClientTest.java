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

class JutuikeUnionVendorClientTest {

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
        assertEquals("test-key", client.lastParams.get("apikey"));
        assertEquals("美团", client.lastParams.get("cate_name"));
        assertEquals(1L, page.getTotal());
        CpsThirdPartyActivity activity = page.getList().get(0);
        assertEquals("jtk:7", activity.getExternalActivityId());
        assertEquals("美团外卖", activity.getActivityName());
        assertEquals("meituan", activity.getPlatformCode());
        assertEquals("CPS", activity.getBillingType());
        assertEquals("T+1", activity.getExtraFields().get("settlement_time"));
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
        assertEquals("O-1", order.getPlatformOrderId());
        assertEquals("meituan", order.getPlatformCode());
        assertEquals("7", order.getItemId());
        assertEquals("北京烤鸭", order.getItemTitle());
        assertEquals("10.0", order.getCommissionRate().toPlainString());
        assertEquals("1.20", order.getCommissionAmount().toPlainString());
        assertEquals("member-1", order.getExternalId());
        assertEquals("已付款", order.getExtraFields().get("status_desc"));
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
