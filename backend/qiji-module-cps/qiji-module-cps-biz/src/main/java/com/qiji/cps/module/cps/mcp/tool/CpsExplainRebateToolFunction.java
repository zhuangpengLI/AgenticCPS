package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

@Component("cps_explain_rebate")
public class CpsExplainRebateToolFunction
        implements BiFunction<CpsExplainRebateToolFunction.Request, ToolContext,
        CpsExplainRebateToolFunction.Response> {

    @Resource
    private CpsGetRebateSummaryToolFunction rebateSummaryToolFunction;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("Explain CPS rebate calculation, freezing, settlement, withdrawal, and current account summary")
    public static class Request {

        @JsonProperty(value = "question")
        @JsonPropertyDescription("Natural-language rebate question")
        private String question;

        @JsonProperty(value = "recent_count")
        @JsonPropertyDescription("Number of recent rebate records to include, default 5, max 20")
        private Integer recentCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private String answer;

        private List<String> calculationPriority;

        private List<String> settlementSteps;

        private AccountSummary accountSummary;

        private List<CpsGetRebateSummaryToolFunction.Response.RecentRebateVO> recentRecords;

        private List<String> warnings;

        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountSummary {
        private BigDecimal availableBalance;
        private BigDecimal frozenBalance;
        private BigDecimal totalRebate;
        private BigDecimal withdrawnAmount;
        private String accountStatus;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        try {
            CpsGetRebateSummaryToolFunction.Request summaryRequest =
                    new CpsGetRebateSummaryToolFunction.Request();
            summaryRequest.setRecentCount(request == null ? null : request.getRecentCount());
            CpsGetRebateSummaryToolFunction.Response summary =
                    rebateSummaryToolFunction.apply(summaryRequest, toolContext);
            if (summary == null || StringUtils.hasText(summary.getError())) {
                Response response = error(summary == null ? "rebate summary unavailable" : summary.getError());
                CpsMcpToolAuditSupport.record(accessLogMapper, "cps_explain_rebate", request, response,
                        new IllegalStateException("summary unavailable"), toolContext, startedAt);
                return response;
            }

            Response response = new Response(
                    buildAnswer(request, summary),
                    calculationPriority(),
                    settlementSteps(),
                    new AccountSummary(summary.getAvailableBalance(), summary.getFrozenBalance(),
                            summary.getTotalRebate(), summary.getWithdrawnAmount(), summary.getAccountStatus()),
                    summary.getRecentRecords() == null ? Collections.emptyList() : summary.getRecentRecords(),
                    warnings(),
                    null);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_explain_rebate", request, response,
                    null, toolContext, startedAt);
            return response;
        } catch (Exception e) {
            Response response = error("rebate explanation failed");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_explain_rebate", request, response,
                    e, toolContext, startedAt);
            return response;
        }
    }

    private String buildAnswer(Request request, CpsGetRebateSummaryToolFunction.Response summary) {
        String topic = request != null && StringUtils.hasText(request.getQuestion())
                ? request.getQuestion().trim() : "rebate calculation";
        return "For " + topic + ", rebate is calculated from the trusted platform order and the configured "
                + "rebate rule priority. Your available balance is " + summary.getAvailableBalance()
                + ", frozen balance is " + summary.getFrozenBalance()
                + ", total rebate is " + summary.getTotalRebate()
                + ", and withdrawn amount is " + summary.getWithdrawnAmount() + ".";
    }

    private List<String> calculationPriority() {
        return List.of(
                "member personal platform rule",
                "member personal all-platform rule",
                "level platform rule",
                "level all-platform rule",
                "platform default rule",
                "global default rule");
    }

    private List<String> settlementSteps() {
        return List.of(
                "trusted order attribution",
                "platform paid and received status check",
                "rebate calculation snapshot",
                "freeze until the configured release day",
                "unfreeze into available balance or reverse on refund",
                "withdraw or exchange only from available balance");
    }

    private List<String> warnings() {
        return List.of(
                "Estimated rebate on goods pages is not a settlement promise.",
                "Final rebate depends on platform order status, refund status, and trusted attribution.",
                "Pending, frozen, debt-related, withdrawn, or refunded amounts are not exchangeable available balance.");
    }

    private Response error(String message) {
        return new Response(null, calculationPriority(), settlementSteps(), null,
                Collections.emptyList(), warnings(), message);
    }
}
