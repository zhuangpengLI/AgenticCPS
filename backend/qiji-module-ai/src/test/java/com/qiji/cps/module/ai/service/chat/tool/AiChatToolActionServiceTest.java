package com.qiji.cps.module.ai.service.chat.tool;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiToolDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import com.qiji.cps.module.ai.service.model.AiToolService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AiChatToolActionServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiChatToolActionService actionService;
    @Mock
    private AiChatRoleService chatRoleService;
    @Mock
    private AiToolService toolService;

    @Test
    void getAvailableActions_onlyReturnsActionsBoundToConversationRole() {
        AiChatToolActionProvider provider = () -> List.of(
                action("SEARCH_GOODS", "cps_search_goods"),
                action("COMPARE_PRICES", "cps_compare_prices"));
        ReflectionTestUtils.setField(actionService, "providers", List.of(provider));
        when(chatRoleService.getChatRole(10L)).thenReturn(new AiChatRoleDO().setToolIds(List.of(1L)));
        when(toolService.getToolList(List.of(1L))).thenReturn(List.of(new AiToolDO().setName("cps_search_goods")));

        List<AiChatToolAction> actions = actionService.getAvailableActions(new AiChatConversationDO().setRoleId(10L));

        assertEquals(List.of("SEARCH_GOODS"), actions.stream().map(AiChatToolAction::getIntent).toList());
    }

    @Test
    void requireAllowedAction_rejectsUnknownOrUnboundIntent() {
        ReflectionTestUtils.setField(actionService, "providers",
                List.of((AiChatToolActionProvider) () -> List.of(action("SEARCH_GOODS", "cps_search_goods"))));
        when(chatRoleService.getChatRole(10L)).thenReturn(new AiChatRoleDO().setToolIds(List.of(1L)));
        when(toolService.getToolList(List.of(1L))).thenReturn(List.of(new AiToolDO().setName("other_tool")));
        AiChatConversationDO conversation = new AiChatConversationDO().setRoleId(10L);

        assertThrows(RuntimeException.class, () -> actionService.requireAllowedAction(conversation, "UNKNOWN"));
        assertThrows(RuntimeException.class, () -> actionService.requireAllowedAction(conversation, "SEARCH_GOODS"));
    }

    private static AiChatToolAction action(String intent, String toolName) {
        return new AiChatToolAction().setIntent(intent).setToolName(toolName).setLabel("商品搜索")
                .setGroup("COMMON").setRiskLevel(AiChatToolRiskLevel.READ_ONLY)
                .setInteractionType(AiChatToolInteractionType.FORM)
                .setRunningMessage("正在搜索商品").setSuccessMessage("商品搜索完成")
                .setPromptTemplate("帮我搜索商品").setFields(List.of());
    }
}
