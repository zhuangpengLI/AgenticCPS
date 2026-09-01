package com.qiji.cps.module.cps.client.dataoke;

import cn.hutool.crypto.digest.DigestUtil;
import com.qiji.cps.module.cps.client.dto.CpsContentParseRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AbstractDtkVendorClient} 签名逻辑单元测试
 *
 * @author CPS System
 */
class AbstractDtkVendorClientTest {

    /**
     * 最小可测试的 Dtk 子类
     */
    private static class TestDtkVendorClient extends AbstractDtkVendorClient {
        @Override
        public String getPlatformCode() { return "taobao"; }
        @Override protected String getSearchApiPath() { return "/test/search"; }
        @Override protected Map<String, Object> buildSearchParams(
                com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult parseSearchResponse(
                JsonNode response, com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest request) {
            return null;
        }
        @Override protected String getPromotionLinkApiPath() { return "/test/link"; }
        @Override protected Map<String, Object> buildPromotionLinkParams(
                com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
            return null;
        }
        @Override protected String getOrderQueryApiPath() { return "/test/order"; }
        @Override protected Map<String, Object> buildOrderQueryParams(
                com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected java.util.List<com.qiji.cps.module.cps.client.dto.CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
            return java.util.Collections.emptyList();
        }
        @Override protected String getTestConnectionApiPath() { return "/test/conn"; }
        @Override protected Map<String, Object> buildTestConnectionParams() { return new HashMap<>(); }

        // 暴露 protected 方法供测试使用
        public Map<String, String> testComputeSignContext(Map<String, Object> params, CpsVendorConfig config) {
            return computeSignContext(params, config);
        }

        public void testInjectSignParams(Map<String, Object> params, CpsVendorConfig config,
                                         Map<String, String> signContext) {
            injectSignParams(params, config, signContext);
        }

        public boolean testIsSuccessResponse(JsonNode root) {
            return isSuccessResponse(root);
        }

        public JsonNode testUnwrapResponse(JsonNode root) {
            return unwrapResponse(root);
        }

        public String testBuildUrlWithParams(String baseUrl, Map<String, Object> params) {
            return buildUrlWithParams(baseUrl, params);
        }
    }

    private final TestDtkVendorClient client = new TestDtkVendorClient();

    @Test
    @DisplayName("供应商编码应返回 dataoke")
    void testGetVendorCode() {
        assertEquals("dataoke", client.getVendorCode());
    }

    @Test
    @DisplayName("签名上下文应包含 timer, nonce, sign")
    void testComputeSignContext_containsKeys() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("testAppKey")
                .appSecret("testAppSecret")
                .build();
        Map<String, String> context = client.testComputeSignContext(new HashMap<>(), config);

        assertNotNull(context);
        assertTrue(context.containsKey("timer"));
        assertTrue(context.containsKey("nonce"));
        assertTrue(context.containsKey("sign"));
        assertFalse(context.get("timer").isBlank());
        assertEquals(6, context.get("nonce").length());
    }

    @Test
    @DisplayName("签名计算结果应与手动计算一致")
    void testComputeSignContext_signValue() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("myKey123")
                .appSecret("mySecret456")
                .build();
        Map<String, String> context = client.testComputeSignContext(new HashMap<>(), config);

        String timer = context.get("timer");
        String nonce = context.get("nonce");
        String expectedSign = DigestUtil.md5Hex(
                String.format("appKey=%s&timer=%s&nonce=%s&key=%s", "myKey123", timer, nonce, "mySecret456")
        ).toUpperCase();

