package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AbstractHdkVendorClient} 好单库适配逻辑单元测试。
 *
 * @author CPS System
 */
class AbstractHdkVendorClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static class TestHdkVendorClient extends AbstractHdkVendorClient {
        @Override public String getPlatformCode() { return "taobao"; }
        @Override protected String getSearchApiPath() { return "/test/search"; }
        @Override protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected CpsGoodsSearchResult parseSearchResponse(JsonNode responseRoot, CpsGoodsSearchRequest request) {
            return null;
        }
        @Override protected String getPromotionLinkApiPath() { return "/test/link"; }
        @Override protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot) {
            return null;
        }
        @Override protected String getOrderQueryApiPath() { return "/test/order"; }
        @Override protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected java.util.List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot) {
            return Collections.emptyList();
        }
        @Override protected String getTestConnectionApiPath() { return "/test/conn"; }
        @Override protected Map<String, Object> buildTestConnectionParams() { return new HashMap<>(); }
    }

    private final TestHdkVendorClient client = new TestHdkVendorClient();

    @Test
    @DisplayName("供应商编码应返回 haodanku")
    void testGetVendorCode() {
        assertEquals("haodanku", client.getVendorCode());
    }

    @Test
    @DisplayName("好单库鉴权应注入 apikey 且不生成签名")
    void testInjectSignParams() {
        CpsVendorConfig config = CpsVendorConfig.builder().appKey("hdk-key").build();
        Map<String, Object> params = new HashMap<>();

        Map<String, String> signContext = client.computeSignContext(params, config);
        client.injectSignParams(params, config, signContext);

        assertTrue(signContext.isEmpty());
        assertEquals("hdk-key", params.get("apikey"));
    }

    @Test
    @DisplayName("好单库成功码应兼容 code=1 和 code=200")
    void testIsSuccessResponse() throws Exception {
        assertTrue(client.isSuccessResponse(OBJECT_MAPPER.readTree("{\"code\":1}")));
        assertTrue(client.isSuccessResponse(OBJECT_MAPPER.readTree("{\"code\":200}")));
        assertFalse(client.isSuccessResponse(OBJECT_MAPPER.readTree("{\"code\":0}")));
        assertFalse(client.isSuccessResponse(null));
    }

    @Test
    @DisplayName("转链域名应从 v2 自动切换到 v3")
    void testPromotionLinkBaseUrl() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .apiBaseUrl("http://v2.api.haodanku.com")
                .build();

        assertEquals("http://v3.api.haodanku.com", client.getPromotionLinkBaseUrl(config));
    }

    @Test
    @DisplayName("京东搜索参数应匹配好单库 jd_goods_search")
    void testJdSearchParams() {
        HdkJdVendorClient jdClient = new HdkJdVendorClient();
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("手机");
        request.setPageNo(2);
        request.setPageSize(10);
        request.setSortType(4);
        request.setHasCoupon(1);
        CpsVendorConfig config = CpsVendorConfig.builder().authToken("jd-user").build();

        Map<String, Object> params = jdClient.buildSearchParams(request, config);

        assertEquals("jd-user", params.get("jd_user_id"));
        assertEquals("%E6%89%8B%E6%9C%BA", params.get("keyword"));
        assertEquals(2, params.get("min_id"));
        assertEquals(10, params.get("back"));
        assertEquals(1, params.get("has_coupon"));
        assertEquals(6, params.get("sort"));
    }

    @Test
    @DisplayName("京东转链应使用 unify_jditems_link 并解析根节点返回字段")
    void testJdPromotionLink() throws Exception {
        HdkJdVendorClient jdClient = new HdkJdVendorClient();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("encrypted_item");
        request.setChannelId("channel_1");

        Map<String, Object> params = jdClient.buildPromotionLinkParams(request, CpsVendorConfig.builder().build());
        CpsPromotionLinkResult result = jdClient.parsePromotionLinkResponse(
                OBJECT_MAPPER.readTree("{\"code\":200,\"shortURL\":\"https://u.jd.com/a\",\"clickURL\":\"https://union-click.jd.com/b\"}"));

        assertEquals("/unify_jditems_link", jdClient.getPromotionLinkApiPath());
        assertEquals("encrypted_item", params.get("material_id"));
        assertEquals("channel_1", params.get("subUnionId"));
        assertEquals("https://u.jd.com/a", result.getShortUrl());
        assertEquals("https://union-click.jd.com/b", result.getLongUrl());
    }

    @Test
    @DisplayName("拼多多搜索和转链参数应匹配好单库 unify 接口")
    void testPddParams() {
        HdkPddVendorClient pddClient = new HdkPddVendorClient();
        CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
        searchRequest.setKeyword("芝麻糊");
        searchRequest.setPageNo(3);
        searchRequest.setPageSize(20);
        searchRequest.setSortType(1);

        Map<String, Object> searchParams = pddClient.buildSearchParams(searchRequest, CpsVendorConfig.builder().build());

        CpsPromotionLinkRequest linkRequest = new CpsPromotionLinkRequest();
        linkRequest.setGoodsSign("pdd-goods-sign");
        linkRequest.setExternalId("user_1");
        Map<String, Object> linkParams = pddClient.buildPromotionLinkParams(linkRequest, CpsVendorConfig.builder().build());

        assertEquals("%E8%8A%9D%E9%BA%BB%E7%B3%8A", searchParams.get("keyword"));
        assertEquals(3, searchParams.get("min_id"));
        assertEquals(20, searchParams.get("limit"));
        assertEquals(2, searchParams.get("sort"));
        assertEquals("/unify_pdditems_link", pddClient.getPromotionLinkApiPath());
        assertEquals("pdd-goods-sign", linkParams.get("itemid"));
        assertEquals("user_1", linkParams.get("channel"));
    }

    @Test
    @DisplayName("JD/PDD 订单查询应使用 min_id/back/date_type/state/start_date/end_date")
    void testUnifyOrderParams() {
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setPositionIndex("99");
        request.setPageSize(10);
        request.setQueryType(2);
        request.setOrderStatus(1);
        request.setStartTime("2024-01-01 00:00:00");
        request.setEndTime("2024-01-01 01:00:00");

        Map<String, Object> jdParams = new HdkJdVendorClient()
                .buildOrderQueryParams(request, CpsVendorConfig.builder().build());
        Map<String, Object> pddParams = new HdkPddVendorClient()
                .buildOrderQueryParams(request, CpsVendorConfig.builder().build());

        assertEquals("/unify_jd_order_list", new HdkJdVendorClient().getOrderQueryApiPath());
        assertEquals("/unify_pdd_order_list", new HdkPddVendorClient().getOrderQueryApiPath());
        assertEquals("99", jdParams.get("min_id"));
        assertEquals(10, pddParams.get("back"));
        assertEquals(2, jdParams.get("date_type"));
        assertEquals(1, pddParams.get("state"));
        assertEquals("1704038400", jdParams.get("start_date"));
        assertEquals("1704042000", pddParams.get("end_date"));
    }
}
