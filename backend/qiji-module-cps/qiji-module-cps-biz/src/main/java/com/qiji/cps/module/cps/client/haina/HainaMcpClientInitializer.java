package com.qiji.cps.module.cps.client.haina;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HainaMcpClientInitializer implements ApplicationRunner {

    private final HainaDecisionProperties properties;
    private final List<McpSyncClient> mcpClients;

    @Override
    public void run(ApplicationArguments args) {
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
