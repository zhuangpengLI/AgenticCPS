package com.qiji.cps.module.cps.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CpsMcpToolConfigurationTest {

    @Test
    void cpsMcpToolCallbacks_exposesGoodsPriceCouponAndRebateTools() {
        CpsMcpToolConfiguration configuration = new CpsMcpToolConfiguration();

        List<ToolCallback> callbacks = configuration.cpsMcpToolCallbacks(
                mock(CpsSearchGoodsToolFunction.class),
                mock(CpsComparePricesToolFunction.class),
                mock(CpsGenerateLinkToolFunction.class),
                mock(CpsQueryOrdersToolFunction.class),
                mock(CpsGetRebateSummaryToolFunction.class),
                mock(CpsRecommendBySceneToolFunction.class),
                mock(CpsPurchaseDecisionToolFunction.class),
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
                mock(CpxSearchArticlesToolFunction.class));

        Set<String> names = callbacks.stream()
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertTrue(names.containsAll(Set.of("cps_search_goods", "cps_compare_prices", "cps_generate_link")));
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
    }
}
