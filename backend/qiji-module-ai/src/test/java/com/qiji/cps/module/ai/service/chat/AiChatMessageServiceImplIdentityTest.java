package com.qiji.cps.module.ai.service.chat;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import com.qiji.cps.module.ai.dal.mysql.chat.AiChatMessageMapper;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolActionService;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolAction;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolRiskLevel;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class AiChatMessageServiceImplIdentityTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiChatMessageServiceImpl messageService;

    @Mock
    private AiChatConversationService conversationService;
    @Mock
    private AiChatMessageMapper messageMapper;
    @Mock
    private AiChatToolActionService toolActionService;

    @Test
    void sendMessage_bindsConversationLookupToAdminOwner() {
        when(conversationService.getOwnedConversation(7L, "ADMIN", 42L))
                .thenThrow(exception(CHAT_CONVERSATION_NOT_EXISTS));

        assertThrows(RuntimeException.class, () -> messageService.sendMessage(
                new AiChatMessageSendReqVO().setConversationId(7L), 42L));

        verify(conversationService).getOwnedConversation(7L, "ADMIN", 42L);
    }

    @Test
    void sendMessageStream_bindsConversationLookupToAdminOwner() {
        when(conversationService.getOwnedConversation(7L, "ADMIN", 42L))
                .thenThrow(exception(CHAT_CONVERSATION_NOT_EXISTS));

        assertThrows(RuntimeException.class, () -> messageService.sendChatMessageStream(
                new AiChatMessageSendReqVO().setConversationId(7L), 42L));

        verify(conversationService).getOwnedConversation(7L, "ADMIN", 42L);
    }

    @Test
    void sendMessage_bindsConversationLookupToMemberOwner() {
        when(conversationService.getOwnedConversation(7L, "MEMBER", 99L))
                .thenThrow(exception(CHAT_CONVERSATION_NOT_EXISTS));

        assertThrows(RuntimeException.class, () -> messageService.sendMessage(
                new AiChatMessageSendReqVO().setConversationId(7L), "MEMBER", 99L));

        verify(conversationService).getOwnedConversation(7L, "MEMBER", 99L);
    }

    @Test
    void sendMessageStream_bindsConversationLookupToMemberOwner() {
        when(conversationService.getOwnedConversation(7L, "MEMBER", 99L))
                .thenThrow(exception(CHAT_CONVERSATION_NOT_EXISTS));

        assertThrows(RuntimeException.class, () -> messageService.sendChatMessageStream(
                new AiChatMessageSendReqVO().setConversationId(7L), "MEMBER", 99L));

        verify(conversationService).getOwnedConversation(7L, "MEMBER", 99L);
    }

    @Test
    void sendMessage_rejectsUnboundToolIntentBeforePersistingMessage() {
        AiChatConversationDO conversation = new AiChatConversationDO().setId(7L).setRoleId(10L);
        when(conversationService.getOwnedConversation(7L, "ADMIN", 42L)).thenReturn(conversation);
        when(toolActionService.requireAllowedAction(conversation, "CREATE_TOKEN_EXCHANGE"))
                .thenThrow(new IllegalArgumentException("not allowed"));

        assertThrows(IllegalArgumentException.class, () -> messageService.sendMessage(
                new AiChatMessageSendReqVO().setConversationId(7L).setContent("exchange")
                        .setToolIntent("CREATE_TOKEN_EXCHANGE"), 42L));

        verifyNoInteractions(messageMapper);
    }

    @Test
    void sendMessage_requiresRequestIdForAssetWriteIntent() {
        AiChatConversationDO conversation = new AiChatConversationDO().setId(7L).setRoleId(10L);
        when(conversationService.getOwnedConversation(7L, "ADMIN", 42L)).thenReturn(conversation);
        when(toolActionService.requireAllowedAction(conversation, "CREATE_TOKEN_EXCHANGE"))
                .thenReturn(new AiChatToolAction().setIntent("CREATE_TOKEN_EXCHANGE")
                        .setRiskLevel(AiChatToolRiskLevel.ASSET_WRITE));

        assertThrows(RuntimeException.class, () -> messageService.sendMessage(
                new AiChatMessageSendReqVO().setConversationId(7L).setContent("exchange")
                        .setToolIntent("CREATE_TOKEN_EXCHANGE"), 42L));

        verifyNoInteractions(messageMapper);
    }

}
