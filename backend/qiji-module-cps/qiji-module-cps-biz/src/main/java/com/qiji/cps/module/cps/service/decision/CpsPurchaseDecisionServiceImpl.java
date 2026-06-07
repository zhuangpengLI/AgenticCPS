package com.qiji.cps.module.cps.service.decision;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.haina.HainaDecisionClient;
import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionEvidence;
import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionRequest;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CpsPurchaseDecisionServiceImpl implements CpsPurchaseDecisionService {

    private static final String MODE_BEST_VALUE = "best_value";
    private static final String MODE_LOW_PRICE = "low_price";
    private static final String MODE_HIGH_REBATE = "high_rebate";
    private static final String MODE_RELIABLE_SHOP = "reliable_shop";
    private static final String HAINA_FALLBACK_REASON = "海纳 MCP 暂不可用，已使用 CPS 自有商品数据生成建议";

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private HainaDecisionClient hainaDecisionClient;

    @Override
    public CpsPurchaseDecisionResponse decide(CpsPurchaseDecisionRequest request, Long trustedMemberId) {
        if (request == null || !StringUtils.hasText(request.getNeed())) {
            return CpsPurchaseDecisionResponse.error("购买需求不能为空");
        }
        HainaDecisionEvidence hainaEvidence = collectHainaEvidence(request);
        List<CpsGoodsItem> candidates = searchCandidates(request);
        List<CpsGoodsItem> filtered = filterByBudget(candidates, request.getBudgetMin(), request.getBudgetMax());
        if (filtered.isEmpty()) {
            return CpsPurchaseDecisionResponse.builder()
                    .summary("未找到符合预算和需求的 CPS 商品候选")
                    .hainaAvailable(Boolean.TRUE.equals(hainaEvidence.getAvailable()))
                    .hainaUnavailableReason(hainaEvidence.getUnavailableReason())
                    .evidence(mapEvidence(hainaEvidence, candidates))
                    .risks(defaultRisks())
                    .error("未找到符合条件的商品")
                    .build();
        }

        String mode = normalizeMode(request.getDecisionMode());
        List<CpsPurchaseDecisionResponse.DecisionItem> items = filtered.stream()
                .map(item -> mapDecisionItem(item, mode, hainaEvidence))
                .sorted(comparatorFor(mode))
                .limit(10)
                .toList();
        if (Boolean.TRUE.equals(request.getGenerateLink())) {
            generateLinks(items, trustedMemberId);
        }
        CpsPurchaseDecisionResponse.DecisionItem bestChoice = items.get(0);
        List<CpsPurchaseDecisionResponse.DecisionItem> alternatives = items.size() <= 1
                ? List.of() : new ArrayList<>(items.subList(1, items.size()));

        return CpsPurchaseDecisionResponse.builder()
                .summary(buildSummary(bestChoice, mode, hainaEvidence))
                .bestChoice(bestChoice)
                .alternatives(alternatives)
                .evidence(mapEvidence(hainaEvidence, filtered))
                .risks(defaultRisks())
                .hainaAvailable(Boolean.TRUE.equals(hainaEvidence.getAvailable()))
                .hainaUnavailableReason(hainaEvidence.getUnavailableReason())
                .build();
    }

    private HainaDecisionEvidence collectHainaEvidence(CpsPurchaseDecisionRequest request) {
        try {
            HainaDecisionEvidence evidence = hainaDecisionClient.collectEvidence(HainaDecisionRequest.builder()
                    .need(request.getNeed())
                    .scenario(request.getScenario())
                    .budgetMin(request.getBudgetMin())
                    .budgetMax(request.getBudgetMax())
                    .preferredPlatforms(request.getPreferredPlatforms())
                    .maxResults(10)
                    .build());
            return evidence == null ? HainaDecisionEvidence.unavailable(HAINA_FALLBACK_REASON) : evidence;
        } catch (Exception e) {
            return HainaDecisionEvidence.unavailable(HAINA_FALLBACK_REASON);
        }
    }

    private List<CpsGoodsItem> searchCandidates(CpsPurchaseDecisionRequest request) {
        CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
        searchRequest.setKeyword(buildSearchKeyword(request));
        searchRequest.setPageNo(1);
        searchRequest.setPageSize(10);
        searchRequest.setPriceLowerLimit(request.getBudgetMin());
        searchRequest.setPriceUpperLimit(request.getBudgetMax());
        if (CollectionUtils.isEmpty(request.getPreferredPlatforms())) {
            List<CpsGoodsItem> items = goodsService.searchGoodsAllPlatforms(searchRequest);
            return items == null ? List.of() : new ArrayList<>(items);
        }
        List<CpsGoodsItem> items = new ArrayList<>();
        for (String platform : request.getPreferredPlatforms()) {
            if (!StringUtils.hasText(platform)) {
                continue;
            }
            CpsGoodsSearchResult result = goodsService.searchGoods(platform, searchRequest);
            if (result != null && result.getList() != null) {
                items.addAll(result.getList());
            }
        }
        return items;
    }

    private String buildSearchKeyword(CpsPurchaseDecisionRequest request) {
        String keyword = request.getNeed().trim();
        if (StringUtils.hasText(request.getScenario())) {
            keyword = keyword + " " + request.getScenario().trim();
        }
        return keyword;
    }

    private List<CpsGoodsItem> filterByBudget(List<CpsGoodsItem> items, BigDecimal budgetMin, BigDecimal budgetMax) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item.getActualPrice() != null)
                .filter(item -> budgetMin == null || item.getActualPrice().compareTo(budgetMin) >= 0)
                .filter(item -> budgetMax == null || item.getActualPrice().compareTo(budgetMax) <= 0)
                .toList();
    }

    private CpsPurchaseDecisionResponse.DecisionItem mapDecisionItem(CpsGoodsItem item, String mode,
                                                                     HainaDecisionEvidence hainaEvidence) {
        BigDecimal actualPrice = nvl(item.getActualPrice());
        BigDecimal estimatedRebate = nvl(item.getCommissionAmount());
        BigDecimal netPrice = actualPrice.subtract(estimatedRebate);
        List<String> reasons = buildReasons(item, hainaEvidence);
        return CpsPurchaseDecisionResponse.DecisionItem.builder()
                .goodsId(item.getGoodsId())
                .goodsSign(item.getGoodsSign())
                .platformCode(item.getPlatformCode())
                .vendorCode(item.getVendorCode())
                .title(item.getTitle())
                .mainPic(item.getMainPic())
                .actualPrice(item.getActualPrice())
                .estimatedRebate(item.getCommissionAmount())
                .netPrice(netPrice)
                .shopName(item.getShopName())
                .decisionScore(score(item, mode, hainaEvidence))
                .reasons(reasons)
                .build();
    }

    private List<String> buildReasons(CpsGoodsItem item, HainaDecisionEvidence hainaEvidence) {
        List<String> reasons = new ArrayList<>();
        reasons.add("预算匹配");
        if (item.getCommissionAmount() != null && item.getCommissionAmount().compareTo(BigDecimal.ZERO) > 0) {
            reasons.add("返利后净价较优");
        }
        if (isReliableShop(item)) {
            reasons.add("店铺或商城可信度较高");
        }
        if (hainaEvidence != null && !CollectionUtils.isEmpty(hainaEvidence.getDiscounts())) {
            reasons.add("存在近期好价证据");
        }
        if (hainaEvidence != null && !CollectionUtils.isEmpty(hainaEvidence.getGraphEvidence())) {
            reasons.add("海纳商品知识图谱提供规格和口碑参考");
        }
        return reasons;
    }

    private Integer score(CpsGoodsItem item, String mode, HainaDecisionEvidence hainaEvidence) {
        int score = 25;
        score += item.getActualPrice() != null ? 20 : 0;
        score += rebateScore(item.getCommissionAmount());
        score += hainaEvidence != null && !CollectionUtils.isEmpty(hainaEvidence.getDiscounts()) ? 15 : 0;
        score += isReliableShop(item) ? 10 : 4;
        score += hainaEvidence != null && !CollectionUtils.isEmpty(hainaEvidence.getGraphEvidence()) ? 10 : 0;
        if (MODE_LOW_PRICE.equals(mode)) {
            score += inversePriceScore(item.getActualPrice());
        } else if (MODE_HIGH_REBATE.equals(mode)) {
            score += rebateScore(item.getCommissionAmount());
        } else if (MODE_RELIABLE_SHOP.equals(mode)) {
            score += isReliableShop(item) ? 15 : 0;
        } else {
            score += netPriceScore(item);
        }
        return Math.min(score, 100);
    }

    private int inversePriceScore(BigDecimal actualPrice) {
        if (actualPrice == null) {
            return 0;
        }
        if (actualPrice.compareTo(new BigDecimal("50")) <= 0) {
            return 10;
        }
        if (actualPrice.compareTo(new BigDecimal("100")) <= 0) {
            return 6;
        }
        return 2;
    }

    private int rebateScore(BigDecimal commissionAmount) {
        if (commissionAmount == null || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return Math.min(20, commissionAmount.setScale(0, RoundingMode.DOWN).intValue());
    }

    private int netPriceScore(CpsGoodsItem item) {
        BigDecimal netPrice = nvl(item.getActualPrice()).subtract(nvl(item.getCommissionAmount()));
        if (netPrice.compareTo(new BigDecimal("50")) <= 0) {
            return 10;
        }
        if (netPrice.compareTo(new BigDecimal("100")) <= 0) {
            return 6;
        }
        return 2;
    }

    private Comparator<CpsPurchaseDecisionResponse.DecisionItem> comparatorFor(String mode) {
        if (MODE_LOW_PRICE.equals(mode)) {
            return Comparator.comparing(CpsPurchaseDecisionResponse.DecisionItem::getActualPrice,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (MODE_HIGH_REBATE.equals(mode)) {
            return Comparator.comparing(CpsPurchaseDecisionResponse.DecisionItem::getEstimatedRebate,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if (MODE_RELIABLE_SHOP.equals(mode)) {
            return Comparator.comparing(CpsPurchaseDecisionResponse.DecisionItem::getDecisionScore,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(CpsPurchaseDecisionResponse.DecisionItem::getNetPrice,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private void generateLinks(List<CpsPurchaseDecisionResponse.DecisionItem> items, Long trustedMemberId) {
        for (CpsPurchaseDecisionResponse.DecisionItem item : items) {
            if (trustedMemberId == null) {
                item.setLinkError("缺少可信会员身份，未生成推广链接");
                continue;
            }
            try {
                CpsPromotionLinkResult link = goodsService.generatePromotionLink(
                        item.getPlatformCode(), item.getGoodsId(), item.getGoodsSign(),
                        trustedMemberId, null, item.getVendorCode());
                item.setPromotionUrl(firstNonBlank(link == null ? null : link.getShortUrl(),
                        link == null ? null : link.getMobileUrl(),
                        link == null ? null : link.getLongUrl()));
                if (!StringUtils.hasText(item.getPromotionUrl())) {
                    item.setLinkError("转链未返回可用链接");
                }
            } catch (Exception e) {
                item.setLinkError("转链失败，请稍后重试");
            }
        }
    }

    private CpsPurchaseDecisionResponse.EvidenceVO mapEvidence(HainaDecisionEvidence hainaEvidence,
                                                               List<CpsGoodsItem> cpsCandidates) {
        HainaDecisionEvidence evidence = hainaEvidence == null
                ? HainaDecisionEvidence.unavailable(HAINA_FALLBACK_REASON) : hainaEvidence;
        return CpsPurchaseDecisionResponse.EvidenceVO.builder()
                .hainaGraph(evidence.getGraphEvidence().stream()
                        .map(item -> CpsPurchaseDecisionResponse.HainaGraphVO.builder()
                                .entityName(item.getEntityName())
                                .summary(item.getSummary())
                                .pros(item.getPros())
                                .cons(item.getCons())
                                .build())
                        .toList())
                .hainaDiscounts(evidence.getDiscounts().stream()
                        .map(item -> CpsPurchaseDecisionResponse.HainaDiscountVO.builder()
                                .title(item.getTitle())
                                .mallName(item.getMallName())
                                .price(item.getPrice())
                                .content(item.getContent())
                                .url(item.getUrl())
                                .pubdate(item.getPubdate())
                                .build())
                        .toList())
                .hainaProducts(evidence.getProducts().stream()
                        .map(item -> CpsPurchaseDecisionResponse.HainaProductVO.builder()
                                .productName(item.getProductName())
                                .mallName(item.getMallName())
                                .shopName(item.getShopName())
                                .price(item.getPrice())
                                .productUrl(item.getProductUrl())
                                .build())
                        .toList())
                .cpsCandidates(cpsCandidates.stream()
                        .limit(10)
                        .map(item -> CpsPurchaseDecisionResponse.CpsCandidateVO.builder()
                                .goodsId(item.getGoodsId())
                                .platformCode(item.getPlatformCode())
                                .title(item.getTitle())
                                .actualPrice(item.getActualPrice())
                                .estimatedRebate(item.getCommissionAmount())
                                .build())
                        .toList())
                .build();
    }

    private String buildSummary(CpsPurchaseDecisionResponse.DecisionItem bestChoice, String mode,
                                HainaDecisionEvidence hainaEvidence) {
        String modeText = switch (mode) {
            case MODE_LOW_PRICE -> "低价优先";
            case MODE_HIGH_REBATE -> "返利优先";
            case MODE_RELIABLE_SHOP -> "可信店铺优先";
            default -> "综合价值优先";
        };
        String hainaText = Boolean.TRUE.equals(hainaEvidence.getAvailable()) ? "，并结合海纳商品证据" : "";
        return "按" + modeText + "推荐：" + bestChoice.getTitle() + hainaText;
    }

    private List<String> defaultRisks() {
        return List.of("价格、库存、优惠券和活动以电商平台实时页面为准", "海纳证据仅用于购买决策参考，不参与返利结算");
    }

    private String normalizeMode(String decisionMode) {
        if (MODE_LOW_PRICE.equalsIgnoreCase(decisionMode)) {
            return MODE_LOW_PRICE;
        }
        if (MODE_HIGH_REBATE.equalsIgnoreCase(decisionMode)) {
            return MODE_HIGH_REBATE;
        }
        if (MODE_RELIABLE_SHOP.equalsIgnoreCase(decisionMode)) {
            return MODE_RELIABLE_SHOP;
        }
        return MODE_BEST_VALUE;
    }

    private boolean isReliableShop(CpsGoodsItem item) {
        return item.getShopType() != null && item.getShopType() == 1
                || StringUtils.hasText(item.getShopName()) && item.getShopName().contains("自营");
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
