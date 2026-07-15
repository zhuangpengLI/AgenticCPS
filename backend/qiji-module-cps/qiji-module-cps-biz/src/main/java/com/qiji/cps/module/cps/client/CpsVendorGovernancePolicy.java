package com.qiji.cps.module.cps.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CpsVendorGovernancePolicy {

    int timeoutMillis;
    int rateLimitPerMinute;
    int circuitBreakerFailureThreshold;
    int circuitBreakerOpenMillis;
    boolean tokenRefreshSupported;
    boolean metricsEnabled;
    boolean maskedDiagnosticsEnabled;
    CpsVendorRetryPolicy retryPolicy;

    public static CpsVendorGovernancePolicy standard() {
        return CpsVendorGovernancePolicy.builder()
                .timeoutMillis(5_000)
                .rateLimitPerMinute(60)
                .circuitBreakerFailureThreshold(5)
                .circuitBreakerOpenMillis(10_000)
                .tokenRefreshSupported(false)
                .metricsEnabled(true)
                .maskedDiagnosticsEnabled(true)
                .retryPolicy(CpsVendorRetryPolicy.standard())
                .build();
    }
}
