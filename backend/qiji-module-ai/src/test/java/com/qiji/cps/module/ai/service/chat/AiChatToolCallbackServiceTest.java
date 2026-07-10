package com.qiji.cps.module.ai.service.chat;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiToolDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import com.qiji.cps.module.ai.service.model.AiToolService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatToolCallbackServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiChatToolCallbackService toolCallbackService;

    @Mock
    private AiChatRoleService chatRoleService;
    @Mock
    private AiToolService toolService;
    @Mock
    private ToolCallbackResolver toolCallbackResolver;
    @Mock
    private McpSyncClient externalMcpClient;
    @Mock
    private ToolCallback localCallback;

    @BeforeEach
    void setUpMcpClient() {
        McpClientCommonProperties properties = new McpClientCommonProperties();
        properties.setName("ai-client");
        ReflectionTestUtils.setField(toolCallbackService, "mcpClients", List.of(externalMcpClient));
        ReflectionTestUtils.setField(toolCallbackService, "mcpClientCommonProperties", properties);
        when(externalMcpClient.getClientInfo()).thenReturn(new McpSchema.Implementation("ai-client - external", "1"));
        when(externalMcpClient.listTools()).thenReturn(new McpSchema.ListToolsResult(
                List.of(McpSchema.Tool.builder().name("external_search").description("external")
                        .inputSchema(new McpSchema.JsonSchema("object", Map.of(), List.of(), false, Map.of(), Map.of()))
                        .build()), null));
        when(externalMcpClient.callTool(any())).thenReturn(new McpSchema.CallToolResult("ok", false));
    }

    @Test
    void getToolCallbacks_mixesLocalCallbacksAndExternalMcpWithoutIdentityMetadata() {
        AiChatRoleDO role = new AiChatRoleDO().setToolIds(List.of(1L)).setMcpClientNames(List.of("external"));
        when(chatRoleService.getChatRole(10L)).thenReturn(role);
        when(toolService.getToolList(List.of(1L))).thenReturn(List.of(new AiToolDO().setName("local_tool")));
        when(toolCallbackResolver.resolve("local_tool")).thenReturn(localCallback);

        List<ToolCallback> callbacks = toolCallbackService.getToolCallbacks(
                new AiChatConversationDO().setRoleId(10L).setChatMode("STANDARD"));

        assertEquals(2, callbacks.size());
        assertSame(localCallback, callbacks.get(0));
        ToolCallback externalCallback = callbacks.get(1);
        externalCallback.call("{}", new ToolContext(Map.of(
                "LOGIN_USER_ID", 42L, "TENANT_ID", 7L, "ACTOR_USER_ID", 42L,
                "ACTOR_USER_TYPE", "MEMBER", "CONVERSATION_ID", 99L,
                "CHAT_MODE", "STANDARD", "TRACE_ID", "trace-1")));

        ArgumentCaptor<McpSchema.CallToolRequest> requestCaptor = ArgumentCaptor.forClass(McpSchema.CallToolRequest.class);
        verify(externalMcpClient).callTool(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().meta().isEmpty());
    }

}
