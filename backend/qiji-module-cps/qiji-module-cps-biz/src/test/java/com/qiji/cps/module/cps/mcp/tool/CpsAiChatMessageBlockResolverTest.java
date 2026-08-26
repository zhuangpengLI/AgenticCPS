package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CpsAiChatMessageBlockResolverTest {

    @Test
    void resolvesProductItemsWithoutLeakingIdentityOrRawToolFields() throws Exception {
        var mapper = new ObjectMapper();
        CpsAiChatMessageBlockResolver resolver = resolver(mapper);

        List<Map<String, Object>> blocks = resolver.resolve("cps_search_goods", mapper.writeValueAsString(Map.of(
                "goods", List.of(Map.of("platformCode", "jd", "goodsId", "100", "title", "耳机",
                        "actualPrice", 99.9, "memberId", 42, "toolInput", "secret")))));

        assertEquals(1, blocks.size());
        Map<String, Object> product = ((List<Map<String, Object>>) blocks.get(0).get("items")).get(0);
        assertEquals("jd", product.get("platformCode"));
        assertFalse(product.containsKey("memberId"));
        assertFalse(product.containsKey("toolInput"));
        Map<String, Object> action = ((List<Map<String, Object>>) product.get("actions")).get(0);
        assertEquals("READ_ONLY", action.get("riskLevel"));
    }

    @Test
    void resolvesResonanceGoodsAsSelectionReportWithWhitelistedEvidence() throws Exception {
        var mapper = new ObjectMapper();
        CpsAiChatMessageBlockResolver resolver = resolver(mapper);

        List<Map<String, Object>> blocks = resolver.resolve("cps_find_resonance_goods",
                mapper.writeValueAsString(Map.of(
                        "candidateCount", 8,
                        "successfulSources", 5,
                        "sourceCounts", Map.of("综合搜索", 5),
                        "selectionNote", "仅用于选品分析",
                        "sourceErrors", Map.of("销量候选", "数据源暂不可用"),
                        "goods", List.of(Map.of("goodsId", "100", "title", "纸巾", "resonanceScore", 90,
                                "sourceHits", List.of("综合搜索", "2 小时热销"),
                                "reasons", List.of("同时命中 2 个候选源"), "memberId", 42)))));

        assertEquals("SELECTION_REPORT", blocks.get(0).get("type"));
        assertEquals(8, blocks.get(0).get("candidateCount"));
        Map<String, Object> product = ((List<Map<String, Object>>) blocks.get(0).get("items")).get(0);
        assertEquals(90, product.get("resonanceScore"));
        assertEquals(List.of("综合搜索", "2 小时热销"), product.get("sourceHits"));
        assertEquals(List.of("综合搜索", "2 小时热销"), product.get("rankSources"));
        assertEquals(List.of("同时命中 2 个候选源"), product.get("evidence"));
        assertEquals(List.of("销量候选：数据源暂不可用"), blocks.get(0).get("riskNotes"));
        assertFalse(product.containsKey("memberId"));
    }

    @Test
    void resolvesOrderProfileWithoutRawIdentityFields() throws Exception {
        var mapper = new ObjectMapper();
        CpsAiChatMessageBlockResolver resolver = resolver(mapper);

        List<Map<String, Object>> blocks = resolver.resolve("cps_analyze_order_profile",
                mapper.writeValueAsString(Map.of("days", 30, "analyzedOrders", 12, "gmv", 499.50,
                        "platformBreakdown", List.of(Map.of("name", "淘宝", "orderCount", 8)),
                        "insights", List.of("淘宝订单占比较高"), "memberId", 42)));

        assertEquals("ORDER_PROFILE", blocks.get(0).get("type"));
        assertEquals(12, blocks.get(0).get("analyzedOrders"));
        assertEquals(List.of("淘宝订单占比较高"), blocks.get(0).get("insights"));
        assertFalse(blocks.get(0).containsKey("memberId"));
    }

    @Test
    void resolvesAlternativesAsStructuredReadOnlyReport() throws Exception {
        var mapper = new ObjectMapper();
        CpsAiChatMessageBlockResolver resolver = resolver(mapper);

        List<Map<String, Object>> blocks = resolver.resolve("cps_find_alternatives",
                mapper.writeValueAsString(Map.of("keyword", "蓝牙耳机", "referencePrice", 50,
                        "candidateCount", 1, "selectionNote", "仅用于选品分析，不生成推广链接",
                        "goods", List.of(Map.of("goodsId", "alt-1", "title", "高佣替代耳机",
                                "alternativeScore", 92, "priceDelta", 3,
                                "reasons", List.of("佣金比例 35%"), "memberId", 42)))));

        assertEquals("ALTERNATIVES_REPORT", blocks.get(0).get("type"));
        assertEquals(1, blocks.get(0).get("candidateCount"));
        Map<String, Object> item = ((List<Map<String, Object>>) blocks.get(0).get("items")).get(0);
        assertEquals(92, item.get("alternativeScore"));
        assertEquals(List.of("佣金比例 35%"), item.get("evidence"));
        assertFalse(item.containsKey("memberId"));
    }

    @Test
    void resolvesOrderTrendWithoutRawIdentityFields() throws Exception {
        var mapper = new ObjectMapper();
        CpsAiChatMessageBlockResolver resolver = resolver(mapper);
        List<Map<String, Object>> blocks = resolver.resolve("cps_analyze_order_trend",
                mapper.writeValueAsString(Map.of("days", 30, "granularity", "daily", "analyzedOrders", 3,
                        "totalGmv", 130, "points", List.of(Map.of("period", "2026-08-24", "gmv", 50)),
                        "insights", List.of("最近周期增长"), "memberId", 42)));

        assertEquals("ORDER_TREND", blocks.get(0).get("type"));
        assertEquals(3, blocks.get(0).get("analyzedOrders"));
        assertFalse(blocks.get(0).containsKey("memberId"));
    }

    @Test
    void resolvesGoodsDetailAsAnalysisReportWithoutRawIdentityFields() throws Exception {
        var mapper = new ObjectMapper();
        CpsAiChatMessageBlockResolver resolver = resolver(mapper);
        List<Map<String, Object>> blocks = resolver.resolve("cps_analyze_goods_detail",
                mapper.writeValueAsString(Map.of("keyword", "耳机", "eligibleCount", 2,
                        "insights", List.of("有券商品占比 50%"),
                        "topGoods", List.of(Map.of("goodsId", "g-1", "title", "深度分析耳机",
                                "analysisScore", 88, "reasons", List.of("有优惠券"),
                                "risks", List.of("优惠券有效期需再次确认"), "memberId", 42)))));

        assertEquals("GOODS_ANALYSIS", blocks.get(0).get("type"));
        assertEquals(2, blocks.get(0).get("eligibleCount"));
        Map<String, Object> item = ((List<Map<String, Object>>) blocks.get(0).get("items")).get(0);
        assertEquals(88, item.get("analysisScore"));
        assertEquals(List.of("有优惠券"), item.get("evidence"));
        assertEquals(List.of("优惠券有效期需再次确认"), item.get("riskNotes"));
        assertFalse(item.containsKey("memberId"));
    }

    private CpsAiChatMessageBlockResolver resolver(ObjectMapper mapper) throws Exception {
        CpsAiChatMessageBlockResolver resolver = new CpsAiChatMessageBlockResolver();
        var field = CpsAiChatMessageBlockResolver.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(resolver, mapper);
        return resolver;
    }
}
