package com.qiji.cps.module.ai.service.chat;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import com.qiji.cps.module.ai.dal.mysql.chat.AiChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatMessageServiceImplIdentityTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiChatMessageServiceImpl messageService;

    @Mock
    private AiChatConversationService conversationService;
    @Mock
    private AiChatMessageMapper messageMapper;

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

}
