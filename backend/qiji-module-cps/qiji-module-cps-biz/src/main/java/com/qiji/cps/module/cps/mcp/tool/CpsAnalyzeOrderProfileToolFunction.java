package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 基于可信会员订单的只读成交画像分析。
 *
 * <p>只聚合订单中已有的商品、平台、金额和返利字段，不推断性别、年龄等系统没有证据的数据。</p>
 */
@Component("cps_analyze_order_profile")
public class CpsAnalyzeOrderProfileToolFunction implements
        BiFunction<CpsAnalyzeOrderProfileToolFunction.Request, ToolContext,
                CpsAnalyzeOrderProfileToolFunction.Response> {

    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Resource
    private CpsOrderMapper orderMapper;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("分析当前可信会员最近一段时间的 CPS 成交画像，仅返回聚合数据")
    public static class Request {

        @JsonProperty(value = "days")
        @JsonPropertyDescription("分析最近天数，默认30，最小1，最大90")
        private Integer days;

        @JsonProperty(value = "exclude_item_ids")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @JsonPropertyDescription("排除已推广或不希望重复推荐的商品ID")
        private List<String> excludeItemIds;

        @JsonProperty(value = "exclude_item_ids_csv")
        @JsonPropertyDescription("逗号、中文逗号或换行分隔的排除商品ID；用于表单输入兼容")
        private String excludeItemIdsCsv;

        @JsonProperty(value = "max_orders")
        @JsonPropertyDescription("最多分析订单数，默认200，最大500")
        private Integer maxOrders;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Integer days;
        private Integer analyzedOrders;
        private Integer excludedOrders;
        private BigDecimal gmv;
        private BigDecimal estimatedRebate;
        private BigDecimal realRebate;
        private BigDecimal averageOrderValue;
        private List<Breakdown> platformBreakdown;
        private List<Breakdown> priceBandBreakdown;
        private List<TopProduct> topProducts;
        private List<String> insights;
        private List<String> dataLimitations;
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Breakdown {
        private String name;
        private Integer orderCount;
        private BigDecimal gmv;
        private BigDecimal rebate;
        private BigDecimal share;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProduct {
        private String itemId;
        private String title;
        private String platformCode;
        private Integer orderCount;
        private BigDecimal gmv;
        private BigDecimal rebate;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        Request safeRequest = request == null ? new Request() : request;
        Long memberId = extractMemberId(toolContext);
        if (memberId == null) {
            Response response = error("未登录或无法获取可信会员信息，请先登录");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_analyze_order_profile", safeRequest, response,
                    new IllegalStateException("missing trusted member context"), toolContext, startedAt);
            return response;
        }
        try {
            int days = normalizeDays(safeRequest.getDays());
            int maxOrders = normalizeMaxOrders(safeRequest.getMaxOrders());
            LocalDateTime endTime = LocalDateTime.now();
            List<CpsOrderDO> source = orderMapper.selectRecentListByMemberId(
                    memberId, endTime.minusDays(days), endTime, maxOrders);
            List<CpsOrderDO> orders = source == null ? Collections.emptyList() : source;
            Set<String> excludedIds = excludedItemIds(safeRequest);
            List<CpsOrderDO> included = orders.stream()
                    .filter(order -> !excludedIds.contains(order.getItemId()))
                    .toList();
            Response response = analyze(days, orders.size() - included.size(), included);
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_analyze_order_profile", safeRequest, response,
                    null, toolContext, startedAt);
            return response;
        } catch (Exception exception) {
            Response response = error("成交画像分析失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, "cps_analyze_order_profile", safeRequest, response,
                    exception, toolContext, startedAt);
            return response;
        }
    }

    private Response analyze(int days, int excludedOrders, List<CpsOrderDO> orders) {
        BigDecimal gmv = sum(orders, CpsOrderDO::getFinalPrice);
        BigDecimal estimatedRebate = sum(orders, CpsOrderDO::getEstimateRebate);
        BigDecimal realRebate = sum(orders, CpsOrderDO::getRealRebate);
        BigDecimal averageOrderValue = orders.isEmpty() ? ZERO
                : gmv.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
        List<Breakdown> platforms = breakdown(orders, order -> textOr(order.getPlatformCode(), "unknown"), gmv);
        List<Breakdown> priceBands = breakdown(orders, order -> priceBand(order.getFinalPrice()), gmv);
        List<TopProduct> topProducts = topProducts(orders);
        List<String> insights = buildInsights(orders, platforms, priceBands, topProducts, gmv, realRebate);
        List<String> limitations = List.of(
                "画像仅基于已归因到当前可信会员的 CPS 订单，不包含未归因或外部渠道订单",
                "系统没有可靠的人口属性证据，因此不会推断性别、年龄或家庭身份",
                "订单金额和返利用于运营分析；最终结算仍以平台订单及返利资产流水为准");
        return new Response(days, orders.size(), excludedOrders, gmv, estimatedRebate, realRebate,
                averageOrderValue, platforms, priceBands, topProducts, insights, limitations, null);
    }

    private List<Breakdown> breakdown(List<CpsOrderDO> orders,
                                      java.util.function.Function<CpsOrderDO, String> classifier,
                                      BigDecimal totalGmv) {
        Map<String, List<CpsOrderDO>> groups = orders.stream().collect(
                Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream().map(entry -> {
                    BigDecimal groupGmv = sum(entry.getValue(), CpsOrderDO::getFinalPrice);
                    BigDecimal groupRebate = sum(entry.getValue(), this::effectiveRebate);
                    BigDecimal share = totalGmv.signum() == 0 ? ZERO
                            : groupGmv.multiply(BigDecimal.valueOf(100))
                            .divide(totalGmv, 2, RoundingMode.HALF_UP);
                    return new Breakdown(entry.getKey(), entry.getValue().size(), groupGmv, groupRebate, share);
                }).sorted(Comparator.comparing(Breakdown::getGmv).reversed()).toList();
    }

    private List<TopProduct> topProducts(List<CpsOrderDO> orders) {
        Map<String, List<CpsOrderDO>> groups = orders.stream().collect(Collectors.groupingBy(
                order -> textOr(order.getPlatformCode(), "unknown") + ":" + textOr(order.getItemId(), "unknown"),
                LinkedHashMap::new, Collectors.toList()));
        return groups.values().stream().map(group -> {
                    CpsOrderDO first = group.get(0);
                    return new TopProduct(first.getItemId(), first.getItemTitle(), first.getPlatformCode(), group.size(),
                            sum(group, CpsOrderDO::getFinalPrice), sum(group, this::effectiveRebate));
                }).sorted(Comparator.comparing(TopProduct::getOrderCount).reversed()
                        .thenComparing(TopProduct::getGmv, Comparator.reverseOrder()))
                .limit(10).toList();
    }

    private List<String> buildInsights(List<CpsOrderDO> orders, List<Breakdown> platforms,
                                       List<Breakdown> priceBands, List<TopProduct> topProducts,
                                       BigDecimal gmv, BigDecimal realRebate) {
        List<String> insights = new ArrayList<>();
        if (orders.isEmpty()) {
            insights.add("当前时间范围内没有可用于分析的已归因订单");
            return insights;
        }
        if (!priceBands.isEmpty()) {
            Breakdown topBand = priceBands.get(0);
            insights.add("成交金额主要集中在" + topBand.getName() + "价格带，占比 " + topBand.getShare() + "%");
        }
        if (!platforms.isEmpty()) {
            Breakdown topPlatform = platforms.get(0);
            insights.add("贡献最高的平台是 " + topPlatform.getName() + "，成交 " + topPlatform.getOrderCount() + " 单");
        }
        if (!topProducts.isEmpty()) {
            TopProduct top = topProducts.get(0);
            insights.add("复推候选优先参考「" + textOr(top.getTitle(), top.getItemId()) + "」，窗口内成交 "
                    + top.getOrderCount() + " 单");
        }
        if (gmv.signum() > 0 && realRebate.signum() > 0) {
            BigDecimal yield = realRebate.multiply(BigDecimal.valueOf(100))
                    .divide(gmv, 2, RoundingMode.HALF_UP);
            insights.add("已确认返利约占成交金额的 " + yield + "%；该比例只用于复盘，不代表未来结算承诺");
        }
        return insights;
    }

    private BigDecimal sum(List<CpsOrderDO> orders,
                           java.util.function.Function<CpsOrderDO, BigDecimal> extractor) {
        return orders.stream().map(extractor).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveRebate(CpsOrderDO order) {
        return order.getRealRebate() != null ? order.getRealRebate() : order.getEstimateRebate();
    }

    private String priceBand(BigDecimal price) {
        if (price == null) return "价格未知";
        if (price.compareTo(new BigDecimal("10")) < 0) return "0-10元";
        if (price.compareTo(new BigDecimal("40")) < 0) return "10-40元";
        if (price.compareTo(new BigDecimal("100")) < 0) return "40-100元";
        if (price.compareTo(new BigDecimal("300")) < 0) return "100-300元";
        return "300元以上";
    }

    private int normalizeDays(Integer days) {
        return days == null ? 30 : Math.max(1, Math.min(days, 90));
    }

    private int normalizeMaxOrders(Integer maxOrders) {
        return maxOrders == null ? 200 : Math.max(1, Math.min(maxOrders, 500));
    }

    private Long extractMemberId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) return null;
        Object value = toolContext.getContext().get(TOOL_CONTEXT_LOGIN_USER_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    private Response error(String message) {
        return new Response(null, 0, 0, ZERO, ZERO, ZERO, ZERO,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), message);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Set<String> excludedItemIds(Request request) {
        Set<String> ids = new LinkedHashSet<>();
        if (request.getExcludeItemIds() != null) {
            request.getExcludeItemIds().stream().filter(this::hasText)
                    .flatMap(value -> java.util.Arrays.stream(value.split("[,，\\r\\n]+")))
                    .map(String::trim).filter(this::hasText).forEach(ids::add);
        }
        if (hasText(request.getExcludeItemIdsCsv())) {
            java.util.Arrays.stream(request.getExcludeItemIdsCsv().split("[,，\\r\\n]+"))
                    .map(String::trim).filter(this::hasText).forEach(ids::add);
        }
        return ids;
    }

    private String textOr(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }
}
