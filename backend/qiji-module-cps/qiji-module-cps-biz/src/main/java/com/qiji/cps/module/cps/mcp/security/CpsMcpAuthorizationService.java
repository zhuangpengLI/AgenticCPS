package com.qiji.cps.module.cps.mcp.security;

import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Final authorization boundary for self-MCP calls before a CPS callback receives tool context.
 */
@Service
public class CpsMcpAuthorizationService {

    /** Key populated only by the dedicated self-MCP metadata converter. */
    public static final String IDENTITY_ENVELOPE_CONTEXT_KEY = "CPS_MCP_IDENTITY_ENVELOPE";
    /** Marker accompanying the envelope so a malformed self-test request cannot fall back to a regular MCP call. */
    public static final String SELF_TEST_INVOCATION_CONTEXT_KEY = "CPS_MCP_SELF_TEST_INVOCATION";
    public static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";
    public static final String TOOL_CONTEXT_TENANT_ID = "TENANT_ID";
    public static final String TOOL_CONTEXT_ACTOR_USER_ID = "ACTOR_USER_ID";
    public static final String TOOL_CONTEXT_ACTOR_USER_TYPE = "ACTOR_USER_TYPE";
    public static final String TOOL_CONTEXT_CONVERSATION_ID = "CONVERSATION_ID";
    public static final String TOOL_CONTEXT_MCP_CLIENT_NAME = "MCP_CLIENT_NAME";
    public static final String TOOL_CONTEXT_ALLOW_MUTATION = "ALLOW_MUTATION";
    public static final String TOOL_CONTEXT_TRACE_ID = "TRACE_ID";

    private final CpsMcpIdentityVerifier identityVerifier;
    private final CpsMcpToolRiskRegistry toolRiskRegistry;

    public CpsMcpAuthorizationService(CpsMcpIdentityVerifier identityVerifier,
                                      CpsMcpToolRiskRegistry toolRiskRegistry) {
        this.identityVerifier = identityVerifier;
        this.toolRiskRegistry = toolRiskRegistry;
    }

    /**
     * Keeps ordinary API-Key/external MCP calls unchanged when no self-test envelope is present.
     * Any attempted self-test call, however, must validate and rebuild its context from signed claims.
     */
    public ToolContext authorize(String toolName, ToolContext toolContext) {
        McpIdentityEnvelope envelope = extractEnvelope(toolContext);
        if (envelope == null) {
            if (isSelfTestInvocation(toolContext)) {
                throw new SecurityException("MCP self-test identity is invalid");
            }
            return toolContext == null ? new ToolContext(Map.of()) : toolContext;
        }
        McpIdentityClaims claims = identityVerifier.verify(envelope);
        CpsMcpToolRisk risk = toolRiskRegistry.getRisk(toolName);
        if (risk == null || (risk != CpsMcpToolRisk.READ_ONLY && !claims.allowMutation())) {
            throw new SecurityException("MCP self-test tool is not authorized");
        }
        return new ToolContext(buildTrustedContext(claims));
    }

    private static boolean isSelfTestInvocation(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return false;
        }
        Object marker = toolContext.getContext().get(SELF_TEST_INVOCATION_CONTEXT_KEY);
        return Boolean.TRUE.equals(marker) || "true".equalsIgnoreCase(String.valueOf(marker));
    }

    private static McpIdentityEnvelope extractEnvelope(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object envelope = toolContext.getContext().get(IDENTITY_ENVELOPE_CONTEXT_KEY);
        if (envelope == null) {
            return null;
        }
        if (!(envelope instanceof McpIdentityEnvelope identityEnvelope)) {
            throw new SecurityException("MCP self-test identity is invalid");
        }
        return identityEnvelope;
    }

    private static Map<String, Object> buildTrustedContext(McpIdentityClaims claims) {
        Map<String, Object> context = new HashMap<>();
        context.put(TOOL_CONTEXT_LOGIN_USER_ID, claims.memberId());
        context.put(TOOL_CONTEXT_TENANT_ID, claims.tenantId());
        context.put(TOOL_CONTEXT_ACTOR_USER_ID, claims.actorUserId());
        context.put(TOOL_CONTEXT_ACTOR_USER_TYPE, "ADMIN");
        context.put(TOOL_CONTEXT_CONVERSATION_ID, claims.conversationId());
        context.put(TOOL_CONTEXT_MCP_CLIENT_NAME, claims.clientName());
        context.put(TOOL_CONTEXT_ALLOW_MUTATION, claims.allowMutation());
        context.put(TOOL_CONTEXT_TRACE_ID, claims.traceId());
        return context;
    }

}
