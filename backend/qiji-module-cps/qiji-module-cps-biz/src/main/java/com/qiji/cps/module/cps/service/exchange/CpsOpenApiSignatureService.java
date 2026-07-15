package com.qiji.cps.module.cps.service.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.config.CpsAitokenExchangeProperties;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsOpenApiAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.exchange.CpsOpenApiAccessLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.OPENAPI_HEADER_MISSING;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.OPENAPI_SIGNATURE_INVALID;

@Service
@Slf4j
public class CpsOpenApiSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Resource
    private CpsAitokenExchangeProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CpsOpenApiNonceStore nonceStore;

    @Resource
    private CpsOpenApiAccessLogMapper accessLogMapper;

    private Clock clock = Clock.systemUTC();

    public void verify(HttpServletRequest request, Object body) {
        try {
            doVerify(request, body);
        } catch (RuntimeException e) {
            recordFailure(request, e);
            throw e;
        }
    }

    private void doVerify(HttpServletRequest request, Object body) {
        String appId = requireHeader(request, "X-App-Id");
        if (!properties.getAppId().equals(appId)) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
        String timestamp = requireHeader(request, "X-Timestamp");
        String nonce = requireHeader(request, "X-Nonce");
        String tenantId = requireHeader(request, "X-Tenant-Id");
        validateTenantContext(tenantId);
        String signature = requireHeader(request, "X-Signature");
        Instant requestTime = parseAndValidateTimestamp(timestamp);
        String idempotencyKey = request.getHeader("X-Idempotency-Key");
        String expected = sign(properties.getAppSecret(), request.getMethod(), request.getRequestURI(),
                timestamp, nonce, tenantId, idempotencyKey, body);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
        long expiresAt = requestTime.getEpochSecond() + properties.getSignatureValiditySeconds();
        long ttlSeconds = expiresAt - clock.instant().getEpochSecond();
        if (!nonceStore.consume(appId + ":" + tenantId, nonce,
                Duration.ofSeconds(Math.max(1, ttlSeconds)))) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
    }

    public String sign(String secret, String method, String path, String timestamp, String nonce,
                       String tenantId, String idempotencyKey, Object body) {
        String canonical = method.toUpperCase()
                + "\n" + path
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + tenantId
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

    private void recordFailure(HttpServletRequest request, RuntimeException exception) {
        if (accessLogMapper == null) {
            return;
        }
        try {
            CpsOpenApiAccessLogDO accessLog = CpsOpenApiAccessLogDO.builder()
                    .appId(request.getHeader("X-App-Id"))
                    .requestMethod(request.getMethod())
                    .requestUri(request.getRequestURI())
                    .idempotencyKey(request.getHeader("X-Idempotency-Key"))
                    .requestHeaders(toHeaderSnapshot(request))
                    .status(0)
                    .failureReason(resolveFailureReason(exception))
                    .clientIp(request.getRemoteAddr())
                    .build();
            accessLog.setTenantId(resolveTenantId(request.getHeader("X-Tenant-Id")));
            accessLogMapper.insert(accessLog);
        } catch (Exception auditException) {
            log.warn("[CPS OpenAPI] failed to write signature failure audit log: {}",
                    auditException.getMessage());
        }
    }

    private String toHeaderSnapshot(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-App-Id", request.getHeader("X-App-Id"));
        headers.put("X-Tenant-Id", request.getHeader("X-Tenant-Id"));
        headers.put("X-Timestamp", request.getHeader("X-Timestamp"));
        headers.put("X-Nonce", redact(request.getHeader("X-Nonce")));
        headers.put("X-Idempotency-Key", request.getHeader("X-Idempotency-Key"));
        headers.put("X-Signature", redact(request.getHeader("X-Signature")));
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Long resolveTenantId(String tenantId) {
        try {
            Long currentTenantId = TenantContextHolder.getTenantId();
            return StringUtils.hasText(tenantId) ? Long.valueOf(tenantId) : defaultTenantId(currentTenantId);
        } catch (NumberFormatException e) {
            return defaultTenantId(TenantContextHolder.getTenantId());
        }
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String resolveFailureReason(RuntimeException exception) {
        if (exception instanceof ServiceException serviceException) {
            if (OPENAPI_HEADER_MISSING.getCode().equals(serviceException.getCode())) {
                return "OPENAPI_HEADER_MISSING";
            }
            if (OPENAPI_SIGNATURE_INVALID.getCode().equals(serviceException.getCode())) {
                return "OPENAPI_SIGNATURE_INVALID";
            }
        }
        return exception.getClass().getSimpleName();
    }

    private String redact(String value) {
        return StringUtils.hasText(value) ? "***" : value;
    }

    private Instant parseAndValidateTimestamp(String timestamp) {
        try {
            Instant requestTime = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Instant now = clock.instant();
            if (requestTime.isBefore(now.minusSeconds(properties.getSignatureValiditySeconds()))
                    || requestTime.isAfter(now.plusSeconds(properties.getSignatureMaxFutureSkewSeconds()))) {
                throw exception(OPENAPI_SIGNATURE_INVALID);
            }
            return requestTime;
        } catch (NumberFormatException e) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
    }

    private void validateTenantContext(String tenantId) {
        try {
            Long currentTenantId = TenantContextHolder.getTenantId();
            if (currentTenantId == null || !currentTenantId.equals(Long.valueOf(tenantId))) {
                throw exception(OPENAPI_SIGNATURE_INVALID);
            }
        } catch (NumberFormatException e) {
            throw exception(OPENAPI_SIGNATURE_INVALID);
        }
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
