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
                || "cps_get_rebate_summary".equals(toolName) || "cps_generate_link".equals(toolName);
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
}
