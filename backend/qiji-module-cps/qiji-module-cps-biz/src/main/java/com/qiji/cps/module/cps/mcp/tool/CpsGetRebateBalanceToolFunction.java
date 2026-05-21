package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.OpenApiCpsRebateBalanceRespVO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

@Component("cps_get_rebate_balance")
public class CpsGetRebateBalanceToolFunction
        implements BiFunction<CpsGetRebateBalanceToolFunction.Request, ToolContext, OpenApiCpsRebateBalanceRespVO> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsRebateTokenExchangeService exchangeService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("查询当前登录用户的CPS返利余额，包含可用、冻结、已提现等金额")
    public static class Request {
    }

    @Override
    public OpenApiCpsRebateBalanceRespVO apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        Long memberId = extractMemberId(toolContext);
        try {
            OpenApiCpsRebateBalanceRespVO response = exchangeService.getBalance(memberId);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_get_rebate_balance", request, response, null, startedAt);
            return response;
        } catch (Exception e) {
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_get_rebate_balance", request, null, e, startedAt);
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
