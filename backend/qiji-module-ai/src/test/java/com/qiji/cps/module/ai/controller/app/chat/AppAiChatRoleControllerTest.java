package com.qiji.cps.module.ai.controller.app.chat;

import com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAiChatRoleControllerTest extends com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest {

    @InjectMocks
    private AppAiChatRoleController controller;
    @Mock
    private AiChatRoleService roleService;

    @Test
    void exposesExactMemberRoleRoute() throws Exception {
        RequestMapping mapping = AppAiChatRoleController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertEquals("/ai/chat/role", mapping.value()[0]);
        GetMapping get = AppAiChatRoleController.class.getDeclaredMethod("getRoleSimpleList")
                .getAnnotation(GetMapping.class);
        assertNotNull(get);
        assertEquals("/simple-list", get.value()[0]);
    }

    @Test
    void usesMemberEnabledRoleQuery() {
        when(roleService.getMemberEnabledChatRoleList()).thenReturn(List.of(new AiChatRoleDO().setId(1L)));

        assertEquals(1, controller.getRoleSimpleList().getData().size());
        verify(roleService).getMemberEnabledChatRoleList();
    }
}
