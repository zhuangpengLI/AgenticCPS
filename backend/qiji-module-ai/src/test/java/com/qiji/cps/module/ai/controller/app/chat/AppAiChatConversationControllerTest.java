package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.module.ai.controller.app.chat.vo.conversation.AppAiChatConversationCreateReqVO;
import com.qiji.cps.module.ai.controller.app.chat.vo.conversation.AppAiChatConversationUpdateReqVO;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import com.qiji.cps.framework.security.core.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAiChatConversationControllerTest extends com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest {

    @InjectMocks
    private AppAiChatConversationController controller;
    @Mock
    private AiChatConversationService conversationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesMemberConversationRoutes() {
        RequestMapping mapping = AppAiChatConversationController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/ai/chat/conversation", mapping.value()[0]);
        assertEquals("/create", path("create"));
        assertEquals("/list", path("list"));
        assertEquals("/get", path("get"));
        assertEquals("/update", path("update"));
        assertEquals("/delete", path("delete"));
    }

    @Test
    void requestDtosDoNotExposeIdentityOrMutationFields() {
        for (Class<?> type : Arrays.asList(AppAiChatConversationCreateReqVO.class,
                AppAiChatConversationUpdateReqVO.class)) {
            assertNoField(type, "memberId", "userId", "chatMode", "mcpClientName", "allowMutation");
        }
    }

    @Test
    void getBindsLookupToCurrentMemberOwner() {
        LoginUser loginUser = new LoginUser().setId(99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
        when(conversationService.getOwnedConversation(7L, "MEMBER", 99L))
                .thenThrow(new IllegalStateException("not owned"));

        assertThrows(IllegalStateException.class, () -> controller.get(7L));
        verify(conversationService).getOwnedConversation(7L, "MEMBER", 99L);
    }

    private static String path(String methodName) {
        Method method = Arrays.stream(AppAiChatConversationController.class.getDeclaredMethods())
                .filter(it -> it.getName().equals(methodName)).findFirst().orElseThrow();
        if (method.isAnnotationPresent(PostMapping.class)) return method.getAnnotation(PostMapping.class).value()[0];
        if (method.isAnnotationPresent(GetMapping.class)) return method.getAnnotation(GetMapping.class).value()[0];
        if (method.isAnnotationPresent(PutMapping.class)) return method.getAnnotation(PutMapping.class).value()[0];
        return method.getAnnotation(DeleteMapping.class).value()[0];
    }

    private static void assertNoField(Class<?> type, String... names) {
        for (String name : names) {
            assertFalse(Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.getName().equals(name)),
                    type.getSimpleName() + " must not expose " + name);
        }
    }
}
