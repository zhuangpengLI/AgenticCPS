package com.qiji.cps.module.ai.service.mcp;

import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.module.ai.framework.ai.config.QijiAiProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMcpIdentitySignerTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-07-10T14:22:10Z");

    @Test
    void sign_shouldProduceDeterministicHmacEnvelopeWithDefaultTtl() throws Exception {
        AiMcpIdentitySigner signer = new AiMcpIdentitySigner(properties("self-mcp", "test-secret"),
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC), () -> "fixed-nonce");

        McpIdentityEnvelope first = signer.sign(validClaims("self-mcp"));
        McpIdentityEnvelope second = signer.sign(validClaims("self-mcp"));
        McpIdentityClaims signedClaims = McpIdentityClaims.fromCanonicalPayload(first.payload());

        assertEquals(first, second);
        assertEquals(ISSUED_AT, signedClaims.issuedAt());
        assertEquals(ISSUED_AT.plusSeconds(60), signedClaims.expiresAt());
        assertEquals("fixed-nonce", signedClaims.nonce());
        assertEquals(expectedSignature("test-secret", first.payload()), first.signature());
    }

    @Test
    void sign_shouldRejectMissingRequiredClaims() {
        AiMcpIdentitySigner signer = new AiMcpIdentitySigner(properties("self-mcp", "test-secret"),
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC), () -> "fixed-nonce");
        McpIdentityClaims claimsWithoutMember = new McpIdentityClaims(null, 20L, 30L, 40L,
                "self-mcp", false, null, null, null, "trace-1");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> signer.sign(claimsWithoutMember));

        assertTrue(exception.getMessage().contains("memberId"));
    }

    @Test
    void sign_shouldRejectClaimForAnotherClientAudience() {
        AiMcpIdentitySigner signer = new AiMcpIdentitySigner(properties("self-mcp", "test-secret"),
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC), () -> "fixed-nonce");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> signer.sign(validClaims("another-client")));

        assertTrue(exception.getMessage().contains("clientName"));
    }

    @Test
    void sign_shouldFailClosedWhenSecretIsBlank() {
        AiMcpIdentitySigner signer = new AiMcpIdentitySigner(properties("self-mcp", "  "),
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC), () -> "fixed-nonce");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> signer.sign(validClaims("self-mcp")));

        assertTrue(exception.getMessage().contains("secret"));
    }

    private static McpIdentityClaims validClaims(String clientName) {
        return new McpIdentityClaims(10L, 20L, 30L, 40L, clientName, false,
                null, null, null, "trace-1");
    }

    private static QijiAiProperties properties(String clientName, String secret) {
        QijiAiProperties properties = new QijiAiProperties();
        QijiAiProperties.McpSelfTest selfTest = new QijiAiProperties.McpSelfTest();
        selfTest.setEnabled(true);
        selfTest.setClientName(clientName);
        selfTest.setBaseUrl("http://127.0.0.1:48080");
        selfTest.setEndpoint("/mcp/cps");
        selfTest.setConnectTimeout(Duration.ofSeconds(3));
        selfTest.setReadTimeout(Duration.ofSeconds(10));
        selfTest.setTtl(Duration.ofSeconds(60));
        selfTest.setSecret(secret);
        properties.getMcp().setSelfTest(selfTest);
        return properties;
    }

    private static String expectedSignature(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

}
