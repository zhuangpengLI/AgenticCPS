package com.qiji.cps.module.cps.mcp.security;

import java.time.Duration;

/**
 * One-time nonce storage for self-MCP identity envelopes.
 */
public interface CpsMcpNonceStore {

    /**
     * Atomically records a nonce for the specified remaining validity window.
     *
     * @return {@code true} only when the nonce was not already consumed
     */
    boolean consume(String nonce, Duration ttl);

}
