package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.ai.service.chat.tool.AiChatToolAction;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolActionField;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolActionOption;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolActionProvider;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolInteractionType;
import com.qiji.cps.module.ai.service.chat.tool.AiChatToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * User-facing descriptions for the CPS and CPX tools available in AI chat.
 * Internal tool names remain server-side and are removed by the controller response mapping.
 */
@Component
public class CpsAiChatToolActionProvider implements AiChatToolActionProvider {

    private static final String GROUP_GOODS = "商品决策";
    private static final String GROUP_PROMOTION = "推广选品";
    private static final String GROUP_ACCOUNT = "账户订单";
    private static final String GROUP_EXCHANGE = "Token 兑换";
    private static final String GROUP_CPX = "CPX 任务";

    private static final List<AiChatToolAction> ACTIONS = List.of(
            action("SEARCH_GOODS", "cps_search_goods", "搜商品", GROUP_GOODS,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在搜索商品", "商品搜索完成",
                    "帮我在全平台找{{keyword}}，预算 {{budget}} 元以内，返回 {{limit}} 件，按{{sort}}排序。",
                    text("keyword", "商品关键词", true, "例如：无线降噪耳机", null),
                    number("budget", "预算上限（元）", false, "500"),
                    number("limit", "返回数量", true, "5"),
                    select("sort", "排序方式", true, "券后价从低到高",
                            option("券后价从低到高", "券后价从低到高"),
                            option("预计返利从高到低", "预计返利从高到低"),
                            option("销量从高到低", "销量从高到低"))),
            action("COMPARE_PRICES", "cps_compare_prices", "全网比价", GROUP_GOODS,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在进行全网比价", "全网比价完成",
                    "比较{{keyword}}在全平台的到手价和返利，每个平台取前 {{limit}} 条，按{{sort}}排序。",
                    text("keyword", "商品关键词或型号", true, "例如：iPhone 16 256G", null),
                    number("limit", "每个平台数量", true, "5"),
                    select("sort", "排序方式", true, "预计净价从低到高",
                            option("预计净价从低到高", "预计净价从低到高"),
                            option("券后价从低到高", "券后价从低到高"),
                            option("预计返利从高到低", "预计返利从高到低"))),
            action("GENERATE_LINK", "cps_generate_link", "生成推广链接", GROUP_PROMOTION,
                    AiChatToolRiskLevel.ATTRIBUTION_WRITE, AiChatToolInteractionType.FORM,
                    "正在生成推广链接", "推广链接生成完成",
                    "为{{platform}}平台商品 {{goodsId}} 生成当前测试会员的推广返利链接，并返回券后价和预计返利。",
                    select("platform", "平台", true, "京东", platformOptions()),
                    text("goodsId", "商品 ID", true, "请输入商品 ID", null)),
            action("QUERY_ORDERS", "cps_query_orders", "查订单", GROUP_ACCOUNT,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在查询订单", "订单查询完成",
                    "查询当前测试会员近 {{days}} 天的{{platform}}{{status}}订单，最多返回 {{limit}} 条。",
                    select("platform", "平台", true, "京东", platformOptions()),
                    select("status", "订单状态", true, "已付款",
                            option("全部状态", "全部"), option("已付款", "已付款"),
                            option("已收货", "已收货"), option("已结算", "已结算"),
                            option("已失效", "已失效")),
                    number("days", "最近天数", true, "30"),
                    number("limit", "最多返回", true, "20")),
            action("GET_REBATE_SUMMARY", "cps_get_rebate_summary", "返利汇总", GROUP_ACCOUNT,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.DIRECT,
                    "正在汇总返利", "返利汇总完成",
                    "汇总当前测试会员的返利账户，并列出最近 5 条返利记录。"),
            action("RECOMMEND_BY_SCENE", "cps_recommend_by_scene", "场景推荐", GROUP_GOODS,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在按场景推荐商品", "场景商品推荐完成",
                    "根据场景“{{scene}}”推荐适合的商品，预算 {{budget}} 元以内，返回 {{limit}} 件并说明推荐理由。",
                    textarea("scene", "使用场景", true, "例如：通勤时降噪听音乐", null),
                    number("budget", "预算上限（元）", false, "500"),
                    number("limit", "返回数量", true, "5")),
            action("PURCHASE_DECISION", "cps_purchase_decision", "购买决策", GROUP_GOODS,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.FORM,
                    "正在分析购买方案", "购买建议生成完成",
                    "围绕“{{requirement}}”生成购买决策，预算 {{budget}} 元，重点比较{{focus}}并说明理由。",
                    textarea("requirement", "购买需求", true, "描述商品、型号或候选方案", null),
                    number("budget", "预算（元）", false, ""),
                    text("focus", "重点关注", false, "例如：净价、品质和售后", "净价、品质和售后")),
            action("PROMOTION_STRATEGY_ADVICE", "cps_promotion_strategy_advice", "推广策略", GROUP_PROMOTION,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.FORM,
                    "正在制定推广策略", "推广策略生成完成",
                    "针对{{product}}，面向{{audience}}，在{{channel}}渠道仅生成策略建议，不要生成推广链接。",
                    text("product", "商品或品类", true, "请输入商品或品类", null),
                    text("audience", "目标人群", true, "例如：大学生", null),
                    select("channel", "推广渠道", true, "社群",
                            option("社群", "社群"), option("短视频", "短视频"),
                            option("直播", "直播"), option("图文", "图文"))),
            action("EXPLAIN_REBATE", "cps_explain_rebate", "返利说明", GROUP_ACCOUNT,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在解释返利规则", "返利规则说明完成",
                    "结合当前测试会员账户解释“{{topic}}”，说明计算、冻结和结算规则。",
                    text("topic", "想了解的问题", true, "例如：为什么返利还在冻结中", "返利如何计算")),
            action("LIST_SELECTION_THEMES", "cps_list_selection_themes", "选品主题", GROUP_PROMOTION,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.DIRECT,
                    "正在查询选品主题", "选品主题查询完成",
                    "列出当前已发布的选品主题，优先展示近期大促主题。"),
            action("RECOMMEND_FROM_SELECTION_THEME", "cps_recommend_from_selection_theme", "主题选品推荐",
                    GROUP_PROMOTION, AiChatToolRiskLevel.ATTRIBUTION_WRITE, AiChatToolInteractionType.FORM,
                    "正在从主题中选品", "主题选品推荐完成",
                    "从选品主题 {{themeId}} 中为当前测试会员推荐 {{limit}} 件符合“{{requirement}}”的商品。",
                    text("themeId", "主题 ID", true, "请输入选品主题 ID", null),
                    number("limit", "推荐数量", true, "5"),
                    text("requirement", "选品要求", false, "例如：高佣金且价格低于 100 元", "高性价比")),
            action("GET_REBATE_BALANCE", "cps_get_rebate_balance", "可兑换余额", GROUP_ACCOUNT,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.DIRECT,
                    "正在查询可兑换余额", "余额查询完成",
                    "查询当前测试会员可兑换的返利余额，并说明冻结余额。"),
            action("CREATE_TOKEN_EXCHANGE", "cps_create_token_exchange", "创建兑换", GROUP_EXCHANGE,
                    AiChatToolRiskLevel.ASSET_WRITE, AiChatToolInteractionType.FORM,
                    "正在创建返利兑换", "返利兑换请求已创建",
                    "将当前测试会员 {{amount}} 元可用返利兑换为 Token；请返回兑换订单号和处理状态。",
                    number("amount", "兑换金额（元）", true, "")),
            action("QUERY_EXCHANGE_STATUS", "cps_query_exchange_status", "查询兑换进度", GROUP_EXCHANGE,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在查询兑换进度", "兑换进度查询完成",
                    "查询当前测试会员返利兑换订单 {{exchangeOrderNo}} 的最新状态和失败原因。",
                    text("exchangeOrderNo", "兑换订单号", true, "请输入兑换订单号", null)),
            action("LIST_TASKS", "cpx_list_tasks", "任务列表", GROUP_CPX,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在查询任务列表", "任务列表查询完成",
                    "查询当前已发布的 CPX 任务，筛选关键词“{{keyword}}”，最多返回 {{limit}} 条。",
                    text("keyword", "关键词", false, "可不填", ""),
                    number("limit", "返回数量", true, "20")),
            action("GET_TASK_DETAIL", "cpx_get_task_detail", "任务详情", GROUP_CPX,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在查询任务详情", "任务详情查询完成",
                    "查询 CPX 任务 {{taskId}} 的详情、奖励规则、有效期和参与条件。",
                    text("taskId", "任务 ID", true, "请输入任务 ID", null)),
            action("GENERATE_TRACKING_LINK", "cpx_generate_tracking_link", "生成跟踪链接", GROUP_CPX,
                    AiChatToolRiskLevel.ATTRIBUTION_WRITE, AiChatToolInteractionType.FORM,
                    "正在生成任务跟踪链接", "任务跟踪链接生成完成",
                    "为 CPX 任务 {{taskId}} 生成当前测试会员的跟踪链接，并说明有效期。",
                    text("taskId", "任务 ID", true, "请输入任务 ID", null)),
            action("QUERY_CONVERSIONS", "cpx_query_conversions", "转化查询", GROUP_CPX,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在查询转化记录", "转化记录查询完成",
                    "查询当前测试会员在 {{dateRange}} 内的 CPX 转化记录，状态为{{status}}。",
                    dateRange("dateRange", "时间范围", true),
                    select("status", "转化状态", true, "全部",
                            option("全部", "全部"), option("待确认", "待确认"),
                            option("已确认", "已确认"), option("已失效", "已失效"))),
            action("RECOMMEND_TASKS_BY_SCENE", "cpx_recommend_tasks_by_scene", "场景任务推荐", GROUP_CPX,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在推荐场景任务", "场景任务推荐完成",
                    "根据场景“{{scene}}”推荐 {{limit}} 个适合的 CPX 任务，并说明奖励和匹配理由。",
                    textarea("scene", "推广场景", true, "例如：母婴社群周末促销", null),
                    number("limit", "推荐数量", true, "5")),
            action("SEARCH_ARTICLES", "cpx_search_articles", "推广文章搜索", GROUP_CPX,
                    AiChatToolRiskLevel.READ_ONLY, AiChatToolInteractionType.SIMPLE_FORM,
                    "正在搜索推广内容", "推广内容搜索完成",
                    "搜索与“{{keyword}}”相关的 CPX 推广文章，返回 {{limit}} 条并概括重点。",
                    text("keyword", "搜索关键词", true, "请输入关键词", null),
                    number("limit", "返回数量", true, "10"))
    );

