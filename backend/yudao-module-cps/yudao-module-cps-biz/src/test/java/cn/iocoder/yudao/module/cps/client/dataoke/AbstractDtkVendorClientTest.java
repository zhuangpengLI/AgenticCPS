package cn.iocoder.yudao.module.cps.client.dataoke;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.cps.client.dto.CpsVendorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                cn.iocoder.yudao.module.cps.client.dto.CpsGoodsSearchRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected cn.iocoder.yudao.module.cps.client.dto.CpsGoodsSearchResult parseSearchResponse(
                JsonNode response, cn.iocoder.yudao.module.cps.client.dto.CpsGoodsSearchRequest request) {
            return null;
        }
        @Override protected String getPromotionLinkApiPath() { return "/test/link"; }
        @Override protected Map<String, Object> buildPromotionLinkParams(
                cn.iocoder.yudao.module.cps.client.dto.CpsPromotionLinkRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected cn.iocoder.yudao.module.cps.client.dto.CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
            return null;
        }
        @Override protected String getOrderQueryApiPath() { return "/test/order"; }
        @Override protected Map<String, Object> buildOrderQueryParams(
                cn.iocoder.yudao.module.cps.client.dto.CpsOrderQueryRequest request, CpsVendorConfig config) {
            return new HashMap<>();
        }
        @Override protected java.util.List<cn.iocoder.yudao.module.cps.client.dto.CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
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
                String.format("appKey=%s&timer=%s&nonce=%s", "myKey123", timer, nonce) + "mySecret456"
        ).toLowerCase();

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

}
