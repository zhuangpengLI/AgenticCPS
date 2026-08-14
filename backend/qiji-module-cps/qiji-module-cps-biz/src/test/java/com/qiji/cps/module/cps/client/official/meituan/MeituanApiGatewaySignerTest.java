package com.qiji.cps.module.cps.client.official.meituan;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeituanApiGatewaySignerTest {

    @Test
    void buildsCanonicalStringUsingOfficialHeaderOrder() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-MD5", "abc");
        headers.put("S-Ca-App", "app");
        headers.put("S-Ca-Timestamp", "123");

        assertEquals("POST\nabc\nS-Ca-App:app\nS-Ca-Timestamp:123\n"
                        + "/cps_open/common/api/v1/query_coupon",
                MeituanApiGatewaySigner.buildStringToSign("POST",
                        "/cps_open/common/api/v1/query_coupon", headers, Map.of(),
                        List.of("S-Ca-Timestamp", "S-Ca-App")));
    }

    @Test
    void doesNotAppendQuestionMarkWhenQueryHasNoValidKeys() {
        assertEquals("GET\n\n/path",
                MeituanApiGatewaySigner.buildStringToSign("GET", "/path", Map.of(),
                        Map.of("", "ignored"), List.of()));
    }
}