    @Override
    public List<AiChatToolAction> getToolActions() {
        return ACTIONS;
    }

    private static AiChatToolAction action(String intent, String toolName, String label, String group,
                                           AiChatToolRiskLevel riskLevel,
                                           AiChatToolInteractionType interactionType,
                                           String runningMessage, String successMessage,
                                           String promptTemplate, AiChatToolActionField... fields) {
        return new AiChatToolAction().setIntent(intent).setToolName(toolName).setLabel(label).setGroup(group)
                .setRiskLevel(riskLevel).setInteractionType(interactionType)
                .setRunningMessage(runningMessage).setSuccessMessage(successMessage)
                .setPromptTemplate(promptTemplate).setFields(List.of(fields));
    }

    private static AiChatToolActionField text(String name, String label, boolean required,
                                               String placeholder, String defaultValue) {
        return field(name, label, "TEXT", required, placeholder, defaultValue, List.of());
    }

    private static AiChatToolActionField textarea(String name, String label, boolean required,
                                                   String placeholder, String defaultValue) {
        return field(name, label, "TEXTAREA", required, placeholder, defaultValue, List.of());
    }

    private static AiChatToolActionField number(String name, String label, boolean required, String defaultValue) {
        return field(name, label, "NUMBER", required, null, defaultValue, List.of());
    }

