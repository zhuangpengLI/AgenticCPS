package com.qiji.cps.module.ai.controller.admin.chat;

import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.service.chat.AiChatConversationService;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolAction;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolActionService;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolInteractionType;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolRiskLevel;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatConversationControllerToolActionTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiChatConversationController controller;
    @Mock
    private AiChatConversationService conversationService;
    @Mock
    private AiChatToolActionService actionService;

    @Test
    void getToolActions_checksAdminOwnershipAndNeverExposesToolName() {
        AiChatConversationDO conversation = new AiChatConversationDO().setId(7L).setRoleId(10L);
        when(conversationService.getOwnedConversation(7L, "ADMIN", 42L)).thenReturn(conversation);
        when(actionService.getAvailableActions(conversation)).thenReturn(List.of(new AiChatToolAction()
                .setIntent("SEARCH_GOODS").setToolName("cps_search_goods").setLabel("搜商品")
                .setGroup("COMMON").setRiskLevel(AiChatToolRiskLevel.READ_ONLY)
                .setInteractionType(AiChatToolInteractionType.FORM).setRunningMessage("正在搜索商品")
                .setSuccessMessage("商品搜索完成").setPromptTemplate("帮我搜索商品").setFields(List.of())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(42L);
            var result = controller.getToolActions(7L);
            assertEquals("SEARCH_GOODS", result.getData().get(0).getIntent());
            assertFalse(Arrays.stream(result.getData().get(0).getClass().getDeclaredFields())
                    .anyMatch(field -> "toolName".equals(field.getName())));
        }

        verify(conversationService).getOwnedConversation(7L, "ADMIN", 42L);
    }
}
