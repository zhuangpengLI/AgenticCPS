package com.qiji.cps.module.cps.mcp.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed nonce store. Redis SETNX makes concurrent replay attempts race safely.
 */
@Component
public class CpsMcpRedisNonceStore implements CpsMcpNonceStore {

    private static final String KEY_PREFIX = "cps:mcp:self-test:nonce:";

    private final StringRedisTemplate stringRedisTemplate;

    public CpsMcpRedisNonceStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean consume(String nonce, Duration ttl) {
        if (nonce == null || nonce.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + nonce, "", ttl));
    }

}
