package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxConversionDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component("cpx_query_conversions")
public class CpxQueryConversionsToolFunction
        implements BiFunction<CpxQueryConversionsToolFunction.Request, ToolContext, CpxQueryConversionsToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpxTaskService taskService;
    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("查询当前会员的 CPX 转化和奖励状态")
    public static class Request {
        @JsonProperty(value = "limit")
        private Integer limit;
    }

    @Data
    public static class Response {
        private String status;
        private String reason;
        private List<CpxConversionDO> conversions;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        Response response = new Response();
        try {
            Long memberId = resolveMemberId(toolContext);
            response.setConversions(memberId == null ? List.of() : taskService.listMemberConversions(memberId,
                    request == null ? null : request.getLimit()));
            response.setStatus("SUCCESS");
            response.setReason("已返回 CPX 转化记录");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_query_conversions", request, response, null, startedAt);
            return response;
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setReason("CPX 转化查询失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cpx_query_conversions", request, response, e, startedAt);
            return response;
        }
    }

    private Long resolveMemberId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Map<String, Object> context = toolContext.getContext();
        Object userId = context.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
