package com.qiji.cps.module.cps.service.decision;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.haina.HainaDecisionClient;
import com.qiji.cps.module.cps.client.haina.dto.HainaDecisionEvidence;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsPurchaseDecisionServiceImplTest {

    @InjectMocks
    private CpsPurchaseDecisionServiceImpl purchaseDecisionService;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private HainaDecisionClient hainaDecisionClient;

    @Test
    @DisplayName("decide - 海纳可用时整合海纳证据与 CPS 候选并按综合价值排序")
    void decide_combinesHainaEvidenceAndRanksBestValue() {
        CpsPurchaseDecisionRequest request = buildRequest("best_value", false);
        when(goodsService.searchGoodsAllPlatforms(any(CpsGoodsSearchRequest.class))).thenReturn(List.of(
                buildGoods("expensive", "jd", "iPhone 16 防摔壳 京东自营", "88.00", "12.00", 1),
                buildGoods("best", "taobao", "iPhone 16 磁吸防摔手机壳", "59.00", "8.00", 0)));
        when(hainaDecisionClient.collectEvidence(any())).thenReturn(HainaDecisionEvidence.builder()
                .available(true)
                .graphEvidence(List.of(HainaDecisionEvidence.GraphEvidence.builder()
                        .entityName("iPhone 16 手机壳")
                        .summary("防摔和磁吸是高频购买关注点")
                        .pros(List.of("防摔"))
                        .cons(List.of("厚重"))
                        .build()))
                .discounts(List.of(HainaDecisionEvidence.DiscountEvidence.builder()
                        .title("近期 iPhone 16 手机壳好价")
                        .mallName("天猫精选")
                        .price(new BigDecimal("59.00"))
                        .content("近期好价证据")
                        .build()))
                .products(List.of(HainaDecisionEvidence.ProductEvidence.builder()
                        .productName("iPhone 16 磁吸防摔手机壳")
                        .mallName("淘宝")
                        .price(new BigDecimal("59.00"))
                        .build()))
                .build());

        CpsPurchaseDecisionResponse response = purchaseDecisionService.decide(request, null);

        assertNull(response.getError());
        assertTrue(response.getHainaAvailable());
        assertEquals("best", response.getBestChoice().getGoodsId());
        assertEquals(1, response.getAlternatives().size());
        assertEquals(1, response.getEvidence().getHainaGraph().size());
        assertEquals(1, response.getEvidence().getHainaDiscounts().size());
        assertTrue(response.getBestChoice().getReasons().contains("预算匹配"));
        verify(goodsService, never()).generatePromotionLink(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("decide - 海纳不可用时降级为 CPS-only 且不泄漏底层异常")
    void decide_degradesToCpsOnlyWhenHainaFails() {
        CpsPurchaseDecisionRequest request = buildRequest("best_value", false);
        when(goodsService.searchGoodsAllPlatforms(any(CpsGoodsSearchRequest.class))).thenReturn(List.of(
                buildGoods("goods-1", "jd", "iPhone 16 防摔壳", "49.00", "5.00", 1)));
        when(hainaDecisionClient.collectEvidence(any())).thenThrow(new IllegalStateException("secret token leaked"));

        CpsPurchaseDecisionResponse response = purchaseDecisionService.decide(request, null);

        assertNull(response.getError());
        assertFalse(response.getHainaAvailable());
        assertEquals("海纳 MCP 暂不可用，已使用 CPS 自有商品数据生成建议", response.getHainaUnavailableReason());
        assertEquals("goods-1", response.getBestChoice().getGoodsId());
    }

    @Test
    @DisplayName("decide - 低价模式按券后价排序且应用预算过滤")
    void decide_lowPriceModeFiltersBudgetAndRanksByActualPrice() {
        CpsPurchaseDecisionRequest request = buildRequest("low_price", false);
        request.setBudgetMin(new BigDecimal("30.00"));
        request.setBudgetMax(new BigDecimal("80.00"));
        when(goodsService.searchGoodsAllPlatforms(any(CpsGoodsSearchRequest.class))).thenReturn(List.of(
                buildGoods("too-cheap", "pdd", "iPhone 16 手机壳", "19.90", "1.00", 0),
                buildGoods("mid", "jd", "iPhone 16 手机壳", "39.90", "2.00", 1),
                buildGoods("high", "taobao", "iPhone 16 手机壳", "79.90", "10.00", 0)));
        when(hainaDecisionClient.collectEvidence(any())).thenReturn(HainaDecisionEvidence.unavailable("disabled"));

        CpsPurchaseDecisionResponse response = purchaseDecisionService.decide(request, null);

        assertEquals("mid", response.getBestChoice().getGoodsId());
        assertEquals(1, response.getAlternatives().size());
        assertEquals("high", response.getAlternatives().get(0).getGoodsId());
    }

    @Test
    @DisplayName("decide - 显式转链但没有可信会员身份时不调用转链")
    void decide_doesNotGenerateLinkWithoutTrustedMember() {
        CpsPurchaseDecisionRequest request = buildRequest("best_value", true);
        when(goodsService.searchGoodsAllPlatforms(any(CpsGoodsSearchRequest.class))).thenReturn(List.of(
                buildGoods("goods-1", "jd", "iPhone 16 防摔壳", "49.00", "5.00", 1)));
        when(hainaDecisionClient.collectEvidence(any())).thenReturn(HainaDecisionEvidence.unavailable("disabled"));

        CpsPurchaseDecisionResponse response = purchaseDecisionService.decide(request, null);

        assertNull(response.getBestChoice().getPromotionUrl());
        assertEquals("缺少可信会员身份，未生成推广链接", response.getBestChoice().getLinkError());
        verify(goodsService, never()).generatePromotionLink(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("decide - 显式转链且有可信会员身份时为推荐商品生成推广链接")
    void decide_generatesLinkWhenExplicitAndTrustedMemberExists() {
        CpsPurchaseDecisionRequest request = buildRequest("best_value", true);
        when(goodsService.searchGoodsAllPlatforms(any(CpsGoodsSearchRequest.class))).thenReturn(List.of(
                buildGoods("goods-1", "jd", "iPhone 16 防摔壳", "49.00", "5.00", 1)));
        when(hainaDecisionClient.collectEvidence(any())).thenReturn(HainaDecisionEvidence.unavailable("disabled"));
        when(goodsService.generatePromotionLink(eq("jd"), eq("goods-1"), eq("sign-goods-1"), eq(100L), any(), eq("haodanku")))
                .thenReturn(CpsPromotionLinkResult.builder().shortUrl("https://cps.example/s").build());

        CpsPurchaseDecisionResponse response = purchaseDecisionService.decide(request, 100L);

        assertEquals("https://cps.example/s", response.getBestChoice().getPromotionUrl());
        ArgumentCaptor<CpsGoodsSearchRequest> searchCaptor = ArgumentCaptor.forClass(CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoodsAllPlatforms(searchCaptor.capture());
        assertEquals("给 iPhone 16 买一个防摔手机壳 日常通勤", searchCaptor.getValue().getKeyword());
    }

    private CpsPurchaseDecisionRequest buildRequest(String decisionMode, boolean generateLink) {
        CpsPurchaseDecisionRequest request = new CpsPurchaseDecisionRequest();
        request.setNeed("给 iPhone 16 买一个防摔手机壳");
        request.setScenario("日常通勤");
        request.setBudgetMin(new BigDecimal("20.00"));
        request.setBudgetMax(new BigDecimal("150.00"));
        request.setDecisionMode(decisionMode);
        request.setGenerateLink(generateLink);
        return request;
    }

    private CpsGoodsItem buildGoods(String goodsId, String platformCode, String title,
                                    String actualPrice, String commissionAmount, Integer shopType) {
        return CpsGoodsItem.builder()
                .goodsId(goodsId)
                .goodsSign("sign-" + goodsId)
                .platformCode(platformCode)
                .vendorCode("haodanku")
                .title(title)
                .actualPrice(new BigDecimal(actualPrice))
                .commissionAmount(new BigDecimal(commissionAmount))
                .commissionRate(new BigDecimal("10.00"))
                .shopName(shopType != null && shopType == 1 ? "京东自营旗舰店" : "品牌旗舰店")
                .shopType(shopType)
                .monthSales(1000L)
                .build();
    }
}
