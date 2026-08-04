package com.qiji.cps.module.cps.client.haina;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HainaMcpClientConfigurationTest {

    private static final URI HAINA_URI = URI.create(
            "https://sse-gw-openapi.zhidemai.com/mcp-servers/ai-mcp/sse");

    @Test
    void propertiesEnableHainaByDefault() {
        assertTrue(new HainaDecisionProperties().isEnabled());
    }

    @Test
    void requestCustomizerAddsApiKeyOnlyForHaina() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        properties.setApiKey("test-key");
        McpSyncHttpClientRequestCustomizer customizer =
                new HainaMcpClientConfiguration().hainaMcpHttpRequestCustomizer(properties);

        HttpRequest.Builder hainaRequest = HttpRequest.newBuilder(HAINA_URI);
        customizer.customize(hainaRequest, "GET", HAINA_URI, null, null);
        assertEquals("test-key", hainaRequest.build().headers().firstValue("x-api-key").orElseThrow());

        URI otherUri = URI.create("http://127.0.0.1:48080/sse");
        HttpRequest.Builder otherRequest = HttpRequest.newBuilder(otherUri);
        customizer.customize(otherRequest, "GET", otherUri, null, null);
        assertFalse(otherRequest.build().headers().firstValue("x-api-key").isPresent());
    }

    @Test
    void requestCustomizerSkipsBlankApiKey() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        properties.setApiKey(" ");
        McpSyncHttpClientRequestCustomizer customizer =
                new HainaMcpClientConfiguration().hainaMcpHttpRequestCustomizer(properties);
        HttpRequest.Builder request = HttpRequest.newBuilder(HAINA_URI);

        customizer.customize(request, "GET", HAINA_URI, null, null);

        assertFalse(request.build().headers().firstValue("x-api-key").isPresent());
    }

}
