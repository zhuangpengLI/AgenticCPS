package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.framework.common.mcp.McpIdentityClaims;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.ai.dal.dataobject.chat.AiChatConversationDO;
import com.qiji.cps.module.ai.service.chat.AiChatIdentityContextService;
import com.qiji.cps.module.cps.mcp.security.CpsMcpAuthorizationService;
import com.qiji.cps.module.cps.mcp.security.CpsMcpIdentityVerifier;
import com.qiji.cps.module.cps.mcp.security.CpsMcpToolRisk;
import com.qiji.cps.module.cps.mcp.security.CpsMcpToolRiskRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsMcpToolConfigurationTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void cpsMcpToolCallbacks_exposesGoodsPriceCouponAndRebateTools() {
        CpsMcpToolConfiguration configuration = new CpsMcpToolConfiguration();
        CpsMcpAuthorizationService authorizationService = mock(CpsMcpAuthorizationService.class);
        when(authorizationService.authorize(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<ToolCallback> callbacks = configuration.cpsMcpToolCallbacks(
                mock(CpsSearchGoodsToolFunction.class),
                mock(CpsFindResonanceGoodsToolFunction.class),
                mock(CpsFindAlternativesToolFunction.class),
                mock(CpsAnalyzeGoodsDetailToolFunction.class),
                mock(CpsAnalyzeOrderProfileToolFunction.class),
                mock(CpsAnalyzeOrderTrendToolFunction.class),
                mock(CpsComparePricesToolFunction.class),
                mock(CpsGenerateLinkToolFunction.class),
                mock(CpsQueryOrdersToolFunction.class),
                mock(CpsGetRebateSummaryToolFunction.class),
                mock(CpsRecommendBySceneToolFunction.class),
                mock(CpsPurchaseDecisionToolFunction.class),
                mock(CpsPromotionStrategyAdviceToolFunction.class),
                mock(CpsExplainRebateToolFunction.class),
                mock(CpsListSelectionThemesToolFunction.class),
                mock(CpsRecommendFromSelectionThemeToolFunction.class),
                mock(CpsGetRebateBalanceToolFunction.class),
                mock(CpsCreateTokenExchangeToolFunction.class),
                mock(CpsQueryExchangeStatusToolFunction.class),
                mock(CpxListTasksToolFunction.class),
                mock(CpxGetTaskDetailToolFunction.class),
                mock(CpxGenerateTrackingLinkToolFunction.class),
                mock(CpxQueryConversionsToolFunction.class),
                mock(CpxRecommendTasksBySceneToolFunction.class),
                mock(CpxSearchArticlesToolFunction.class),
                authorizationService);

        Set<String> names = callbacks.stream()
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertTrue(names.containsAll(Set.of("cps_search_goods", "cps_find_resonance_goods",
                "cps_find_alternatives", "cps_analyze_goods_detail", "cps_analyze_order_profile", "cps_analyze_order_trend",
                "cps_compare_prices", "cps_generate_link",
                "cps_promotion_strategy_advice", "cps_explain_rebate")));
        assertFalse(names.stream().anyMatch(name -> name.startsWith("ps_")));

        ToolCallback searchGoods = callbacks.stream()
                .filter(callback -> callback.getToolDefinition().name().equals("cps_search_goods"))
                .findFirst()
                .orElseThrow();
        String searchSchema = searchGoods.getToolDefinition().inputSchema();
        assertTrue(searchSchema.contains("keyword"));
        assertTrue(searchSchema.contains("price_min"));
        assertTrue(searchSchema.contains("price_max"));

        ToolCallback[] requiredCallbacks = callbacks.stream()
                .filter(callback -> Set.of("cps_search_goods", "cps_compare_prices", "cps_generate_link")
                        .contains(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        String joinedDefinitions = Arrays.stream(requiredCallbacks)
                .map(callback -> callback.getToolDefinition().description() + "\n"
                        + callback.getToolDefinition().inputSchema())
                .collect(Collectors.joining("\n"));
        assertTrue(joinedDefinitions.contains("券"));
        assertTrue(joinedDefinitions.contains("返利") || joinedDefinitions.contains("佣金"));

        ToolCallback securedSearchGoods = callbacks.stream()
                .filter(callback -> callback.getToolDefinition().name().equals("cps_search_goods"))
                .findFirst().orElseThrow();
        ToolContext originalContext = new ToolContext(Map.of());
        try {
            securedSearchGoods.call("{\"keyword\":\"test\"}", originalContext);
        } catch (RuntimeException ignored) {
            // The mocked function has no response; authorization happens before function invocation.
        }
        verify(authorizationService).authorize("cps_search_goods", originalContext);
    }

    @Test
    void cpsMcpToolCallbacks_qualifiesCpxListTasksBeanBecauseSceneRecommendationExtendsIt() throws Exception {
        Method method = CpsMcpToolConfiguration.class.getDeclaredMethod("cpsMcpToolCallbacks",
                CpsSearchGoodsToolFunction.class,
                CpsFindResonanceGoodsToolFunction.class,
                CpsFindAlternativesToolFunction.class,
                CpsAnalyzeGoodsDetailToolFunction.class,
                CpsAnalyzeOrderProfileToolFunction.class,
                CpsAnalyzeOrderTrendToolFunction.class,
                CpsComparePricesToolFunction.class,
                CpsGenerateLinkToolFunction.class,
                CpsQueryOrdersToolFunction.class,
                CpsGetRebateSummaryToolFunction.class,
                CpsRecommendBySceneToolFunction.class,
                CpsPurchaseDecisionToolFunction.class,
                CpsPromotionStrategyAdviceToolFunction.class,
                CpsExplainRebateToolFunction.class,
                CpsListSelectionThemesToolFunction.class,
                CpsRecommendFromSelectionThemeToolFunction.class,
                CpsGetRebateBalanceToolFunction.class,
                CpsCreateTokenExchangeToolFunction.class,
                CpsQueryExchangeStatusToolFunction.class,
                CpxListTasksToolFunction.class,
                CpxGetTaskDetailToolFunction.class,
                CpxGenerateTrackingLinkToolFunction.class,
                CpxQueryConversionsToolFunction.class,
                CpxRecommendTasksBySceneToolFunction.class,
                CpxSearchArticlesToolFunction.class,
                CpsMcpAuthorizationService.class);

        Parameter listTasksParameter = Arrays.stream(method.getParameters())
                .filter(parameter -> parameter.getType().equals(CpxListTasksToolFunction.class))
                .findFirst()
                .orElseThrow();

        Qualifier qualifier = listTasksParameter.getAnnotation(Qualifier.class);
        assertNotNull(qualifier);
        assertEquals("cpx_list_tasks", qualifier.value());
    }

    @Test
    void cpsMcpToolCallbacks_restoresTenantForTrustedLocalContext() {
        CpsMcpToolConfiguration configuration = new CpsMcpToolConfiguration();
        CpsMcpAuthorizationService authorizationService = mock(CpsMcpAuthorizationService.class);
        when(authorizationService.authorize(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        CpsSearchGoodsToolFunction searchFunction = mock(CpsSearchGoodsToolFunction.class);
        when(searchFunction.apply(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            assertEquals(42L, TenantContextHolder.getRequiredTenantId());
            return new CpsSearchGoodsToolFunction.Response(0, List.of(), null);
        });
        List<ToolCallback> callbacks = configuration.cpsMcpToolCallbacks(
                searchFunction,
                mock(CpsFindResonanceGoodsToolFunction.class),
                mock(CpsFindAlternativesToolFunction.class),
                mock(CpsAnalyzeGoodsDetailToolFunction.class),
                mock(CpsAnalyzeOrderProfileToolFunction.class),
                mock(CpsAnalyzeOrderTrendToolFunction.class),
                mock(CpsComparePricesToolFunction.class),
                mock(CpsGenerateLinkToolFunction.class),
                mock(CpsQueryOrdersToolFunction.class),
                mock(CpsGetRebateSummaryToolFunction.class),
                mock(CpsRecommendBySceneToolFunction.class),
                mock(CpsPurchaseDecisionToolFunction.class),
                mock(CpsPromotionStrategyAdviceToolFunction.class),
                mock(CpsExplainRebateToolFunction.class),
                mock(CpsListSelectionThemesToolFunction.class),
                mock(CpsRecommendFromSelectionThemeToolFunction.class),
                mock(CpsGetRebateBalanceToolFunction.class),
                mock(CpsCreateTokenExchangeToolFunction.class),
                mock(CpsQueryExchangeStatusToolFunction.class),
                mock(CpxListTasksToolFunction.class),
                mock(CpxGetTaskDetailToolFunction.class),
                mock(CpxGenerateTrackingLinkToolFunction.class),
                mock(CpxQueryConversionsToolFunction.class),
                mock(CpxRecommendTasksBySceneToolFunction.class),
                mock(CpxSearchArticlesToolFunction.class),
                authorizationService);
        TenantContextHolder.setTenantId(42L);
        ToolContext trustedContext = new ToolContext(new AiChatIdentityContextService().buildToolContext(
                new AiChatConversationDO().setId(99L).setUserId(5L).setMemberId(8L)
                        .setOwnerUserType("ADMIN").setChatMode("STANDARD")));
        TenantContextHolder.clear();

        ToolCallback searchGoods = callbacks.stream()
                .filter(callback -> callback.getToolDefinition().name().equals("cps_search_goods"))
                .findFirst().orElseThrow();
        searchGoods.call("{\"keyword\":\"test\"}", trustedContext);

        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void cpsMcpToolCallbacks_verifiesCompatibilityIdentityAndStripsReservedArguments() {
        CpsMcpIdentityVerifier identityVerifier = mock(CpsMcpIdentityVerifier.class);
        CpsMcpToolRiskRegistry riskRegistry = mock(CpsMcpToolRiskRegistry.class);
        CpsMcpAuthorizationService authorizationService = new CpsMcpAuthorizationService(identityVerifier, riskRegistry);
        McpIdentityEnvelope envelope = new McpIdentityEnvelope("signed-payload", "signed-signature");
        when(identityVerifier.verify(envelope)).thenReturn(new McpIdentityClaims(
                42L, 7L, 5L, 99L, "cps", false, null, null, null, "trace-1"));
        when(riskRegistry.getRisk("cps_search_goods")).thenReturn(CpsMcpToolRisk.READ_ONLY);
        CpsSearchGoodsToolFunction searchFunction = mock(CpsSearchGoodsToolFunction.class);
        when(searchFunction.apply(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            CpsSearchGoodsToolFunction.Request request = invocation.getArgument(0);
            assertEquals("test", request.getKeyword());
            assertEquals(7L, TenantContextHolder.getRequiredTenantId());
            return new CpsSearchGoodsToolFunction.Response(0, List.of(), null);
        });
        List<ToolCallback> callbacks = new CpsMcpToolConfiguration().cpsMcpToolCallbacks(
                searchFunction,
                mock(CpsFindResonanceGoodsToolFunction.class),
                mock(CpsFindAlternativesToolFunction.class),
                mock(CpsAnalyzeGoodsDetailToolFunction.class),
                mock(CpsAnalyzeOrderProfileToolFunction.class),
                mock(CpsAnalyzeOrderTrendToolFunction.class),
                mock(CpsComparePricesToolFunction.class),
                mock(CpsGenerateLinkToolFunction.class),
                mock(CpsQueryOrdersToolFunction.class),
                mock(CpsGetRebateSummaryToolFunction.class),
                mock(CpsRecommendBySceneToolFunction.class),
                mock(CpsPurchaseDecisionToolFunction.class),
                mock(CpsPromotionStrategyAdviceToolFunction.class),
                mock(CpsExplainRebateToolFunction.class),
                mock(CpsListSelectionThemesToolFunction.class),
                mock(CpsRecommendFromSelectionThemeToolFunction.class),
                mock(CpsGetRebateBalanceToolFunction.class),
                mock(CpsCreateTokenExchangeToolFunction.class),
                mock(CpsQueryExchangeStatusToolFunction.class),
                mock(CpxListTasksToolFunction.class),
                mock(CpxGetTaskDetailToolFunction.class),
                mock(CpxGenerateTrackingLinkToolFunction.class),
                mock(CpxQueryConversionsToolFunction.class),
                mock(CpxRecommendTasksBySceneToolFunction.class),
                mock(CpxSearchArticlesToolFunction.class),
                authorizationService);

        ToolCallback searchGoods = callbacks.stream()
                .filter(callback -> callback.getToolDefinition().name().equals("cps_search_goods"))
                .findFirst().orElseThrow();
        searchGoods.call("{\"keyword\":\"test\",\"_cps_mcp_identity_payload\":\"signed-payload\","
                + "\"_cps_mcp_identity_signature\":\"signed-signature\",\"_cps_mcp_self_test\":true}",
                new ToolContext(Map.of()));

        verify(identityVerifier).verify(envelope);
        assertNull(TenantContextHolder.getTenantId());
    }
}
