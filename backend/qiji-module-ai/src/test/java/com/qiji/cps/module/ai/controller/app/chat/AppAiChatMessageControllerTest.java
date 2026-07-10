package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.module.ai.controller.app.chat.vo.message.AppAiChatMessageSendReqVO;
import com.qiji.cps.module.ai.service.chat.AiChatMessageService;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAiChatMessageControllerTest extends com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest {

    @InjectMocks
    private AppAiChatMessageController controller;
    @Mock
    private AiChatMessageService messageService;
    @Mock
    private AiChatConversationService conversationService;

    @Test
    void exposesMemberMessageRoutes() {
        RequestMapping mapping = AppAiChatMessageController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/ai/chat/message", mapping.value()[0]);
        assertEquals("/send", path("sendMessage"));
        assertEquals("/send-stream", path("sendChatMessageStream"));
        assertEquals("/list", path("listMessages"));
    }

    @Test
    void sendRequestDoesNotAcceptMemberIdentityOrMutationMode() {
        for (String name : new String[]{"memberId", "userId", "chatMode", "mcpClientName", "allowMutation"}) {
            assertFalse(Arrays.stream(AppAiChatMessageSendReqVO.class.getDeclaredFields())
                            .anyMatch(field -> field.getName().equals(name)), name);
        }
    }

    @Test
    void sendAndStreamBindCurrentMemberOwner() {
        AppAiChatMessageSendReqVO req = new AppAiChatMessageSendReqVO().setConversationId(7L).setContent("hi");
        when(messageService.sendMessage(any(), eq("MEMBER"), eq(42L)))
                .thenReturn(new AiChatMessageSendRespVO());
        when(messageService.sendChatMessageStream(any(), eq("MEMBER"), eq(42L)))
                .thenReturn(Flux.just(CommonResult.success(new AiChatMessageSendRespVO())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(42L);
            controller.sendMessage(req);
            controller.sendChatMessageStream(req).collectList().block();
        }

        verify(messageService).sendMessage(any(), eq("MEMBER"), eq(42L));
        verify(messageService).sendChatMessageStream(any(), eq("MEMBER"), eq(42L));
    }

    @Test
    void listRejectsConversationOwnedByAnotherMemberBeforeReadingMessages() {
        when(conversationService.getOwnedConversation(7L, "MEMBER", 42L))
                .thenThrow(new IllegalStateException("not owned"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(42L);
            assertThrows(IllegalStateException.class, () -> controller.listMessages(7L));
        }

        verify(conversationService).getOwnedConversation(7L, "MEMBER", 42L);
    }

    private static String path(String methodName) {
        Method method = Arrays.stream(AppAiChatMessageController.class.getDeclaredMethods())
                .filter(it -> it.getName().equals(methodName)).findFirst().orElseThrow();
        if (method.isAnnotationPresent(PostMapping.class)) return method.getAnnotation(PostMapping.class).value()[0];
        return method.getAnnotation(GetMapping.class).value()[0];
    }
}
