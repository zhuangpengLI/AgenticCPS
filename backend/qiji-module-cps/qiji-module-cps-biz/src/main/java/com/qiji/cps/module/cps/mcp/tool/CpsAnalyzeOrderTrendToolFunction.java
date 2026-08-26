package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP Tool：可信会员订单成交趋势。
 *
 * <p>只聚合已有、已归因订单的日期、金额和返利字段，不写订单、不触发归因或结算。</p>
 */
@Component("cps_analyze_order_trend")
public class CpsAnalyzeOrderTrendToolFunction implements
        BiFunction<CpsAnalyzeOrderTrendToolFunction.Request, ToolContext,
                CpsAnalyzeOrderTrendToolFunction.Response> {

    private static final String TOOL_NAME = "cps_analyze_order_trend";
    private static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Resource
    private CpsOrderMapper orderMapper;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("分析当前可信会员已归因 CPS 订单的成交与返利趋势，仅返回聚合数据")
    public static class Request {

        @JsonProperty(value = "days")
        @JsonPropertyDescription("分析最近天数，默认30，最小7，最大180")
        private Integer days;

        @JsonProperty(value = "granularity")
        @JsonPropertyDescription("趋势粒度：daily 或 weekly；不传时30天以内按 daily，否则按 weekly")
        private String granularity;

        @JsonProperty(value = "max_orders")
        @JsonPropertyDescription("最多分析订单数，默认300，最大500")
        private Integer maxOrders;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Integer days;
        private String granularity;
        private Integer analyzedOrders;
        private BigDecimal totalGmv;
        private BigDecimal totalEstimatedRebate;
        private BigDecimal totalRealRebate;
        private List<TrendPoint> points;
        private List<String> insights;
        private List<String> dataLimitations;
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendPoint {
        private String period;
        private Integer orderCount;
        private BigDecimal gmv;
        private BigDecimal estimatedRebate;
        private BigDecimal realRebate;
        private BigDecimal averageOrderValue;
    }

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        long startedAt = System.currentTimeMillis();
        Request safeRequest = request == null ? new Request() : request;
        Long memberId = trustedMemberId(toolContext);
        if (memberId == null) {
            Response response = error("未登录或无法获取可信会员信息，请先登录");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, safeRequest, response,
                    new IllegalStateException("missing trusted member context"), toolContext, startedAt);
            return response;
        }
        try {
            int days = normalizeDays(safeRequest.getDays());
            String granularity = normalizeGranularity(safeRequest.getGranularity(), days);
            LocalDateTime end = LocalDateTime.now();
            List<CpsOrderDO> orders = orderMapper.selectRecentListByMemberId(memberId, end.minusDays(days), end,
                    normalizeMaxOrders(safeRequest.getMaxOrders()));
            List<CpsOrderDO> safeOrders = orders == null ? Collections.emptyList() : orders;
            Response response = analyze(days, granularity, safeOrders);
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, safeRequest, response, null, toolContext, startedAt);
            return response;
        } catch (RuntimeException exception) {
            Response response = error("成交趋势分析失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, safeRequest, response, exception, toolContext, startedAt);
            return response;
        }
    }

    private Response analyze(int days, String granularity, List<CpsOrderDO> orders) {
        Map<String, List<CpsOrderDO>> groups = new LinkedHashMap<>();
        for (CpsOrderDO order : orders) {
            if (order == null) continue;
            groups.computeIfAbsent(period(order.getCreateTime(), granularity), ignored -> new ArrayList<>()).add(order);
        }
        List<TrendPoint> points = groups.entrySet().stream().map(entry -> point(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TrendPoint::getPeriod)).toList();
        BigDecimal totalGmv = sum(orders, CpsOrderDO::getFinalPrice);
        BigDecimal totalEstimatedRebate = sum(orders, CpsOrderDO::getEstimateRebate);
        BigDecimal totalRealRebate = sum(orders, CpsOrderDO::getRealRebate);
        return new Response(days, granularity, orders.size(), totalGmv, totalEstimatedRebate, totalRealRebate,
                points, insights(points), List.of(
                        "趋势仅基于当前可信会员已有归因的 CPS 订单，不包含未归因或外部渠道订单",
                        "订单金额和返利仅用于运营复盘；最终结算仍以平台订单及返利资产流水为准",
                        "时间段按订单创建时间归集，平台延迟同步可能影响最近周期的完整性"), null);
    }

    private TrendPoint point(String period, List<CpsOrderDO> orders) {
        BigDecimal gmv = sum(orders, CpsOrderDO::getFinalPrice);
        return new TrendPoint(period, orders.size(), gmv, sum(orders, CpsOrderDO::getEstimateRebate),
                sum(orders, CpsOrderDO::getRealRebate), orders.isEmpty() ? ZERO
                        : gmv.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP));
    }

    private List<String> insights(List<TrendPoint> points) {
        if (points.isEmpty()) return List.of("当前时间范围内没有可用于趋势分析的已归因订单");
        List<String> insights = new ArrayList<>();
        TrendPoint peak = points.stream().max(Comparator.comparing(TrendPoint::getGmv)).orElseThrow();
        insights.add("成交金额最高的周期是 " + peak.getPeriod() + "，GMV " + peak.getGmv() + " 元");
        if (points.size() >= 2) {
            TrendPoint previous = points.get(points.size() - 2);
            TrendPoint latest = points.get(points.size() - 1);
            if (previous.getGmv().signum() > 0) {
                BigDecimal change = latest.getGmv().subtract(previous.getGmv()).multiply(BigDecimal.valueOf(100))
                        .divide(previous.getGmv(), 2, RoundingMode.HALF_UP);
                insights.add("最近周期较上一周期 GMV " + (change.signum() >= 0 ? "+" : "") + change + "%");
            } else {
                insights.add("上一周期无 GMV，最近周期 GMV 为 " + latest.getGmv() + " 元");
            }
        }
        return insights;
    }

    private String period(LocalDateTime createTime, String granularity) {
        if (createTime == null) return "时间未知";
        LocalDate date = createTime.toLocalDate();
        return "weekly".equals(granularity)
                ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                : date.toString();
    }

    private BigDecimal sum(List<CpsOrderDO> orders,
                           java.util.function.Function<CpsOrderDO, BigDecimal> extractor) {
        return orders.stream().filter(java.util.Objects::nonNull).map(extractor).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private Long trustedMemberId(ToolContext context) {
        if (context == null || context.getContext() == null) return null;
        Object value = context.getContext().get(TOOL_CONTEXT_LOGIN_USER_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    private int normalizeDays(Integer days) {
        return days == null ? 30 : Math.max(7, Math.min(days, 180));
    }

    private int normalizeMaxOrders(Integer maxOrders) {
        return maxOrders == null ? 300 : Math.max(1, Math.min(maxOrders, 500));
    }

    private String normalizeGranularity(String granularity, int days) {
        if ("daily".equalsIgnoreCase(granularity) || "weekly".equalsIgnoreCase(granularity)) {
            return granularity.toLowerCase(java.util.Locale.ROOT);
        }
        return days <= 30 ? "daily" : "weekly";
    }

    private Response error(String message) {
        return new Response(null, null, 0, ZERO, ZERO, ZERO, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), message);
    }
}
