package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsFindAlternativesToolFunctionTest {

    @InjectMocks
    private CpsFindAlternativesToolFunction toolFunction;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_ranksHighCommissionPriceComparableCandidatesAndExcludesOriginalGoods() {
        CpsGoodsItem original = item("original", "45", "50", 80_000L, "10");
        CpsGoodsItem best = item("best", "35", "53", 15_000L, "8");
        CpsGoodsItem tooExpensive = item("too-expensive", "50", "80", 100_000L, "10");
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(original, best, tooExpensive));
        CpsFindAlternativesToolFunction.Request request = request("蓝牙耳机");
        request.setReferencePrice(new BigDecimal("50"));
        request.setReferenceCommissionRate(new BigDecimal("20"));
        request.setReferenceCommissionAmount(new BigDecimal("5"));
        request.setMaxPricePremiumPercent(new BigDecimal("10"));
        request.setExcludeGoodsIds(List.of("original"));

        CpsFindAlternativesToolFunction.Response response = toolFunction.apply(request);

        assertNull(response.getError());
        assertEquals(1, response.getCandidateCount());
        assertEquals("best", response.getGoods().get(0).getGoodsId());
        assertEquals(new BigDecimal("3.00"), response.getGoods().get(0).getPriceDelta());
        assertEquals(new BigDecimal("15.00"), response.getGoods().get(0).getCommissionRateDelta());
        assertEquals(new BigDecimal("3.00"), response.getGoods().get(0).getCommissionAmountDelta());
        assertTrue(response.getSelectionNote().contains("不生成推广链接"));
        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService).searchGoodsAllPlatforms(captor.capture());
        assertEquals(4, captor.getValue().getSortType());
    }

    @Test
    void apply_enforcesCommissionSalesAndCouponFiltersLocally() {
        CpsGoodsItem accepted = item("accepted", "25", "40", 2_000L, "5");
        CpsGoodsItem lowCommission = item("low-commission", "10", "40", 9_000L, "5");
        CpsGoodsItem noCoupon = item("no-coupon", "30", "40", 9_000L, "0");
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(accepted, lowCommission, noCoupon));
        CpsFindAlternativesToolFunction.Request request = request("纸巾");
        request.setMinCommissionRate(new BigDecimal("20"));
        request.setMinMonthSales(1_000L);
        request.setHasCoupon(true);

        CpsFindAlternativesToolFunction.Response response = toolFunction.apply(request);

        assertEquals(1, response.getCandidateCount());
        assertEquals("accepted", response.getGoods().get(0).getGoodsId());
    }

    @Test
    void apply_requiresPriceAndHigherCommissionWhenReferenceBaselinesAreProvided() {
        CpsGoodsItem accepted = item("accepted", "25", "40", 2_000L, "5");
        CpsGoodsItem lowerCommission = item("lower", "15", "40", 2_000L, "5");
        CpsGoodsItem missingPrice = item("missing-price", "30", "40", 2_000L, "5");
        missingPrice.setActualPrice(null);
        when(goodsService.searchGoodsAllPlatforms(any()))
                .thenReturn(List.of(accepted, lowerCommission, missingPrice));
        CpsFindAlternativesToolFunction.Request request = request("纸巾");
        request.setReferencePrice(new BigDecimal("45"));
        request.setReferenceCommissionRate(new BigDecimal("20"));

        CpsFindAlternativesToolFunction.Response response = toolFunction.apply(request);

        assertEquals(1, response.getCandidateCount());
        assertEquals("accepted", response.getGoods().get(0).getGoodsId());
        assertEquals(new BigDecimal("5.00"), response.getGoods().get(0).getCommissionRateDelta());
    }

    @Test
    void apply_rejectsBlankKeywordWithoutCallingSearch() {
        CpsFindAlternativesToolFunction.Response response = toolFunction.apply(request(" "));

        assertEquals("关键词不能为空", response.getError());
        assertTrue(response.getGoods().isEmpty());
        verify(goodsService, never()).searchGoodsAllPlatforms(any());
    }

    @Test
    void apply_failsClosedWhenCandidateCommissionIsNotHigherThanReference() {
        CpsGoodsItem sameRate = item("same", "20", "40", 10_000L, "5");
        CpsGoodsItem missingRate = item("missing", "20", "40", 10_000L, "5");
        missingRate.setCommissionRate(null);
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(sameRate, missingRate));
        CpsFindAlternativesToolFunction.Request request = request("纸巾");
        request.setReferenceCommissionRate(new BigDecimal("20"));

        CpsFindAlternativesToolFunction.Response response = toolFunction.apply(request);

        assertTrue(response.getGoods().isEmpty());
        assertTrue(response.getSelectionNote().contains("给定原商品佣金基准"));
    }

    private CpsFindAlternativesToolFunction.Request request(String keyword) {
        CpsFindAlternativesToolFunction.Request request = new CpsFindAlternativesToolFunction.Request();
        request.setKeyword(keyword);
        request.setPageSize(10);
        return request;
    }

    private CpsGoodsItem item(String goodsId, String commissionRate, String price, Long sales, String coupon) {
        return CpsGoodsItem.builder().goodsId(goodsId).title(goodsId).platformCode("taobao")
                .actualPrice(new BigDecimal(price)).commissionRate(new BigDecimal(commissionRate))
                .commissionAmount(new BigDecimal("8.00")).monthSales(sales).couponPrice(new BigDecimal(coupon))
                .vendorCode("dataoke").build();
    }
}
