package com.qiji.cps.module.cps.service.exchange;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redis SETNX based replay protection shared by all application instances. */
@Component
public class CpsOpenApiRedisNonceStore implements CpsOpenApiNonceStore {

    private static final String KEY_PREFIX = "cps:openapi:nonce:";

    private final StringRedisTemplate stringRedisTemplate;

    public CpsOpenApiRedisNonceStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean consume(String appId, String nonce, Duration ttl) {
        if (appId == null || appId.isBlank() || nonce == null || nonce.isBlank()
                || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + appId + ":" + nonce, "", ttl));
    }
}
