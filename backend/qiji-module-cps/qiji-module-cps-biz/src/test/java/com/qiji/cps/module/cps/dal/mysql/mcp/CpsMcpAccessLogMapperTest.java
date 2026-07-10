package com.qiji.cps.module.cps.dal.mysql.mcp;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(CpsMcpAccessLogMapperTest.TenantTestConfiguration.class)
class CpsMcpAccessLogMapperTest extends BaseDbUnitTest {

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
    private CpsMcpAccessLogMapper accessLogMapper;
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
    void shouldPersistInvocationIdentityFields() {
        CpsMcpAccessLogDO accessLog = CpsMcpAccessLogDO.builder()
                .apiKeyId(10L)
                .memberId(20L)
                .actorUserId(30L)
                .actorUserType("MEMBER")
                .conversationId(40L)
                .mcpClientName("cps")
                .invocationSource("AI_CHAT")
                .traceId("trace-20260710")
                .toolName("cps_search_goods")
                .requestParams("{}")
                .responseData("ok")
                .status(1)
                .durationMs(12)
                .clientIp("127.0.0.1")
                .build();
        accessLogMapper.insert(accessLog);
        Long tenantOneAccessLogId = accessLog.getId();

        TenantContextHolder.setTenantId(2L);
        accessLog.setId(null);
        accessLogMapper.insert(accessLog);
        Long tenantTwoAccessLogId = accessLog.getId();

        TenantContextHolder.setTenantId(1L);

        CpsMcpAccessLogDO saved = accessLogMapper.selectById(tenantOneAccessLogId);

        assertNotNull(saved);
        assertNull(accessLogMapper.selectById(tenantTwoAccessLogId));
        assertEquals(20L, saved.getMemberId());
        assertEquals(30L, saved.getActorUserId());
        assertEquals("MEMBER", saved.getActorUserType());
        assertEquals(40L, saved.getConversationId());
        assertEquals("cps", saved.getMcpClientName());
        assertEquals("AI_CHAT", saved.getInvocationSource());
        assertEquals("trace-20260710", saved.getTraceId());
    }

}
