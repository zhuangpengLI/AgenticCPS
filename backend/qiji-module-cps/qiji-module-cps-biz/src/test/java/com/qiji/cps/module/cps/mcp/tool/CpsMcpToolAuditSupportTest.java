package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.mcp.security.CpsMcpAuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CpsMcpToolAuditSupportTest {

    @Test
    void record_usesToolContextIdentityInsteadOfRequestAndRedactsSensitiveValuesRecursively() {
        CpsMcpAccessLogMapper mapper = mock(CpsMcpAccessLogMapper.class);
        ToolContext toolContext = new ToolContext(Map.of(
                CpsMcpAuthorizationService.TOOL_CONTEXT_LOGIN_USER_ID, 42L,
                CpsMcpAuthorizationService.TOOL_CONTEXT_ACTOR_USER_ID, 7L,
                CpsMcpAuthorizationService.TOOL_CONTEXT_ACTOR_USER_TYPE, "ADMIN",
                CpsMcpAuthorizationService.TOOL_CONTEXT_CONVERSATION_ID, 99L,
                CpsMcpAuthorizationService.TOOL_CONTEXT_MCP_CLIENT_NAME, "external-mcp",
                CpsMcpAuthorizationService.TOOL_CONTEXT_TRACE_ID, "trace-1"));
        Map<String, Object> request = Map.of(
                "memberId", 999L,
                "keyword", "phone",
                "apiKey", "api-key-secret",
                "envelope", "envelope-secret",
                "nested", Map.of("signature", "signature-secret", "normal", "kept"),
                "items", List.of(Map.of("nonce", "nonce-secret")));

        CpsMcpToolAuditSupport.record(mapper, "cps_search_goods", request,
                "{\"authorization\":\"Bearer response-token\",\"title\":\"kept response\"}",
                null, toolContext, System.currentTimeMillis());

        CpsMcpAccessLogDO log = captured(mapper);
        assertEquals(42L, log.getMemberId());
        assertEquals(7L, log.getActorUserId());
        assertEquals("ADMIN", log.getActorUserType());
        assertEquals(99L, log.getConversationId());
        assertEquals("external-mcp", log.getMcpClientName());
        assertEquals("EXTERNAL_MCP", log.getInvocationSource());
        assertEquals("trace-1", log.getTraceId());
        assertTrue(log.getRequestParams().contains("phone"));
        assertTrue(log.getRequestParams().contains("kept"));
        assertTrue(log.getResponseData().contains("kept response"));
        assertRedacted(log.getRequestParams(), "api-key-secret", "envelope-secret", "signature-secret", "nonce-secret");
        assertRedacted(log.getResponseData(), "response-token");
    }

    @Test
    void record_redactsSensitiveAliasesFromPojoAndPlainTextValues() {
        CpsMcpAccessLogMapper mapper = mock(CpsMcpAccessLogMapper.class);
        SensitivePayload request = new SensitivePayload("pojo-api-key", "pojo-envelope",
                new NestedPayload("nested-secret"));

        CpsMcpToolAuditSupport.record(mapper, "cps_search_goods", request,
                "signature=plain-signature token=plain-token nonce=plain-nonce apiKey=plain-api-key",
                null, null, System.currentTimeMillis());

        CpsMcpAccessLogDO log = captured(mapper);
        assertRedacted(log.getRequestParams(), "pojo-api-key", "pojo-envelope", "nested-secret");
        assertRedacted(log.getResponseData(), "plain-signature", "plain-token", "plain-nonce", "plain-api-key");
    }

    @Test
    void record_doesNotTrustSpoofedInvocationSourceFromOrdinaryContext() {
        CpsMcpAccessLogMapper mapper = mock(CpsMcpAccessLogMapper.class);
        ToolContext toolContext = new ToolContext(Map.of(
                CpsMcpAuthorizationService.TOOL_CONTEXT_INVOCATION_SOURCE, "SELF_MCP_TEST"));

        CpsMcpToolAuditSupport.record(mapper, "cps_search_goods", Map.of("keyword", "phone"),
                Map.of("ok", true), null, toolContext, System.currentTimeMillis());

        assertEquals("EXTERNAL_MCP", captured(mapper).getInvocationSource());
    }

    @Test
    void record_keepsLegacyCallersWorkingWhenToolContextIsMissing() {
        CpsMcpAccessLogMapper mapper = mock(CpsMcpAccessLogMapper.class);

        CpsMcpToolAuditSupport.record(mapper, "cps_search_goods", Map.of("keyword", "phone"),
                Map.of("ok", true), null, null, System.currentTimeMillis());

        CpsMcpAccessLogDO log = captured(mapper);
        assertNull(log.getMemberId());
        assertNull(log.getActorUserId());
        assertEquals("LOCAL", log.getInvocationSource());
    }

    private static CpsMcpAccessLogDO captured(CpsMcpAccessLogMapper mapper) {
        ArgumentCaptor<CpsMcpAccessLogDO> captor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }

    private static void assertRedacted(String value, String... secrets) {
        for (String secret : secrets) {
            assertFalse(value.contains(secret));
        }
        assertTrue(value.contains("***"));
    }

    private record SensitivePayload(String apiKey, String envelope, NestedPayload nested) {
    }

    private record NestedPayload(String secret) {
    }
}