        assertEquals(expectedSign, context.get("sign"));
    }

    @Test
    @DisplayName("签名注入应将 appKey, timer, nonce, signRan 放入参数")
    void testInjectSignParams() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("testKey")
                .appSecret("testSecret")
                .build();
        Map<String, String> signContext = Map.of(
                "timer", "1234567890",
                "nonce", "000001",
                "sign", "abc123"
        );
        Map<String, Object> params = new HashMap<>();
        client.testInjectSignParams(params, config, signContext);

        assertEquals("testKey", params.get("appKey"));
        assertEquals("1234567890", params.get("timer"));
        assertEquals("000001", params.get("nonce"));
        assertEquals("abc123", params.get("signRan"));
    }

    @Test
    @DisplayName("isSuccessResponse: code=0 应返回 true")
    void testIsSuccessResponse_success() throws Exception {
        JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"code\": 0, \"data\": {}}");
        assertTrue(client.testIsSuccessResponse(root));
    }

    @Test
    @DisplayName("isSuccessResponse: 大淘客新响应结构 data.code=0 应返回 true")
    void testIsSuccessResponse_wrappedSuccess() throws Exception {
        JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"status\": 200, \"data\": {\"code\": 0, \"msg\": \"成功\", \"data\": {\"list\": []}}}");
        assertTrue(client.testIsSuccessResponse(root));
    }

    @Test
    @DisplayName("unwrapResponse: 大淘客新响应结构应拆到业务响应层")
    void testUnwrapResponse() throws Exception {
        JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"status\": 200, \"data\": {\"code\": 0, \"msg\": \"成功\", \"data\": {\"list\": []}}}");
        JsonNode unwrapped = client.testUnwrapResponse(root);

        assertEquals("0", unwrapped.path("code").asText());
        assertTrue(unwrapped.path("data").path("list").isArray());
    }

    @Test
    @DisplayName("isSuccessResponse: code=1 应返回 false")
    void testIsSuccessResponse_failure() throws Exception {
        JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree("{\"code\": 1, \"msg\": \"error\"}");
        assertFalse(client.testIsSuccessResponse(root));
    }

    @Test
    @DisplayName("isSuccessResponse: null 应返回 false")
    void testIsSuccessResponse_null() {
        assertFalse(client.testIsSuccessResponse(null));
    }

    @Test
    @DisplayName("buildUrlWithParams 应对中文、空格和链接参数做 URL 编码")
    void testBuildUrlWithParams_encodeValues() {
        Map<String, Object> params = new HashMap<>();
        params.put("content", "中文 口令");
        params.put("url", "https://example.com/a?b=1&c=2");

        String url = client.testBuildUrlWithParams("https://openapi.dataoke.com/api/test", params);

        assertTrue(url.contains("content=%E4%B8%AD%E6%96%87+%E5%8F%A3%E4%BB%A4"));
        assertTrue(url.contains("url=https%3A%2F%2Fexample.com%2Fa%3Fb%3D1%26c%3D2"));
    }

    @Test
    @DisplayName("淘宝选品筛选应映射到大淘客搜索参数")
    void testTaobaoSelectionSearchParams() {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("洗衣液");
        request.setPageNo(2);
        request.setPageSize(20);
        request.setCategoryId("10");
        request.setMinCommissionRate(new BigDecimal("20"));
        request.setMinMonthSales(1000L);
        request.setCouponAmountMin(new BigDecimal("5"));
        request.setTmallOnly(true);
        request.setBrandOnly(true);
        request.setHaitaoOnly(true);
        request.setCommercialOnly(true);
        request.setPreSaleOnly(true);

        Map<String, Object> params = new DtkTaobaoVendorClient().buildSearchParams(request, CpsVendorConfig.builder().build());

        assertEquals("10", params.get("cids"));
        assertEquals(new BigDecimal("20"), params.get("commissionRateLowerLimit"));
        assertEquals(1000L, params.get("monthSalesLowerLimit"));
        assertEquals(new BigDecimal("5"), params.get("couponPriceLowerLimit"));
        assertEquals(1, params.get("tmall"));
        assertEquals(1, params.get("brand"));
        assertEquals(1, params.get("haitao"));
        assertEquals(3, params.get("directCommissionType"));
        assertEquals(1, params.get("pre"));
    }

    @Test
    @DisplayName("大淘客热搜记录应调用 get-top100 并映射热词")
    void testTaobaoHotKeywordsUsesTop100Api() {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "成功",
                              "data": {
                                "hotWords": ["螺蛳粉", "耳机"]
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();

        var result = client.getHotKeywords(1, CpsVendorConfig.builder().build());

        assertEquals("/category/get-top100", client.requestedPath);
        assertEquals("v1.0.1", client.requestedParams.get("version"));
        assertEquals(1, client.requestedParams.get("type"));
        assertEquals(2, result.size());
        assertEquals("螺蛳粉", result.get(0).getLabel());
    }

    @Test
    @DisplayName("大淘客联想词应调用 search-suggestion 并映射商品数量")
    void testTaobaoSearchSuggestionUsesSuggestionApi() {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "成功",
                              "data": [
                                {"kw": "裙子套装", "total": 128},
                                {"kw": "裙子半身裙", "total": 110}
                              ]
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();

        var result = client.suggestKeywords("裙子", 1, CpsVendorConfig.builder().build());

        assertEquals("/goods/search-suggestion", client.requestedPath);
        assertEquals("v1.0.2", client.requestedParams.get("version"));
        assertEquals("裙子", client.requestedParams.get("keyWords"));
        assertEquals(1, client.requestedParams.get("type"));
        assertEquals(2, result.size());
        assertEquals("裙子套装", result.get(0).getLabel());
        assertEquals("128 个商品", result.get(0).getDescription());
    }

    @Test
    @DisplayName("淘宝万能解析应调用大淘客 parse-content 并映射商品信息")
    void testTaobaoParseContent() throws Exception {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "成功",
                              "data": {
                                "goodsId": "625171500599",
                                "originUrl": "https://m.tb.cn/h.RyYoJdn",
                                "originType": "商品链接",
                                "originInfo": {
                                  "title": "测试商品标题",
                                  "image": "https://img.alicdn.com/test.jpg"
                                }
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();
        CpsContentParseRequest request = new CpsContentParseRequest();
        request.setPlatformCode("taobao");
        request.setOriginalContent("https://m.tb.cn/h.RyYoJdn");

        var result = client.parseContent(request, CpsVendorConfig.builder()
                .defaultAdzoneId("mm_111_222_333")
                .build());

        assertEquals("/tb-service/parse-content", client.requestedPath);
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals("https://m.tb.cn/h.RyYoJdn", client.requestedParams.get("content"));
        assertFalse(client.requestedParams.containsKey("pid"));
        assertTrue(result.getSupported());
        assertEquals("625171500599", result.getGoodsId());
        assertEquals("https://m.tb.cn/h.RyYoJdn", result.getItemLink());
        assertEquals("测试商品标题", result.getTitle());
    }

    @Test
    @DisplayName("淘宝万能转链应携带原文和 PID 调用 parse-content 并直接映射推广素材")
    void testTaobaoPromotionLinkUsesParseContentForOriginalContent() throws Exception {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "成功",
                              "data": {
                                "goodsId": "839586501738",
                                "shortUrl": "https://s.click.taobao.com/short",
                                "cpsLongUrl": "https://s.click.taobao.com/long",
                                "shortTpwd": "9￥ newTpwd￥",
                                "commissionRate": 2.25,
                                "actualPrice": 9.9
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        String originalContent = "https://e.tb.cn/h.84VUo3aSGRvlgaM?tk=9aINgxwNAdO HU293 「活力28衣物柔顺剂官方旗舰店正品柔软防静电持久留香护理衣物家用」";
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("839586501738");
        request.setOriginalContent(originalContent);
        request.setAdzoneId("mm_4480323_46012675_116291900443");
        request.setChannelId("REL-285");
        request.setSpecialId("SPECIAL-285");

        var result = client.generatePromotionLink(request, CpsVendorConfig.builder().build());

        assertEquals("/tb-service/parse-content", client.requestedPath);
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals(originalContent, client.requestedParams.get("content"));
        assertEquals("mm_4480323_46012675_116291900443", client.requestedParams.get("pid"));
        assertEquals("REL-285", client.requestedParams.get("channelId"));
        assertEquals("SPECIAL-285", client.requestedParams.get("specialId"));
        assertEquals("https://s.click.taobao.com/short", result.getShortUrl());
        assertEquals("https://s.click.taobao.com/long", result.getLongUrl());
        assertEquals("9￥ newTpwd￥", result.getTpwd());
        assertEquals(new BigDecimal("2.25"), result.getCommissionRate());
        assertEquals(new BigDecimal("9.9"), result.getActualPrice());
    }

    @Test
    @DisplayName("Taobao Dataoke link should send member specialId separately from channelId")
    void testTaobaoPromotionLinkSendsSpecialIdAndChannelIdSeparately() throws Exception {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "shortUrl": "https://s.click.taobao.com/abc",
                                "itemUrl": "https://uland.taobao.com/coupon/edetail?id=123",
                                "tpwd": "cmd"
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("625171500599");
        request.setAdzoneId("mm_111_222_333");
        request.setExternalId("285");
        request.setSpecialId("SPECIAL-285");
        request.setChannelId("REL-8001");

        var result = client.generatePromotionLink(request, CpsVendorConfig.builder().build());

        assertEquals("/tb-service/get-privilege-link", client.requestedPath);
        assertEquals("625171500599", client.requestedParams.get("goodsId"));
        assertEquals("mm_111_222_333", client.requestedParams.get("pid"));
        assertEquals("285", client.requestedParams.get("externalId"));
        assertEquals("SPECIAL-285", client.requestedParams.get("specialId"));
        assertEquals("REL-8001", client.requestedParams.get("channelId"));
        assertEquals("https://s.click.taobao.com/abc", result.getShortUrl());
    }

    @Test
    @DisplayName("Taobao parse failure should return Dataoke failure message")
    void testTaobaoParseContentFailureMessage() throws Exception {
        class FailingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 1,
                              "msg": "渠道ID校验失败"
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CpsContentParseRequest request = new CpsContentParseRequest();
        request.setPlatformCode("taobao");
        request.setOriginalContent("https://m.tb.cn/h.RyYoJdn");

        var result = new FailingDtkTaobaoVendorClient().parseContent(request, CpsVendorConfig.builder().build());

        assertFalse(result.getSupported());
        assertEquals("PARSE_FAILED", result.getFailureCode());
        assertEquals("渠道ID校验失败", result.getFailureReason());
    }

    @Test
    @DisplayName("queryCouponInfo should call Dataoke coupon info API")
    void testTaobaoQueryCouponInfoUsesDtkCouponApi() {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "couponReceiveNum": 7000,
                                "couponLink": "https://uland.taobao.com/quan/detail?activityId=abc",
                                "couponEndTime": "2026-07-31 23:59:59",
                                "couponStartTime": "2026-07-01 00:00:00",
                                "couponConditions": "59",
                                "couponId": "abc",
                                "couponAmount": 40,
                                "couponTotalNum": 100000,
                                "couponRemainNum": 93000
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();

        var result = client.queryCouponInfo("￥FV25gmAa2SI￥", CpsVendorConfig.builder().build());

        assertEquals("/dels/taobao/kit/coupon/get-coupon-info", client.requestedPath);
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals("￥FV25gmAa2SI￥", client.requestedParams.get("content"));
        assertEquals("abc", result.getCouponId());
        assertEquals(new BigDecimal("40"), result.getCouponAmount());
        assertEquals(new BigDecimal("59"), result.getCouponConditions());
        assertEquals(93000L, result.getCouponRemainNum());
        assertEquals("2026-07-31 23:59:59", result.getCouponEndTime());
    }

    @Test
    @DisplayName("淘宝订单查询应映射大淘客订单状态、归因和状态时间")
    void testTaobaoQueryOrdersMapsDataokeOrderFields() {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            private String requestedPath;
            private Map<String, Object> requestedParams;

            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                this.requestedPath = path;
                this.requestedParams = params;
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "positionIndex": "NEXT_1",
                                "results": {
                                  "publisher_order_dto": [
                                    {
                                      "trade_id": "1234567890",
                                      "trade_parent_id": "P1234567890",
                                      "item_id": "625171500599",
                                      "item_title": "测试订单商品",
                                      "item_img": "https://img.alicdn.com/test.jpg",
                                      "item_price": "100.00",
                                      "pay_price": "80.00",
                                      "total_commission_rate": "20.00",
                                      "pub_share_fee": "12.34",
                                      "item_num": 2,
                                      "tk_status": 3,
                                      "tk_create_time": "2026-07-06 10:00:00",
                                      "tk_paid_time": "2026-07-06 10:05:00",
                                      "tk_earning_time": "2026-07-10 12:00:00",
                                      "tb_deposit_time": "2026-07-08 12:00:00",
                                      "adzone_id": "mm_111_222_333",
                                      "special_id": "1002",
                                      "relation_id": "2002",
                                      "refund_tag": 0
                                    }
                                  ]
                                }
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CapturingDtkTaobaoVendorClient client = new CapturingDtkTaobaoVendorClient();
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setQueryType(4);
        request.setStartTime("2026-07-06 00:00:00");
        request.setEndTime("2026-07-06 12:00:00");
        request.setPageSize(50);
        request.setPageNo(2);
        request.setPositionIndex("PREV_1");
        request.setOrderStatus(3);
        request.setOrderScene(3);

        var orders = client.queryOrders(request, CpsVendorConfig.builder().build());

        assertEquals("/tb-service/get-order-details", client.requestedPath);
        assertEquals(4, client.requestedParams.get("queryType"));
        assertEquals("2026-07-06 00:00:00", client.requestedParams.get("startTime"));
        assertEquals("2026-07-06 12:00:00", client.requestedParams.get("endTime"));
        assertEquals(50, client.requestedParams.get("pageSize"));
        assertEquals(2, client.requestedParams.get("pageNo"));
        assertEquals(3, client.requestedParams.get("tkStatus"));
        assertEquals(1, client.requestedParams.get("jumpType"));
        assertEquals("PREV_1", client.requestedParams.get("positionIndex"));
        assertEquals(3, client.requestedParams.get("orderScene"));
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals(1, orders.size());
        var order = orders.get(0);
        assertEquals("1234567890", order.getPlatformOrderId());
        assertEquals("1002", order.getSpecialId());
        assertEquals("2002", order.getRelationId());
        assertEquals(3, order.getOrderScene());
        assertEquals(null, order.getExternalId());
        assertEquals(3, order.getPlatformStatus());
        assertEquals("2026-07-08 12:00:00", order.getReceiveTime());
        assertEquals("2026-07-10 12:00:00", order.getSettleTime());
        assertEquals("NEXT_1", order.getNextPositionIndex());
        assertEquals("2002", order.getExtraFields().get("relationId"));
    }

    @Test
    @DisplayName("大淘客淘宝订单四种查询类型均透传 queryType 和时间分页参数")
    void testTaobaoOrderQuerySupportsAllQueryTypes() {
        class ParamCapturingClient extends DtkTaobaoVendorClient {
            Map<String, Object> expose(CpsOrderQueryRequest request) {
                return buildOrderQueryParams(request, CpsVendorConfig.builder().build());
            }
        }
        ParamCapturingClient client = new ParamCapturingClient();
        for (int queryType = 1; queryType <= 4; queryType++) {
            CpsOrderQueryRequest request = new CpsOrderQueryRequest();
            request.setQueryType(queryType);
            request.setStartTime("2026-08-31 09:00:00");
            request.setEndTime("2026-08-31 12:00:00");
            request.setPageNo(1);
            request.setPageSize(100);
            Map<String, Object> params = client.expose(request);

            assertEquals(queryType, params.get("queryType"));
            assertEquals("2026-08-31 09:00:00", params.get("startTime"));
            assertEquals("2026-08-31 12:00:00", params.get("endTime"));
            assertEquals(1, params.get("pageNo"));
            assertEquals(100, params.get("pageSize"));
            assertFalse(params.containsKey("tkStatus"));
            assertFalse(params.containsKey("jumpType"));
        }
    }

    @Test
    @DisplayName("淘宝订单查询应优先使用真实 external_id 归因")
    void testTaobaoQueryOrdersPrefersExternalIdForAttribution() {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "results": {
                                  "publisher_order_dto": [
                                    {
                                      "trade_id": "TB-EXTERNAL-1",
                                      "item_id": "ITEM-1",
                                      "tk_status": 12,
                                      "external_id": "1001",
                                      "special_id": "SPECIAL-999",
                                      "relation_id": "REL-888"
                                    }
                                  ]
                                }
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        var orders = new CapturingDtkTaobaoVendorClient().queryOrders(
                new CpsOrderQueryRequest(), CpsVendorConfig.builder().build());

        assertEquals(1, orders.size());
        assertEquals("1001", orders.get(0).getExternalId());
        assertEquals("SPECIAL-999", orders.get(0).getSpecialId());
        assertEquals("REL-888", orders.get(0).getRelationId());
    }

    @Test
    @DisplayName("拼多多搜索应按大淘客聚合接口的元单位保留价格")
    void testPddSearchKeepsDataokeYuanPrices() {
        class CapturingDtkPddVendorClient extends DtkPddVendorClient {
            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "totalCount": 1,
                                "goodsList": [
                                  {
                                    "goodsSign": "PDD-TOWEL-1",
                                    "goodsName": "纯棉毛巾",
                                    "minNormalPrice": 5.35,
                                    "minGroupPrice": 3.35,
                                    "couponDiscount": 2.00,
                                    "promotionRate": 60
                                  }
                                ]
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("毛巾");
        request.setPageNo(1);
        request.setPageSize(5);

        var result = new CapturingDtkPddVendorClient().searchGoods(request, CpsVendorConfig.builder().build());

        assertEquals(1, result.getList().size());
        assertEquals(new BigDecimal("5.35"), result.getList().get(0).getOriginalPrice());
        assertEquals(new BigDecimal("3.35"), result.getList().get(0).getActualPrice());
        assertEquals(0, new BigDecimal("2.00").compareTo(result.getList().get(0).getCouponPrice()));
        assertEquals(new BigDecimal("6"), result.getList().get(0).getCommissionRate());
    }

    @Test
    @DisplayName("拼多多转链应把会员归因写成 customParameters JSON")
    void testPddPromotionLinkSerializesMemberAttributionAsJson() throws Exception {
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsSign("PDD-GOODS-SIGN");
        request.setAdzoneId("8248392_314766559");
        request.setExternalId("285");

        Map<String, Object> params = new DtkPddVendorClient().buildPromotionLinkParams(
                request, CpsVendorConfig.builder().build());

        JsonNode customParameters = new ObjectMapper().readTree((String) params.get("customParameters"));
        assertEquals("285", customParameters.path("uid").asText());
    }

    @Test
    @DisplayName("大淘客详细转链模式应保留上游错误码和文案")
    void promotionLinkDetailModePreservesUpstreamRejection() throws Exception {
        JsonNode rejection = new ObjectMapper().readTree("""
                {"code":400,"msg":"pid未授权"}
                """);
        TestDtkVendorClient client = new TestDtkVendorClient() {
            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                return rejection;
            }
        };
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("goods-1");
        request.setPropagateVendorError(true);

        CpsVendorException exception = assertThrows(CpsVendorException.class,
                () -> client.generatePromotionLink(request, CpsVendorConfig.builder().build()));

        assertEquals("UPSTREAM_REJECTED", exception.getCode());
        assertEquals("400", exception.getUpstreamCode());
        assertEquals("pid未授权", exception.getUpstreamMessage());
        assertEquals("dataoke", exception.getVendorCode());
        assertEquals("taobao", exception.getPlatformCode());
        assertEquals(CpsVendorCapability.PROMOTION_LINK, exception.getCapability());
    }

    @Test
    @DisplayName("拼多多订单查询应映射 customParameters 用于会员归因")
    void testPddQueryOrdersMapsCustomParametersForAttribution() {
        class CapturingDtkPddVendorClient extends DtkPddVendorClient {
            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "orderList": [
                                  {
                                    "orderSn": "PDD-1",
                                    "goodsSign": "PDD-GOODS-1",
                                    "goodsName": "测试拼多多商品",
                                    "promotionRate": "20",
                                    "promotionAmount": "100",
                                    "orderStatus": 1,
                                    "orderCreateTime": "2026-07-06 20:00:00",
                                    "pid": "pdd-pid-1",
                                    "customParameters": "1001"
                                  }
                                ]
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        var orders = new CapturingDtkPddVendorClient().queryOrders(
                new CpsOrderQueryRequest(), CpsVendorConfig.builder().build());

        assertEquals(1, orders.size());
        assertEquals("1001", orders.get(0).getExternalId());
        assertEquals("pdd-pid-1", orders.get(0).getAdzoneId());
    }

    @Test
    @DisplayName("淘宝订单查询应兼容联盟付款金额和预估佣金字段")
    void testTaobaoQueryOrdersMapsOfficialPaymentAndCommissionFields() {
        class CapturingDtkTaobaoVendorClient extends DtkTaobaoVendorClient {
            @Override
            protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
                try {
                    return new ObjectMapper().readTree("""
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "results": {
                                  "publisher_order_dto": [
                                    {
                                      "trade_id": "3311060703815126366",
                                      "trade_parent_id": "3311060703815126366",
                                      "item_id": "839050000001",
                                      "item_title": "旗舰婴儿推车",
                                      "item_price": "999.00",
                                      "alipay_total_price": "399.00",
                                      "pub_share_rate": "4.5",
                                      "pub_share_pre_fee": "17.96",
                                      "tk_status": 12,
                                      "tk_create_time": "2026-07-06 18:02:08",
                                      "tk_paid_time": "2026-07-06 18:02:38",
                                      "adzone_id": "mm_111_222_333",
                                      "special_id": "1002",
                                      "refund_tag": 0
                                    }
                                  ]
                                }
                              }
                            }
                            """);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        var orders = new CapturingDtkTaobaoVendorClient().queryOrders(
                new CpsOrderQueryRequest(), CpsVendorConfig.builder().build());

        assertEquals(1, orders.size());
        var order = orders.get(0);
        assertEquals(new BigDecimal("999.00"), order.getItemPrice());
        assertEquals(new BigDecimal("399.00"), order.getFinalPrice());
        assertEquals(new BigDecimal("4.5"), order.getCommissionRate());
        assertEquals(new BigDecimal("17.96"), order.getCommissionAmount());
        assertEquals("1002", order.getSpecialId());
        assertEquals(null, order.getExternalId());
        assertEquals(1, order.getPlatformStatus());
    }

}
