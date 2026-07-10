package com.qiji.cps.module.cps.mcp.security;

import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.ai.framework.ai.config.QijiAiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpsMcpIdentityVerifierTest {

    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");
    private static final String SECRET = "test-secret";

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void springContext_createsVerifierWithProductionConstructor() {
        assertDoesNotThrow(() -> {
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.registerBean(QijiAiProperties.class, () -> properties(SECRET));
                context.registerBean(CpsMcpNonceStore.class, InMemoryNonceStore::new);
                context.register(CpsMcpIdentityVerifier.class);
                context.refresh();
                context.getBean(CpsMcpIdentityVerifier.class);
            }
        });
    }

    @Test
    void verify_acceptsValidEnvelopeAndConsumesNonceOnce() {
        TenantContextHolder.setTenantId(20L);
        CpsMcpIdentityVerifier verifier = verifier(new InMemoryNonceStore());
        McpIdentityClaims claims = claims(20L, "nonce-1", NOW.minusSeconds(1), NOW.plusSeconds(30));

        McpIdentityClaims verified = verifier.verify(envelope(claims));

        assertEquals(10L, verified.memberId());
        assertThrows(SecurityException.class, () -> verifier.verify(envelope(claims)));
    }

    @Test
    void verify_rejectsTamperingAndInvalidTemporalAudienceTenantOrSecretInputs() {
        TenantContextHolder.setTenantId(20L);
        CpsMcpIdentityVerifier verifier = verifier(new InMemoryNonceStore());
        McpIdentityClaims valid = claims(20L, "nonce-2", NOW.minusSeconds(1), NOW.plusSeconds(30));

        McpIdentityEnvelope signed = envelope(valid);
        assertThrows(SecurityException.class, () -> verifier.verify(new McpIdentityEnvelope(
                signed.payload() + "\nextra=bad", signed.signature())));
        assertThrows(SecurityException.class, () -> verifier.verify(new McpIdentityEnvelope(
                signed.payload(), signed.signature() + "x")));
        String nonCanonicalPayload = signed.payload().replace("clientName=c2VsZi1tY3A", "clientName=c2VsZi1tY3A=");
        assertThrows(SecurityException.class, () -> verifier.verify(new McpIdentityEnvelope(
                nonCanonicalPayload, hmac(nonCanonicalPayload))));
        assertThrows(SecurityException.class, () -> verifier.verify(envelope(
                claims(20L, "expired", NOW.minusSeconds(30), NOW.minusSeconds(1)))));
        assertThrows(SecurityException.class, () -> verifier.verify(envelope(
                claims(20L, "future", NOW.plusSeconds(1), NOW.plusSeconds(30)))));
        assertThrows(SecurityException.class, () -> verifier.verify(envelope(
                new McpIdentityClaims(10L, 20L, 30L, 40L, "other-client", false,
                        NOW.minusSeconds(1), NOW.plusSeconds(30), "wrong-client", "trace"))));
        assertThrows(SecurityException.class, () -> verifier.verify(envelope(
                claims(21L, "wrong-tenant", NOW.minusSeconds(1), NOW.plusSeconds(30)))));
        assertThrows(SecurityException.class, () -> verifier.verify(new McpIdentityEnvelope("memberId=10", "signature")));

        QijiAiProperties blankSecret = properties("  ");
        CpsMcpIdentityVerifier blankSecretVerifier = new CpsMcpIdentityVerifier(blankSecret,
                new InMemoryNonceStore(), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(SecurityException.class, () -> blankSecretVerifier.verify(envelope(valid)));
    }

    private static CpsMcpIdentityVerifier verifier(CpsMcpNonceStore nonceStore) {
        return new CpsMcpIdentityVerifier(properties(SECRET), nonceStore, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static QijiAiProperties properties(String secret) {
        QijiAiProperties properties = new QijiAiProperties();
        properties.getMcp().getSelfTest().setEnabled(true);
        properties.getMcp().getSelfTest().setClientName("self-mcp");
        properties.getMcp().getSelfTest().setSecret(secret);
        return properties;
    }

    private static McpIdentityClaims claims(Long tenantId, String nonce, Instant issuedAt, Instant expiresAt) {
        return new McpIdentityClaims(10L, tenantId, 30L, 40L, "self-mcp", false,
                issuedAt, expiresAt, nonce, "trace");
    }

    private static McpIdentityEnvelope envelope(McpIdentityClaims claims) {
        String payload = claims.toCanonicalPayload();
        return new McpIdentityEnvelope(payload, hmac(payload));
    }

    private static String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }

    private static class InMemoryNonceStore implements CpsMcpNonceStore {

        private final Set<String> nonces = ConcurrentHashMap.newKeySet();

        @Override
        public boolean consume(String nonce, Duration ttl) {
            return ttl != null && !ttl.isNegative() && !ttl.isZero() && nonces.add(nonce);
        }
    }

}
