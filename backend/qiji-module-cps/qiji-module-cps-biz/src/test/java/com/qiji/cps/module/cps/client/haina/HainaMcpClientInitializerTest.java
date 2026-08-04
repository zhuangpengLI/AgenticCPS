package com.qiji.cps.module.cps.client.haina;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HainaMcpClientInitializerTest extends BaseMockitoUnitTest {

    @Mock
    private McpSyncClient hainaClient;

    @Test
    void runInitializesConfiguredHainaClient() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        when(hainaClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - haina", "1"));
        when(hainaClient.isInitialized()).thenReturn(false);
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(properties, List.of(hainaClient));

        initializer.run(null);

        verify(hainaClient).initialize();
    }

    @Test
    void runDoesNotBreakStartupWhenHainaIsUnavailable() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        when(hainaClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - haina", "1"));
        when(hainaClient.isInitialized()).thenReturn(false);
        when(hainaClient.initialize()).thenThrow(new IllegalStateException("unauthorized"));
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(properties, List.of(hainaClient));

        assertDoesNotThrow(() -> initializer.run(null));
    }

    @Test
    void runSkipsInitializationWhenHainaIsDisabled() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        properties.setEnabled(false);
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(properties, List.of(hainaClient));

        initializer.run(null);

        verify(hainaClient, never()).initialize();
    }

}