    private static AiChatToolActionField dateRange(String name, String label, boolean required) {
        return field(name, label, "DATE_RANGE", required, null, null, List.of());
    }

    private static AiChatToolActionField select(String name, String label, boolean required, String defaultValue,
                                                 AiChatToolActionOption... options) {
        return select(name, label, required, defaultValue, List.of(options));
    }

    private static AiChatToolActionField select(String name, String label, boolean required, String defaultValue,
                                                 List<AiChatToolActionOption> options) {
        return field(name, label, "SELECT", required, "请选择", defaultValue, options);
    }

    private static AiChatToolActionField field(String name, String label, String type, boolean required,
                                                String placeholder, String defaultValue,
                                                List<AiChatToolActionOption> options) {
        return new AiChatToolActionField().setName(name).setLabel(label).setType(type).setRequired(required)
                .setPlaceholder(placeholder).setDefaultValue(defaultValue).setOptions(options);
    }

    private static AiChatToolActionOption option(String label, String value) {
        return new AiChatToolActionOption().setLabel(label).setValue(value);
    }

    private static List<AiChatToolActionOption> platformOptions() {
        return List.of(option("全平台", "全平台"), option("淘宝", "淘宝"), option("京东", "京东"),
                option("拼多多", "拼多多"), option("抖音", "抖音"));
    }

}
