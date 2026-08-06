package com.qiji.cps.module.ai.service.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.framework.common.mcp.McpIdentityTransportKeys;
import com.qiji.cps.framework.common.util.json.JsonUtils;
import com.qiji.cps.module.ai.service.chat.AiChatIdentityContextService;
import com.qiji.cps.module.ai.util.AiUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.ToolContextToMcpMetaConverter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Signs the minimum trusted identity required by the local CPS MCP server.
 */
public class AiSelfMcpIdentityTransport implements ToolContextToMcpMetaConverter {

    private final AiMcpIdentitySigner identitySigner;

    public AiSelfMcpIdentityTransport(AiMcpIdentitySigner identitySigner) {
        this.identitySigner = identitySigner;
    }

    @Override
    public Map<String, Object> convert(ToolContext toolContext) {
        McpIdentityEnvelope envelope = sign(toolContext);
        return Map.of(
                McpIdentityTransportKeys.META_IDENTITY_ENVELOPE, envelope,
                McpIdentityTransportKeys.META_SELF_TEST_INVOCATION, true);
    }

    /**
     * Compatibility tunnel for Spring AI 1.1.x, whose MCP server callback adapter drops request _meta.
     * The CPS callback removes these reserved arguments before deserializing the public tool request.
     */
    public String addCompatibilityArguments(String toolInput, ToolContext toolContext) {
        Map<String, Object> arguments = JsonUtils.parseObject(toolInput,
                new TypeReference<LinkedHashMap<String, Object>>() { });
        if (arguments == null) {
            arguments = new LinkedHashMap<>();
        }
        McpIdentityEnvelope envelope = sign(toolContext);
        arguments.put(McpIdentityTransportKeys.ARG_IDENTITY_PAYLOAD, envelope.payload());
        arguments.put(McpIdentityTransportKeys.ARG_IDENTITY_SIGNATURE, envelope.signature());
        arguments.put(McpIdentityTransportKeys.ARG_SELF_TEST_INVOCATION, true);
        return JsonUtils.toJsonString(arguments);
    }

    private McpIdentityEnvelope sign(ToolContext toolContext) {
        if (!AiChatIdentityContextService.isTrustedLocalToolContext(toolContext)) {
            throw new SecurityException("MCP self-test requires trusted local chat context");
        }
        Map<String, Object> context = toolContext.getContext();
        if (!"SELF_MCP_TEST".equals(context.get(AiUtils.TOOL_CONTEXT_CHAT_MODE))) {
            throw new SecurityException("MCP self-test chat mode is required");
        }
        McpIdentityClaims claims = new McpIdentityClaims(
                requiredLong(context, AiUtils.TOOL_CONTEXT_LOGIN_USER_ID),
                requiredLong(context, AiUtils.TOOL_CONTEXT_TENANT_ID),
                requiredLong(context, AiUtils.TOOL_CONTEXT_ACTOR_USER_ID),
                requiredLong(context, AiUtils.TOOL_CONTEXT_CONVERSATION_ID),
                requiredText(context, AiUtils.TOOL_CONTEXT_MCP_CLIENT_NAME),
                Boolean.TRUE.equals(context.get(AiUtils.TOOL_CONTEXT_ALLOW_MUTATION)),
                null, null, null,
                requiredText(context, AiUtils.TOOL_CONTEXT_TRACE_ID));
        return identitySigner.sign(claims);
    }

    private static Long requiredLong(Map<String, Object> context, String key) {
        Object value = context.get(key);
        long parsed;
        try {
            parsed = value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
        } catch (RuntimeException exception) {
            throw new SecurityException(key + " is invalid", exception);
        }
        if (parsed <= 0) {
            throw new SecurityException(key + " is invalid");
        }
        return parsed;
    }

    private static String requiredText(Map<String, Object> context, String key) {
        Object value = context.get(key);
        String text = value == null ? null : value.toString();
        if (text == null || text.isBlank()) {
            throw new SecurityException(key + " is invalid");
        }
        return text;
    }

}
