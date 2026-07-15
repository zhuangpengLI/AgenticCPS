package com.qiji.cps.module.cps.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CpsVendorRetryPolicy {

    int maxAttempts;
    int initialBackoffMillis;
    int maxBackoffMillis;
    boolean idempotentOnly;
    boolean retryOnTimeout;
    boolean retryOnRateLimit;
    boolean retryOnBusinessError;

    public static CpsVendorRetryPolicy standard() {
        return CpsVendorRetryPolicy.builder()
                .maxAttempts(2)
                .initialBackoffMillis(100)
                .maxBackoffMillis(1_000)
                .idempotentOnly(true)
                .retryOnTimeout(true)
                .retryOnRateLimit(true)
                .retryOnBusinessError(false)
                .build();
    }
}
