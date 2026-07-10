package com.qiji.cps.module.ai.service.mcp;

import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.module.ai.framework.ai.config.QijiAiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Creates short-lived HMAC-SHA256 envelopes for trusted calls from AI chat to the local MCP server.
 */
@Service
public class AiMcpIdentitySigner {

    private final QijiAiProperties properties;
    private final Clock clock;
    private final Supplier<String> nonceSupplier;

    @Autowired
    public AiMcpIdentitySigner(QijiAiProperties properties) {
        this(properties, Clock.systemUTC(), () -> UUID.randomUUID().toString());
    }

    AiMcpIdentitySigner(QijiAiProperties properties, Clock clock, Supplier<String> nonceSupplier) {
        this.properties = properties;
        this.clock = clock;
        this.nonceSupplier = nonceSupplier;
    }

    public McpIdentityEnvelope sign(McpIdentityClaims claims) {
        QijiAiProperties.McpSelfTest selfTest = selfTestProperties();
        String secret = requiredText("secret", selfTest.getSecret(), IllegalStateException::new);
        String clientName = requiredText("clientName", selfTest.getClientName(), IllegalStateException::new);
        validateClaims(claims, clientName);

        Duration ttl = selfTest.getTtl() == null ? Duration.ofSeconds(60) : selfTest.getTtl();
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("ttl must be positive");
        }
        Instant issuedAt = clock.instant();
        String nonce = requiredText("nonce", nonceSupplier.get(), IllegalStateException::new);
        McpIdentityClaims signedClaims = claims.withSignatureMetadata(issuedAt, issuedAt.plus(ttl), nonce);
        String payload = signedClaims.toCanonicalPayload();
        return new McpIdentityEnvelope(payload, hmacSha256(payload, secret));
    }

    private QijiAiProperties.McpSelfTest selfTestProperties() {
        if (properties == null || properties.getMcp() == null || properties.getMcp().getSelfTest() == null) {
            throw new IllegalStateException("MCP self-test configuration is required");
        }
        return properties.getMcp().getSelfTest();
    }

    private static void validateClaims(McpIdentityClaims claims, String clientName) {
        if (claims == null) {
            throw new IllegalArgumentException("claims must not be null");
        }
        positive("memberId", claims.memberId());
        positive("tenantId", claims.tenantId());
        positive("actorUserId", claims.actorUserId());
        positive("conversationId", claims.conversationId());
        String claimClientName = requiredText("clientName", claims.clientName(), IllegalArgumentException::new);
        requiredText("traceId", claims.traceId(), IllegalArgumentException::new);
        if (!clientName.equals(claimClientName)) {
            throw new IllegalArgumentException("clientName does not match the configured MCP audience");
        }
    }

    private static void positive(String fieldName, Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private static <T extends RuntimeException> String requiredText(String fieldName, String value,
                                                                       java.util.function.Function<String, T> exceptionFactory) {
        if (value == null || value.isBlank()) {
            throw exceptionFactory.apply(fieldName + " must not be blank");
        }
        return value;
    }

    private static String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign MCP identity payload", exception);
        }
    }

}
