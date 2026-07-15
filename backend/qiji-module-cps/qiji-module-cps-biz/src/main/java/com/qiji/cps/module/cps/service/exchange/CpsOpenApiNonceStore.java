package com.qiji.cps.module.cps.service.exchange;

import java.time.Duration;

/** Atomic one-time nonce storage for signed CPS OpenAPI requests. */
public interface CpsOpenApiNonceStore {

    boolean consume(String appId, String nonce, Duration ttl);
}
