package com.qiji.cps.module.cps.service.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsOpenApiAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsOpenApiAccessLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsOpenApiSignatureServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-13T07:00:00Z");

    private CpsOpenApiSignatureService signatureService;
    private CpsAitokenExchangeProperties properties;
    private TestNonceStore nonceStore;
    private CpsOpenApiAccessLogMapper accessLogMapper;

    @BeforeEach
    void setUp() {
        signatureService = new CpsOpenApiSignatureService();
        properties = new CpsAitokenExchangeProperties();
        properties.setAppId("AgenticCPS");
        properties.setAppSecret("agentic-cps-dev-secret");
        properties.setSignatureValiditySeconds(300);
        properties.setSignatureMaxFutureSkewSeconds(30);
        nonceStore = new TestNonceStore();
        ReflectionTestUtils.setField(signatureService, "properties", properties);
        ReflectionTestUtils.setField(signatureService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(signatureService, "nonceStore", nonceStore);
        accessLogMapper = Mockito.mock(CpsOpenApiAccessLogMapper.class);
        ReflectionTestUtils.setField(signatureService, "accessLogMapper", accessLogMapper);
        ReflectionTestUtils.setField(signatureService, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void verifyShouldPassForValidSignature() {
        HttpServletRequest request = mockRequest("AgenticCPS", "1", "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "nonce-1", "idem-1");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("platformCode", "taobao");
        body.put("promotionMethod", "CPC");
        body.put("eventType", "CLICK");

        String signature = signatureService.sign(properties.getAppSecret(), "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "nonce-1", "1", "idem-1", body);
        Mockito.when(request.getHeader("X-Signature")).thenReturn(signature);

        assertDoesNotThrow(() -> signatureService.verify(request, body));
    }

    @Test
    void verifyShouldRejectInvalidAppId() {
        HttpServletRequest request = mockRequest("WrongApp", "1", "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "nonce-1", "idem-1");

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
    }

    @Test
    void verifyShouldRejectExpiredTimestamp() {
        HttpServletRequest request = signedRequest(NOW.minusSeconds(301), "expired", null);

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
        org.junit.jupiter.api.Assertions.assertEquals(0, nonceStore.consumeAttempts.get());
    }

    @Test
    void verifyShouldRejectTimestampTooFarInFuture() {
        HttpServletRequest request = signedRequest(NOW.plusSeconds(31), "future", null);

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
        org.junit.jupiter.api.Assertions.assertEquals(0, nonceStore.consumeAttempts.get());
    }

    @Test
    void verifyShouldRejectReplay() {
        HttpServletRequest request = signedRequest(NOW, "replay", null);

        assertDoesNotThrow(() -> signatureService.verify(request, null));
        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
    }

    @Test
    void invalidSignatureShouldNotConsumeNonce() {
        HttpServletRequest invalid = mockRequest("AgenticCPS", "1", "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "retry-after-invalid", null);
        Mockito.when(invalid.getHeader("X-Signature")).thenReturn("invalid");

        assertThrows(RuntimeException.class, () -> signatureService.verify(invalid, null));
        assertDoesNotThrow(() -> signatureService.verify(
                signedRequest(NOW, "retry-after-invalid", null), null));
    }

    @Test
    void invalidSignatureShouldAppendImmutableAuditLogWithoutSecrets() {
        HttpServletRequest invalid = mockRequest("AgenticCPS", "1", "POST", "/openapi/cps/rebate/freeze",
                String.valueOf(NOW.getEpochSecond()), "nonce-secret-1", "idem-1");
        Mockito.when(invalid.getHeader("X-Signature")).thenReturn("signature-secret-1");
        Mockito.when(invalid.getRemoteAddr()).thenReturn("10.0.0.8");

        assertThrows(RuntimeException.class, () -> signatureService.verify(invalid, null));

        ArgumentCaptor<CpsOpenApiAccessLogDO> captor = ArgumentCaptor.forClass(CpsOpenApiAccessLogDO.class);
        Mockito.verify(accessLogMapper).insert(captor.capture());
        CpsOpenApiAccessLogDO log = captor.getValue();
        assertEquals("AgenticCPS", log.getAppId());
        assertEquals(1L, log.getTenantId());
        assertEquals("POST", log.getRequestMethod());
        assertEquals("/openapi/cps/rebate/freeze", log.getRequestUri());
        assertEquals("idem-1", log.getIdempotencyKey());
        assertEquals(0, log.getStatus());
        assertEquals("OPENAPI_SIGNATURE_INVALID", log.getFailureReason());
        assertEquals("10.0.0.8", log.getClientIp());
        assertTrue(log.getRequestHeaders().contains("\"X-Signature\":\"***\""));
        assertTrue(log.getRequestHeaders().contains("\"X-Nonce\":\"***\""));
        assertFalse(log.getRequestHeaders().contains("signature-secret-1"));
        assertFalse(log.getRequestHeaders().contains("nonce-secret-1"));
    }

    @Test
    void concurrentReplayShouldAllowExactlyOneRequest() throws Exception {
        HttpServletRequest request = signedRequest(NOW, "concurrent", null);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    signatureService.verify(request, null);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // Replay attempts are expected to fail.
                }
            });
        }
        org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        org.junit.jupiter.api.Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        org.junit.jupiter.api.Assertions.assertEquals(1, successes.get());
    }

    @Test
    void verifyShouldRejectMissingTenantHeader() {
        HttpServletRequest request = mockRequest("AgenticCPS", null, "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "missing-tenant", null);
        Mockito.when(request.getHeader("X-Signature")).thenReturn(signatureService.sign(
                properties.getAppSecret(), "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "missing-tenant", "1", null, null));

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
    }

    @Test
    void verifyShouldRejectSignatureWhenTenantHeaderWasTampered() {
        TenantContextHolder.setTenantId(2L);
        HttpServletRequest request = mockRequest("AgenticCPS", "2", "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "tampered-tenant", null);
        Mockito.when(request.getHeader("X-Signature")).thenReturn(signatureService.sign(
                properties.getAppSecret(), "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "tampered-tenant", "1", null, null));

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
    }

    @Test
    void verifyShouldRejectTenantHeaderDifferentFromTenantContext() {
        HttpServletRequest request = mockRequest("AgenticCPS", "2", "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "context-mismatch", null);
        Mockito.when(request.getHeader("X-Signature")).thenReturn(signatureService.sign(
                properties.getAppSecret(), "POST", "/openapi/cpx/event",
                String.valueOf(NOW.getEpochSecond()), "context-mismatch", "2", null, null));

        assertThrows(RuntimeException.class, () -> signatureService.verify(request, null));
    }

    private HttpServletRequest signedRequest(Instant instant, String nonce, Object body) {
        String timestamp = String.valueOf(instant.getEpochSecond());
        HttpServletRequest request = mockRequest("AgenticCPS", "1", "POST", "/openapi/cpx/event",
                timestamp, nonce, null);
        Mockito.when(request.getHeader("X-Signature")).thenReturn(signatureService.sign(
                properties.getAppSecret(), "POST", "/openapi/cpx/event", timestamp, nonce, "1", null, body));
        return request;
    }

    private HttpServletRequest mockRequest(String appId, String tenantId, String method, String uri, String timestamp,
                                           String nonce, String idempotencyKey) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-App-Id")).thenReturn(appId);
        Mockito.when(request.getHeader("X-Tenant-Id")).thenReturn(tenantId);
        Mockito.when(request.getHeader("X-Timestamp")).thenReturn(timestamp);
        Mockito.when(request.getHeader("X-Nonce")).thenReturn(nonce);
        Mockito.when(request.getHeader("X-Idempotency-Key")).thenReturn(idempotencyKey);
        Mockito.when(request.getMethod()).thenReturn(method);
        Mockito.when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    private static class TestNonceStore implements CpsOpenApiNonceStore {

        private final AtomicBoolean consumed = new AtomicBoolean();
        private final AtomicInteger consumeAttempts = new AtomicInteger();

        @Override
        public boolean consume(String appId, String nonce, Duration ttl) {
            consumeAttempts.incrementAndGet();
            return consumed.compareAndSet(false, true);
        }
    }
}
