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

import java.util.function.Function;

@Component("cps_query_exchange_status")
public class CpsQueryExchangeStatusToolFunction
        implements Function<CpsQueryExchangeStatusToolFunction.Request, CpsRebateTokenExchangeOrderDO> {

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
    public CpsRebateTokenExchangeOrderDO apply(Request request) {
        long startedAt = System.currentTimeMillis();
        try {
            CpsRebateTokenExchangeOrderDO response = exchangeService.getExchangeOrder(request.getExchangeOrderNo());
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_query_exchange_status", request, response, null, null, startedAt);
            return response;
        } catch (Exception e) {
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_query_exchange_status", request, null, e, null, startedAt);
            throw e;
        }
    }
}
