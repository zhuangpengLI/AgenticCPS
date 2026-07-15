package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.function.BiFunction;

@Component("cps_query_exchange_status")
public class CpsQueryExchangeStatusToolFunction
        implements BiFunction<CpsQueryExchangeStatusToolFunction.Request, ToolContext, CpsRebateTokenExchangeOrderDO> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsRebateTokenExchangeService exchangeService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("查询CPS返利兑换AI Token订单状态")
    public static class Request {
        @JsonProperty(required = true, value = "exchange_order_no")
        @JsonPropertyDescription("CPS兑换订单号")
        private String exchangeOrderNo;
    }

    @Override
    public CpsRebateTokenExchangeOrderDO apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        try {
            CpsRebateTokenExchangeOrderDO response = exchangeService.getExchangeOrder(
                    extractMemberId(toolContext), request.getExchangeOrderNo());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_query_exchange_status", request, response, null,
                    toolContext, startedAt);
            return response;
        } catch (Exception e) {
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_query_exchange_status", request, null, e,
                    toolContext, startedAt);
            throw e;
        }
    }

    private Long extractMemberId(ToolContext toolContext) {
        if (toolContext == null) {
            throw new IllegalStateException("missing tool context user");
        }
        Map<String, Object> context = toolContext.getContext();
        Object userId = context.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Long value) return value;
        if (userId instanceof Number value) return value.longValue();
        throw new IllegalStateException("missing tool context user");
    }
}
