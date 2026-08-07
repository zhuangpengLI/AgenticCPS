package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    @DisplayName("好单库详细转链模式应返回京东账号未授权原因")
    void promotionLinkDetailModePreservesAuthorizationFailure() throws Exception {
        JsonNode rejection = OBJECT_MAPPER.readTree("""
                {"code":500,"msg":"未开通京东官方账号"}
                """);
        TestHdkVendorClient failingClient = new TestHdkVendorClient() {
            @Override
            protected JsonNode executePostRequest(String fullUrl, Map<String, Object> params,
                                                  CpsVendorConfig config) {
                return rejection;
            }
        };
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("encrypted-item");
        request.setPropagateVendorError(true);

        CpsVendorException exception = assertThrows(CpsVendorException.class,
                () -> failingClient.generatePromotionLink(request, CpsVendorConfig.builder().build()));

        assertEquals("500", exception.getUpstreamCode());
        assertEquals("未开通京东官方账号", exception.getUpstreamMessage());
        assertEquals("haodanku", exception.getVendorCode());
        assertEquals(CpsVendorCapability.PROMOTION_LINK, exception.getCapability());
    }

    @Test
    @DisplayName("供应商编码应返回 haodanku")
    void testGetVendorCode() {
        assertEquals("haodanku", client.getVendorCode());
    }

    @Test
    @DisplayName("好单库配置只要求 apikey 和基础地址")
    void configSchemaShouldAcceptApiKeyWithoutAppSecret() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("hdk-key")
                .apiBaseUrl("http://v2.api.haodanku.com")
                .build();

        assertTrue(client.getConfigSchema().validate(config).isValid());
        assertFalse(client.getConfigSchema().getFields().stream()
                .anyMatch(field -> field.isRequired() && "appSecret".equals(field.getName())));
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

        assertEquals("https://v2.api.haodanku.com", client.resolveApiBaseUrl(config));
        assertEquals("https://v3.api.haodanku.com", client.getPromotionLinkBaseUrl(config));
    }

    @Test
    @DisplayName("淘宝商品转链应传递可信会员运营和渠道关系标识")
    void taobaoPromotionLinkShouldForwardTrustedAttributionFields() {
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("TB-ITEM-1");
        request.setSpecialId("special-1001");
        request.setRelationId("relation-2001");

        Map<String, Object> params = new HdkTaobaoVendorClient().buildPromotionLinkParams(request,
                CpsVendorConfig.builder().defaultAdzoneId("mm_1_2_3").build());

        assertEquals("special-1001", params.get("special_id"));
        assertEquals("relation-2001", params.get("relation_id"));
        assertEquals("mm_1_2_3", params.get("pid"));
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
    @DisplayName("京东商品转链应使用 v2 get_jditems_link 并解析 data.short_url")
    void testJdPromotionLink() throws Exception {
        HdkJdVendorClient jdClient = new HdkJdVendorClient();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("encrypted_item");
        request.setItemLink("https://item.jd.com/100012043978.html");
        request.setCouponUrl("https://coupon.m.jd.com/coupons/show.action?key=test");
        request.setChannelId("channel_1");
        CpsVendorConfig config = CpsVendorConfig.builder()
                .apiBaseUrl("https://v2.api.haodanku.com")
                .authToken("union_1")
                .defaultAdzoneId("123_456_789")
                .extraConfig(Map.of("proType", "5", "scene_id", "1"))
                .build();

        Map<String, Object> params = jdClient.buildPromotionLinkParams(request, config);
        CpsPromotionLinkResult result = jdClient.parsePromotionLinkResponse(
                OBJECT_MAPPER.readTree("{\"code\":200,\"msg\":\"success\",\"data\":{\"short_url\":\"https://u.jd.com/a\"}}"));

        assertEquals("/get_jditems_link", jdClient.getPromotionLinkApiPath());
        assertEquals("https://v2.api.haodanku.com", jdClient.getPromotionLinkBaseUrl(config));
        assertEquals("encrypted_item", params.get("material_id"));
        assertEquals("union_1", params.get("union_id"));
        assertEquals("https://coupon.m.jd.com/coupons/show.action?key=test", params.get("coupon_url"));
        assertEquals("123_456_789", params.get("pid"));
        assertEquals("channel_1", params.get("subUnionId"));
        assertEquals("5", params.get("proType"));
        assertEquals("1", params.get("scene_id"));
        assertFalse(params.containsKey("weChatType"));
        assertEquals("https://u.jd.com/a", result.getShortUrl());
        assertNull(result.getLongUrl());
    }

    @Test
    @DisplayName("京东商品转链应忽略无效的占位 PID")
    void jdPromotionLinkShouldIgnorePlaceholderPid() {
        HdkJdVendorClient jdClient = new HdkJdVendorClient();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("encrypted_item");
        request.setAdzoneId("test");

        Map<String, Object> params = jdClient.buildPromotionLinkParams(request,
                CpsVendorConfig.builder().defaultAdzoneId("test").build());

        assertFalse(params.containsKey("pid"));
    }

    @Test
    @DisplayName("拼多多搜索应使用好单库 v2 超级搜索，转链保持独立 unify 接口")
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

        assertEquals("芝麻糊", searchParams.get("keyword"));
        assertEquals(3, searchParams.get("min_id"));
        assertEquals(20, searchParams.get("limit"));
        assertEquals(2, searchParams.get("sort"));
        assertEquals("/pdd_goods_search", pddClient.getSearchApiPath());
        assertEquals("/pdd_goods_search", pddClient.getTestConnectionApiPath());
        assertEquals("/unify_pdditems_link", pddClient.getPromotionLinkApiPath());
        assertEquals("pdd-goods-sign", linkParams.get("itemid"));
        assertEquals("user_1", linkParams.get("channel"));
    }

    @Test
    @DisplayName("拼多多连接测试应携带配置的默认 PID")
    void pddConnectionTestShouldIncludeConfiguredPid() {
        class CapturingHdkPddVendorClient extends HdkPddVendorClient {
            private Map<String, Object> capturedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                capturedParams = new HashMap<>(params);
                return OBJECT_MAPPER.createObjectNode().put("code", 200);
            }
        }
        CapturingHdkPddVendorClient pddClient = new CapturingHdkPddVendorClient();
        CpsVendorConfig config = CpsVendorConfig.builder()
                .defaultAdzoneId("8248392_317210977")
                .build();

        assertTrue(pddClient.testConnection(config));
        assertEquals("8248392_317210977", pddClient.capturedParams.get("pid"));
        assertEquals("手机", pddClient.capturedParams.get("keyword"));
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

    @Test
    @DisplayName("淘宝订单同步应兼容好单库与淘宝官方字段")
    void testTaobaoOrderResponseMapsOfficialAndHdkFields() throws Exception {
        HdkTaobaoVendorClient taobaoClient = new HdkTaobaoVendorClient();

        List<CpsOrderDTO> orders = taobaoClient.parseOrderQueryResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"data":[{
                  "trade_id":"3311060703815126366",
                  "trade_parent_id":"3311060703815126366",
                  "item_id":"TB-ITEM-1",
                  "item_title":"测试商品",
                  "item_img":"https://img.example/tb.jpg",
                  "item_price":"999.00",
                  "pay_price":"0.00",
                  "alipay_total_price":"399.00",
                  "commission_rate":"0",
                  "pub_share_rate":"4.5",
                  "commission":"0.00",
                  "pub_share_pre_fee":"17.96",
                  "tk_status":12,
                  "create_time":"2026-07-06 18:02:08",
                  "tk_paid_time":"2026-07-06 18:03:08",
                  "special_id":"1002",
                  "relation_id":"relation-2002",
                  "channel_code":"channel-3002",
                  "adzone_id":"mm_1_2_3"
                }]}
                """));

        CpsOrderDTO order = orders.get(0);
        assertEquals("3311060703815126366", order.getPlatformOrderId());
        assertEquals("TB-ITEM-1", order.getItemId());
        assertEquals(new BigDecimal("999.00"), order.getItemPrice());
        assertEquals(new BigDecimal("399.00"), order.getFinalPrice());
        assertEquals(new BigDecimal("4.5"), order.getCommissionRate());
        assertEquals(new BigDecimal("17.96"), order.getCommissionAmount());
        assertEquals(1, order.getPlatformStatus());
        assertEquals("1002", order.getSpecialId());
        assertEquals("relation-2002", order.getRelationId());
        assertEquals("channel-3002", order.getExternalId());
        assertEquals("mm_1_2_3", order.getAdzoneId());
        assertEquals("2026-07-06 18:03:08", order.getPayTime());
    }

    @Test
    @DisplayName("京东订单同步应按好单库文档字段映射佣金和渠道")
    void testJdOrderResponseMapsHdkFields() throws Exception {
        HdkJdVendorClient jdClient = new HdkJdVendorClient();

        List<CpsOrderDTO> orders = jdClient.parseOrderQueryResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"data":[{
                  "trade_id":"JD-ORDER-1",
                  "trade_parent_id":"JD-PARENT-1",
                  "item_id":"JD-SKU-1",
                  "item_title":"京东测试商品",
                  "item_img":"https://img.example/jd.jpg",
                  "pay_price":"4599.00",
                  "predict_money":"195.46",
                  "actual_money":"188.00",
                  "commission_rate":"4.25",
                  "order_status":1,
                  "create_time":"2026-07-06 18:02:08",
                  "paid_time":"2026-07-06 18:04:08",
                  "sub_union_id":"1002"
                }]}
                """));

        CpsOrderDTO order = orders.get(0);
        assertEquals("JD-ORDER-1", order.getPlatformOrderId());
        assertEquals("JD-PARENT-1", order.getParentOrderId());
        assertEquals("JD-SKU-1", order.getItemId());
        assertEquals(new BigDecimal("4599.00"), order.getFinalPrice());
        assertEquals(new BigDecimal("4.25"), order.getCommissionRate());
        assertEquals(new BigDecimal("188.00"), order.getCommissionAmount());
        assertEquals(1, order.getPlatformStatus());
        assertEquals("1002", order.getExternalId());
    }

    @Test
    @DisplayName("拼多多订单同步应按好单库文档字段映射佣金和渠道")
    void testPddOrderResponseMapsHdkFields() throws Exception {
        HdkPddVendorClient pddClient = new HdkPddVendorClient();

        List<CpsOrderDTO> orders = pddClient.parseOrderQueryResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"data":[{
                  "trade_id":"PDD-ORDER-1",
                  "trade_parent_id":"PDD-PARENT-1",
                  "goods_sign":"PDD-GOODS-SIGN",
                  "goods_id":"PDD-GOODS-1",
                  "item_title":"拼多多测试商品",
                  "item_img":"https://img.example/pdd.jpg",
                  "pay_price":"29.90",
                  "predict_money":"2.87",
                  "actual_money":"2.00",
                  "commission_rate":"9.6",
                  "order_status":1,
                  "create_time":"2026-07-06 18:02:08",
                  "paid_time":"2026-07-06 18:04:08",
                  "channel_code":"1002"
                }]}
                """));

        CpsOrderDTO order = orders.get(0);
        assertEquals("PDD-ORDER-1", order.getPlatformOrderId());
        assertEquals("PDD-PARENT-1", order.getParentOrderId());
        assertEquals("PDD-GOODS-SIGN", order.getItemId());
        assertEquals(new BigDecimal("29.90"), order.getFinalPrice());
        assertEquals(new BigDecimal("9.6"), order.getCommissionRate());
        assertEquals(new BigDecimal("2.00"), order.getCommissionAmount());
        assertEquals(1, order.getPlatformStatus());
        assertEquals("1002", order.getExternalId());
    }

    @Test
    @DisplayName("淘宝选品类目筛选应使用好单库 column 参数")
    void testTaobaoColumnSelectionParams() {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setPageNo(3);
        request.setPageSize(20);
        request.setCategoryId("10");
        request.setChannelCode("brand");
        request.setSortType(4);
        request.setPriceLowerLimit(new BigDecimal("10"));
        request.setPriceUpperLimit(new BigDecimal("80"));
        request.setCouponAmountMin(new BigDecimal("5"));
        request.setMinMonthSales(1000L);

        HdkTaobaoVendorClient client = new HdkTaobaoVendorClient();
        Map<String, Object> params = client.buildColumnSearchParams(request);

        assertEquals(8, params.get("type"));
        assertEquals(20, params.get("back"));
        assertEquals(3, params.get("min_id"));
        assertEquals(10, params.get("cid"));
        assertEquals(5, params.get("sort"));
        assertEquals(new BigDecimal("10"), params.get("price_min"));
        assertEquals(new BigDecimal("80"), params.get("price_max"));
        assertEquals(new BigDecimal("5"), params.get("coupon_min"));
        assertEquals(1000L, params.get("sale_min"));
    }

    @Test
    @DisplayName("淘宝关键词搜索仍使用好单库 supersearch")
    void testTaobaoKeywordSearchKeepsSuperSearch() {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("牙刷");
        request.setHasCoupon(1);

        HdkTaobaoVendorClient client = new HdkTaobaoVendorClient();
        Map<String, Object> params = client.buildSearchParams(request, CpsVendorConfig.builder().build());

        assertEquals("/supersearch", client.getSearchApiPath());
        assertEquals(1, params.get("is_coupon"));
        assertFalse(params.containsKey("type"));
    }
}
