package com.qiji.cps.module.cps.mcp.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers CPS/CPX function beans as Spring AI tool callbacks for MCP exposure.
 */
@Configuration
public class CpsMcpToolConfiguration {

    @Bean
    public List<ToolCallback> cpsMcpToolCallbacks(
            CpsSearchGoodsToolFunction searchGoodsToolFunction,
            CpsComparePricesToolFunction comparePricesToolFunction,
            CpsGenerateLinkToolFunction generateLinkToolFunction,
            CpsQueryOrdersToolFunction queryOrdersToolFunction,
            CpsGetRebateSummaryToolFunction getRebateSummaryToolFunction,
            CpsRecommendBySceneToolFunction recommendBySceneToolFunction,
            CpsPurchaseDecisionToolFunction purchaseDecisionToolFunction,
            CpsListSelectionThemesToolFunction listSelectionThemesToolFunction,
            CpsRecommendFromSelectionThemeToolFunction recommendFromSelectionThemeToolFunction,
            CpsGetRebateBalanceToolFunction getRebateBalanceToolFunction,
            CpsCreateTokenExchangeToolFunction createTokenExchangeToolFunction,
            CpsQueryExchangeStatusToolFunction queryExchangeStatusToolFunction,
            CpxListTasksToolFunction listTasksToolFunction,
            CpxGetTaskDetailToolFunction getTaskDetailToolFunction,
            CpxGenerateTrackingLinkToolFunction generateTrackingLinkToolFunction,
            CpxQueryConversionsToolFunction queryConversionsToolFunction,
            CpxRecommendTasksBySceneToolFunction recommendTasksBySceneToolFunction,
            CpxSearchArticlesToolFunction searchArticlesToolFunction) {
        return List.of(
                FunctionToolCallback.builder("cps_search_goods", searchGoodsToolFunction)
                        .description("多平台商品搜索，返回商品标题、价格、券后价、优惠券金额、佣金比例、预估返利和销量等信息")
                        .inputType(CpsSearchGoodsToolFunction.Request.class)
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
    }
}
