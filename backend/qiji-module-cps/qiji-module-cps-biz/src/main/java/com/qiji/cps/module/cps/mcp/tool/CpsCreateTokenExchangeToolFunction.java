package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiFunction;

@Component("cps_create_token_exchange")
public class CpsCreateTokenExchangeToolFunction
        implements BiFunction<CpsCreateTokenExchangeToolFunction.Request, ToolContext, CpsRebateTokenExchangeOrderDO> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsRebateTokenExchangeService exchangeService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("将当前登录用户的可用CPS返利兑换为AI Token")
    public static class Request {
        @JsonProperty(required = true, value = "amount")
        @JsonPropertyDescription("兑换返利金额，单位元")
        private BigDecimal amount;

        @JsonProperty(required = true, value = "idempotency_key")
        @JsonPropertyDescription("幂等键，防止重复兑换")
        private String idempotencyKey;
    }

    @Override
    public CpsRebateTokenExchangeOrderDO apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        try {
            CpsRebateTokenExchangeOrderDO response =
                    exchangeService.submit(extractMemberId(toolContext), request.getAmount(), request.getIdempotencyKey());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_create_token_exchange", request, response, null, startedAt);
            return response;
        } catch (Exception e) {
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_create_token_exchange", request, null, e, startedAt);
            throw e;
        }
    }

    private Long extractMemberId(ToolContext toolContext) {
        if (toolContext == null) {
            throw new IllegalStateException("missing tool context user");
        }
        Map<String, Object> ctx = toolContext.getContext();
        Object userId = ctx.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Long value) return value;
        if (userId instanceof Number value) return value.longValue();
        throw new IllegalStateException("missing tool context user");
    }
}
