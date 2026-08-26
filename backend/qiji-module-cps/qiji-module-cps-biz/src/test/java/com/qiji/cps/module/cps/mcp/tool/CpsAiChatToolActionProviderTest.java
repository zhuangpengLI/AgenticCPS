package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.ai.service.chat.tool.AiChatToolAction;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolRiskLevel;
import com.qiji.cps.module.cps.mcp.security.CpsMcpToolRiskRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsAiChatToolActionProviderTest {

    private final CpsAiChatToolActionProvider provider = new CpsAiChatToolActionProvider();

    @Test
    void shouldProvideAllActionsWithUniqueStableIntents() {
        List<AiChatToolAction> actions = provider.getToolActions();

        assertEquals(25, actions.size());
        assertEquals(Set.of(
                "SEARCH_GOODS", "COMPARE_PRICES", "GENERATE_LINK", "QUERY_ORDERS",
                "GET_REBATE_SUMMARY", "RECOMMEND_BY_SCENE", "PURCHASE_DECISION",
                "PROMOTION_STRATEGY_ADVICE", "EXPLAIN_REBATE", "LIST_SELECTION_THEMES",
                "RECOMMEND_FROM_SELECTION_THEME", "GET_REBATE_BALANCE", "CREATE_TOKEN_EXCHANGE",
                "QUERY_EXCHANGE_STATUS", "LIST_TASKS", "GET_TASK_DETAIL", "GENERATE_TRACKING_LINK",
                "QUERY_CONVERSIONS", "RECOMMEND_TASKS_BY_SCENE", "SEARCH_ARTICLES", "FIND_RESONANCE_GOODS",
                "FIND_ALTERNATIVES", "ANALYZE_GOODS_DETAIL", "ANALYZE_ORDER_PROFILE", "ANALYZE_ORDER_TREND"),
                actions.stream().map(AiChatToolAction::getIntent).collect(java.util.stream.Collectors.toSet()));
        assertEquals(actions.size(), new HashSet<>(actions.stream().map(AiChatToolAction::getIntent).toList()).size());
        assertEquals(actions.size(), new HashSet<>(actions.stream().map(AiChatToolAction::getToolName).toList()).size());
    }

    @Test
    void shouldMatchTheAuthoritativeMcpRiskRegistry() {
        CpsMcpToolRiskRegistry riskRegistry = new CpsMcpToolRiskRegistry();

        for (AiChatToolAction action : provider.getToolActions()) {
            assertNotNull(riskRegistry.getRisk(action.getToolName()), action.getToolName());
            assertEquals(AiChatToolRiskLevel.valueOf(riskRegistry.getRisk(action.getToolName()).name()),
                    action.getRiskLevel(), action.getIntent());
        }
    }

    @Test
    void shouldKeepInternalToolNamesOutOfAllUserFacingMetadata() {
        for (AiChatToolAction action : provider.getToolActions()) {
            String visibleMetadata = String.join(" ", action.getIntent(), action.getLabel(), action.getGroup(),
                    action.getRunningMessage(), action.getSuccessMessage(), action.getPromptTemplate(),
                    action.getFields().toString()).toLowerCase();
            assertFalse(visibleMetadata.contains("cps_"), action.getIntent());
            assertFalse(visibleMetadata.contains("cpx_"), action.getIntent());
        }
    }

    @Test
    void shouldGiveCommonActionsUsefulDefaultsAndKeepPromotionAdviceReadOnly() {
        Set<String> commonIntents = Set.of("SEARCH_GOODS", "COMPARE_PRICES", "QUERY_ORDERS",
                "GET_REBATE_SUMMARY", "GET_REBATE_BALANCE", "LIST_SELECTION_THEMES");

        for (AiChatToolAction action : provider.getToolActions()) {
            assertNotNull(action.getInteractionType(), action.getIntent());
            assertNotNull(action.getPromptTemplate(), action.getIntent());
            assertTrue(!action.getPromptTemplate().isBlank(), action.getIntent());
            if (commonIntents.contains(action.getIntent())) {
                assertTrue(action.getPromptTemplate().contains("当前测试会员")
                                || action.getPromptTemplate().contains("全平台")
                                || action.getPromptTemplate().contains("已发布"),
                        action.getIntent());
            }
        }

        AiChatToolAction promotionAdvice = provider.getToolActions().stream()
                .filter(action -> "PROMOTION_STRATEGY_ADVICE".equals(action.getIntent()))
                .findFirst().orElseThrow();
        assertEquals(AiChatToolRiskLevel.READ_ONLY, promotionAdvice.getRiskLevel());
        assertTrue(promotionAdvice.getPromptTemplate().contains("仅生成策略建议"));
        assertTrue(promotionAdvice.getPromptTemplate().contains("不要生成推广链接"));
    }

}
