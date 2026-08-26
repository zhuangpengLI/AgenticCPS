package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.ai.service.chat.AiChatMessageBlockResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts CPS tool JSON into safe, display-oriented blocks for the member chat.
 * It intentionally exposes no raw tool arguments or member identity fields.
 */
@Component
public class CpsAiChatMessageBlockResolver implements AiChatMessageBlockResolver {

    private static final List<String> PRODUCT_TOOLS = Arrays.asList(
            "cps_search_goods", "cps_recommend_by_scene", "cps_recommend_from_selection_theme");

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(String toolName) {
        return PRODUCT_TOOLS.contains(toolName) || "cps_compare_prices".equals(toolName)
                || "cps_get_rebate_summary".equals(toolName) || "cps_generate_link".equals(toolName)
                || "cps_find_resonance_goods".equals(toolName)
                || "cps_find_alternatives".equals(toolName)
                || "cps_analyze_goods_detail".equals(toolName)
                || "cps_analyze_order_profile".equals(toolName)
                || "cps_analyze_order_trend".equals(toolName);
    }

    @Override
    public List<Map<String, Object>> resolve(String toolName, String resultPayload) {
        try {
            JsonNode root = objectMapper.readTree(resultPayload);
            if (PRODUCT_TOOLS.contains(toolName)) {
                JsonNode items = root.has("goods") ? root.path("goods") : root.path("items");
                return productBlock("PRODUCT_RECOMMEND", "为你找到这些商品", items);
            }
            if ("cps_compare_prices".equals(toolName)) {
                return productBlock("PRODUCT_COMPARE", "多平台价格对比", root.path("items"));
            }
            if ("cps_get_rebate_summary".equals(toolName)) {
                return Collections.singletonList(rebateBlock(root));
            }
            if ("cps_generate_link".equals(toolName)) {
                return Collections.singletonList(linkBlock(root));
            }
            if ("cps_find_resonance_goods".equals(toolName)) {
                return selectionReportBlock(root);
            }
            if ("cps_find_alternatives".equals(toolName)) {
                return alternativesReportBlock(root);
            }
            if ("cps_analyze_goods_detail".equals(toolName)) {
                return goodsDetailBlock(root);
            }
            if ("cps_analyze_order_profile".equals(toolName)) {
                return Collections.singletonList(orderProfileBlock(root));
            }
            if ("cps_analyze_order_trend".equals(toolName)) {
                return Collections.singletonList(orderTrendBlock(root));
            }
        } catch (Exception ignored) {
            // The model still receives the original tool result; UI blocks are optional.
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> productBlock(String type, String title, JsonNode items) {
        List<Map<String, Object>> products = new ArrayList<>();
        if (items.isArray()) {
            for (JsonNode item : items) {
                Map<String, Object> product = new LinkedHashMap<>();
                copy(product, item, "platformCode", "platformName", "goodsId", "goodsSign", "title", "mainPic",
                        "originalPrice", "actualPrice", "couponPrice", "couponConditions", "couponStartTime",
                        "couponEndTime", "netPrice", "commissionAmount", "commissionRate",
                        "monthSales", "shopName", "vendorCode", "itemLink", "promotionUrl");
                copyJson(product, item, "resonanceScore", "sourceCount", "sourceHits", "scoreBreakdown",
                        "alternativeScore", "priceDelta", "commissionRateDelta", "commissionAmountDelta",
                        "commissionDelta", "analysisScore", "reasons", "riskWarnings", "risks");
                // Keep the raw evidence fields for API compatibility and expose UI-friendly aliases.
                aliasJson(product, "rankSources", "sourceHits");
                aliasJson(product, "evidence", "reasons");
                aliasJson(product, "riskNotes", "riskWarnings");
                aliasJson(product, "riskNotes", "risks");
                if (!product.isEmpty()) {
                    product.put("actions", Collections.singletonList(action("OPEN_DETAIL", "查看详情", "READ_ONLY", product)));
                    products.add(product);
                }
            }
        }
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> block = base(type, title);
        block.put("items", products);
        return Collections.singletonList(block);
    }

    private Map<String, Object> rebateBlock(JsonNode root) {
        Map<String, Object> block = base("REBATE_SUMMARY", "我的返利概览");
        copy(block, root, "availableBalance", "frozenBalance", "totalRebate", "withdrawnAmount", "accountStatus");
        return block;
    }

    private Map<String, Object> linkBlock(JsonNode root) {
        Map<String, Object> block = base("FOLLOW_UP", "推广链接已准备好");
        copy(block, root, "actualPrice", "commissionAmount", "shortUrl", "mobileUrl", "promotionUrl", "tpwd",
                "commandLabel", "command");
        return block;
    }

    private List<Map<String, Object>> selectionReportBlock(JsonNode root) {
        List<Map<String, Object>> blocks = productBlock("SELECTION_REPORT", "多来源共振选品报告",
                root.path("goods"));
        if (blocks.isEmpty()) {
            return blocks;
        }
        Map<String, Object> block = blocks.get(0);
        copyJson(block, root, "candidateCount", "successfulSources", "sourceCounts", "sourceErrors",
                "selectionNote");
        if (root.has("selectionNote")) {
            block.put("summary", root.path("selectionNote").asText());
        }
        if (root.has("sourceCounts")) {
            block.put("evidence", Collections.singletonList(objectMapper.convertValue(root.path("sourceCounts"), Object.class)));
        }
        if (root.has("sourceErrors")) {
            List<String> sourceErrors = new ArrayList<>();
            root.path("sourceErrors").fields().forEachRemaining(entry ->
                    sourceErrors.add(entry.getKey() + "：" + entry.getValue().asText()));
            if (!sourceErrors.isEmpty()) {
                block.put("riskNotes", sourceErrors);
            }
        }
        return blocks;
    }

    private Map<String, Object> orderProfileBlock(JsonNode root) {
        Map<String, Object> block = base("ORDER_PROFILE", "成交画像分析");
        copyJson(block, root, "days", "analyzedOrders", "excludedOrders", "gmv", "estimatedRebate",
                "realRebate", "averageOrderValue", "platformBreakdown", "priceBandBreakdown", "topProducts",
                "insights", "dataLimitations", "error");
        return block;
    }

    private List<Map<String, Object>> alternativesReportBlock(JsonNode root) {
        List<Map<String, Object>> blocks = productBlock("ALTERNATIVES_REPORT", "高佣替代品分析",
                root.path("goods"));
        if (blocks.isEmpty()) {
            return blocks;
        }
        Map<String, Object> block = blocks.get(0);
        copyJson(block, root, "keyword", "referencePrice", "candidateCount", "selectionNote", "error");
        if (root.has("selectionNote")) {
            block.put("summary", root.path("selectionNote").asText());
        }
        return blocks;
    }

    private List<Map<String, Object>> goodsDetailBlock(JsonNode root) {
        List<Map<String, Object>> blocks = productBlock("GOODS_ANALYSIS", "商品深度分析",
                root.path("topGoods"));
        Map<String, Object> block = blocks.isEmpty() ? base("GOODS_ANALYSIS", "商品深度分析") : blocks.get(0);
        copyJson(block, root, "keyword", "platformCode", "sampledCount", "eligibleCount", "price",
                "commission", "coupon", "sales", "platformBreakdown", "insights", "dataLimitations", "error");
        if (root.has("insights")) {
            block.put("evidence", objectMapper.convertValue(root.path("insights"), Object.class));
        }
        if (root.has("error")) {
            block.put("summary", root.path("error").asText());
        } else if (root.has("keyword")) {
            block.put("summary", "已完成“" + root.path("keyword").asText() + "”的当前候选快照分析");
        }
        return Collections.singletonList(block);
    }

    private Map<String, Object> orderTrendBlock(JsonNode root) {
        Map<String, Object> block = base("ORDER_TREND", "成交趋势分析");
        copyJson(block, root, "days", "granularity", "analyzedOrders", "totalGmv",
                "totalEstimatedRebate", "totalRealRebate", "points", "insights", "dataLimitations", "error");
        return block;
    }

    private Map<String, Object> base(String type, String title) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", type.toLowerCase() + "-" + System.nanoTime());
        block.put("version", 1);
        block.put("type", type);
        block.put("title", title);
        return block;
    }

    private Map<String, Object> action(String type, String label, String riskLevel, Map<String, Object> product) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", type);
        action.put("label", label);
        action.put("riskLevel", riskLevel);
        Map<String, Object> payload = new LinkedHashMap<>();
        for (String key : Arrays.asList("platformCode", "goodsId", "goodsSign", "vendorCode", "itemLink")) {
            if (product.containsKey(key)) {
                payload.put(key, product.get(key));
            }
        }
        action.put("payload", payload);
        return action;
    }

    private void copy(Map<String, Object> target, JsonNode source, String... fields) {
        for (String field : fields) {
            JsonNode value = source.get(field);
            if (value != null && !value.isNull()) {
                target.put(field, value.isNumber() ? value.numberValue() : value.asText());
            }
        }
    }

    private void copyJson(Map<String, Object> target, JsonNode source, String... fields) {
        for (String field : fields) {
            JsonNode value = source.get(field);
            if (value != null && !value.isNull()) {
                target.put(field, objectMapper.convertValue(value, Object.class));
            }
        }
    }

    private void aliasJson(Map<String, Object> target, String targetField, String sourceField) {
        if (target.containsKey(sourceField) && !target.containsKey(targetField)) {
            target.put(targetField, target.get(sourceField));
        }
    }
}
