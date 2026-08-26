package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qiji.cps.framework.common.mcp.McpIdentityEnvelope;
import com.qiji.cps.framework.common.mcp.McpIdentityTransportKeys;
import com.qiji.cps.framework.common.util.json.JsonUtils;
import com.qiji.cps.framework.tenant.core.util.TenantUtils;
import com.qiji.cps.module.ai.service.chat.AiChatIdentityContextService;
import com.qiji.cps.module.cps.mcp.security.CpsMcpAuthorizationService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers CPS/CPX function beans as Spring AI tool callbacks for MCP exposure.
 */
@Configuration
public class CpsMcpToolConfiguration {

    @Bean
    public List<ToolCallback> cpsMcpToolCallbacks(
            CpsSearchGoodsToolFunction searchGoodsToolFunction,
            CpsFindResonanceGoodsToolFunction findResonanceGoodsToolFunction,
            CpsFindAlternativesToolFunction findAlternativesToolFunction,
            CpsAnalyzeGoodsDetailToolFunction analyzeGoodsDetailToolFunction,
            CpsAnalyzeOrderProfileToolFunction analyzeOrderProfileToolFunction,
            CpsAnalyzeOrderTrendToolFunction analyzeOrderTrendToolFunction,
            CpsComparePricesToolFunction comparePricesToolFunction,
            CpsGenerateLinkToolFunction generateLinkToolFunction,
            CpsQueryOrdersToolFunction queryOrdersToolFunction,
            CpsGetRebateSummaryToolFunction getRebateSummaryToolFunction,
            CpsRecommendBySceneToolFunction recommendBySceneToolFunction,
            CpsPurchaseDecisionToolFunction purchaseDecisionToolFunction,
            CpsPromotionStrategyAdviceToolFunction promotionStrategyAdviceToolFunction,
            CpsExplainRebateToolFunction explainRebateToolFunction,
            CpsListSelectionThemesToolFunction listSelectionThemesToolFunction,
            CpsRecommendFromSelectionThemeToolFunction recommendFromSelectionThemeToolFunction,
            CpsGetRebateBalanceToolFunction getRebateBalanceToolFunction,
            CpsCreateTokenExchangeToolFunction createTokenExchangeToolFunction,
            CpsQueryExchangeStatusToolFunction queryExchangeStatusToolFunction,
            @Qualifier("cpx_list_tasks") CpxListTasksToolFunction listTasksToolFunction,
            CpxGetTaskDetailToolFunction getTaskDetailToolFunction,
            CpxGenerateTrackingLinkToolFunction generateTrackingLinkToolFunction,
            CpxQueryConversionsToolFunction queryConversionsToolFunction,
            CpxRecommendTasksBySceneToolFunction recommendTasksBySceneToolFunction,
            CpxSearchArticlesToolFunction searchArticlesToolFunction,
            CpsMcpAuthorizationService authorizationService) {
        List<ToolCallback> callbacks = List.of(
                FunctionToolCallback.builder("cps_search_goods", searchGoodsToolFunction)
                        .description("多平台商品搜索，返回商品标题、价格、券后价、优惠券金额、佣金比例、预估返利和销量等信息")
                        .inputType(CpsSearchGoodsToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_find_resonance_goods", findResonanceGoodsToolFunction)
                        .description("聚合综合搜索、销量、高佣、2 小时热销和全天热销候选，规则评分并返回多来源共振商品与证据")
                        .inputType(CpsFindResonanceGoodsToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_find_alternatives", findAlternativesToolFunction)
                        .description("为商品或品类寻找价格相近、销量可靠且佣金更优的替代候选；只做选品分析")
                        .inputType(CpsFindAlternativesToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_analyze_goods_detail", analyzeGoodsDetailToolFunction)
                        .description("分析商品搜索候选的价格带、优惠券、佣金、销量和平台覆盖，仅返回运营快照")
                        .inputType(CpsAnalyzeGoodsDetailToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_analyze_order_profile", analyzeOrderProfileToolFunction)
                        .description("分析当前可信会员最近一段时间的成交画像，聚合订单、GMV、返利、平台、价格带和热销商品")
                        .inputType(CpsAnalyzeOrderProfileToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_analyze_order_trend", analyzeOrderTrendToolFunction)
                        .description("分析当前可信会员已归因 CPS 订单的成交、客单价与返利时间趋势")
                        .inputType(CpsAnalyzeOrderTrendToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_compare_prices", comparePricesToolFunction)
                        .description("跨平台或单平台比价，按券后价、返利金额和到手净价推荐最优商品")
                        .inputType(CpsComparePricesToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_generate_link", generateLinkToolFunction)
                        .description("为指定商品生成 CPS 推广/返利链接，返回短链、长链、淘口令、券后价、优惠券和预估返利")
                        .inputType(CpsGenerateLinkToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_query_orders", queryOrdersToolFunction)
                        .description("查询当前可信会员上下文下的 CPS 订单和返利状态")
                        .inputType(CpsQueryOrdersToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_get_rebate_summary", getRebateSummaryToolFunction)
                        .description("查询当前可信会员上下文下的返利账户汇总、可用返利、待结算返利和近期记录")
                        .inputType(CpsGetRebateSummaryToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_recommend_by_scene", recommendBySceneToolFunction)
                        .description("按 AIoT 或自然语言场景需求推荐 CPS 商品，返回价格、优惠券、佣金和推荐原因")
                        .inputType(CpsRecommendBySceneToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_purchase_decision", purchaseDecisionToolFunction)
                        .description("基于 CPS 候选商品和外部证据生成购买决策建议，可返回价格、优惠券、返利和推荐理由")
                        .inputType(CpsPurchaseDecisionToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_promotion_strategy_advice", promotionStrategyAdviceToolFunction)
                        .description("根据商品需求、人群、渠道、价格、优惠券、返利和归因约束生成 CPS 推广策略建议")
                        .inputType(CpsPromotionStrategyAdviceToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_explain_rebate", explainRebateToolFunction)
                        .description("解释 CPS 返利规则、计算优先级、冻结结算流程，并返回当前可信账户汇总")
                        .inputType(CpsExplainRebateToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_list_selection_themes", listSelectionThemesToolFunction)
                        .description("查询已发布选品主题")
                        .inputType(CpsListSelectionThemesToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_recommend_from_selection_theme", recommendFromSelectionThemeToolFunction)
                        .description("从已发布选品主题中推荐商品，默认只读返回价格、优惠券、佣金和推荐理由")
                        .inputType(CpsRecommendFromSelectionThemeToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_get_rebate_balance", getRebateBalanceToolFunction)
                        .description("查询当前可信会员上下文下可兑换返利余额")
                        .inputType(CpsGetRebateBalanceToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_create_token_exchange", createTokenExchangeToolFunction)
                        .description("创建返利兑换 Token 订单，只能兑换 AVAILABLE 可用返利")
                        .inputType(CpsCreateTokenExchangeToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cps_query_exchange_status", queryExchangeStatusToolFunction)
                        .description("查询返利兑换 Token 订单状态")
                        .inputType(CpsQueryExchangeStatusToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cpx_list_tasks", listTasksToolFunction)
                        .description("查询已发布 CPX/CPS 任务列表")
                        .inputType(CpxListTasksToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cpx_get_task_detail", getTaskDetailToolFunction)
                        .description("查询 CPX/CPS 任务详情")
                        .inputType(CpxGetTaskDetailToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cpx_generate_tracking_link", generateTrackingLinkToolFunction)
                        .description("生成 CPX/CPS 任务跟踪链接，使用可信会员上下文做归因")
                        .inputType(CpxGenerateTrackingLinkToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cpx_query_conversions", queryConversionsToolFunction)
                        .description("查询当前可信会员上下文下的 CPX/CPS 转化记录")
                        .inputType(CpxQueryConversionsToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cpx_recommend_tasks_by_scene", recommendTasksBySceneToolFunction)
                        .description("按场景推荐 CPX/CPS 任务")
                        .inputType(CpxRecommendTasksBySceneToolFunction.Request.class)
                        .build(),
                FunctionToolCallback.builder("cpx_search_articles", searchArticlesToolFunction)
                        .description("搜索 CPX/CPS 内容和资讯")
                        .inputType(CpxSearchArticlesToolFunction.Request.class)
                        .build());
        return callbacks.stream().<ToolCallback>map(callback -> new AuthorizedToolCallback(callback, authorizationService))
                .toList();
    }

    /**
     * A callback must authorize self-test calls immediately before the actual tool receives context.
     */
    private static class AuthorizedToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final CpsMcpAuthorizationService authorizationService;

        private AuthorizedToolCallback(ToolCallback delegate, CpsMcpAuthorizationService authorizationService) {
            this.delegate = delegate;
            this.authorizationService = authorizationService;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, new ToolContext(java.util.Map.of()));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            TransportInput transportInput = extractTransportInput(toolInput, toolContext);
            ToolContext trustedContext = authorizationService.authorize(
                    getToolDefinition().name(), transportInput.toolContext());
            if (!AiChatIdentityContextService.isTrustedLocalToolContext(trustedContext)
                    && !CpsMcpAuthorizationService.isTrustedSelfTestContext(trustedContext)) {
                return delegate.call(transportInput.toolInput(), trustedContext);
            }
            Long tenantId = requiredTenantId(trustedContext);
            String[] result = new String[1];
            TenantUtils.execute(tenantId,
                    () -> result[0] = delegate.call(transportInput.toolInput(), trustedContext));
            return result[0];
        }

        private static TransportInput extractTransportInput(String toolInput, ToolContext toolContext) {
            Map<String, Object> arguments = JsonUtils.parseObject(toolInput,
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            if (arguments == null) {
                arguments = new LinkedHashMap<>();
            }
            boolean attemptedSelfTest = arguments.containsKey(McpIdentityTransportKeys.ARG_SELF_TEST_INVOCATION)
                    || arguments.containsKey(McpIdentityTransportKeys.ARG_IDENTITY_PAYLOAD)
                    || arguments.containsKey(McpIdentityTransportKeys.ARG_IDENTITY_SIGNATURE);
            Object invocationMarker = arguments.remove(McpIdentityTransportKeys.ARG_SELF_TEST_INVOCATION);
            Object payload = arguments.remove(McpIdentityTransportKeys.ARG_IDENTITY_PAYLOAD);
            Object signature = arguments.remove(McpIdentityTransportKeys.ARG_IDENTITY_SIGNATURE);
            if (!attemptedSelfTest) {
                return new TransportInput(toolInput, toolContext);
            }
            Map<String, Object> context = new HashMap<>();
            if (toolContext != null && toolContext.getContext() != null) {
                context.putAll(toolContext.getContext());
            }
            context.put(McpIdentityTransportKeys.META_SELF_TEST_INVOCATION,
                    Boolean.TRUE.equals(invocationMarker) || "true".equalsIgnoreCase(String.valueOf(invocationMarker)));
            if (payload instanceof String payloadText && signature instanceof String signatureText) {
                context.put(McpIdentityTransportKeys.META_IDENTITY_ENVELOPE,
                        new McpIdentityEnvelope(payloadText, signatureText));
            }
            return new TransportInput(JsonUtils.toJsonString(arguments), new ToolContext(context));
        }

        private static Long requiredTenantId(ToolContext toolContext) {
            Object value = toolContext.getContext().get(CpsMcpAuthorizationService.TOOL_CONTEXT_TENANT_ID);
            long tenantId;
            try {
                tenantId = value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
            } catch (RuntimeException exception) {
                throw new SecurityException("Trusted tool context tenant is invalid", exception);
            }
            if (tenantId <= 0) {
                throw new SecurityException("Trusted tool context tenant is invalid");
            }
            return tenantId;
        }

        private record TransportInput(String toolInput, ToolContext toolContext) {
        }
    }
}
