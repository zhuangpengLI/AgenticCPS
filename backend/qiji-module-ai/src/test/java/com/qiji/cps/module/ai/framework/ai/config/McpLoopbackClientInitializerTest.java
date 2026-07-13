package com.qiji.cps.module.ai.framework.ai.config;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpLoopbackClientInitializerTest extends BaseMockitoUnitTest {

    @Mock
    private McpSyncClient cpsClient;
    @Mock
    private McpSyncClient filesystemClient;

    private McpLoopbackClientInitializer initializer;

    @BeforeEach
    void setUp() {
        QijiAiProperties properties = new QijiAiProperties();
        properties.getMcp().getLoopbackClient().setEnabled(true);
        properties.getMcp().getLoopbackClient().setConnectionName("cps");
        initializer = new McpLoopbackClientInitializer(properties, List.of(cpsClient, filesystemClient));
    }

    @Test
    void onApplicationReady_initializesOnlyTheConfiguredUninitializedLoopbackClient() {
        when(cpsClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - cps", "1"));
        when(cpsClient.isInitialized()).thenReturn(false);

        initializer.onApplicationReady(null);

        verify(cpsClient).initialize();
        verify(filesystemClient, never()).initialize();
    }

    @Test
    void onApplicationReady_skipsTheConfiguredClientWhenItIsAlreadyInitialized() {
        when(cpsClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - cps", "1"));
        when(cpsClient.isInitialized()).thenReturn(true);

        initializer.onApplicationReady(null);

        verify(cpsClient, never()).initialize();
        verify(filesystemClient, never()).initialize();
    }

    @Test
    void onApplicationReady_allowsTheConfiguredLoopbackClientToBeAbsent() {
        when(filesystemClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - filesystem", "1"));
        McpLoopbackClientInitializer missingClientInitializer = new McpLoopbackClientInitializer(
                createEnabledProperties(), List.of(filesystemClient));

        assertDoesNotThrow(() -> missingClientInitializer.onApplicationReady(null));

        verify(filesystemClient, never()).initialize();
    }

    private static QijiAiProperties createEnabledProperties() {
        QijiAiProperties properties = new QijiAiProperties();
        properties.getMcp().getLoopbackClient().setEnabled(true);
        properties.getMcp().getLoopbackClient().setConnectionName("cps");
        return properties;
    }

}
