package com.qiji.cps.module.ai.service.chat;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.util.AiUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiChatIdentityContextServiceTest {

    private final AiChatIdentityContextService identityContextService = new AiChatIdentityContextService();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void buildToolContext_usesPersistedMemberConversationIdentity() {
        TenantContextHolder.setTenantId(7L);
        AiChatConversationDO conversation = new AiChatConversationDO().setId(99L)
                .setUserId(42L).setOwnerUserType("MEMBER").setMemberId(42L).setChatMode("STANDARD");

        Map<String, Object> context = identityContextService.buildToolContext(conversation);

        assertEquals(Set.of(AiUtils.TOOL_CONTEXT_LOGIN_USER_ID, AiUtils.TOOL_CONTEXT_TENANT_ID,
                AiUtils.TOOL_CONTEXT_ACTOR_USER_ID, AiUtils.TOOL_CONTEXT_ACTOR_USER_TYPE,
                AiUtils.TOOL_CONTEXT_CONVERSATION_ID, AiUtils.TOOL_CONTEXT_CHAT_MODE,
                AiUtils.TOOL_CONTEXT_TRACE_ID), context.keySet());
        assertEquals(42L, context.get(AiUtils.TOOL_CONTEXT_LOGIN_USER_ID));
        assertEquals(7L, context.get(AiUtils.TOOL_CONTEXT_TENANT_ID));
        assertEquals(42L, context.get(AiUtils.TOOL_CONTEXT_ACTOR_USER_ID));
        assertEquals("MEMBER", context.get(AiUtils.TOOL_CONTEXT_ACTOR_USER_TYPE));
        assertEquals(99L, context.get(AiUtils.TOOL_CONTEXT_CONVERSATION_ID));
        assertEquals("STANDARD", context.get(AiUtils.TOOL_CONTEXT_CHAT_MODE));
        assertFalse(((String) context.get(AiUtils.TOOL_CONTEXT_TRACE_ID)).isBlank());
    }

    @Test
    void buildToolContext_keepsAdminActorAndDoesNotInventMemberForStandardConversation() {
        TenantContextHolder.setTenantId(8L);
        AiChatConversationDO conversation = new AiChatConversationDO().setId(100L)
                .setUserId(5L).setOwnerUserType("ADMIN").setChatMode("STANDARD");

        Map<String, Object> context = identityContextService.buildToolContext(conversation);

        assertNull(context.get(AiUtils.TOOL_CONTEXT_LOGIN_USER_ID));
        assertEquals(5L, context.get(AiUtils.TOOL_CONTEXT_ACTOR_USER_ID));
        assertEquals("ADMIN", context.get(AiUtils.TOOL_CONTEXT_ACTOR_USER_TYPE));
        assertEquals("STANDARD", context.get(AiUtils.TOOL_CONTEXT_CHAT_MODE));
    }

}
