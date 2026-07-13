package com.qiji.cps.module.ai.framework.ai.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Delays initialization of the configured in-process MCP loopback client until the
 * embedded web server is accepting connections.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpLoopbackClientInitializer {

    private static final String CLIENT_NAME_PREFIX = "mcp - ";

    private final QijiAiProperties properties;
    private final List<McpSyncClient> mcpClients;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        QijiAiProperties.McpLoopbackClient loopbackClient = properties.getMcp().getLoopbackClient();
        if (!loopbackClient.isEnabled() || !StringUtils.hasText(loopbackClient.getConnectionName())) {
            return;
        }
        String expectedClientName = CLIENT_NAME_PREFIX + loopbackClient.getConnectionName();
        McpSyncClient client = mcpClients.stream()
                .filter(candidate -> expectedClientName.equals(candidate.getClientInfo().name()))
                .findFirst()
                .orElse(null);
        if (client == null) {
            log.warn("Configured MCP loopback client [{}] is unavailable; skip delayed initialization", expectedClientName);
            return;
        }
        if (client.isInitialized()) {
            return;
        }
        log.info("Initializing MCP loopback client [{}] after application startup", expectedClientName);
        client.initialize();
    }

}
