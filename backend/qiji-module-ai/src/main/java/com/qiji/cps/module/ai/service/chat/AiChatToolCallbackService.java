package com.qiji.cps.module.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.qiji.cps.framework.common.util.collection.CollectionUtils;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiToolDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import com.qiji.cps.module.ai.service.model.AiToolService;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Assembles role-configured local and external tool callbacks for a conversation.
 */
@Service
public class AiChatToolCallbackService {

    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiToolService toolService;
    @Resource
    private ToolCallbackResolver toolCallbackResolver;

    @Autowired(required = false)
    private List<McpSyncClient> mcpClients;
    @Autowired(required = false)
    private McpClientCommonProperties mcpClientCommonProperties;

    public List<ToolCallback> getToolCallbacks(AiChatConversationDO conversation) {
        if (conversation.getRoleId() == null) {
            return Collections.emptyList();
        }
        AiChatRoleDO chatRole = chatRoleService.getChatRole(conversation.getRoleId());
        if (chatRole == null) {
            return Collections.emptyList();
        }
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        addLocalCallbacks(chatRole, toolCallbacks);
        addExternalMcpCallbacks(chatRole, toolCallbacks);
        return toolCallbacks;
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
            mcpClients.stream()
                    .filter(client -> ObjUtil.equal(client.getClientInfo().name(), configuredClientName))
                    .forEach(client -> CollUtil.addAll(toolCallbacks, SyncMcpToolCallbackProvider.builder()
                            .mcpClients(client)
                            // External MCP servers never receive AgenticCPS internal identity metadata by default.
                            .toolContextToMcpMetaConverter(ToolContextToMcpMetaConverter.noOp())
                            .build().getToolCallbacks()));
        });
    }

}
