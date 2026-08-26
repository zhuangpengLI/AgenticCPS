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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * MCP Tool：多来源共振选品。
 *
 * <p>该工具只读取商品搜索与榜单候选，不生成推广链接，也不参与订单归因或返利结算。</p>
 */
@Component("cps_find_resonance_goods")
public class CpsFindResonanceGoodsToolFunction implements
        Function<CpsFindResonanceGoodsToolFunction.Request, CpsFindResonanceGoodsToolFunction.Response> {

    private static final String TOOL_NAME = "cps_find_resonance_goods";
    private static final List<SourcePlan> SOURCE_PLANS = List.of(
            new SourcePlan("comprehensive", "综合搜索", 0, null),
            new SourcePlan("sales", "销量优先", 1, null),
            new SourcePlan("commission", "高佣优先", 4, null),
            new SourcePlan("two_hours", "2 小时热销", 1, "two_hours"),
            new SourcePlan("daily", "全天热销", 1, "daily"));

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsMcpAccessLogMapper accessLogMapper;

    @Data
    @JsonClassDescription("从综合搜索、销量、高佣、2 小时热销和全天热销候选中发现多来源共振商品")
    public static class Request {

        @JsonProperty(required = true, value = "keyword")
        @JsonPropertyDescription("商品关键词或品类，例如：抽纸、儿童牛奶")
        private String keyword;

        @JsonProperty(value = "platform_code")
        @JsonPropertyDescription("平台编码：taobao、jd、pdd、douyin；不传则聚合全平台")
        private String platformCode;

        @JsonProperty(value = "vendor_code")
        @JsonPropertyDescription("指定单平台时可选的 API 供应商编码")
        private String vendorCode;

        @JsonProperty(value = "page_size")
        @JsonPropertyDescription("最终返回数量，默认10，最大20")
        private Integer pageSize;

        @JsonProperty(value = "price_min")
        @JsonPropertyDescription("最低券后价（元）")
        private BigDecimal priceMin;

        @JsonProperty(value = "price_max")
        @JsonPropertyDescription("最高券后价（元）")
        private BigDecimal priceMax;

        @JsonProperty(value = "min_commission_rate")
        @JsonPropertyDescription("最低佣金比例（百分比）")
        private BigDecimal minCommissionRate;

        @JsonProperty(value = "min_month_sales")
        @JsonPropertyDescription("最低月销量")
        private Long minMonthSales;

        @JsonProperty(value = "has_coupon")
        @JsonPropertyDescription("是否只返回有优惠券的商品")
        private Boolean hasCoupon;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        /** 去重并执行本地条件过滤后的候选商品数量。 */
        private Integer candidateCount;

        /** 成功返回（包括空结果）的来源数量。 */
        private Integer successfulSources;

        /** 各候选来源命中数量。 */
        private Map<String, Integer> sourceCounts;

        /** 降级来源及原因。 */
        private Map<String, String> sourceErrors;

        /** 按共振分降序排列的商品。 */
        private List<GoodsVO> goods;

        /** 评分及业务边界说明。 */
        private String selectionNote;

        /** 全部来源失败或参数错误时的信息。 */
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GoodsVO {

        private String goodsId;
        private String platformCode;
        private String title;
        private String mainPic;
        private BigDecimal actualPrice;
        private BigDecimal couponPrice;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private Long monthSales;
        private String shopName;
        private String brandName;
        private String rankTag;
        private String vendorCode;
        private String itemLink;
        private Integer resonanceScore;
        private Integer sourceCount;
        private List<String> sourceHits;
        private Map<String, Integer> scoreBreakdown;
        private List<String> reasons;
        private List<String> riskWarnings;
    }

    @Override
    public Response apply(Request request) {
        long startedAt = System.currentTimeMillis();
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()) {
            Response response = new Response(0, 0, Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyList(), null, "关键词不能为空");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, request, response,
                    new IllegalArgumentException("keyword required"), null, startedAt);
            return response;
        }

        Map<String, Candidate> candidates = new LinkedHashMap<>();
        Map<String, Integer> sourceCounts = new LinkedHashMap<>();
        Map<String, String> sourceErrors = new LinkedHashMap<>();
        int successfulSources = 0;
        for (SourcePlan sourcePlan : SOURCE_PLANS) {
            try {
                List<CpsGoodsItem> items = search(request, sourcePlan);
                List<CpsGoodsItem> accepted = items.stream().filter(item -> matches(item, request)).toList();
                sourceCounts.put(sourcePlan.label(), accepted.size());
                successfulSources++;
                for (CpsGoodsItem item : accepted) {
                    String key = candidateKey(item);
                    if (key == null) {
                        continue;
                    }
                    Candidate candidate = candidates.computeIfAbsent(key, ignored -> new Candidate(item));
                    candidate.sourceHits.add(sourcePlan.label());
                    candidate.item = richer(candidate.item, item);
                }
            } catch (RuntimeException exception) {
                sourceErrors.put(sourcePlan.label(), safeFailureMessage(exception));
            }
        }

        if (successfulSources == 0) {
            Response response = new Response(0, 0, sourceCounts, sourceErrors, Collections.emptyList(), null,
                    "候选来源暂时不可用，请稍后重试");
            CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, request, response,
                    new IllegalStateException("all candidate sources failed"), null, startedAt);
            return response;
        }

        List<GoodsVO> goods = candidates.values().stream()
                .map(this::toGoodsVO)
                .sorted(Comparator.comparing(GoodsVO::getResonanceScore).reversed()
                        .thenComparing(GoodsVO::getMonthSales, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GoodsVO::getGoodsId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(normalizePageSize(request.getPageSize()))
                .toList();
        String note = "共振分由候选源命中数、月销量、佣金、优惠券和榜单标签组成；"
                + "结果仅用于选品分析，不生成推广链接，不作为订单归因、返利结算或资产变更依据";
        Response response = new Response(candidates.size(), successfulSources, sourceCounts, sourceErrors,
                goods, note, null);
        CpsMcpToolAuditSupport.record(accessLogMapper, TOOL_NAME, request, response, null, null, startedAt);
        return response;
    }

    private List<CpsGoodsItem> search(Request request, SourcePlan sourcePlan) {
        CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
        searchRequest.setKeyword(request.getKeyword().trim());
        searchRequest.setPageSize(candidatePageSize(request.getPageSize()));
        searchRequest.setPriceLowerLimit(request.getPriceMin());
        searchRequest.setPriceUpperLimit(request.getPriceMax());
        searchRequest.setMinCommissionRate(request.getMinCommissionRate());
        searchRequest.setMinMonthSales(request.getMinMonthSales());
        searchRequest.setHasCoupon(Boolean.TRUE.equals(request.getHasCoupon()) ? 1 : null);
        searchRequest.setSortType(sourcePlan.sortType());
        searchRequest.setGoodsPerformance(sourcePlan.goodsPerformance());
        List<CpsGoodsItem> items;
        if (request.getPlatformCode() == null || request.getPlatformCode().isBlank()) {
            items = goodsService.searchGoodsAllPlatforms(searchRequest);
        } else {
            var result = goodsService.searchGoods(request.getPlatformCode().trim(), searchRequest,
                    blankToNull(request.getVendorCode()));
            items = result == null ? null : result.getList();
        }
        return items == null ? Collections.emptyList() : items;
    }

    private boolean matches(CpsGoodsItem item, Request request) {
        if (item == null) {
            return false;
        }
        if ((request.getPriceMin() != null || request.getPriceMax() != null) && item.getActualPrice() == null) {
            return false;
        }
        if (request.getPriceMin() != null && item.getActualPrice().compareTo(request.getPriceMin()) < 0) {
            return false;
        }
        if (request.getPriceMax() != null && item.getActualPrice().compareTo(request.getPriceMax()) > 0) {
            return false;
        }
        if (request.getMinCommissionRate() != null
                && (item.getCommissionRate() == null
                || item.getCommissionRate().compareTo(request.getMinCommissionRate()) < 0)) {
            return false;
        }
        if (request.getMinMonthSales() != null
                && (item.getMonthSales() == null || item.getMonthSales() < request.getMinMonthSales())) {
            return false;
        }
        return !Boolean.TRUE.equals(request.getHasCoupon())
                || item.getCouponPrice() != null && item.getCouponPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private GoodsVO toGoodsVO(Candidate candidate) {
        CpsGoodsItem item = candidate.item;
        Map<String, Integer> scoreBreakdown = score(item, candidate.sourceHits.size());
        int totalScore = scoreBreakdown.values().stream().mapToInt(Integer::intValue).sum();
        List<String> reasons = new ArrayList<>();
        if (candidate.sourceHits.size() > 1) {
            reasons.add("同时命中 " + candidate.sourceHits.size() + " 个候选源："
                    + String.join("、", candidate.sourceHits));
        } else {
            reasons.add("命中候选源：" + String.join("、", candidate.sourceHits));
        }
        if (item.getMonthSales() != null) {
            reasons.add("月销量 " + item.getMonthSales());
        }
        if (item.getCommissionRate() != null) {
            reasons.add("佣金比例 " + item.getCommissionRate().stripTrailingZeros().toPlainString() + "%");
        }
        if (positive(item.getCouponPrice())) {
            reasons.add("优惠券 " + item.getCouponPrice().stripTrailingZeros().toPlainString() + " 元");
        }
        if (!isBlank(item.getRankTag())) {
            reasons.add("榜单标签：" + item.getRankTag());
        }
        List<String> warnings = new ArrayList<>();
        if (item.getMonthSales() == null) {
            warnings.add("缺少月销量数据");
        }
        if (item.getCommissionRate() == null) {
            warnings.add("缺少佣金比例数据");
        }
        if (candidate.sourceHits.size() == 1) {
            warnings.add("仅命中单一候选源，建议人工复核");
        }
        return new GoodsVO(item.getGoodsId(), item.getPlatformCode(), item.getTitle(), item.getMainPic(),
                item.getActualPrice(), item.getCouponPrice(), item.getCommissionRate(), item.getCommissionAmount(),
                item.getMonthSales(), item.getShopName(), item.getBrandName(), item.getRankTag(),
                item.getVendorCode(), item.getItemLink(), Math.min(totalScore, 100), candidate.sourceHits.size(),
                List.copyOf(candidate.sourceHits), scoreBreakdown, reasons, warnings);
    }

    private Map<String, Integer> score(CpsGoodsItem item, int sourceCount) {
        Map<String, Integer> score = new LinkedHashMap<>();
        // Five independent hits represent the strongest evidence and should be able to
        // reach the report's 100-point ceiling even when individual source metrics differ.
        score.put("sourceResonance", Math.min(sourceCount * 14, 70));
        score.put("sales", salesScore(item.getMonthSales()));
        score.put("commission", commissionScore(item.getCommissionRate()));
        score.put("coupon", positive(item.getCouponPrice()) ? 5 : 0);
        score.put("rankEvidence", isBlank(item.getRankTag()) ? 0 : 5);
        return score;
    }

    private int salesScore(Long sales) {
        if (sales == null || sales <= 0) return 0;
        if (sales >= 100_000) return 15;
        if (sales >= 10_000) return 12;
        if (sales >= 1_000) return 9;
        if (sales >= 100) return 5;
        return 2;
    }

    private int commissionScore(BigDecimal commissionRate) {
        if (commissionRate == null || commissionRate.signum() <= 0) return 0;
        if (commissionRate.compareTo(new BigDecimal("30")) >= 0) return 15;
        if (commissionRate.compareTo(new BigDecimal("20")) >= 0) return 12;
        if (commissionRate.compareTo(new BigDecimal("10")) >= 0) return 8;
        return 4;
    }

    private CpsGoodsItem richer(CpsGoodsItem current, CpsGoodsItem candidate) {
        return completeness(candidate) > completeness(current) ? candidate : current;
    }

    private int completeness(CpsGoodsItem item) {
        int score = 0;
        if (item.getActualPrice() != null) score++;
        if (item.getCouponPrice() != null) score++;
        if (item.getCommissionRate() != null) score++;
        if (item.getCommissionAmount() != null) score++;
        if (item.getMonthSales() != null) score++;
        if (!isBlank(item.getRankTag())) score++;
        if (!isBlank(item.getVendorCode())) score++;
        return score;
    }

    private String candidateKey(CpsGoodsItem item) {
        String platform = blankToNull(item.getPlatformCode());
        String goodsId = blankToNull(item.getGoodsId());
        if (goodsId != null) {
            return normalize(platform) + "|" + goodsId;
        }
        String title = blankToNull(item.getTitle());
        return title == null ? null : normalize(platform) + "|title|" + normalize(title);
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 20));
    }

    private int candidatePageSize(Integer pageSize) {
        return Math.min(50, Math.max(20, normalizePageSize(pageSize) * 3));
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeFailureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return isBlank(message) ? exception.getClass().getSimpleName() : message;
    }

    private record SourcePlan(String code, String label, int sortType, String goodsPerformance) {
    }

    private static final class Candidate {
        private CpsGoodsItem item;
        private final Set<String> sourceHits = new LinkedHashSet<>();

        private Candidate(CpsGoodsItem item) {
            this.item = item;
        }
    }

}
