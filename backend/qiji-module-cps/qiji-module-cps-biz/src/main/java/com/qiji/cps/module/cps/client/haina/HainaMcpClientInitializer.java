package com.qiji.cps.module.cps.client.haina;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Component
public class HainaMcpClientInitializer {

    private final HainaDecisionProperties properties;
    private final List<McpSyncClient> mcpClients;
    private final Executor executor;

    @Autowired
    public HainaMcpClientInitializer(HainaDecisionProperties properties, List<McpSyncClient> mcpClients) {
        this(properties, mcpClients, ForkJoinPool.commonPool());
    }

    HainaMcpClientInitializer(HainaDecisionProperties properties, List<McpSyncClient> mcpClients,
                              Executor executor) {
        this.properties = properties;
        this.mcpClients = mcpClients;
        this.executor = executor;
    }

    /**
     * External MCP initialization must not be part of Spring Boot's startup critical path.
     * The SSE connection can take several seconds or fail when the remote gateway is
     * unavailable, while the rest of the CPS service can operate without Haina evidence.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            log.info("[HainaMcpClientInitializer] 应用已就绪，异步初始化海纳 MCP Client");
            CompletableFuture.runAsync(this::initializeHainaClient, executor)
                    .exceptionally(ex -> {
                        log.warn("[HainaMcpClientInitializer] 海纳 MCP 异步初始化任务异常，AI 对话将继续使用本地 CPS 工具: {}",
                                ex.getMessage());
                        return null;
                    });
        } catch (RuntimeException ex) {
            log.warn("[HainaMcpClientInitializer] 无法提交海纳 MCP 异步初始化任务，AI 对话将继续使用本地 CPS 工具: {}",
                    ex.getMessage());
        }
    }

    void initializeHainaClient() {
        if (!properties.isEnabled()) {
            return;
        }
        McpSyncClient hainaClient = findHainaClient();
        if (hainaClient == null) {
            log.warn("[HainaMcpClientInitializer] 未找到海纳 MCP Client，AI 对话将继续使用本地 CPS 工具");
            return;
        }
        try {
            if (!hainaClient.isInitialized()) {
                hainaClient.initialize();
            }
        } catch (RuntimeException ex) {
            log.warn("[HainaMcpClientInitializer] 海纳 MCP 初始化失败，AI 对话将继续使用本地 CPS 工具: {}",
                    ex.getMessage());
        }
    }

    private McpSyncClient findHainaClient() {
        for (McpSyncClient client : mcpClients) {
            try {
                String clientName = client.getClientInfo() == null ? null : client.getClientInfo().name();
                if (StringUtils.hasText(clientName) && StringUtils.hasText(properties.getClientName())
                        && clientName.toLowerCase().contains(properties.getClientName().toLowerCase())) {
                    return client;
                }
            } catch (RuntimeException ex) {
                log.debug("[HainaMcpClientInitializer] 跳过不可用的 MCP Client: {}", ex.getMessage());
            }
        }
        return null;
    }

}
