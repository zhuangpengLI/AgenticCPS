package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.module.ai.controller.app.chat.vo.message.AppAiChatMessageSendReqVO;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppAiChatMessageControllerTest {

    @Test
    void exposesMemberMessageRoutes() {
        RequestMapping mapping = AppAiChatMessageController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/ai/chat/message", mapping.value()[0]);
        assertEquals("/send", path("sendMessage"));
        assertEquals("/send-stream", path("sendChatMessageStream"));
        assertEquals("/list", path("listMessages"));
        assertEquals("/role/simple-list", path("getRoleSimpleList"));
    }

    @Test
    void sendRequestDoesNotAcceptMemberIdentityOrMutationMode() {
        for (String name : new String[]{"memberId", "userId", "chatMode", "mcpClientName", "allowMutation"}) {
            assertFalse(Arrays.stream(AppAiChatMessageSendReqVO.class.getDeclaredFields())
                            .anyMatch(field -> field.getName().equals(name)), name);
        }
    }

    private static String path(String methodName) {
        Method method = Arrays.stream(AppAiChatMessageController.class.getDeclaredMethods())
                .filter(it -> it.getName().equals(methodName)).findFirst().orElseThrow();
        if (method.isAnnotationPresent(PostMapping.class)) return method.getAnnotation(PostMapping.class).value()[0];
        return method.getAnnotation(GetMapping.class).value()[0];
    }
}
