package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.mcp.security.CpsMcpAuthorizationService;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

final class CpsMcpToolAuditSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CpsMcpToolAuditSupport() {
    }

    static void record(CpsMcpAccessLogMapper mapper, String toolName, Object request,
                       Object response, Throwable error, ToolContext toolContext, long startedAtMs) {
        if (mapper == null) {
            return;
        }
        Map<String, Object> context = toolContext == null ? null : toolContext.getContext();
        CpsMcpAccessLogDO log = CpsMcpAccessLogDO.builder()
                .memberId(getLong(context, CpsMcpAuthorizationService.TOOL_CONTEXT_LOGIN_USER_ID))
                .actorUserId(getLong(context, CpsMcpAuthorizationService.TOOL_CONTEXT_ACTOR_USER_ID))
                .actorUserType(getString(context, CpsMcpAuthorizationService.TOOL_CONTEXT_ACTOR_USER_TYPE))
                .conversationId(getLong(context, CpsMcpAuthorizationService.TOOL_CONTEXT_CONVERSATION_ID))
                .mcpClientName(getString(context, CpsMcpAuthorizationService.TOOL_CONTEXT_MCP_CLIENT_NAME))
                .invocationSource(resolveInvocationSource(toolContext))
                .traceId(getString(context, CpsMcpAuthorizationService.TOOL_CONTEXT_TRACE_ID))
                .toolName(toolName)
                .requestParams(toSanitizedJson(request))
                .responseData(error == null ? summarize(response) : null)
                .status(error == null ? 1 : 0)
                .errorMessage(error == null ? null : error.getClass().getSimpleName())
                .durationMs((int) Math.max(0, System.currentTimeMillis() - startedAtMs))
                .build();
        mapper.insert(log);
    }

    static String sanitizeError(String userMessage) {
        return redactString(userMessage);
    }

    private static String toSanitizedJson(Object value) {
        try {
            if (value instanceof String string) {
                JsonNode parsed = parseStringNode(string);
                if (parsed.isTextual()) {
                    return OBJECT_MAPPER.writeValueAsString(redactString(string));
                }
                redact(parsed);
                return OBJECT_MAPPER.writeValueAsString(parsed);
            }
            JsonNode node = OBJECT_MAPPER.valueToTree(value);
            redact(node);
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String summarize(Object value) {
        String json = toSanitizedJson(value);
        return json.length() <= 1000 ? json : json.substring(0, 1000);
    }

    private static JsonNode parseStringNode(String value) {
        try {
            return OBJECT_MAPPER.readTree(value);
        } catch (Exception ignored) {
            return OBJECT_MAPPER.valueToTree(value);
        }
    }

    private static void redact(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveKey(field.getKey())) {
                    objectNode.put(field.getKey(), "***");
                } else if (field.getValue().isTextual()) {
                    objectNode.put(field.getKey(), redactString(field.getValue().asText()));
                } else {
                    redact(field.getValue());
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode item = arrayNode.get(i);
                if (item.isTextual()) {
                    arrayNode.set(i, TextNode.valueOf(redactString(item.asText())));
                } else {
                    redact(item);
                }
            }
        }
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return normalized.contains("signature") || normalized.contains("apikey") || normalized.contains("secret")
                || normalized.contains("nonce") || normalized.contains("authorization") || normalized.contains("token")
                || normalized.contains("envelope") || normalized.contains("mcpidentity");
    }

    private static String redactString(String value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = parseStringNode(value);
            if (!node.isTextual()) {
                redact(node);
                return OBJECT_MAPPER.writeValueAsString(node);
            }
        } catch (Exception ignored) {
            // Fall through to key/value pattern redaction for non-JSON exception messages.
        }
        String redacted = value.replaceAll("(?i)(authorization)\\s*[=:]\\s*"
                + "(?:bearer|basic|token|apikey)\\s+[^,\\s]+", "$1=***");
        redacted = redacted.replaceAll("(?i)(authorization)\\s+"
                + "(?:bearer|basic|token|apikey)\\s+[^,\\s]+", "$1=***");
        redacted = redacted.replaceAll("(?i)(envelope)\\s*[=:]\\s*[^,\\s]+", "$1=***");
        redacted = redacted.replaceAll("(?i)(envelope)\\s+[^,\\s]+", "$1=***");
        return redacted.replaceAll("(?i)(signature|api[\\s_-]?key|secret|nonce|authorization|token|envelope)"
                + "\\s*[=:]\\s*[^,\\s]+", "$1=***");
    }

    private static Long getLong(Map<String, Object> context, String key) {
        if (context == null || context.get(key) == null) {
            return null;
        }
        Object value = context.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String getString(Map<String, Object> context, String key) {
        if (context == null || context.get(key) == null) {
            return null;
        }
        return String.valueOf(context.get(key));
    }

    private static String resolveInvocationSource(ToolContext toolContext) {
        if (CpsMcpAuthorizationService.isTrustedSelfTestContext(toolContext)) {
            return "SELF_MCP_TEST";
        }
        return toolContext == null ? "LOCAL" : "EXTERNAL_MCP";
    }
}
