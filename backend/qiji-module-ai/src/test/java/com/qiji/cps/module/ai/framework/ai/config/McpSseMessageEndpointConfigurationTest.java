package com.qiji.cps.module.ai.framework.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerSseProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpSseMessageEndpointConfigurationTest {

    @Test
    void shouldReturnHelpfulMethodNotAllowedForBrowserGetOnMessageEndpoint() throws Exception {
        McpServerSseProperties properties = new McpServerSseProperties();
        properties.setSseEndpoint("/sse");
        properties.setSseMessageEndpoint("/mcp/message");

        MockMvc mockMvc = MockMvcBuilders.routerFunctions(
                new McpSseMessageEndpointConfiguration().mcpSseMessageEndpointGetGuard(properties))
                .build();

        mockMvc.perform(get("/mcp/message"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "POST"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("POST")))
                .andExpect(jsonPath("$.sseEndpoint").value("/sse"))
                .andExpect(jsonPath("$.messageEndpoint").value("/mcp/message"));
    }
}
