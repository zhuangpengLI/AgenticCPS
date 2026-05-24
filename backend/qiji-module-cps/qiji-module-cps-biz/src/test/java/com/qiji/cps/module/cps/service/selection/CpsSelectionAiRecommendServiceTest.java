package com.qiji.cps.module.cps.service.selection;

import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsSelectionAiRecommendServiceTest {

    private final CpsSelectionAiRecommendService service = new CpsSelectionAiRecommendService();

    @Test
    @DisplayName("recommend - 模型不可用时仍按可解释规则评分排序")
    void recommend_fallsBackToRuleScoreRanking() {
        CpsSelectionThemeDO theme = CpsSelectionThemeDO.builder()
                .themeName("618抢先购")
                .promotionEvent("618")
                .ruleJson("{\"activityTags\":[\"618\"],\"platformWeights\":{\"taobao\":2,\"jd\":1}}")
                .build();
        CpsGoodsSquareGoodsRespVO highRebate = goods("taobao", "goods-high", "618爆款", "99.00", "20", "18.00", 2000L);
        CpsGoodsSquareGoodsRespVO highSales = goods("jd", "goods-sales", "普通商品", "59.00", "5", "2.00", 80000L);

        List<CpsSelectionAiRecommendService.RecommendedGoods> result =
                service.recommend(theme, List.of(highSales, highRebate), 10);

        assertEquals("goods-high", result.get(0).getGoods().getGoodsId());
        assertTrue(result.get(0).getRecommendScore().compareTo(result.get(1).getRecommendScore()) > 0);
        assertTrue(result.get(0).getRecommendReason().contains("佣金"));
    }

    @Test
    @DisplayName("recommend - LLM 文案建议不得覆盖第三方事实字段")
    void recommend_keepsGoodsFactFieldsImmutable() {
        CpsSelectionThemeDO theme = CpsSelectionThemeDO.builder().themeName("开学季").build();
        CpsGoodsSquareGoodsRespVO origin = goods("jd", "goods-1", "书包", "79.00", "10", "7.90", 1000L);

        List<CpsSelectionAiRecommendService.RecommendedGoods> result = service.recommend(theme, List.of(origin), 5);

        CpsGoodsSquareGoodsRespVO after = result.get(0).getGoods();
        assertEquals("goods-1", after.getGoodsId());
        assertEquals(new BigDecimal("79.00"), after.getActualPrice());
        assertEquals(new BigDecimal("7.90"), after.getCommissionAmount());
    }

    private CpsGoodsSquareGoodsRespVO goods(String platformCode, String goodsId, String title, String price,
                                           String rate, String commission, Long sales) {
        CpsGoodsSquareGoodsRespVO item = new CpsGoodsSquareGoodsRespVO();
        item.setPlatformCode(platformCode);
        item.setVendorCode("haodanku");
        item.setGoodsId(goodsId);
        item.setTitle(title);
        item.setActualPrice(new BigDecimal(price));
        item.setCouponPrice(new BigDecimal("10"));
        item.setCommissionRate(new BigDecimal(rate));
        item.setCommissionAmount(new BigDecimal(commission));
        item.setMonthSales(sales);
        item.setActivityTag(title.contains("618") ? "618抢先购" : null);
        return item;
    }
}
