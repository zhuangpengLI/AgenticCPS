package com.qiji.cps.module.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.qiji.cps.framework.common.util.collection.CollectionUtils;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiToolDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import com.qiji.cps.module.ai.service.model.AiToolService;
import com.qiji.cps.module.ai.service.mcp.AiMcpIdentitySigner;
import com.qiji.cps.module.ai.service.mcp.AiSelfMcpIdentityTransport;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolAction;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolExecutionEvent;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolExecutionListener;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Assembles role-configured local and external tool callbacks for a conversation.
 */
@Service
@Slf4j
public class AiChatToolCallbackService {

    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiToolService toolService;
    @Resource
    private ToolCallbackResolver toolCallbackResolver;
    @Resource
    private AiMcpIdentitySigner identitySigner;

    @Autowired(required = false)
    private List<McpSyncClient> mcpClients;
    @Autowired(required = false)
    private McpClientCommonProperties mcpClientCommonProperties;

    public List<ToolCallback> getToolCallbacks(AiChatConversationDO conversation) {
        return getToolCallbacks(conversation, Collections.emptyMap(), AiChatToolExecutionListener.NOOP);
    }

    public List<ToolCallback> getToolCallbacks(AiChatConversationDO conversation,
                                               Map<String, AiChatToolAction> actionsByToolName,
                                               AiChatToolExecutionListener listener) {
        if (conversation.getRoleId() == null) {
            return Collections.emptyList();
        }
        AiChatRoleDO chatRole = chatRoleService.getChatRole(conversation.getRoleId());
        if (chatRole == null) {
            return Collections.emptyList();
        }
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        if ("SELF_MCP_TEST".equals(conversation.getChatMode())) {
            addSelfMcpCallbacks(conversation, toolCallbacks);
        } else {
            addLocalCallbacks(chatRole, toolCallbacks);
            addExternalMcpCallbacks(chatRole, toolCallbacks);
        }
        if (CollUtil.isEmpty(actionsByToolName)) {
            return toolCallbacks;
        }
        return toolCallbacks.stream().map(callback -> {
            AiChatToolAction action = actionsByToolName.get(callback.getToolDefinition().name());
            return action == null ? callback : new ObservableToolCallback(callback, action, listener);
        }).toList();
    }

    private void addLocalCallbacks(AiChatRoleDO chatRole, List<ToolCallback> toolCallbacks) {
        if (CollUtil.isEmpty(chatRole.getToolIds())) {
            return;
        }
        Set<String> toolNames = CollectionUtils.convertSet(toolService.getToolList(chatRole.getToolIds()), AiToolDO::getName);
        toolNames.forEach(toolName -> {
            ToolCallback toolCallback = toolCallbackResolver.resolve(toolName);
            if (toolCallback != null) {
                toolCallbacks.add(toolCallback);
            }
        });
    }

    private void addExternalMcpCallbacks(AiChatRoleDO chatRole, List<ToolCallback> toolCallbacks) {
        if (CollUtil.isEmpty(mcpClients) || CollUtil.isEmpty(chatRole.getMcpClientNames())
                || mcpClientCommonProperties == null) {
            return;
        }
        chatRole.getMcpClientNames().forEach(mcpClientName -> {
            String configuredClientName = mcpClientCommonProperties.getName() + " - " + mcpClientName;
            mcpClients.forEach(client -> addExternalMcpCallbacks(client, configuredClientName,
                    ToolContextToMcpMetaConverter.noOp(), null, toolCallbacks));
        });
    }

    private void addSelfMcpCallbacks(AiChatConversationDO conversation, List<ToolCallback> toolCallbacks) {
        if (CollUtil.isEmpty(mcpClients) || mcpClientCommonProperties == null
                || conversation.getMcpClientName() == null || conversation.getMcpClientName().isBlank()) {
            log.warn("[addSelfMcpCallbacks] 自测会话 [{}] 未配置可用的 MCP Client", conversation.getId());
            return;
        }
        String configuredClientName = mcpClientCommonProperties.getName() + " - " + conversation.getMcpClientName();
        AiSelfMcpIdentityTransport identityTransport = new AiSelfMcpIdentityTransport(identitySigner);
        mcpClients.forEach(client -> addExternalMcpCallbacks(client, configuredClientName,
                identityTransport, identityTransport, toolCallbacks));
    }

    private void addExternalMcpCallbacks(McpSyncClient client, String configuredClientName,
                                         ToolContextToMcpMetaConverter metaConverter,
                                         AiSelfMcpIdentityTransport identityTransport,
                                         List<ToolCallback> toolCallbacks) {
        try {
            if (!ObjUtil.equal(client.getClientInfo().name(), configuredClientName)) {
                return;
            }
            List<ToolCallback> callbacks = List.of(SyncMcpToolCallbackProvider.builder()
                    .mcpClients(client)
                    .toolContextToMcpMetaConverter(metaConverter)
                    .build().getToolCallbacks());
            if (identityTransport == null) {
                CollUtil.addAll(toolCallbacks, callbacks);
            } else {
                callbacks.stream().map(callback -> new SelfMcpToolCallback(callback, identityTransport))
                        .forEach(toolCallbacks::add);
            }
        } catch (RuntimeException ex) {
            log.warn("[addExternalMcpCallbacks] MCP Client [{}] 暂不可用: {}",
                    configuredClientName, ex.getMessage());
        }
    }

    private static final class SelfMcpToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final AiSelfMcpIdentityTransport identityTransport;

        private SelfMcpToolCallback(ToolCallback delegate, AiSelfMcpIdentityTransport identityTransport) {
            this.delegate = delegate;
            this.identityTransport = identityTransport;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, new ToolContext(Map.of()));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return delegate.call(identityTransport.addCompatibilityArguments(toolInput, toolContext), toolContext);
        }
    }

    private static final class ObservableToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final AiChatToolAction action;
        private final AiChatToolExecutionListener listener;

        private ObservableToolCallback(ToolCallback delegate, AiChatToolAction action,
                                       AiChatToolExecutionListener listener) {
            this.delegate = delegate;
            this.action = action;
            this.listener = listener;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, new ToolContext(Map.of()));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            String executionId = UUID.randomUUID().toString();
            listener.onEvent(event("TOOL_STARTED", executionId, "RUNNING", action.getRunningMessage()));
            try {
                String result = delegate.call(toolInput, toolContext);
                listener.onEvent(event("TOOL_SUCCEEDED", executionId, "SUCCEEDED", action.getSuccessMessage()));
                return result;
            } catch (RuntimeException ex) {
                listener.onEvent(event("TOOL_FAILED", executionId, "FAILED", "操作失败，请稍后重试"));
                throw ex;
            }
        }

        private AiChatToolExecutionEvent event(String eventType, String executionId, String status, String message) {
            return new AiChatToolExecutionEvent().setEventType(eventType).setExecutionId(executionId)
                    .setIntent(action.getIntent()).setLabel(action.getLabel()).setStatus(status).setMessage(message);
        }
    }

}
