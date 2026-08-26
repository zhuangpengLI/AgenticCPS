package com.qiji.cps.module.cps.mcp.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * MCP Tool：商品深度分析。
 *
 * <p>对当前平台搜索快照做价格、优惠、佣金、销量和平台覆盖分析；不声称提供历史价格、库存或转化事实。</p>
 */
@Component("cps_analyze_goods_detail")
public class CpsAnalyzeGoodsDetailToolFunction implements
        Function<CpsAnalyzeGoodsDetailToolFunction.Request, CpsAnalyzeGoodsDetailToolFunction.Response> {

    private static final String TOOL_NAME = "cps_analyze_goods_detail";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("对商品搜索候选做价格、优惠券、佣金、销量和平台覆盖的深度分析，仅返回运营分析快照")
    public static class Request {

        @JsonProperty(required = true, value = "keyword")
        @JsonPropertyDescription("商品关键词、品牌或型号，例如：无线降噪耳机")
        private String keyword;

        @JsonProperty(value = "platform_code")
        @JsonPropertyDescription("指定平台编码，不传则聚合全部可用平台")
        private String platformCode;

        @JsonProperty(value = "vendor_code")
        @JsonPropertyDescription("API 供应商编码；指定单平台时可选")
        private String vendorCode;

        @JsonProperty(value = "price_min")
        @JsonPropertyDescription("最低券后价（元），启用后缺失价格的商品会被排除")
        private BigDecimal priceMin;

        @JsonProperty(value = "price_max")
        @JsonPropertyDescription("最高券后价（元），启用后缺失价格的商品会被排除")
        private BigDecimal priceMax;

        @JsonProperty(value = "min_month_sales")
        @JsonPropertyDescription("最低近30天销量")
        private Long minMonthSales;

        @JsonProperty(value = "sample_size")
        @JsonPropertyDescription("分析候选数量，默认20，最大50")
        private Integer sampleSize;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private String keyword;
        private String platformCode;
        private Integer sampledCount;
        private Integer eligibleCount;
        private PriceSummary price;
        private CommissionSummary commission;
        private CouponSummary coupon;
        private SalesSummary sales;
        private Map<String, Integer> platformBreakdown;
        private List<GoodsInsight> topGoods;
        private List<String> insights;
        private List<String> dataLimitations;
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PriceSummary {
        private BigDecimal min;
        private BigDecimal median;
        private BigDecimal max;
        private BigDecimal p25;
        private BigDecimal p75;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommissionSummary {
        private BigDecimal averageRate;
        private BigDecimal minRate;
        private BigDecimal maxRate;
        private BigDecimal averageAmount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CouponSummary {
        private Integer availableCount;
        private Integer coveredCount;
        private BigDecimal coveragePercent;
        private BigDecimal averageAmount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SalesSummary {
        private Long maxMonthSales;
        private BigDecimal averageMonthSales;
        private BigDecimal totalMonthSales;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GoodsInsight {
        private String goodsId;
        private String platformCode;
        private String vendorCode;
        private String title;
        private String mainPic;
        private BigDecimal actualPrice;
        private BigDecimal couponPrice;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private Long monthSales;
        private BigDecimal analysisScore;
        private List<String> reasons;
        private List<String> risks;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        Request safeRequest = request == null ? new Request() : request;
        if (safeRequest.getKeyword() == null || safeRequest.getKeyword().isBlank()) {
            Response response = error(null, "关键词不能为空");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, safeRequest, response,
                    new IllegalArgumentException("keyword required"), null, startedAt);
            return response;
        }
        try {
            int sampleSize = normalizeSampleSize(safeRequest.getSampleSize());
            CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
            searchRequest.setKeyword(safeRequest.getKeyword().trim());
            searchRequest.setPageSize(sampleSize);
            searchRequest.setSortType(0);
            List<CpsGoodsItem> candidates;
            if (safeRequest.getPlatformCode() == null || safeRequest.getPlatformCode().isBlank()) {
                candidates = goodsService.searchGoodsAllPlatforms(searchRequest);
            } else {
                var result = goodsService.searchGoods(safeRequest.getPlatformCode(), searchRequest,
                        safeRequest.getVendorCode());
                candidates = result == null ? List.of() : result.getList();
            }
            List<CpsGoodsItem> filtered = filter(candidates, safeRequest);
            Response response = analyze(safeRequest, filtered, candidates == null ? 0 : candidates.size(), sampleSize);
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, safeRequest, response, null, null, startedAt);
            return response;
        } catch (Exception exception) {
            Response response = error(safeRequest.getKeyword(), "商品深度分析失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, safeRequest, response, exception, null, startedAt);
            return response;
        }
    }

    private Response analyze(Request request, List<CpsGoodsItem> items, int sampledCount, int sampleSize) {
        List<CpsGoodsItem> safeItems = items == null ? List.of() : items.stream()
                .filter(Objects::nonNull).limit(sampleSize).toList();
        List<BigDecimal> prices = values(safeItems, CpsGoodsItem::getActualPrice);
        List<BigDecimal> rates = values(safeItems, CpsGoodsItem::getCommissionRate);
        List<BigDecimal> amounts = values(safeItems, CpsGoodsItem::getCommissionAmount);
        List<BigDecimal> coupons = values(safeItems, CpsGoodsItem::getCouponPrice);
        List<Long> sales = safeItems.stream().map(CpsGoodsItem::getMonthSales).filter(Objects::nonNull).toList();
        int couponCovered = (int) safeItems.stream().filter(item -> positive(item.getCouponPrice())).count();
        Map<String, Integer> platformBreakdown = new LinkedHashMap<>();
        safeItems.forEach(item -> platformBreakdown.merge(normalize(item.getPlatformCode(), "unknown"), 1, Integer::sum));
        List<GoodsInsight> topGoods = safeItems.stream().sorted(Comparator.comparing(this::score).reversed())
                .limit(10).map(this::toInsight).toList();
        List<String> insights = buildInsights(safeItems, prices, rates, couponCovered);
        return new Response(request.getKeyword().trim(), normalize(request.getPlatformCode(), "all"), sampledCount,
                safeItems.size(), new PriceSummary(min(prices), percentile(prices, 0.5), max(prices),
                percentile(prices, 0.25), percentile(prices, 0.75)),
                new CommissionSummary(average(rates), min(rates), max(rates), average(amounts)),
                new CouponSummary(safeItems.size(), couponCovered, percent(couponCovered, safeItems.size()), average(coupons)),
                new SalesSummary(sales.stream().max(Long::compareTo).orElse(0L), averageLong(sales),
                        BigDecimal.valueOf(sales.stream().mapToLong(Long::longValue).sum()).setScale(2, RoundingMode.HALF_UP)),
                platformBreakdown, topGoods, insights, List.of(
                "分析基于本次平台搜索返回的商品快照，不包含历史价格、库存、点击率或真实转化率",
                "佣金、优惠券和销量可能受平台同步延迟影响，最终事实以平台订单和结算流水为准",
                "结果只用于运营选品和人工复核，不生成推广链接，也不改变归因或返利资产"), null);
    }

    private List<CpsGoodsItem> filter(List<CpsGoodsItem> candidates, Request request) {
        if (candidates == null) return List.of();
        return candidates.stream().filter(Objects::nonNull).filter(item -> {
            if (request.getPriceMin() != null || request.getPriceMax() != null) {
                if (item.getActualPrice() == null) return false;
                if (request.getPriceMin() != null && item.getActualPrice().compareTo(request.getPriceMin()) < 0) return false;
                if (request.getPriceMax() != null && item.getActualPrice().compareTo(request.getPriceMax()) > 0) return false;
            }
            return request.getMinMonthSales() == null || (item.getMonthSales() != null
                    && item.getMonthSales() >= request.getMinMonthSales());
        }).toList();
    }

    private GoodsInsight toInsight(CpsGoodsItem item) {
        List<String> reasons = new ArrayList<>();
        if (positive(item.getCouponPrice())) reasons.add("有优惠券");
        if (positive(item.getCommissionRate())) reasons.add("佣金率 " + item.getCommissionRate() + "%");
        if (item.getMonthSales() != null && item.getMonthSales() > 0) reasons.add("近30天销量 " + item.getMonthSales());
        List<String> risks = new ArrayList<>();
        if (item.getActualPrice() == null) risks.add("缺少券后价");
        if (item.getMonthSales() == null) risks.add("缺少销量数据");
        if (item.getCouponEndTime() != null) risks.add("优惠券有效期需再次确认");
        return new GoodsInsight(item.getGoodsId(), item.getPlatformCode(), item.getVendorCode(), item.getTitle(),
                item.getMainPic(), item.getActualPrice(), item.getCouponPrice(), item.getCommissionRate(),
                item.getCommissionAmount(), item.getMonthSales(), score(item), reasons, risks);
    }

    private List<String> buildInsights(List<CpsGoodsItem> items, List<BigDecimal> prices,
                                       List<BigDecimal> rates, int couponCovered) {
        if (items.isEmpty()) return List.of("当前条件下没有可用于深度分析的商品快照");
        List<String> result = new ArrayList<>();
        if (!prices.isEmpty()) result.add("主价格带约为 " + percentile(prices, 0.25) + " 至 " + percentile(prices, 0.75) + " 元");
        if (!rates.isEmpty()) result.add("佣金率中位数为 " + percentile(rates, 0.5) + "%");
        result.add("有券商品占比 " + percent(couponCovered, items.size()) + "%");
        if (items.stream().map(CpsGoodsItem::getPlatformCode).filter(Objects::nonNull).distinct().count() > 1) {
            result.add("候选覆盖多个平台，可进一步做跨平台到手价和收益对比");
        }
        return result;
    }

    private BigDecimal score(CpsGoodsItem item) {
        BigDecimal rate = item.getCommissionRate() == null ? ZERO : item.getCommissionRate();
        BigDecimal coupon = item.getCouponPrice() == null ? ZERO : item.getCouponPrice();
        BigDecimal sales = item.getMonthSales() == null ? ZERO : BigDecimal.valueOf(item.getMonthSales());
        BigDecimal salesScore = BigDecimal.valueOf(Math.log10(sales.add(BigDecimal.ONE).doubleValue()));
        return rate.multiply(new BigDecimal("0.5")).add(coupon.multiply(new BigDecimal("0.2")))
                .add(salesScore.multiply(new BigDecimal("0.3")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private <T> List<BigDecimal> values(List<CpsGoodsItem> items, Function<CpsGoodsItem, BigDecimal> getter) {
        return items.stream().map(getter).filter(Objects::nonNull).sorted().toList();
    }

    private BigDecimal percentile(List<BigDecimal> values, double ratio) {
        if (values.isEmpty()) return ZERO;
        int index = (int) Math.round((values.size() - 1) * ratio);
        return values.get(Math.max(0, Math.min(values.size() - 1, index))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? ZERO : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageLong(List<Long> values) {
        return values.isEmpty() ? ZERO : BigDecimal.valueOf(values.stream().mapToLong(Long::longValue).sum())
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal min(List<BigDecimal> values) {
        return values.isEmpty() ? ZERO : values.get(0).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(List<BigDecimal> values) {
        return values.isEmpty() ? ZERO : values.get(values.size() - 1).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal percent(int value, int total) {
        return total <= 0 ? ZERO : BigDecimal.valueOf(value * 100L)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private int normalizeSampleSize(Integer sampleSize) {
        return sampleSize == null ? 20 : Math.max(1, Math.min(sampleSize, 50));
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Response error(String keyword, String message) {
        return new Response(keyword, null, 0, 0, new PriceSummary(ZERO, ZERO, ZERO, ZERO, ZERO),
                new CommissionSummary(ZERO, ZERO, ZERO, ZERO), new CouponSummary(0, 0, ZERO, ZERO),
                new SalesSummary(0L, ZERO, ZERO), Collections.emptyMap(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), message);
    }
}
