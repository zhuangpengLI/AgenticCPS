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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * MCP Tool：高佣替代品。
 *
 * <p>以商品关键词或品类为召回入口，寻找价格可接受、销量和佣金更优的替代候选。
 * 该工具仅做营销分析，不生成推广链接，也不参与归因、返利结算或资产变更。</p>
 */
@Component("cps_find_alternatives")
public class CpsFindAlternativesToolFunction implements
        Function<CpsFindAlternativesToolFunction.Request, CpsFindAlternativesToolFunction.Response> {

    private static final String TOOL_NAME = "cps_find_alternatives";

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("为指定商品或品类寻找价格相近、销量可靠且佣金更优的替代商品")
    public static class Request {

        @JsonProperty(required = true, value = "keyword")
        @JsonPropertyDescription("原商品标题、品类或搜索关键词，例如：无线降噪耳机")
        private String keyword;

        @JsonProperty(value = "platform_code")
        @JsonPropertyDescription("平台编码：taobao、jd、pdd、douyin；不传则聚合全平台")
        private String platformCode;

        @JsonProperty(value = "vendor_code")
        @JsonPropertyDescription("指定单平台时可选的 API 供应商编码")
        private String vendorCode;

        @JsonProperty(value = "reference_price")
        @JsonPropertyDescription("原商品券后价（元），用于控制价格相近度，可选")
        private BigDecimal referencePrice;

        @JsonProperty(value = "reference_commission_rate")
        @JsonPropertyDescription("原商品佣金比例（百分比）；传入后只保留佣金比例更高的替代候选")
        private BigDecimal referenceCommissionRate;

        @JsonProperty(value = "reference_commission_amount")
        @JsonPropertyDescription("原商品预估佣金金额（元）；传入后只保留预估佣金金额更高的替代候选")
        private BigDecimal referenceCommissionAmount;

        @JsonProperty(value = "max_price_premium_percent")
        @JsonPropertyDescription("相对原商品允许的最高溢价百分比，默认20；仅在传入 reference_price 时生效")
        private BigDecimal maxPricePremiumPercent;

        @JsonProperty(value = "min_commission_rate")
        @JsonPropertyDescription("最低佣金比例（百分比），可选")
        private BigDecimal minCommissionRate;

        @JsonProperty(value = "min_month_sales")
        @JsonPropertyDescription("最低月销量，可选")
        private Long minMonthSales;

        @JsonProperty(value = "exclude_goods_ids")
        @JsonPropertyDescription("需要排除的原商品或已选商品 ID 列表，可选")
        private List<String> excludeGoodsIds;

        @JsonProperty(value = "has_coupon")
        @JsonPropertyDescription("是否只返回有优惠券的候选")
        private Boolean hasCoupon;

        @JsonProperty(value = "page_size")
        @JsonPropertyDescription("返回数量，默认10，最大20")
        private Integer pageSize;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private String keyword;
        private BigDecimal referencePrice;
        private Integer candidateCount;
        private List<AlternativeGoodsVO> goods;
        private String selectionNote;
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AlternativeGoodsVO {

        private String goodsId;
        private String platformCode;
        private String title;
        private String mainPic;
        private BigDecimal originalPrice;
        private BigDecimal actualPrice;
        private BigDecimal couponPrice;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private Long monthSales;
        private String shopName;
        private String brandName;
        private String vendorCode;
        private String itemLink;
        private Integer alternativeScore;
        private BigDecimal priceDelta;
        private BigDecimal commissionRateDelta;
        private BigDecimal commissionAmountDelta;
        /** 兼容性字段：优先表示佣金比例差，否则表示预估佣金金额差。 */
        private BigDecimal commissionDelta;
        private List<String> reasons;
        private List<String> riskWarnings;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()) {
            Response response = new Response(null, null, 0, Collections.emptyList(), null, "关键词不能为空");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, request, response,
                    new IllegalArgumentException("keyword required"), null, startedAt);
            return response;
        }
        try {
            CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
            searchRequest.setKeyword(request.getKeyword().trim());
            searchRequest.setPageSize(candidatePageSize(request.getPageSize()));
            searchRequest.setPriceLowerLimit(null);
            searchRequest.setPriceUpperLimit(null);
            searchRequest.setMinCommissionRate(request.getMinCommissionRate());
            searchRequest.setMinMonthSales(request.getMinMonthSales());
            searchRequest.setHasCoupon(Boolean.TRUE.equals(request.getHasCoupon()) ? 1 : null);
            searchRequest.setSortType(4); // 高佣优先，之后由本工具重新按替代评分排序

            List<CpsGoodsItem> items;
            if (isBlank(request.getPlatformCode())) {
                items = goodsService.searchGoodsAllPlatforms(searchRequest);
            } else {
                var result = goodsService.searchGoods(request.getPlatformCode().trim(), searchRequest,
                        blankToNull(request.getVendorCode()));
                items = result == null ? null : result.getList();
            }
            Set<String> excluded = normalizeIds(request.getExcludeGoodsIds());
            List<AlternativeGoodsVO> candidates = (items == null ? Collections.<CpsGoodsItem>emptyList() : items)
                    .stream()
                    .filter(item -> item != null && !excluded.contains(normalize(item.getGoodsId())))
                    .filter(item -> matchesPriceWindow(item, request))
                    .filter(item -> matchesCommission(item, request))
                    .filter(item -> exceedsReferenceCommission(item, request))
                    .filter(item -> matchesSales(item, request))
                    .filter(item -> matchesCoupon(item, request))
                    .map(item -> toAlternative(item, request))
                    .sorted(Comparator.comparing(AlternativeGoodsVO::getAlternativeScore,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AlternativeGoodsVO::getCommissionRate,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AlternativeGoodsVO::getMonthSales,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AlternativeGoodsVO::getGoodsId,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                    .limit(normalizePageSize(request.getPageSize()))
                    .toList();
            String note = "替代评分综合佣金、价格相近度、销量和优惠券；"
                    + (hasReferenceCommission(request) ? "已按给定原商品佣金基准剔除不更优候选；" : "未提供原商品佣金基准，结果按候选佣金排序；")
                    + "不生成推广链接，不作为订单归因、返利结算或资产变更依据";
            Response response = new Response(request.getKeyword().trim(), request.getReferencePrice(),
                    candidates.size(), candidates, note, null);
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, request, response, null, null, startedAt);
            return response;
        } catch (RuntimeException exception) {
            Response response = new Response(request.getKeyword().trim(), request.getReferencePrice(),
                    0, Collections.emptyList(), null, "替代品搜索失败，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, request, response, exception, null, startedAt);
            return response;
        }
    }

    private AlternativeGoodsVO toAlternative(CpsGoodsItem item, Request request) {
        BigDecimal referencePrice = request.getReferencePrice();
        BigDecimal priceDelta = null;
        if (referencePrice != null && item.getActualPrice() != null) {
            priceDelta = item.getActualPrice().subtract(referencePrice).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal commissionRateDelta = request.getReferenceCommissionRate() == null || item.getCommissionRate() == null
                ? null : item.getCommissionRate().subtract(request.getReferenceCommissionRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionAmountDelta = request.getReferenceCommissionAmount() == null || item.getCommissionAmount() == null
                ? null : item.getCommissionAmount().subtract(request.getReferenceCommissionAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionDelta = commissionRateDelta != null ? commissionRateDelta : commissionAmountDelta;
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (item.getCommissionRate() != null) {
            score += commissionScore(item.getCommissionRate());
            reasons.add("佣金比例 " + item.getCommissionRate().stripTrailingZeros().toPlainString() + "%");
        }
        if (referencePrice == null || item.getActualPrice() == null) {
            score += 15;
            reasons.add("缺少原商品价格，按佣金与销量排序");
        } else {
            int priceScore = priceScore(referencePrice, item.getActualPrice());
            score += priceScore;
            reasons.add("券后价 " + item.getActualPrice().stripTrailingZeros().toPlainString() + " 元，较原商品"
                    + formatDelta(priceDelta) + " 元");
        }
        if (item.getMonthSales() != null && item.getMonthSales() > 0) {
            score += salesScore(item.getMonthSales());
            reasons.add("月销量 " + item.getMonthSales());
        }
        if (positive(item.getCouponPrice())) {
            score += 10;
            reasons.add("优惠券 " + item.getCouponPrice().stripTrailingZeros().toPlainString() + " 元");
        }
        List<String> warnings = new ArrayList<>();
        if (item.getMonthSales() == null) warnings.add("缺少月销量数据，建议人工核验");
        if (item.getCommissionRate() == null) warnings.add("缺少佣金比例数据，收益需再次确认");
        if (referencePrice != null && item.getActualPrice() == null) warnings.add("缺少券后价，无法确认价格相近度");
        return new AlternativeGoodsVO(item.getGoodsId(), item.getPlatformCode(), item.getTitle(), item.getMainPic(),
                item.getOriginalPrice(), item.getActualPrice(), item.getCouponPrice(), item.getCommissionRate(),
                item.getCommissionAmount(), item.getMonthSales(), item.getShopName(), item.getBrandName(),
                item.getVendorCode(), item.getItemLink(), Math.min(score, 100), priceDelta, commissionRateDelta,
                commissionAmountDelta, commissionDelta,
                List.copyOf(reasons), List.copyOf(warnings));
    }

    private boolean matchesPriceWindow(CpsGoodsItem item, Request request) {
        if (request.getReferencePrice() == null) return true;
        if (item.getActualPrice() == null) return false;
        BigDecimal premium = request.getMaxPricePremiumPercent() == null
                ? new BigDecimal("20") : request.getMaxPricePremiumPercent().max(BigDecimal.ZERO).min(new BigDecimal("100"));
        BigDecimal max = request.getReferencePrice().max(BigDecimal.ZERO)
                .multiply(BigDecimal.ONE.add(premium.movePointLeft(2)));
        return item.getActualPrice().compareTo(max) <= 0;
    }

    private boolean matchesCommission(CpsGoodsItem item, Request request) {
        return request.getMinCommissionRate() == null || item.getCommissionRate() != null
                && item.getCommissionRate().compareTo(request.getMinCommissionRate()) >= 0;
    }

    private boolean exceedsReferenceCommission(CpsGoodsItem item, Request request) {
        if (request.getReferenceCommissionRate() != null
                && (item.getCommissionRate() == null
                || item.getCommissionRate().compareTo(request.getReferenceCommissionRate()) <= 0)) {
            return false;
        }
        return request.getReferenceCommissionAmount() == null
                || item.getCommissionAmount() != null
                && item.getCommissionAmount().compareTo(request.getReferenceCommissionAmount()) > 0;
    }

    private boolean hasReferenceCommission(Request request) {
        return request.getReferenceCommissionRate() != null || request.getReferenceCommissionAmount() != null;
    }

    private boolean matchesSales(CpsGoodsItem item, Request request) {
        return request.getMinMonthSales() == null || item.getMonthSales() != null
                && item.getMonthSales() >= request.getMinMonthSales();
    }

    private boolean matchesCoupon(CpsGoodsItem item, Request request) {
        return !Boolean.TRUE.equals(request.getHasCoupon()) || positive(item.getCouponPrice());
    }

    private int priceScore(BigDecimal referencePrice, BigDecimal actualPrice) {
        if (referencePrice.signum() <= 0) return 15;
        BigDecimal ratio = actualPrice.subtract(referencePrice).abs()
                .divide(referencePrice, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.05")) <= 0) return 25;
        if (ratio.compareTo(new BigDecimal("0.15")) <= 0) return 20;
        if (ratio.compareTo(new BigDecimal("0.30")) <= 0) return 12;
        return 5;
    }

    private int commissionScore(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) return 0;
        if (rate.compareTo(new BigDecimal("30")) >= 0) return 40;
        if (rate.compareTo(new BigDecimal("20")) >= 0) return 34;
        if (rate.compareTo(new BigDecimal("10")) >= 0) return 25;
        return 15;
    }

    private int salesScore(Long sales) {
        if (sales >= 100_000) return 20;
        if (sales >= 10_000) return 17;
        if (sales >= 1_000) return 14;
        if (sales >= 100) return 8;
        return 3;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 20));
    }

    private int candidatePageSize(Integer pageSize) {
        return Math.min(50, Math.max(20, normalizePageSize(pageSize) * 3));
    }

    private Set<String> normalizeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptySet();
        Set<String> normalized = new HashSet<>();
        ids.stream().filter(id -> id != null && !id.isBlank()).map(this::normalize).forEach(normalized::add);
        return normalized;
    }

    private String formatDelta(BigDecimal delta) {
        if (delta == null) return "未知";
        return delta.signum() > 0 ? "+" + delta.stripTrailingZeros().toPlainString()
                : delta.stripTrailingZeros().toPlainString();
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
