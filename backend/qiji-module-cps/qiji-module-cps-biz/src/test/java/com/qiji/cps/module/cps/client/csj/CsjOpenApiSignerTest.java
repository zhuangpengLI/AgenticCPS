package com.qiji.cps.module.cps.client.csj;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("穿山甲OpenAPI签名与报文封装测试")
class CsjOpenApiSignerTest {

    @Test
    @DisplayName("MD5签名计算 - 验证ASCII字典序与末尾拼装密钥")
    void generateSign_shouldSortAsciiAndAppendSecret() {
        Map<String, Object> params = new LinkedHashMap<>();
        // 乱序插入参数
        params.put("version", "1");
        params.put("timestamp", 1649389006L);
        params.put("app_id", "5271412");
        params.put("req_id", "req-12345");
        params.put("sign_type", "MD5");
        params.put("data", "{\"size\":50}");
        params.put("sign", "should_be_ignored");
        params.put("empty_val", null);

        String secureKey = "test_secure_key_123456";

        // 预期字典序 (排除 sign, sign_type, version):
        // app_id=5271412&data={"size":50}&req_id=req-12345&timestamp=1649389006 + test_secure_key_123456
        String sign = CsjOpenApiSigner.generateSign(params, secureKey);

        assertNotNull(sign);
        assertEquals(32, sign.length());
        assertEquals("1fb0f578dad2c142f08de063bfe1cb1b", sign);
        assertEquals(sign.toLowerCase(), sign); // 必须是32位全小写十六进制

        // 重复计算必须幂等
        String sign2 = CsjOpenApiSigner.generateSign(params, secureKey);
        assertEquals(sign, sign2);
    }

    @Test
    @DisplayName("构建完整Envelope报文 - 包含公共参数与sign")
    void buildEnvelope_shouldIncludePublicParamsAndSign() {
        String appId = "5271412";
        String secureKey = "mock_secret_key";
        String reqId = "test-uuid-001";
        long timestamp = 1700000000L;
        Map<String, Object> data = Map.of("size", 20, "order_type", 2);

        Map<String, Object> envelope = CsjOpenApiSigner.buildEnvelope(appId, secureKey, data, reqId, timestamp);

        assertEquals("5271412", envelope.get("app_id"));
        assertEquals(1700000000L, envelope.get("timestamp"));
        assertEquals("1", envelope.get("version"));
        assertEquals("test-uuid-001", envelope.get("req_id"));
        assertEquals("MD5", envelope.get("sign_type"));
        assertNotNull(envelope.get("data"));
        assertTrue(envelope.get("data").toString().contains("\"order_type\":2"));
        assertNotNull(envelope.get("sign"));

        // 验证 envelope 中的 sign 校验
        String signCheck = CsjOpenApiSigner.generateSign(envelope, secureKey);
        assertEquals(envelope.get("sign"), signCheck);
    }

    @Test
    @DisplayName("空参数防篡改与异常抛出")
    void generateSign_shouldThrowOnEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> CsjOpenApiSigner.generateSign(Map.of("a", 1), null));
        assertThrows(IllegalArgumentException.class,
                () -> CsjOpenApiSigner.generateSign(null, "key"));
    }

    @Test
    @DisplayName("紧凑JSON序列化 - 无多余空格")
    void compactJson_shouldSerializeCompactly() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("a", 1);
        data.put("b", "test");

        String json = CsjOpenApiSigner.compactJson(data);
        assertEquals("{\"a\":1,\"b\":\"test\"}", json);
    }
}
