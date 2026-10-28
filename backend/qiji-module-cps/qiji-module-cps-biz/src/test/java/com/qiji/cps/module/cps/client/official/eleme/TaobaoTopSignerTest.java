package com.qiji.cps.module.cps.client.official.eleme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaobaoTopSignerTest {

    @Test
    @DisplayName("TOP MD5 签名计算：参数字典序排序并前后拼接 secret 计算 MD5 大写")
    void testSignCalculation() {
        String secret = "test_secret_123456";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "alibaba.alsc.union.eleme.promotion.officialactivity.get");
        params.put("app_key", "30001234");
        params.put("timestamp", "2026-10-28 12:00:00");
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");
        params.put("query_request", "{\"activity_id\":\"10144\",\"pid\":\"alsc_test_pid\"}");

        String sign1 = TaobaoTopSigner.sign(params, secret);
        assertNotNull(sign1);
        assertEquals(32, sign1.length());
        assertEquals(sign1.toUpperCase(), sign1);

        // 打乱参数顺序，签名结果必须完全一致（因为内部会做字典序排序）
        Map<String, String> shuffled = new LinkedHashMap<>();
        shuffled.put("v", "2.0");
        shuffled.put("timestamp", "2026-10-28 12:00:00");
        shuffled.put("query_request", "{\"activity_id\":\"10144\",\"pid\":\"alsc_test_pid\"}");
        shuffled.put("method", "alibaba.alsc.union.eleme.promotion.officialactivity.get");
        shuffled.put("sign_method", "md5");
        shuffled.put("app_key", "30001234");
        shuffled.put("format", "json");

        String sign2 = TaobaoTopSigner.sign(shuffled, secret);
        assertEquals(sign1, sign2);
    }

    @Test
    @DisplayName("TOP MD5 签名应忽略原有 sign 参数与空值参数")
    void testSignIgnoresExistingSignAndEmptyValues() {
        String secret = "sec_abc";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("k1", "v1");
        params.put("k2", "v2");

        String signWithout = TaobaoTopSigner.sign(params, secret);

        params.put("sign", "old_or_dummy_sign");
        params.put("k3_empty", "");
        params.put("k4_null", null);

        String signWith = TaobaoTopSigner.sign(params, secret);
        assertEquals(signWithout, signWith);
    }

    @Test
    @DisplayName("buildSignedParams 应自动注入 TOP 公共系统参数并生成 sign")
    void testBuildSignedParams() {
        String appKey = "test_key_888";
        String appSecret = "test_sec_999";
        Map<String, String> bizParams = Map.of("query_request", "{\"test\":true}");

        Map<String, String> signed = TaobaoTopSigner.buildSignedParams(
                "alibaba.alsc.union.eleme.promotion.storepromotion.query",
                appKey, appSecret, bizParams);

        assertEquals("alibaba.alsc.union.eleme.promotion.storepromotion.query", signed.get("method"));
        assertEquals(appKey, signed.get("app_key"));
        assertEquals("json", signed.get("format"));
        assertEquals("2.0", signed.get("v"));
        assertEquals("md5", signed.get("sign_method"));
        assertEquals("{\"test\":true}", signed.get("query_request"));
        assertNotNull(signed.get("timestamp"));
        assertNotNull(signed.get("sign"));
        assertEquals(32, signed.get("sign").length());
    }

    @Test
    @DisplayName("buildFormUrlEncoded 将键值对编码为表单格式")
    void testBuildFormUrlEncoded() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("k1", "v1");
        params.put("msg", "hello world & foo=bar");

        String encoded = TaobaoTopSigner.buildFormUrlEncoded(params);
        assertTrue(encoded.contains("k1=v1"));
        assertTrue(encoded.contains("msg=hello+world+%26+foo%3Dbar"));
    }
}
