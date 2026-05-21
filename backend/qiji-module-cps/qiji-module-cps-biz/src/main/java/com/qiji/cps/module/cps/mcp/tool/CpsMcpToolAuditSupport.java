package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;

final class CpsMcpToolAuditSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CpsMcpToolAuditSupport() {
    }

    static void record(CpsMcpAccessLogMapper mapper, String toolName, Object request,
                       Object response, Throwable error, long startedAtMs) {
        if (mapper == null) {
            return;
        }
        CpsMcpAccessLogDO log = CpsMcpAccessLogDO.builder()
                .toolName(toolName)
                .requestParams(toJson(request))
                .responseData(error == null ? summarize(response) : null)
                .status(error == null ? 1 : 0)
                .errorMessage(error == null ? null : error.getClass().getSimpleName())
                .durationMs((int) Math.max(0, System.currentTimeMillis() - startedAtMs))
                .build();
        mapper.insert(log);
    }

    static String sanitizeError(String userMessage) {
        return userMessage;
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String summarize(Object value) {
        String json = toJson(value);
        return json.length() <= 1000 ? json : json.substring(0, 1000);
    }
}
