package com.qiji.cps.module.cps.service.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.OPENAPI_HEADER_MISSING;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.OPENAPI_SIGNATURE_INVALID;

@Service
public class CpsOpenApiSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Resource
    private CpsAitokenExchangeProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    public void verify(HttpServletRequest request, Object body) {
        String appId = requireHeader(request, "X-App-Id");
        if (!properties.getAppId().equals(appId)) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
        String timestamp = requireHeader(request, "X-Timestamp");
        String nonce = requireHeader(request, "X-Nonce");
        String signature = requireHeader(request, "X-Signature");
        String idempotencyKey = request.getHeader("X-Idempotency-Key");
        String expected = sign(properties.getAppSecret(), request.getMethod(), request.getRequestURI(),
                timestamp, nonce, idempotencyKey, body);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
    }

    public String sign(String secret, String method, String path, String timestamp, String nonce,
                       String idempotencyKey, Object body) {
        String canonical = method.toUpperCase()
                + "\n" + path
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + (idempotencyKey == null ? "" : idempotencyKey)
                + "\n" + sha256Hex(serializeBody(body));
        return hmacSha256Base64(secret, canonical);
    }

    private String requireHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            throw exception(OPENAPI_HEADER_MISSING, name);
        }
        return value;
    }

    private byte[] serializeBody(Object body) {
        if (body == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize body failed", e);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String hmacSha256Base64(String secret, String text) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getEncoder().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
