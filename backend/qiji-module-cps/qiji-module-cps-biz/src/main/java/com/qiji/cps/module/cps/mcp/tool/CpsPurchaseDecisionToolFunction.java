package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionRequest;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionResponse;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionService;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component("cps_purchase_decision")
public class CpsPurchaseDecisionToolFunction
        implements BiFunction<CpsPurchaseDecisionToolFunction.Request, ToolContext, CpsPurchaseDecisionResponse> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";

    @Resource
    private CpsPurchaseDecisionService purchaseDecisionService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("整合 CPS 商品搜索/比价/返利和海纳商品证据，给出可解释购买决策建议")
    public static class Request {

        @JsonProperty(required = true, value = "need")
        @JsonPropertyDescription("购买需求，例如：给 iPhone 16 买一个防摔手机壳")
        private String need;

        @JsonProperty(value = "scenario")
        @JsonPropertyDescription("使用场景，例如：日常通勤、办公室采购、AIoT 设备维护")
        private String scenario;

        @JsonProperty(value = "budget_min")
        @JsonPropertyDescription("最低预算（元）")
        private BigDecimal budgetMin;

        @JsonProperty(value = "budget_max")
        @JsonPropertyDescription("最高预算（元）")
        private BigDecimal budgetMax;

        @JsonProperty(value = "preferred_platforms")
        @JsonPropertyDescription("偏好的平台编码列表：taobao、jd、pdd、douyin 等；为空时搜索全平台")
        private List<String> preferredPlatforms;

        @JsonProperty(value = "decision_mode")
        @JsonPropertyDescription("决策模式：best_value=综合价值、low_price=低价、high_rebate=高返利、reliable_shop=可信店铺")
        private String decisionMode;

        @JsonProperty(value = "generate_link")
        @JsonPropertyDescription("是否为推荐商品生成 CPS 推广链接；只有存在可信登录上下文时才会转链")
        private Boolean generateLink;
    }

    @Override
    public CpsPurchaseDecisionResponse apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        if (request == null || !StringUtils.hasText(request.getNeed())) {
            CpsPurchaseDecisionResponse response = CpsPurchaseDecisionResponse.error("购买需求不能为空");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_purchase_decision", request, response,
                    new IllegalArgumentException("need required"), toolContext, startedAt);
            return response;
        }
        try {
            CpsPurchaseDecisionResponse response = purchaseDecisionService.decide(toServiceRequest(request),
                    resolveTrustedMemberId(toolContext));
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_purchase_decision", request, response, null, toolContext, startedAt);
            return response;
        } catch (Exception e) {
            CpsPurchaseDecisionResponse response = CpsPurchaseDecisionResponse.error("购买决策失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_purchase_decision", request, response, e, toolContext, startedAt);
            return response;
        }
    }

    private CpsPurchaseDecisionRequest toServiceRequest(Request request) {
        CpsPurchaseDecisionRequest serviceRequest = new CpsPurchaseDecisionRequest();
        serviceRequest.setNeed(request.getNeed());
        serviceRequest.setScenario(request.getScenario());
        serviceRequest.setBudgetMin(request.getBudgetMin());
        serviceRequest.setBudgetMax(request.getBudgetMax());
        serviceRequest.setPreferredPlatforms(request.getPreferredPlatforms());
        serviceRequest.setDecisionMode(request.getDecisionMode());
        serviceRequest.setGenerateLink(request.getGenerateLink());
        return serviceRequest;
    }

    private Long resolveTrustedMemberId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Map<String, Object> context = toolContext.getContext();
        if (context == null) {
            return null;
        }
        Object userId = context.get(TOOL_CONTEXT_LOGIN_USER_ID);
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Number value) {
            return value.longValue();
        }
        return null;
    }
}
