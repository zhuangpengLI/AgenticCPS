package com.qiji.cps.module.cps.client.csj;

import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("穿山甲电商联盟（抖音CPS）客户端测试")
class CsjDouyinVendorClientTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;
    private CsjDouyinVendorClient client;
    private CpsVendorConfig validConfig;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockHttpResponse = (HttpResponse<String>) mock(HttpResponse.class);
        client = new CsjDouyinVendorClient(mockHttpClient);

        validConfig = CpsVendorConfig.builder()
                .appKey("5271412")
                .appSecret("test_secure_key_123456")
                .apiBaseUrl("https://ecom.pangolin-sdk-toutiao.com")
                .defaultAdzoneId("default_adzone_100")
                .extraConfig(Map.of("appId", "5271412", "secureKey", "test_secure_key_123456"))
                .build();
    }

    @Test
    @DisplayName("能力描述与配置契约")
    void descriptor_shouldExposeCsjCapabilitiesAndSchema() {
        CpsVendorDescriptor descriptor = client.describe();

        assertEquals("csj", descriptor.getVendorCode());
        assertEquals("douyin", descriptor.getPlatformCode());
        assertEquals("aggregator", descriptor.getVendorType());
        assertTrue(descriptor.getCapabilities().containsAll(Set.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST
        )));

        // 校验必须包含 appId 与 secureKey (支持从 appKey/appSecret 或 extraConfig 获取)
        assertTrue(descriptor.getConfigSchema().validate(validConfig).isValid());

        CpsVendorConfig invalidConfig = CpsVendorConfig.builder()
                .apiBaseUrl("https://ecom.pangolin-sdk-toutiao.com")
                .build();
        assertFalse(descriptor.getConfigSchema().validate(invalidConfig).isValid());

        // 脱敏输出
        Map<String, String> masked = descriptor.getConfigSchema().maskedSummary(validConfig);
        assertEquals("***", masked.get("appSecret"));
    }

    @Test
    @DisplayName("查询订单列表 - 完整映射 28733 协议字段与四态状态流转")
    void queryOrderPage_shouldParseFlowPointsAndMonetaryUnits() throws IOException, InterruptedException {
        String mockResponseJson = """
                {
                    "code": 0,
                    "message": "success",
                    "data": {
                        "cursor": "20",
                        "has_more": true,
                        "orders": [
                            {
                                "order_id": "csj_order_1001",
                                "parent_order_id": "csj_parent_1001",
                                "product_id": "prod_88801",
                                "product_name": "穿山甲抖音测试商品",
                                "product_img": "https://p3.douyinpic.com/item1.jpg",
                                "total_pay_amount": 9900,
                                "item_price": 9900,
                                "commission": 1980,
                                "commission_rate": 2000,
                                "item_num": 1,
                                "flow_point": "PAY_SUCC",
                                "order_time": 1700000000,
                                "pay_time": 1700000010,
                                "pid": "adzone_01",
                                "custom_info": "member_uid_999"
                            },
                            {
                                "order_id": "csj_order_1002",
                                "product_id": "prod_88802",
                                "product_name": "已收货商品",
                                "total_pay_amount": 5000,
                                "commission": 500,
                                "commission_rate": 10,
                                "flow_point": "CONFIRM",
                                "order_time": 1700000100
                            },
                            {
                                "order_id": "csj_order_1003",
                                "product_id": "prod_88803",
                                "product_name": "已结算商品",
                                "total_pay_amount": 3000,
                                "commission": 300,
                                "flow_point": "SETTLE",
                                "order_time": 1700000200
                            },
                            {
                                "order_id": "csj_order_1004",
                                "product_id": "prod_88804",
                                "product_name": "已退款商品",
                                "total_pay_amount": 2000,
                                "commission": 200,
                                "flow_point": "REFUND",
                                "order_time": 1700000300
                            }
                        ]
                    }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(mockResponseJson);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setPageSize(20);
        request.setPageNo(1);
        request.setStartTime("2026-10-26 00:00:00");
        request.setEndTime("2026-10-26 23:59:59");

        CpsOrderPageResult result = client.queryOrderPage(request, validConfig);

        assertNotNull(result);
        assertTrue(result.isHasMore());
        assertEquals("20", result.getNextCursor());
        assertEquals(4, result.getItems().size());

        // 订单 1: PAY_SUCC -> PAID (1)
        CpsOrderDTO o1 = result.getItems().get(0);
        assertEquals("csj_order_1001", o1.getPlatformOrderId());
        assertEquals("csj_parent_1001", o1.getParentOrderId());
        assertEquals("prod_88801", o1.getItemId());
        assertEquals("穿山甲抖音测试商品", o1.getItemTitle());
        assertEquals(new BigDecimal("99.00"), o1.getFinalPrice()); // 9900分 -> 99.00元
        assertEquals(new BigDecimal("19.80"), o1.getCommissionAmount()); // 1980分 -> 19.80元
        assertEquals(new BigDecimal("20.00"), o1.getCommissionRate()); // 2000基点 -> 20.00%
        assertEquals(1, o1.getPlatformStatus()); // PAID
        assertEquals(0, o1.getRefundTag());
        assertEquals("member_uid_999", o1.getExternalId()); // custom_info 渠道归因标识
        assertEquals("adzone_01", o1.getAdzoneId());
        assertEquals("PAY_SUCC", o1.getExtraFields().get("flowPoint"));

        // 订单 2: CONFIRM -> RECEIVED (2)
        CpsOrderDTO o2 = result.getItems().get(1);
        assertEquals(2, o2.getPlatformStatus());
        assertEquals(new BigDecimal("50.00"), o2.getFinalPrice());
        assertEquals(new BigDecimal("10.00"), o2.getCommissionRate());

        // 订单 3: SETTLE -> SETTLED (3)
        CpsOrderDTO o3 = result.getItems().get(2);
        assertEquals(3, o3.getPlatformStatus());

        // 订单 4: REFUND -> INVALID (-1), refundTag = 1
        CpsOrderDTO o4 = result.getItems().get(3);
        assertEquals(-1, o4.getPlatformStatus());
        assertEquals(1, o4.getRefundTag());

        // 验证发送的 HTTP 请求路径与 Header
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any());
        HttpRequest sentRequest = requestCaptor.getValue();
        assertEquals("POST", sentRequest.method());
        assertEquals("https://ecom.pangolin-sdk-toutiao.com/order/search", sentRequest.uri().toString());
        assertTrue(sentRequest.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    @DisplayName("上游接口报错 - 抛出结构化 CpsVendorException")
    void queryOrderPage_upstreamError_shouldThrowStructuredException() throws IOException, InterruptedException {
        String mockErrorJson = """
                {
                    "code": 10001,
                    "message": "sign check failed"
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(mockErrorJson);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        CpsOrderQueryRequest request = new CpsOrderQueryRequest();

        CpsVendorException exception = assertThrows(CpsVendorException.class,
                () -> client.queryOrderPage(request, validConfig));

        assertEquals("UPSTREAM_REJECTED", exception.getCode());
        assertEquals("csj", exception.getVendorCode());
        assertEquals("douyin", exception.getPlatformCode());
        assertEquals("10001", exception.getUpstreamCode());
        assertTrue(exception.getMessage().contains("sign check failed"));
    }

    @Test
    @DisplayName("测试连接 - code=0 成功，code!=0 失败")
    void testConnection_shouldReturnTrueOnSuccess() throws IOException, InterruptedException {
        String successJson = "{\"code\": 0, \"message\": \"success\", \"data\": {\"orders\": []}}";
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(successJson);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        assertTrue(client.testConnection(validConfig));

        // 模拟失败
        String failJson = "{\"code\": 40003, \"message\": \"invalid app_id\"}";
        when(mockHttpResponse.body()).thenReturn(failJson);
        assertFalse(client.testConnection(validConfig));
    }
}
