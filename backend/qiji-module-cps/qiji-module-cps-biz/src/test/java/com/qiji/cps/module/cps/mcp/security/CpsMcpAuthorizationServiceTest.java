package com.qiji.cps.module.cps.mcp.security;

import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.ai.framework.ai.config.QijiAiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsMcpAuthorizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");
    private static final String SECRET = "test-secret";

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void authorize_rebuildsOnlyTrustedContextForReadOnlySelfTestTool() {
        TenantContextHolder.setTenantId(20L);
        CpsMcpAuthorizationService service = service();
        ToolContext original = new ToolContext(Map.of(
                CpsMcpAuthorizationService.IDENTITY_ENVELOPE_CONTEXT_KEY, envelope(false, "nonce-read"),
                "LOGIN_USER_ID", 999L,
                "memberId", 999L));

        ToolContext trusted = service.authorize("cps_search_goods", original);

        assertEquals(10L, trusted.getContext().get("LOGIN_USER_ID"));
        assertEquals(20L, trusted.getContext().get("TENANT_ID"));
        assertEquals(30L, trusted.getContext().get("ACTOR_USER_ID"));
        assertEquals(40L, trusted.getContext().get("CONVERSATION_ID"));
        assertEquals("self-mcp", trusted.getContext().get("MCP_CLIENT_NAME"));
        assertEquals(false, trusted.getContext().get("ALLOW_MUTATION"));
        assertEquals("SELF_MCP_TEST", trusted.getContext().get(
                CpsMcpAuthorizationService.TOOL_CONTEXT_INVOCATION_SOURCE));
        assertTrue(CpsMcpAuthorizationService.isTrustedSelfTestContext(trusted));
        assertFalse(trusted.getContext().containsKey("memberId"));
    }

    @Test
    void authorize_requiresAllowMutationForAttributionAndAssetWritesAndDeniesUnknownSelfTestTool() {
        TenantContextHolder.setTenantId(20L);
        CpsMcpAuthorizationService service = service();

        assertThrows(SecurityException.class, () -> service.authorize("cps_generate_link",
                new ToolContext(Map.of(CpsMcpAuthorizationService.IDENTITY_ENVELOPE_CONTEXT_KEY,
                        envelope(false, "nonce-attribution")))));
        assertThrows(SecurityException.class, () -> service.authorize("cps_create_token_exchange",
                new ToolContext(Map.of(CpsMcpAuthorizationService.IDENTITY_ENVELOPE_CONTEXT_KEY,
                        envelope(false, "nonce-asset")))));
        assertThrows(SecurityException.class, () -> service.authorize("unknown_tool",
                new ToolContext(Map.of(CpsMcpAuthorizationService.IDENTITY_ENVELOPE_CONTEXT_KEY,
                        envelope(true, "nonce-unknown")))));

        ToolContext writeContext = service.authorize("cps_create_token_exchange",
                new ToolContext(Map.of(CpsMcpAuthorizationService.IDENTITY_ENVELOPE_CONTEXT_KEY,
                        envelope(true, "nonce-write"))));
        assertEquals(true, writeContext.getContext().get("ALLOW_MUTATION"));
    }

    @Test
    void authorizationPolicy_coversEveryRegisteredCpsAndCpxTool() {
        CpsMcpToolRiskRegistry registry = new CpsMcpToolRiskRegistry();
        Set<String> registeredTools = Set.of(
                "cps_search_goods", "cps_compare_prices", "cps_generate_link", "cps_query_orders",
                "cps_get_rebate_summary", "cps_recommend_by_scene", "cps_purchase_decision",
                "cps_promotion_strategy_advice", "cps_explain_rebate",
                "cps_list_selection_themes", "cps_recommend_from_selection_theme", "cps_get_rebate_balance",
                "cps_create_token_exchange", "cps_query_exchange_status", "cpx_list_tasks",
                "cpx_get_task_detail", "cpx_generate_tracking_link", "cpx_query_conversions",
                "cpx_recommend_tasks_by_scene", "cpx_search_articles");

        assertEquals(registeredTools, registry.getRegisteredTools());
        assertEquals(CpsMcpToolRisk.READ_ONLY, registry.getRisk("cps_search_goods"));
        assertEquals(CpsMcpToolRisk.ATTRIBUTION_WRITE, registry.getRisk("cps_generate_link"));
        assertEquals(CpsMcpToolRisk.ASSET_WRITE, registry.getRisk("cps_create_token_exchange"));
        assertTrue(registry.getRisk("unknown_tool") == null);
    }

    @Test
    void authorize_keepsRegularMcpFlowUnchangedWhenNoSelfTestEnvelopeIsPresent() {
        CpsMcpAuthorizationService service = service();
        ToolContext regularContext = new ToolContext(Map.of("LOGIN_USER_ID", 99L));

        assertSame(regularContext, service.authorize("unknown_regular_mcp_tool", regularContext));
    }

    @Test
    void isTrustedSelfTestContext_rejectsSpoofedInvocationSourceFromRegularMcpContext() {
        ToolContext forgedContext = new ToolContext(Map.of(
                CpsMcpAuthorizationService.TOOL_CONTEXT_INVOCATION_SOURCE, "SELF_MCP_TEST"));

        assertFalse(CpsMcpAuthorizationService.isTrustedSelfTestContext(forgedContext));
    }

    @Test
    void authorize_failsClosedWhenSelfTestInvocationMarkerHasNoEnvelope() {
        CpsMcpAuthorizationService service = service();

        assertThrows(SecurityException.class, () -> service.authorize("cps_search_goods",
                new ToolContext(Map.of(CpsMcpAuthorizationService.SELF_TEST_INVOCATION_CONTEXT_KEY, true))));
    }

    private static CpsMcpAuthorizationService service() {
        QijiAiProperties properties = new QijiAiProperties();
        properties.getMcp().getSelfTest().setEnabled(true);
        properties.getMcp().getSelfTest().setClientName("self-mcp");
        properties.getMcp().getSelfTest().setSecret(SECRET);
        CpsMcpIdentityVerifier verifier = new CpsMcpIdentityVerifier(properties, new InMemoryNonceStore(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new CpsMcpAuthorizationService(verifier, new CpsMcpToolRiskRegistry());
    }

    private static McpIdentityEnvelope envelope(boolean allowMutation, String nonce) {
        McpIdentityClaims claims = new McpIdentityClaims(10L, 20L, 30L, 40L, "self-mcp", allowMutation,
                NOW.minusSeconds(1), NOW.plusSeconds(30), nonce, "trace");
        String payload = claims.toCanonicalPayload();
        return new McpIdentityEnvelope(payload, hmac(payload));
    }

    private static String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }

    private static class InMemoryNonceStore implements CpsMcpNonceStore {

        private final Set<String> nonces = ConcurrentHashMap.newKeySet();

        @Override
        public boolean consume(String nonce, Duration ttl) {
            return ttl != null && !ttl.isNegative() && !ttl.isZero() && nonces.add(nonce);
        }
    }

}
