package com.qiji.cps.module.cps.client.haina;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class HainaMcpClientConfiguration {

    static final String HAINA_HOST = "sse-gw-openapi.zhidemai.com";

    @Bean
    @ConditionalOnProperty(prefix = "qiji.cps.haina", name = "enabled", havingValue = "true", matchIfMissing = true)
    public McpSyncHttpClientRequestCustomizer hainaMcpHttpRequestCustomizer(HainaDecisionProperties properties) {
        return (requestBuilder, method, uri, body, context) -> {
            if (uri != null && HAINA_HOST.equalsIgnoreCase(uri.getHost())
                    && StringUtils.hasText(properties.getApiKey())) {
                requestBuilder.setHeader("x-api-key", properties.getApiKey().trim());
            }
        };
    }

}
