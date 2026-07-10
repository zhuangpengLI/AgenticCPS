package com.qiji.cps.module.ai.dal.mysql.chat;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(AiChatConversationMapperTest.TenantTestConfiguration.class)
class AiChatConversationMapperTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantTestConfiguration {

        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor, tenantInterceptor, 0);
            return tenantInterceptor;
        }

    }

    @Resource
    private AiChatConversationMapper conversationMapper;
    @Resource
    private TenantLineInnerInterceptor tenantLineInnerInterceptor;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistAndQueryConversationByExplicitOwner() {
        LocalDateTime boundTime = LocalDateTime.of(2026, 7, 10, 12, 0);
        AiChatConversationDO conversation = AiChatConversationDO.builder()
                .userId(42L)
                .ownerUserType("MEMBER")
                .memberId(42L)
                .chatMode("SELF_MCP_TEST")
                .mcpClientName("cps")
                .allowMutation(true)
                .identityBoundTime(boundTime)
                .title("member MCP self-test")
                .pinned(false)
                .modelId(1L)
                .model("test-model")
                .temperature(0.5D)
                .maxTokens(1024)
                .maxContexts(10)
                .build();
        conversationMapper.insert(conversation);
        Long tenantOneConversationId = conversation.getId();
        conversation.setId(null);
        conversation.setOwnerUserType("ADMIN");
        conversation.setMemberId(null);
        conversation.setChatMode("STANDARD");
        conversation.setMcpClientName(null);
        conversation.setAllowMutation(false);
        conversation.setIdentityBoundTime(null);
        conversation.setTitle("admin conversation with the same user id");
        conversationMapper.insert(conversation);

        TenantContextHolder.setTenantId(2L);
        conversation.setId(null);
        conversation.setOwnerUserType("MEMBER");
        conversation.setMemberId(42L);
        conversation.setChatMode("SELF_MCP_TEST");
        conversation.setMcpClientName("cps");
        conversation.setAllowMutation(true);
        conversation.setIdentityBoundTime(boundTime);
        conversation.setTitle("tenant 2 member conversation");
        conversationMapper.insert(conversation);
        Long tenantTwoConversationId = conversation.getId();

        TenantContextHolder.setTenantId(1L);

        List<AiChatConversationDO> conversations = conversationMapper
                .selectListByOwnerUserTypeAndUserId("MEMBER", 42L);

        assertEquals(1, conversations.size());
        AiChatConversationDO saved = conversations.get(0);
        assertEquals(tenantOneConversationId, saved.getId());
        assertNotNull(tenantTwoConversationId);
        assertNull(conversationMapper.selectById(tenantTwoConversationId));
        assertEquals("MEMBER", saved.getOwnerUserType());
        assertEquals(42L, saved.getMemberId());
        assertEquals("SELF_MCP_TEST", saved.getChatMode());
        assertEquals("cps", saved.getMcpClientName());
        assertTrue(saved.getAllowMutation());
        assertEquals(boundTime, saved.getIdentityBoundTime());
    }

}
