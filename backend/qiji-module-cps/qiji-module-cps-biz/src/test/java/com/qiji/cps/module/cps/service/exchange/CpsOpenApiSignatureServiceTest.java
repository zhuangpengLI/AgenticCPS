package com.qiji.cps.module.cps.service.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpsOpenApiSignatureServiceTest {

    private CpsOpenApiSignatureService signatureService;
    private CpsAitokenExchangeProperties properties;

    @BeforeEach
    void setUp() {
        signatureService = new CpsOpenApiSignatureService();
        properties = new CpsAitokenExchangeProperties();
        properties.setAppId("AgenticCPS");
        properties.setAppSecret("agentic-cps-dev-secret");
        ReflectionTestUtils.setField(signatureService, "properties", properties);
        ReflectionTestUtils.setField(signatureService, "objectMapper", new ObjectMapper());
    }

    @Test
    void verifyShouldPassForValidSignature() {
        HttpServletRequest request = mockRequest("AgenticCPS", "POST", "/openapi/cpx/event",
                "20260526160000", "nonce-1", "idem-1");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("platformCode", "taobao");
        body.put("promotionMethod", "CPC");
        body.put("eventType", "CLICK");

        String signature = signatureService.sign(properties.getAppSecret(), "POST", "/openapi/cpx/event",
                "20260526160000", "nonce-1", "idem-1", body);
        Mockito.when(request.getHeader("X-Signature")).thenReturn(signature);

        assertDoesNotThrow(() -> signatureService.verify(request, body));
    }

    @Test
    void verifyShouldRejectInvalidAppId() {
        HttpServletRequest request = mockRequest("WrongApp", "POST", "/openapi/cpx/event",
                "20260526160000", "nonce-1", "idem-1");

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
    }

    private HttpServletRequest mockRequest(String appId, String method, String uri, String timestamp,
                                           String nonce, String idempotencyKey) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-App-Id")).thenReturn(appId);
        Mockito.when(request.getHeader("X-Timestamp")).thenReturn(timestamp);
        Mockito.when(request.getHeader("X-Nonce")).thenReturn(nonce);
        Mockito.when(request.getHeader("X-Idempotency-Key")).thenReturn(idempotencyKey);
        Mockito.when(request.getMethod()).thenReturn(method);
        Mockito.when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
