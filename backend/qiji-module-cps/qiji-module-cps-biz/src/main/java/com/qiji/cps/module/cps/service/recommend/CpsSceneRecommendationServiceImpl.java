package com.qiji.cps.module.cps.service.recommend;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendReqVO;
import com.qiji.cps.module.cps.controller.openapi.recommend.vo.OpenApiCpsSceneRecommendRespVO;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CpsSceneRecommendationServiceImpl implements CpsSceneRecommendationService {

    private static final String DEFAULT_REBATE_OWNER_TYPE = "ENTERPRISE";

    @Resource
    private CpsGoodsService goodsService;

    @Override
    public OpenApiCpsSceneRecommendRespVO recommendByScene(OpenApiCpsSceneRecommendReqVO request) {
        CpsGoodsSearchRequest searchRequest = new CpsGoodsSearchRequest();
        searchRequest.setKeyword(String.join(" ", request.getKeywords()));
        searchRequest.setPageNo(1);
        searchRequest.setPageSize(10);
        searchRequest.setPriceLowerLimit(request.getBudgetMin());
        searchRequest.setPriceUpperLimit(request.getBudgetMax());
        searchRequest.setSortType(resolveSortType(request.getSortBy()));

        List<CpsGoodsItem> items = searchItems(request, searchRequest);
        items = filterBudget(items, request.getBudgetMin(), request.getBudgetMax());
        sortItems(items, request.getSortBy());

        List<OpenApiCpsSceneRecommendRespVO.RecommendationVO> recommendations = items.stream()
                .limit(10)
                .map(item -> mapRecommendation(request, item))
                .toList();

        return OpenApiCpsSceneRecommendRespVO.builder()
                .sceneCode(request.getSceneCode())
                .rebateOwnerType(StringUtils.hasText(request.getRebateOwnerType())
                        ? request.getRebateOwnerType() : DEFAULT_REBATE_OWNER_TYPE)
                .recommendations(recommendations)
                .build();
    }

    private List<CpsGoodsItem> searchItems(OpenApiCpsSceneRecommendReqVO request, CpsGoodsSearchRequest searchRequest) {
        if (CollectionUtils.isEmpty(request.getPlatforms())) {
            List<CpsGoodsItem> allPlatformItems = goodsService.searchGoodsAllPlatforms(searchRequest);
            return allPlatformItems == null ? new ArrayList<>() : new ArrayList<>(allPlatformItems);
        }
        List<CpsGoodsItem> items = new ArrayList<>();
        for (String platform : request.getPlatforms()) {
            CpsGoodsSearchResult result = goodsService.searchGoods(platform, searchRequest);
            if (result != null && result.getList() != null) {
                items.addAll(result.getList());
            }
        }
        return items;
    }

    private List<CpsGoodsItem> filterBudget(List<CpsGoodsItem> items, BigDecimal budgetMin, BigDecimal budgetMax) {
        return new ArrayList<>(items.stream()
                .filter(item -> item.getActualPrice() != null)
                .filter(item -> budgetMin == null || item.getActualPrice().compareTo(budgetMin) >= 0)
                .filter(item -> budgetMax == null || item.getActualPrice().compareTo(budgetMax) <= 0)
                .toList());
    }

    private void sortItems(List<CpsGoodsItem> items, String sortBy) {
        Comparator<CpsGoodsItem> comparator = Comparator.comparing(
                CpsGoodsItem::getActualPrice, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("high_rebate".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(CpsGoodsItem::getCommissionAmount,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("best_value".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(CpsGoodsItem::getCommissionAmount,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(CpsGoodsItem::getActualPrice, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        items.sort(comparator);
    }

    private OpenApiCpsSceneRecommendRespVO.RecommendationVO mapRecommendation(OpenApiCpsSceneRecommendReqVO request,
                                                                              CpsGoodsItem item) {
        CpsPromotionLinkResult link = goodsService.generatePromotionLink(
                item.getPlatformCode(), item.getGoodsId(), item.getGoodsSign(), request.getUserId(), null);
        BigDecimal estimatedRebate = link != null && link.getCommissionAmount() != null
                ? link.getCommissionAmount() : item.getCommissionAmount();
        String promotionUrl = link == null ? null : firstNonBlank(link.getShortUrl(), link.getMobileUrl(), link.getLongUrl());
        return OpenApiCpsSceneRecommendRespVO.RecommendationVO.builder()
                .platform(item.getPlatformCode())
                .goodsId(item.getGoodsId())
                .goodsSign(item.getGoodsSign())
                .title(item.getTitle())
                .mainPic(item.getMainPic())
                .price(item.getActualPrice())
                .commissionRate(item.getCommissionRate())
                .estimatedRebate(estimatedRebate)
                .reason(buildReason(request, item))
                .promotionUrl(promotionUrl)
                .build();
    }

    private Integer resolveSortType(String sortBy) {
        if ("low_price".equalsIgnoreCase(sortBy)) {
            return 2;
        }
        if ("high_rebate".equalsIgnoreCase(sortBy)) {
            return 4;
        }
        return 0;
    }

    private String buildReason(OpenApiCpsSceneRecommendReqVO request, CpsGoodsItem item) {
        String device = StringUtils.hasText(request.getDeviceType()) ? request.getDeviceType() : "current device";
        return "Matches " + device + " scene " + request.getSceneCode()
                + "; keywords cover the purchase need and price/rebate fit enterprise review.";
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
