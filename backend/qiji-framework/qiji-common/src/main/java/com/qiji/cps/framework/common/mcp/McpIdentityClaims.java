package com.qiji.cps.framework.common.mcp;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Claims carried by a signed MCP identity envelope.
 *
 * <p>The canonical payload has a fixed field order and URL-safe Base64 encoded text values so it can be
 * signed and verified without relying on a JSON serializer's ordering behaviour.</p>
 */
public record McpIdentityClaims(
        Long memberId,
        Long tenantId,
        Long actorUserId,
        Long conversationId,
        String clientName,
        boolean allowMutation,
        Instant issuedAt,
        Instant expiresAt,
        String nonce,
        String traceId) {

    private static final String VERSION = "v1";

    public McpIdentityClaims withSignatureMetadata(Instant issuedAt, Instant expiresAt, String nonce) {
        return new McpIdentityClaims(memberId, tenantId, actorUserId, conversationId, clientName, allowMutation,
                issuedAt, expiresAt, nonce, traceId);
    }

    public String toCanonicalPayload() {
        return String.join("\n",
                "version=" + VERSION,
                "memberId=" + requiredLong("memberId", memberId),
                "tenantId=" + requiredLong("tenantId", tenantId),
                "actorUserId=" + requiredLong("actorUserId", actorUserId),
                "conversationId=" + requiredLong("conversationId", conversationId),
                "clientName=" + encode(requiredText("clientName", clientName)),
                "allowMutation=" + allowMutation,
                "issuedAt=" + requiredInstant("issuedAt", issuedAt).toEpochMilli(),
                "expiresAt=" + requiredInstant("expiresAt", expiresAt).toEpochMilli(),
                "nonce=" + encode(requiredText("nonce", nonce)),
                "traceId=" + encode(requiredText("traceId", traceId)));
    }

    public static McpIdentityClaims fromCanonicalPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
        String[] lines = payload.split("\\n", -1);
        String[] fields = { "version", "memberId", "tenantId", "actorUserId", "conversationId", "clientName",
                "allowMutation", "issuedAt", "expiresAt", "nonce", "traceId" };
        if (lines.length != fields.length) {
            throw new IllegalArgumentException("payload field count is invalid");
        }

        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String prefix = fields[i] + "=";
            if (!lines[i].startsWith(prefix)) {
                throw new IllegalArgumentException("payload field order is invalid");
            }
            values[i] = lines[i].substring(prefix.length());
        }
        if (!VERSION.equals(values[0])) {
            throw new IllegalArgumentException("payload version is unsupported");
        }
        if (!"true".equals(values[6]) && !"false".equals(values[6])) {
            throw new IllegalArgumentException("allowMutation is invalid");
        }
        return new McpIdentityClaims(
                parseLong("memberId", values[1]),
                parseLong("tenantId", values[2]),
                parseLong("actorUserId", values[3]),
                parseLong("conversationId", values[4]),
                decode("clientName", values[5]),
                Boolean.parseBoolean(values[6]),
                parseInstant("issuedAt", values[7]),
                parseInstant("expiresAt", values[8]),
                decode("nonce", values[9]),
                decode("traceId", values[10]));
    }

    private static long requiredLong(String fieldName, Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static Instant requiredInstant(String fieldName, Instant value) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static String requiredText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static long parseLong(String fieldName, String value) {
        try {
            return requiredLong(fieldName, Long.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " is invalid", exception);
        }
    }

    private static Instant parseInstant(String fieldName, String value) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " is invalid", exception);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String fieldName, String value) {
        try {
            return requiredText(fieldName, new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " is invalid", exception);
        }
    }

}
