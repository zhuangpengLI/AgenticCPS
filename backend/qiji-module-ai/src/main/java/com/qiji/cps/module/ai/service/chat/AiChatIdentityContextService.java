package com.qiji.cps.module.ai.service.chat;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.framework.common.util.monitor.TracerUtils;
import com.qiji.cps.framework.security.core.LoginUser;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.enums.chat.AiChatModeEnum;
import com.qiji.cps.module.ai.enums.chat.AiChatOwnerTypeEnum;
import com.qiji.cps.module.ai.util.AiUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the trusted local tool context from the persisted conversation identity.
 */
@Service
public class AiChatIdentityContextService {

    /** Marker key whose private object value distinguishes server-built local chat context from MCP metadata. */
    public static final String TRUSTED_LOCAL_CONTEXT_MARKER = "AI_CHAT_TRUSTED_LOCAL_CONTEXT";
    private static final Object TRUSTED_LOCAL_CONTEXT_VALUE = new Object();

    public Map<String, Object> buildToolContext(AiChatConversationDO conversation) {
        String ownerUserType = StrUtil.blankToDefault(conversation.getOwnerUserType(), AiChatOwnerTypeEnum.ADMIN.name());
        String chatMode = StrUtil.blankToDefault(conversation.getChatMode(), AiChatModeEnum.STANDARD.name());
        Long memberId = conversation.getMemberId();
        if (memberId == null && AiChatOwnerTypeEnum.MEMBER.name().equals(ownerUserType)) {
            memberId = conversation.getUserId();
        }

        Map<String, Object> context = new HashMap<>();
        context.put(AiUtils.TOOL_CONTEXT_LOGIN_USER_ID, memberId);
        context.put(AiUtils.TOOL_CONTEXT_TENANT_ID, TenantContextHolder.getTenantId());
        context.put(AiUtils.TOOL_CONTEXT_ACTOR_USER_ID, conversation.getUserId());
        context.put(AiUtils.TOOL_CONTEXT_ACTOR_USER_TYPE, ownerUserType);
        context.put(AiUtils.TOOL_CONTEXT_CONVERSATION_ID, conversation.getId());
        context.put(AiUtils.TOOL_CONTEXT_CHAT_MODE, chatMode);
        if (AiChatModeEnum.SELF_MCP_TEST.name().equals(chatMode)) {
            context.put(AiUtils.TOOL_CONTEXT_MCP_CLIENT_NAME, conversation.getMcpClientName());
            context.put(AiUtils.TOOL_CONTEXT_ALLOW_MUTATION, Boolean.TRUE.equals(conversation.getAllowMutation()));
        }
        context.put(AiUtils.TOOL_CONTEXT_TRACE_ID,
                StrUtil.blankToDefault(TracerUtils.getTraceId(), UUID.randomUUID().toString()));
        context.put(TRUSTED_LOCAL_CONTEXT_MARKER, TRUSTED_LOCAL_CONTEXT_VALUE);

        // Kept only for the legacy user_profile_query tool. CPS tools must use LOGIN_USER_ID.
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null) {
            context.put(AiUtils.TOOL_CONTEXT_LOGIN_USER, loginUser);
        }
        return context;
    }

    /**
     * Returns true only for a context constructed by this service in the current JVM. The marker value
     * is deliberately an object identity instead of serializable caller-supplied metadata.
     */
    public static boolean isTrustedLocalToolContext(ToolContext toolContext) {
        return toolContext != null && toolContext.getContext() != null
                && toolContext.getContext().get(TRUSTED_LOCAL_CONTEXT_MARKER) == TRUSTED_LOCAL_CONTEXT_VALUE;
    }

}
