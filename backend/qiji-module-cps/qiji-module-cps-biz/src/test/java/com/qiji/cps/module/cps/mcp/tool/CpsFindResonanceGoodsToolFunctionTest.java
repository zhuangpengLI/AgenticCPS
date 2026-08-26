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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsFindResonanceGoodsToolFunctionTest {

    @InjectMocks
    private CpsFindResonanceGoodsToolFunction toolFunction;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_mergesFiveCandidateSourcesAndRanksMultiSourceGoodsFirst() {
        CpsGoodsItem resonance = item("shared", 50_000L, "25", "10", "两小时榜第3");
        when(goodsService.searchGoodsAllPlatforms(any()))
                .thenReturn(List.of(resonance, item("general", 500L, "8", "0", null)))
                .thenReturn(List.of(resonance, item("sales", 100_000L, "5", "0", null)))
                .thenReturn(List.of(resonance, item("commission", 100L, "40", "0", null)))
                .thenReturn(List.of(resonance))
                .thenReturn(List.of(resonance));
        CpsFindResonanceGoodsToolFunction.Request request = request("纸巾");

        CpsFindResonanceGoodsToolFunction.Response response = toolFunction.apply(request);

        assertNull(response.getError());
        assertEquals(4, response.getCandidateCount());
        assertEquals(5, response.getSuccessfulSources());
        assertEquals("shared", response.getGoods().get(0).getGoodsId());
        assertEquals(5, response.getGoods().get(0).getSourceCount());
        assertEquals(100, response.getGoods().get(0).getResonanceScore());
        assertTrue(response.getGoods().get(0).getReasons().stream()
                .anyMatch(reason -> reason.contains("同时命中 5 个候选源")));

        ArgumentCaptor<com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest> captor =
                ArgumentCaptor.forClass(com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest.class);
        verify(goodsService, times(5)).searchGoodsAllPlatforms(captor.capture());
        assertEquals(List.of(0, 1, 4, 1, 1),
                captor.getAllValues().stream().map(
                        com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest::getSortType).toList());
        assertEquals(Arrays.asList(null, null, null, "two_hours", "daily"),
                captor.getAllValues().stream().map(
                        com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest::getGoodsPerformance).toList());
    }

    @Test
    void apply_enforcesFiltersLocallyAndReportsPartialSourceFailure() {
        CpsGoodsItem accepted = item("accepted", 2_000L, "20", "5", null);
        CpsGoodsItem lowCommission = item("low-commission", 10_000L, "5", "5", null);
        when(goodsService.searchGoodsAllPlatforms(any()))
                .thenReturn(List.of(accepted, lowCommission))
                .thenThrow(new IllegalStateException("sales source unavailable"))
                .thenReturn(List.of(accepted))
                .thenReturn(List.of())
                .thenReturn(List.of(accepted));
        CpsFindResonanceGoodsToolFunction.Request request = request("纸巾");
        request.setMinCommissionRate(new BigDecimal("15"));
        request.setMinMonthSales(1_000L);
        request.setHasCoupon(true);

        CpsFindResonanceGoodsToolFunction.Response response = toolFunction.apply(request);

        assertNull(response.getError());
        assertEquals(1, response.getCandidateCount());
        assertEquals(4, response.getSuccessfulSources());
        assertEquals(1, response.getSourceErrors().size());
        assertEquals("accepted", response.getGoods().get(0).getGoodsId());
        assertTrue(response.getSelectionNote().contains("仅用于选品分析"));
    }

    @Test
    void apply_rejectsBlankKeywordWithoutSearching() {
        CpsFindResonanceGoodsToolFunction.Request request = request(" ");

        CpsFindResonanceGoodsToolFunction.Response response = toolFunction.apply(request);

        assertEquals("关键词不能为空", response.getError());
        assertTrue(response.getGoods().isEmpty());
        verify(goodsService, times(0)).searchGoodsAllPlatforms(any());
    }

    @Test
    void apply_excludesCandidatesWithoutActualPriceWhenPriceConstraintIsSpecified() {
        CpsGoodsItem missingPrice = item("missing-price", 10_000L, "25", "5", null);
        missingPrice.setActualPrice(null);
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(missingPrice));
        CpsFindResonanceGoodsToolFunction.Request request = request("纸巾");
        request.setPriceMax(new BigDecimal("50"));

        CpsFindResonanceGoodsToolFunction.Response response = toolFunction.apply(request);

        assertNull(response.getError());
        assertEquals(0, response.getCandidateCount());
        assertTrue(response.getGoods().isEmpty());
    }

    private CpsFindResonanceGoodsToolFunction.Request request(String keyword) {
        CpsFindResonanceGoodsToolFunction.Request request = new CpsFindResonanceGoodsToolFunction.Request();
        request.setKeyword(keyword);
        request.setPageSize(10);
        return request;
    }

    private CpsGoodsItem item(String goodsId, Long sales, String commissionRate, String coupon, String rankTag) {
        return CpsGoodsItem.builder()
                .goodsId(goodsId)
                .platformCode("taobao")
                .vendorCode("dataoke")
                .title(goodsId)
                .actualPrice(new BigDecimal("39.90"))
                .commissionRate(new BigDecimal(commissionRate))
                .commissionAmount(new BigDecimal("5.00"))
                .couponPrice(new BigDecimal(coupon))
                .monthSales(sales)
                .rankTag(rankTag)
                .build();
    }

}
