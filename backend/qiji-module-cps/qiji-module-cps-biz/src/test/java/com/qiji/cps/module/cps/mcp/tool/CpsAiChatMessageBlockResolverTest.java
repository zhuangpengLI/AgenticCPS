package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CpsAiChatMessageBlockResolverTest {

    @Test
    void resolvesProductItemsWithoutLeakingIdentityOrRawToolFields() throws Exception {
        CpsAiChatMessageBlockResolver resolver = new CpsAiChatMessageBlockResolver();
        var mapper = new ObjectMapper();
        var field = CpsAiChatMessageBlockResolver.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(resolver, mapper);

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
}
