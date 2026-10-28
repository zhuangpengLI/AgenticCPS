package com.qiji.cps.module.cps.client.official.eleme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElemeOfficialVendorClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ElemeOfficialVendorClient client = new ElemeOfficialVendorClient();

    @Test
    @DisplayName("基础元数据与全量官方能力集契约校验")
    void metadataAndCapabilities() {
        assertEquals(CpsPlatformCodeEnum.ELEME.getCode(), client.getPlatformCode());
        assertEquals("official", client.getVendorCode());
        assertEquals("official", client.getVendorType());
        assertTrue(client.getCapabilities().containsAll(Set.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST,
                CpsVendorCapability.ACTIVITY_PULL
        )));
    }

    @Test
    @DisplayName("配置契约校验：appKey、appSecret、defaultAdzoneId为必填")
    void configSchemaValidation() {
        var schema = client.getConfigSchema();
        assertNotNull(schema);

        // 缺少必填项
        assertFalse(schema.validate(CpsVendorConfig.builder().appKey("key").build()).isValid());
        assertFalse(schema.validate(CpsVendorConfig.builder().appKey("key").appSecret("sec").build()).isValid());

        // 必填项齐全
        assertTrue(schema.validate(CpsVendorConfig.builder()
                .appKey("key_123")
                .appSecret("sec_456")
                .defaultAdzoneId("alsc_pid_789")
                .build()).isValid());

        // 敏感信息脱敏校验
        Map<String, String> masked = schema.maskedSummary(CpsVendorConfig.builder()
                .appKey("key_123")
                .appSecret("sec_456")
                .defaultAdzoneId("alsc_pid_789")
                .build());
        assertEquals("***", masked.get("appKey"));
        assertEquals("***", masked.get("appSecret"));
        assertEquals("alsc_pid_789", masked.get("defaultAdzoneId"));
    }

    @Test
    @DisplayName("治理策略校验：限流100 QPM，熔断10秒，超时5秒")
    void governancePolicy() {
        var policy = client.getGovernancePolicy();
        assertNotNull(policy);
        assertEquals(5000, policy.getTimeoutMillis());
        assertEquals(100, policy.getRateLimitPerMinute());
        assertEquals(10000, policy.getCircuitBreakerOpenMillis());
        assertTrue(policy.isMetricsEnabled());
    }

    @Test
    @DisplayName("导购选品（门店列表搜索）：请求参数组装")
    void buildSearchParams() throws Exception {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("咖啡");

        CpsVendorConfig config = CpsVendorConfig.builder()
                .defaultAdzoneId("alsc_default_pid")
                .extraConfig(Map.of("session_id", "sess_cursor_001", "city_id", "110100"))
                .build();

        Map<String, Object> params = client.buildSearchParams(request, config);
        assertNotNull(params.get("query_request"));

        JsonNode queryNode = objectMapper.readTree(String.valueOf(params.get("query_request")));
        assertEquals("alsc_default_pid", queryNode.get("pid").asText());
        assertEquals("咖啡", queryNode.get("keyword").asText());
        assertEquals("sess_cursor_001", queryNode.get("session_id").asText());
        assertEquals("110100", queryNode.get("city_id").asText());
    }

    @Test
    @DisplayName("导购选品（门店列表搜索）：响应解析")
    void parseSearchResponse() throws Exception {
        String json = """
                {
                  "alibaba_alsc_union_eleme_promotion_storepromotion_query_response": {
                    "result": {
                      "data": {
                        "session_id": "next_session_page2",
                        "has_more": true,
                        "store_list": [
                          {
                            "shop_id": "E10001",
                            "shop_name": "星巴克咖啡（望京店）",
                            "shop_logo": "https://img.alicdn.com/starbucks.png",
                            "category_name": "咖啡饮品",
                            "min_price": "28.00",
                            "commission_rate": "0.08"
                          }
                        ]
                      },
                      "biz_code": 0,
                      "biz_msg": "success"
                    }
                  }
                }
                """;

        CpsGoodsSearchResult result = client.parseSearchResponse(objectMapper.readTree(json),
                new CpsGoodsSearchRequest());

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("next_session_page2", result.getNextPageId());

        CpsGoodsItem item = result.getList().get(0);
        assertEquals("E10001", item.getGoodsId());
        assertEquals("星巴克咖啡（望京店）", item.getTitle());
        assertEquals("https://img.alicdn.com/starbucks.png", item.getMainPic());
        assertEquals("eleme", item.getPlatformCode());
        assertEquals("official", item.getVendorCode());
        assertEquals(new BigDecimal("28.00"), item.getOriginalPrice());
        assertEquals(new BigDecimal("0.08"), item.getCommissionRate());
        assertEquals("咖啡饮品", item.getCategoryName());
    }

    @Test
    @DisplayName("官方活动推广转链：参数组装与SID会员跟踪注入")
    void buildPromotionLinkParams_officialActivity() throws Exception {
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("10144"); // 天天领红包活动ID
        request.setAdzoneId("alsc_custom_pid");
        request.setExternalId("member_sid_uuid_123");

        CpsVendorConfig config = CpsVendorConfig.builder()
                .defaultAdzoneId("alsc_default_pid")
                .build();

        Map<String, Object> params = client.buildPromotionLinkParams(request, config);
        assertEquals(ElemeOfficialVendorClient.API_OFFICIAL_ACTIVITY_GET, params.get("_api_method_"));

        JsonNode queryNode = objectMapper.readTree(String.valueOf(params.get("query_request")));
        assertEquals("alsc_custom_pid", queryNode.get("pid").asText());
        assertEquals("10144", queryNode.get("activity_id").asText());
        assertEquals("member_sid_uuid_123", queryNode.get("sid").asText());
    }

    @Test
    @DisplayName("单店推广转链：参数组装检测 shop_id 并调用单店转链接口")
    void buildPromotionLinkParams_storePromotion() throws Exception {
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("E998877"); // 门店ID
        request.setAdzoneId("alsc_custom_pid");
        request.setExternalId("member_sid_456");

        CpsVendorConfig config = CpsVendorConfig.builder()
                .defaultAdzoneId("alsc_default_pid")
                .build();

        Map<String, Object> params = client.buildPromotionLinkParams(request, config);
        assertEquals(ElemeOfficialVendorClient.API_STORE_PROMOTION_GET, params.get("_api_method_"));

        JsonNode queryNode = objectMapper.readTree(String.valueOf(params.get("query_request")));
        assertEquals("alsc_custom_pid", queryNode.get("pid").asText());
        assertEquals("E998877", queryNode.get("shop_id").asText());
        assertEquals("member_sid_456", queryNode.get("sid").asText());
        assertFalse(queryNode.get("include_wx_img").asBoolean());
    }

    @Test
    @DisplayName("官方活动推广响应解析：提取短链、长链、淘口令与小程序参数")
    void parsePromotionLinkResponse() throws Exception {
        String json = """
                {
                  "alibaba_alsc_union_eleme_promotion_officialactivity_get_response": {
                    "result": {
                      "data": {
                        "short_url": "https://tb.cn/short_elm_1",
                        "click_url": "https://s.click.ele.me/long_elm_1",
                        "tb_command": "￥shangou_tkl_123￥",
                        "wx_app_id": "wxece3a9a4c82f58c9",
                        "wx_path": "pages/share/index?scene=10144",
                        "url_scheme": "eleme://activity?id=10144"
                      },
                      "biz_code": 0,
                      "biz_msg": "success"
                    }
                  }
                }
                """;

        CpsPromotionLinkResult result = client.parsePromotionLinkResponse(objectMapper.readTree(json));
        assertNotNull(result);
        assertEquals("https://tb.cn/short_elm_1", result.getShortUrl());
        assertEquals("https://s.click.ele.me/long_elm_1", result.getLongUrl());
        assertEquals("￥shangou_tkl_123￥", result.getTpwd());
        assertEquals("eleme://activity?id=10144", result.getMobileUrl());
        assertEquals("wxece3a9a4c82f58c9", result.getExtraFields().get("wx_appid"));
        assertEquals("pages/share/index?scene=10144", result.getExtraFields().get("wx_path"));
    }

    @Test
    @DisplayName("正向订单查询参数组装：支持更新时间维度、biz_unit固定为2、渠道taobao_shangou")
    void buildOrderQueryParams() {
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setQueryType(4); // 4-更新时间
        request.setStartTime("2026-10-28 10:00:00");
        request.setEndTime("2026-10-28 11:00:00");
        request.setPageNo(2);
        request.setPageSize(30);
        request.setOrderStatus(2); // 2-已付款

        CpsVendorConfig config = CpsVendorConfig.builder()
                .defaultAdzoneId("alsc_test_pid")
                .build();

        Map<String, Object> params = client.buildOrderQueryParams(request, config);
        assertEquals(ElemeOfficialVendorClient.API_ORDER_DETAILS_GET, client.getOrderQueryApiPath());
        assertEquals("4", params.get("date_type"));
        assertEquals("2026-10-28 10:00:00", params.get("start_date"));
        assertEquals("2026-10-28 11:00:00", params.get("end_date"));
        assertEquals("2", params.get("biz_unit"));
        assertEquals("30", params.get("page_size"));
        assertEquals("2", params.get("page_number"));
        assertEquals("alsc_test_pid", params.get("pid"));
        assertEquals("2", params.get("order_state"));
        assertEquals("taobao_shangou", params.get("order_channel"));
    }

    @Test
    @DisplayName("正向订单查询响应解析：状态机映射与SID会员追踪归因提取")
    void parseOrderQueryResponse() throws Exception {
        String json = """
                {
                  "alibaba_alsc_union_kbcpx_positive_order_get_response": {
                    "result": {
                      "data": {
                        "total_count": 2,
                        "order_list": [
                          {
                            "biz_order_id": "SG_ORDER_001",
                            "tb_trade_parent_id": "SG_PARENT_001",
                            "item_id": "10144",
                            "item_title": "淘宝闪购天天领红包",
                            "item_pic": "https://img.alicdn.com/hb.png",
                            "pay_amount": "35.50",
                            "income": "3.55",
                            "settle": "3.55",
                            "order_state": 4,
                            "settle_state": 1,
                            "sid": "user_sid_token_abc",
                            "create_time": "2026-10-28 10:01:00",
                            "pay_time": "2026-10-28 10:02:00",
                            "settle_time": "2026-10-28 10:30:00"
                          },
                          {
                            "biz_order_id": "SG_ORDER_002",
                            "tb_trade_parent_id": "SG_PARENT_002",
                            "item_id": "E10002",
                            "item_title": "霸王茶姬外卖",
                            "pay_amount": "20.00",
                            "income": "2.00",
                            "order_state": 3,
                            "order_item_status": 3,
                            "sid": "user_sid_token_def"
                          }
                        ]
                      },
                      "biz_code": 0,
                      "biz_msg": "success"
                    }
                  }
                }
                """;

        List<CpsOrderDTO> orders = client.parseOrderQueryResponse(objectMapper.readTree(json));
        assertEquals(2, orders.size());

        // 订单1：已收货且已结算
        CpsOrderDTO o1 = orders.get(0);
        assertEquals("eleme", o1.getPlatformCode());
        assertEquals("official", o1.getVendorCode());
        assertEquals("SG_ORDER_001", o1.getPlatformOrderId());
        assertEquals("SG_PARENT_001", o1.getParentOrderId());
        assertEquals(new BigDecimal("35.50"), o1.getFinalPrice());
        assertEquals(new BigDecimal("3.55"), o1.getCommissionAmount());
        assertEquals(3, o1.getPlatformStatus()); // SETTLED
        assertEquals(0, o1.getRefundTag());
        assertEquals("user_sid_token_abc", o1.getExternalId()); // 提取 SID 用于会员归因

        // 订单2：售中退款订单
        CpsOrderDTO o2 = orders.get(1);
        assertEquals("SG_ORDER_002", o2.getPlatformOrderId());
        assertEquals(-1, o2.getPlatformStatus()); // INVALID
        assertEquals(1, o2.getRefundTag()); // 标记已退款
        assertEquals("user_sid_token_def", o2.getExternalId());
    }

    @Test
    @DisplayName("fetchActivities：拉取官方常驻活动清单")
    void fetchActivities() {
        var page = client.fetchActivities(CpsThirdPartyActivityRequest.builder().build(),
                CpsVendorConfig.builder().build());

        assertNotNull(page);
        assertEquals(3, page.getList().size());
        assertEquals("淘宝闪购天天领红包", page.getList().get(0).getActivityName());
        assertEquals("10144", page.getList().get(0).getPromotionActivityId());
        assertEquals("浏览店铺做任务", page.getList().get(1).getActivityName());
        assertEquals("10174", page.getList().get(1).getPromotionActivityId());
        assertEquals("零售专属会场", page.getList().get(2).getActivityName());
        assertEquals("10183", page.getList().get(2).getPromotionActivityId());
    }

    @Test
    @DisplayName("isSuccessResponse：处理 TOP error_response 与业务错误码")
    void isSuccessResponse() throws Exception {
        // TOP 成功
        String okJson = """
                {
                  "alibaba_alsc_union_eleme_promotion_officialactivity_get_response": {
                    "result": { "biz_code": 0, "success": true }
                  }
                }
                """;
        assertTrue(client.isSuccessResponse(objectMapper.readTree(okJson)));

        // TOP 系统级错误
        String topErrorJson = """
                {
                  "error_response": {
                    "code": 15,
                    "msg": "Remote service error",
                    "sub_code": "isv.invalid-parameter"
                  }
                }
                """;
        assertFalse(client.isSuccessResponse(objectMapper.readTree(topErrorJson)));

        // 业务级错误
        String bizErrorJson = """
                {
                  "alibaba_alsc_union_eleme_promotion_officialactivity_get_response": {
                    "result": { "biz_code": 50001, "biz_msg": "activity offline" }
                  }
                }
                """;
        assertFalse(client.isSuccessResponse(objectMapper.readTree(bizErrorJson)));
    }

    @Test
    @DisplayName("全流程调用：通过 Mock HttpClient 验证官方活动转链 HTTP 请求与响应流转")
    @SuppressWarnings("unchecked")
    void executePromotionLinkWithMockHttp() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResp = mock(HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn("""
                {
                  "alibaba_alsc_union_eleme_promotion_officialactivity_get_response": {
                    "result": {
                      "data": {
                        "short_url": "https://tb.cn/test_short",
                        "click_url": "https://s.click.ele.me/test_long",
                        "tb_command": "￥test_tkl￥"
                      },
                      "biz_code": 0,
                      "biz_msg": "success"
                    }
                  }
                }
                """);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResp);

        ElemeOfficialVendorClient clientWithMock = new ElemeOfficialVendorClient(mockHttp);
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("10144");
        request.setAdzoneId("pid_mock_1");
        request.setExternalId("sid_member_99");

        CpsVendorConfig config = CpsVendorConfig.builder()
                .appKey("mock_key")
                .appSecret("mock_secret")
                .defaultAdzoneId("pid_mock_1")
                .build();

        CpsPromotionLinkResult result = clientWithMock.generatePromotionLink(request, config);
        assertNotNull(result);
        assertEquals("https://tb.cn/test_short", result.getShortUrl());
        assertEquals("https://s.click.ele.me/test_long", result.getLongUrl());
        assertEquals("￥test_tkl￥", result.getTpwd());
    }
}
