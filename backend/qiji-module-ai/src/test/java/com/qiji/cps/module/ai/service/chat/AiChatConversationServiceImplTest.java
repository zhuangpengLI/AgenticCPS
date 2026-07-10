package com.qiji.cps.module.ai.service.chat;

import com.qiji.cps.framework.test.core.ut.BaseMockitoUnitTest;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import com.qiji.cps.module.ai.controller.admin.chat.vo.conversation.AiChatConversationUpdateMyReqVO;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.dal.dataobject.model.AiModelDO;
import com.qiji.cps.module.ai.dal.mysql.chat.AiChatConversationMapper;
import com.qiji.cps.module.ai.enums.model.AiModelTypeEnum;
import com.qiji.cps.module.ai.service.knowledge.AiKnowledgeService;
import com.qiji.cps.module.ai.service.model.AiChatRoleService;
import com.qiji.cps.module.ai.service.model.AiModelService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static com.qiji.cps.framework.test.core.util.AssertUtils.assertServiceException;
import static com.qiji.cps.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatConversationServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AiChatConversationServiceImpl conversationService;

    @Mock
    private AiChatConversationMapper conversationMapper;
    @Mock
    private AiModelService modelService;
    @Mock
    private AiChatRoleService chatRoleService;
    @Mock
    private AiKnowledgeService knowledgeService;

    @Test
    void createChatConversationMy_persistsAdminStandardIdentity() {
        givenDefaultModel();
        doAnswer(invocation -> {
            invocation.<AiChatConversationDO>getArgument(0).setId(100L);
            return 1;
        }).when(conversationMapper).insert(any(AiChatConversationDO.class));

        Long id = conversationService.createChatConversationMy(new AiChatConversationCreateMyReqVO(), 42L);

        ArgumentCaptor<AiChatConversationDO> captor = ArgumentCaptor.forClass(AiChatConversationDO.class);
        verify(conversationMapper).insert(captor.capture());
        AiChatConversationDO saved = captor.getValue();
        assertEquals(100L, id);
        assertEquals("ADMIN", saved.getOwnerUserType());
        assertEquals(42L, saved.getUserId());
        assertNull(saved.getMemberId());
        assertEquals("STANDARD", saved.getChatMode());
    }

    @Test
    void createMemberConversation_persistsMemberIdentityAndValidatesMemberRole() {
        AiModelDO model = model();
        when(chatRoleService.validateMemberEnabledChatRole(7L)).thenReturn(
                new com.qiji.cps.module.ai.dal.dataobject.model.AiChatRoleDO().setId(7L).setName("member role")
                        .setModelId(model.getId()));
        when(modelService.validateModel(model.getId())).thenReturn(model);
        doAnswer(invocation -> {
            invocation.<AiChatConversationDO>getArgument(0).setId(101L);
            return 1;
        }).when(conversationMapper).insert(any(AiChatConversationDO.class));

        AiChatConversationCreateMyReqVO request = new AiChatConversationCreateMyReqVO().setRoleId(7L);
        Long id = conversationService.createMemberConversation(request, 42L);

        ArgumentCaptor<AiChatConversationDO> captor = ArgumentCaptor.forClass(AiChatConversationDO.class);
        verify(conversationMapper).insert(captor.capture());
        AiChatConversationDO saved = captor.getValue();
        assertEquals(101L, id);
        assertEquals("MEMBER", saved.getOwnerUserType());
        assertEquals(42L, saved.getUserId());
        assertEquals(42L, saved.getMemberId());
        assertEquals("STANDARD", saved.getChatMode());
        verify(chatRoleService).validateMemberEnabledChatRole(7L);
    }

    @Test
    void getOwnedConversation_requiresOwnerTypeAndOwnerId() {
        AiChatConversationDO adminConversation = new AiChatConversationDO().setId(1L).setUserId(42L)
                .setOwnerUserType("ADMIN").setChatMode("STANDARD");
        when(conversationMapper.selectByIdAndOwnerUserTypeAndUserId(1L, "ADMIN", 42L))
                .thenReturn(adminConversation);

        assertSame(adminConversation, conversationService.getOwnedConversation(1L, "ADMIN", 42L));

        when(conversationMapper.selectByIdAndOwnerUserTypeAndUserId(1L, "MEMBER", 42L)).thenReturn(null);
        assertServiceException(() -> conversationService.getOwnedConversation(1L, "MEMBER", 42L),
                CHAT_CONVERSATION_NOT_EXISTS);
    }

    @Test
    void getChatConversationListByOwner_separatesAdminAndMemberWithSameId() {
        List<AiChatConversationDO> admin = List.of(new AiChatConversationDO().setId(1L));
        when(conversationMapper.selectListByOwnerUserTypeAndUserId("ADMIN", 42L)).thenReturn(admin);

        assertEquals(admin, conversationService.getChatConversationListByOwner("ADMIN", 42L));
        verify(conversationMapper).selectListByOwnerUserTypeAndUserId("ADMIN", 42L);
    }

    @Test
    void getChatConversationListByOwner_excludesMemberRowsWithMismatchedMemberId() {
        AiChatConversationDO owned = new AiChatConversationDO().setId(1L).setUserId(42L)
                .setOwnerUserType("MEMBER").setMemberId(42L);
        AiChatConversationDO inconsistent = new AiChatConversationDO().setId(2L).setUserId(42L)
                .setOwnerUserType("MEMBER").setMemberId(99L);
        when(conversationMapper.selectListByOwnerUserTypeAndUserId("MEMBER", 42L))
                .thenReturn(List.of(owned, inconsistent));

        assertEquals(List.of(owned), conversationService.getChatConversationListByOwner("MEMBER", 42L));
    }

    @Test
    void updateChatConversation_copiesOnlyMutableFieldsAndRetainsIdentity() {
        AiChatConversationDO conversation = new AiChatConversationDO().setId(1L).setUserId(42L)
                .setOwnerUserType("MEMBER").setMemberId(42L).setChatMode("SELF_MCP_TEST")
                .setMcpClientName("cps").setAllowMutation(true);
        when(conversationMapper.selectByIdAndOwnerUserTypeAndUserId(1L, "MEMBER", 42L)).thenReturn(conversation);

        conversationService.updateChatConversation(new AiChatConversationUpdateMyReqVO().setId(1L)
                .setTitle("renamed").setPinned(true), "MEMBER", 42L);

        ArgumentCaptor<AiChatConversationDO> captor = ArgumentCaptor.forClass(AiChatConversationDO.class);
        verify(conversationMapper).updateById(captor.capture());
        AiChatConversationDO update = captor.getValue();
        assertEquals(1L, update.getId());
        assertEquals("renamed", update.getTitle());
        assertEquals(Boolean.TRUE, update.getPinned());
        assertNull(update.getOwnerUserType());
        assertNull(update.getMemberId());
        assertNull(update.getChatMode());
        assertNull(update.getMcpClientName());
        assertNull(update.getAllowMutation());
    }

    @Test
    void updateAndDelete_denyWrongOwner() {
        when(conversationMapper.selectByIdAndOwnerUserTypeAndUserId(1L, "ADMIN", 42L)).thenReturn(null);

        assertServiceException(() -> conversationService.updateChatConversation(
                        new AiChatConversationUpdateMyReqVO().setId(1L).setTitle("nope"), "ADMIN", 42L),
                CHAT_CONVERSATION_NOT_EXISTS);
        assertServiceException(() -> conversationService.deleteChatConversation(1L, "ADMIN", 42L),
                CHAT_CONVERSATION_NOT_EXISTS);
    }

    private AiModelDO givenDefaultModel() {
        AiModelDO model = model();
        when(modelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType())).thenReturn(model);
        return model;
    }

    private AiModelDO model() {
        return new AiModelDO().setId(9L).setModel("test-model")
                .setType(AiModelTypeEnum.CHAT.getType()).setTemperature(0.5D).setMaxTokens(1024).setMaxContexts(10);
    }

}
