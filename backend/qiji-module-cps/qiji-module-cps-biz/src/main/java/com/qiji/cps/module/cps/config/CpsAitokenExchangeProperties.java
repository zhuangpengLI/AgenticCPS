package com.qiji.cps.module.cps.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qiji.cps.aitoken")
public class CpsAitokenExchangeProperties {

    private String baseUrl = "http://localhost:8081";

    private String appId = "AgenticCPS";

    private String appSecret = "agentic-cps-dev-secret";

    private String sourceSystem = "AgenticCPS";

    private String sourceAsset = "REBATE";

    private String targetAsset = "TOKEN";

    /** OpenAPI signature validity window, in seconds. */
    private long signatureValiditySeconds = 300;

    /** Maximum accepted clock skew into the future, in seconds. */
    private long signatureMaxFutureSkewSeconds = 30;
}
