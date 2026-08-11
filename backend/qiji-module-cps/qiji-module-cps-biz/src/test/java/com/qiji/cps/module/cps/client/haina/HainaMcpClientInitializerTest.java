package com.qiji.cps.module.cps.client.haina;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HainaMcpClientInitializerTest extends BaseMockitoUnitTest {

    @Mock
    private McpSyncClient hainaClient;

    @Test
    void initializeHainaClientInitializesConfiguredHainaClient() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        when(hainaClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - haina", "1"));
        when(hainaClient.isInitialized()).thenReturn(false);
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(
                properties, List.of(hainaClient), Runnable::run);

        initializer.initializeHainaClient();

        verify(hainaClient).initialize();
    }

    @Test
    void initializeHainaClientDoesNotBreakStartupWhenHainaIsUnavailable() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        when(hainaClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - haina", "1"));
        when(hainaClient.isInitialized()).thenReturn(false);
        when(hainaClient.initialize()).thenThrow(new IllegalStateException("unauthorized"));
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(
                properties, List.of(hainaClient), Runnable::run);

        assertDoesNotThrow(initializer::initializeHainaClient);
    }

    @Test
    void initializeHainaClientSkipsInitializationWhenHainaIsDisabled() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        properties.setEnabled(false);
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(
                properties, List.of(hainaClient), Runnable::run);

        initializer.initializeHainaClient();

        verify(hainaClient, never()).initialize();
    }

    @Test
    void onApplicationReadySchedulesInitializationOutsideTheStartupCaller() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        when(hainaClient.getClientInfo()).thenReturn(new McpSchema.Implementation("mcp - haina", "1"));
        when(hainaClient.isInitialized()).thenReturn(false);
        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        Executor executor = scheduledTask::set;
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(
                properties, List.of(hainaClient), executor);

        assertDoesNotThrow(() -> initializer.onApplicationReady(null));

        verify(hainaClient, never()).initialize();
        assertNotNull(scheduledTask.get());
        scheduledTask.get().run();
        verify(hainaClient).initialize();
    }

    @Test
    void onApplicationReadyIgnoresExecutorRejection() {
        HainaDecisionProperties properties = new HainaDecisionProperties();
        Executor executor = task -> {
            throw new RejectedExecutionException("executor closed");
        };
        HainaMcpClientInitializer initializer = new HainaMcpClientInitializer(
                properties, List.of(hainaClient), executor);

        assertDoesNotThrow(() -> initializer.onApplicationReady(null));
    }

}
