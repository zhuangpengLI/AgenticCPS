package com.qiji.cps.module.cps.client.haina;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionEvidence;
import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionRequest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HainaMcpDecisionClient implements HainaDecisionClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HainaDecisionProperties properties;

    @Autowired(required = false)
    private List<McpSyncClient> mcpClients;

    public HainaMcpDecisionClient(HainaDecisionProperties properties) {
        this.properties = properties;
    }

    @Override
    public HainaDecisionEvidence collectEvidence(HainaDecisionRequest request) {
        if (!properties.isEnabled()) {
            return HainaDecisionEvidence.unavailable("海纳 MCP 未启用");
        }
        McpSyncClient client = findClient();
        if (client == null) {
            return HainaDecisionEvidence.unavailable("未找到海纳 MCP Client");
        }
        try {
            String keyword = buildKeyword(request);
            return HainaDecisionEvidence.builder()
                    .available(true)
                    .graphEvidence(parseGraphEvidence(callTool(client, "content_graph_search", Map.of(
                            "query", keyword,
                            "categories", "电脑数码"
                    ))))
                    .discounts(parseDiscountEvidence(callTool(client, "discount_search", buildDiscountArgs(keyword, request))))
                    .products(parseProductEvidence(callTool(client, "product_search_pro", buildProductArgs(keyword, request))))
                    .build();
        } catch (Exception e) {
            log.warn("[HainaMcpDecisionClient] 海纳 MCP 调用失败: {}", e.getMessage());
            return HainaDecisionEvidence.unavailable("海纳 MCP 暂不可用，已使用 CPS 自有商品数据生成建议");
        }
    }

    private McpSyncClient findClient() {
        if (CollectionUtils.isEmpty(mcpClients)) {
            return null;
        }
        for (McpSyncClient client : mcpClients) {
            String name = client.getClientInfo() == null ? null : client.getClientInfo().name();
            if (!StringUtils.hasText(properties.getClientName())
                    || (StringUtils.hasText(name) && name.toLowerCase().contains(properties.getClientName().toLowerCase()))) {
                return client;
            }
        }
        return null;
    }

    private Map<String, Object> buildDiscountArgs(String keyword, HainaDecisionRequest request) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("product_query", keyword);
        args.put("fields", "title,price,mall_name,url,content,pubdate");
        args.put("size", normalizeMaxResults(request));
        args.put("request_from", "AgenticCPS");
        args.put("question_id", "cps_purchase_decision");
        args.put("query_process", 1);
        return args;
    }

    private Map<String, Object> buildProductArgs(String keyword, HainaDecisionRequest request) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("product_query", keyword);
        args.put("fields", "product_name,price,mall_name,shop_name,product_url");
        args.put("size", normalizeMaxResults(request));
        args.put("request_from", "AgenticCPS");
        args.put("question_id", "cps_purchase_decision");
        args.put("query_process", 1);
        return args;
    }

    private int normalizeMaxResults(HainaDecisionRequest request) {
        int configured = Math.max(1, properties.getMaxResults());
        if (request == null || request.getMaxResults() == null) {
            return Math.min(configured, 10);
        }
        return Math.max(1, Math.min(request.getMaxResults(), configured));
    }

    private String buildKeyword(HainaDecisionRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request != null && StringUtils.hasText(request.getNeed())) {
            builder.append(request.getNeed().trim());
        }
        if (request != null && StringUtils.hasText(request.getScenario())) {
            builder.append(' ').append(request.getScenario().trim());
        }
        return builder.toString().trim();
    }

    private String callTool(McpSyncClient client, String toolName, Map<String, Object> args) {
        McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(toolName, args));
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("haina tool error: " + toolName);
        }
        if (result.structuredContent() != null) {
            return toJson(result.structuredContent());
        }
        if (CollectionUtils.isEmpty(result.content())) {
            return "{}";
        }
        List<String> texts = new ArrayList<>();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent && StringUtils.hasText(textContent.text())) {
                texts.add(textContent.text());
            }
        }
        return texts.isEmpty() ? "{}" : String.join("\n", texts);
    }

    private List<HainaDecisionEvidence.GraphEvidence> parseGraphEvidence(String raw) {
        JsonNode data = readTree(raw).path("data");
        if (!data.isArray()) {
            return Collections.emptyList();
        }
        List<HainaDecisionEvidence.GraphEvidence> evidence = new ArrayList<>();
        for (JsonNode item : data) {
            evidence.add(HainaDecisionEvidence.GraphEvidence.builder()
                    .entityName(text(item, "entity_name"))
                    .summary(limit(text(item, "entity_description"), 240))
                    .pros(extractComments(item, "优点"))
                    .cons(extractComments(item, "缺点"))
                    .build());
            if (evidence.size() >= 3) {
                break;
            }
        }
        return evidence;
    }

    private List<String> extractComments(JsonNode item, String attributeName) {
        JsonNode comments = item.path("entity_comments");
        if (!comments.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode comment : comments) {
            if (attributeName.equals(text(comment, "attribute_name"))) {
                String value = text(comment, "attribute_value");
                if (StringUtils.hasText(value) && !values.contains(value)) {
                    values.add(value);
                }
            }
            if (values.size() >= 5) {
                break;
            }
        }
        return values;
    }

    private List<HainaDecisionEvidence.DiscountEvidence> parseDiscountEvidence(String raw) {
        JsonNode rows = readTree(raw).path("data").path("rows");
        if (!rows.isArray()) {
            return Collections.emptyList();
        }
        List<HainaDecisionEvidence.DiscountEvidence> evidence = new ArrayList<>();
        for (JsonNode row : rows) {
            evidence.add(HainaDecisionEvidence.DiscountEvidence.builder()
                    .title(text(row, "title"))
                    .mallName(text(row, "mall_name"))
                    .price(decimal(row, "digital_price", "price"))
                    .content(limit(text(row, "content"), 160))
                    .url(text(row, "url"))
                    .pubdate(text(row, "pubdate"))
                    .build());
            if (evidence.size() >= properties.getMaxResults()) {
                break;
            }
        }
        return evidence;
    }

    private List<HainaDecisionEvidence.ProductEvidence> parseProductEvidence(String raw) {
        JsonNode data = readTree(raw).path("data");
        if (!data.isArray()) {
            return Collections.emptyList();
        }
        List<HainaDecisionEvidence.ProductEvidence> evidence = new ArrayList<>();
        for (JsonNode item : data) {
            evidence.add(HainaDecisionEvidence.ProductEvidence.builder()
                    .productName(firstText(item, "product_name", "product_query", "title"))
                    .mallName(text(item, "mall_name"))
                    .shopName(text(item, "shop_name"))
                    .price(decimal(item, "price"))
                    .productUrl(firstText(item, "product_url", "url"))
                    .build());
            if (evidence.size() >= properties.getMaxResults()) {
                break;
            }
        }
        return evidence;
    }

    private JsonNode readTree(String raw) {
        try {
            return OBJECT_MAPPER.readTree(StringUtils.hasText(raw) ? raw : "{}");
        } catch (Exception e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node, name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String... names) {
        for (String name : names) {
            String value = text(node, name);
            if (StringUtils.hasText(value)) {
                try {
                    return new BigDecimal(value.replace("¥", "").replace(",", "").trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
