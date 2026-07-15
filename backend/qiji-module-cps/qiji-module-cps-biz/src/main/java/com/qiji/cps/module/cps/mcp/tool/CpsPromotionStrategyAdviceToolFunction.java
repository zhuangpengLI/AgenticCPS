package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionRequest;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionResponse;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component("cps_promotion_strategy_advice")
public class CpsPromotionStrategyAdviceToolFunction
        implements BiFunction<CpsPromotionStrategyAdviceToolFunction.Request, ToolContext,
        CpsPromotionStrategyAdviceToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsPurchaseDecisionService purchaseDecisionService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("Generate CPS promotion strategy advice from a product need, audience, goal, and channel")
    public static class Request {

        @JsonProperty(required = true, value = "product_need")
        @JsonPropertyDescription("Product or category to promote")
        private String productNeed;

        @JsonProperty(value = "campaign_goal")
        @JsonPropertyDescription("Promotion goal such as increase conversion, clear inventory, or maximize rebate")
        private String campaignGoal;

        @JsonProperty(value = "target_audience")
        @JsonPropertyDescription("Audience segment for the promotion")
        private String targetAudience;

        @JsonProperty(value = "content_channel")
        @JsonPropertyDescription("Distribution channel such as WeChat group, short video, blog, or app push")
        private String contentChannel;

        @JsonProperty(value = "budget_min")
        @JsonPropertyDescription("Minimum acceptable item price")
        private BigDecimal budgetMin;

        @JsonProperty(value = "budget_max")
        @JsonPropertyDescription("Maximum acceptable item price")
        private BigDecimal budgetMax;

        @JsonProperty(value = "preferred_platforms")
        @JsonPropertyDescription("Preferred CPS platform codes")
        private List<String> preferredPlatforms;

        @JsonProperty(value = "decision_mode")
        @JsonPropertyDescription("best_value, low_price, high_rebate, or reliable_shop")
        private String decisionMode;

        @JsonProperty(value = "generate_link")
        @JsonPropertyDescription("Whether to generate a promotion link using trusted ToolContext identity")
        private Boolean generateLink;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private String strategySummary;

        private CpsPurchaseDecisionResponse.DecisionItem recommendedGoods;

        private List<CpsPurchaseDecisionResponse.DecisionItem> alternatives;

        private List<String> promotionActions;

        private List<String> copyAngles;

        private List<String> riskNotes;

        private CpsPurchaseDecisionResponse decisionEvidence;

        private String error;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        if (request == null || !StringUtils.hasText(request.getProductNeed())) {
            Response response = error("product_need is required");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_promotion_strategy_advice", request, response,
                    new IllegalArgumentException("product_need required"), toolContext, startedAt);
            return response;
        }
        try {
            CpsPurchaseDecisionResponse decision = purchaseDecisionService.decide(toDecisionRequest(request),
                    resolveTrustedUserId(toolContext));
            if (decision == null || StringUtils.hasText(decision.getError())) {
                Response response = error(decision == null ? "promotion strategy generation failed" : decision.getError());
                CpsMcpToolAuditSupport.record(accessLogMapper, "cps_promotion_strategy_advice", request, response,
                        new IllegalStateException("decision unavailable"), toolContext, startedAt);
                return response;
            }
            Response response = new Response(
                    buildStrategySummary(request, decision),
                    decision.getBestChoice(),
                    decision.getAlternatives() == null ? Collections.emptyList() : decision.getAlternatives(),
                    buildPromotionActions(request, decision),
                    buildCopyAngles(request, decision),
                    decision.getRisks() == null ? Collections.emptyList() : decision.getRisks(),
                    decision,
                    null);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_promotion_strategy_advice", request, response,
                    null, toolContext, startedAt);
            return response;
        } catch (Exception e) {
            Response response = error("promotion strategy generation failed");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_promotion_strategy_advice", request, response,
                    e, toolContext, startedAt);
            return response;
        }
    }

    private CpsPurchaseDecisionRequest toDecisionRequest(Request request) {
        CpsPurchaseDecisionRequest decisionRequest = new CpsPurchaseDecisionRequest();
        decisionRequest.setNeed(request.getProductNeed().trim());
        decisionRequest.setScenario(joinNonBlank(request.getCampaignGoal(), request.getTargetAudience(),
                request.getContentChannel()));
        decisionRequest.setBudgetMin(request.getBudgetMin());
        decisionRequest.setBudgetMax(request.getBudgetMax());
        decisionRequest.setPreferredPlatforms(request.getPreferredPlatforms());
        decisionRequest.setDecisionMode(StringUtils.hasText(request.getDecisionMode())
                ? request.getDecisionMode() : "best_value");
        decisionRequest.setGenerateLink(request.getGenerateLink());
        return decisionRequest;
    }

    private String buildStrategySummary(Request request, CpsPurchaseDecisionResponse decision) {
        String goal = StringUtils.hasText(request.getCampaignGoal()) ? request.getCampaignGoal() : "balanced CPS growth";
        String title = decision.getBestChoice() == null ? "selected goods" : decision.getBestChoice().getTitle();
        return "Use " + goal + " as the primary goal and lead with " + title + ". "
                + nullToEmpty(decision.getSummary());
    }

    private List<String> buildPromotionActions(Request request, CpsPurchaseDecisionResponse decision) {
        List<String> actions = new ArrayList<>();
        String channel = StringUtils.hasText(request.getContentChannel()) ? request.getContentChannel() : "owned channel";
        actions.add("Publish the best-value item first on " + channel + " with price, coupon, and rebate visible.");
        actions.add("Compare alternatives only when they improve price, rebate, or shop reliability.");
        if (decision.getBestChoice() != null && StringUtils.hasText(decision.getBestChoice().getPromotionUrl())) {
            actions.add("Use the generated promotion link for attribution and keep platform price disclaimers nearby.");
        } else {
            actions.add("Generate a trusted promotion link before distributing the final offer.");
        }
        return actions;
    }

    private List<String> buildCopyAngles(Request request, CpsPurchaseDecisionResponse decision) {
        String audience = StringUtils.hasText(request.getTargetAudience()) ? request.getTargetAudience() : "target users";
        List<String> angles = new ArrayList<>();
        angles.add("For " + audience + ": lead with the concrete use case and after-rebate value.");
        if (decision.getBestChoice() != null && decision.getBestChoice().getEstimatedRebate() != null) {
            angles.add("Highlight estimated rebate " + decision.getBestChoice().getEstimatedRebate()
                    + " as a benefit, not a settlement promise.");
        }
        angles.add("State that final price, coupon, stock, and rebate depend on the platform order result.");
        return angles;
    }

    private Long resolveTrustedUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Map<String, Object> context = toolContext.getContext();
        Object value = context.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Response error(String message) {
        return new Response(null, null, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), null, message);
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(" / ", parts);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
