package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsAnalyzeGoodsDetailToolFunctionTest {

    @InjectMocks
    private CpsAnalyzeGoodsDetailToolFunction toolFunction;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void applyBuildsPriceCommissionCouponSalesAndPlatformInsights() {
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(
                item("a", "taobao", "20", "10", 1000L, "5"),
                item("b", "jd", "40", "20", 3000L, "0"),
                item("c", "taobao", "60", "30", 2000L, "8")));
        CpsAnalyzeGoodsDetailToolFunction.Request request = new CpsAnalyzeGoodsDetailToolFunction.Request();
        request.setKeyword("耳机");
        request.setSampleSize(20);

        CpsAnalyzeGoodsDetailToolFunction.Response response = toolFunction.apply(request);

        assertNull(response.getError());
        assertEquals(3, response.getEligibleCount());
        assertEquals(new BigDecimal("20.00"), response.getPrice().getMin());
        assertEquals(new BigDecimal("40.00"), response.getPrice().getMedian());
        assertEquals(new BigDecimal("66.67"), response.getCoupon().getCoveragePercent());
        assertEquals(2, response.getPlatformBreakdown().size());
        assertTrue(response.getInsights().stream().anyMatch(value -> value.contains("覆盖多个平台")));
        assertEquals("c", response.getTopGoods().get(0).getGoodsId());
    }

    @Test
    void applyFailsClosedForPriceFilterAndRejectsBlankKeyword() {
        CpsGoodsItem accepted = item("accepted", "taobao", "30", "20", 100L, "2");
        CpsGoodsItem missingPrice = item("missing", "taobao", "40", "20", 200L, "2");
        missingPrice.setActualPrice(null);
        when(goodsService.searchGoodsAllPlatforms(any())).thenReturn(List.of(accepted, missingPrice));

        CpsAnalyzeGoodsDetailToolFunction.Request request = new CpsAnalyzeGoodsDetailToolFunction.Request();
        request.setKeyword("纸巾");
        request.setPriceMax(new BigDecimal("35"));
        CpsAnalyzeGoodsDetailToolFunction.Response response = toolFunction.apply(request);
        assertEquals(1, response.getEligibleCount());
        assertEquals("accepted", response.getTopGoods().get(0).getGoodsId());

        clearInvocations(goodsService);
        CpsAnalyzeGoodsDetailToolFunction.Request blank = new CpsAnalyzeGoodsDetailToolFunction.Request();
        blank.setKeyword(" ");
        CpsAnalyzeGoodsDetailToolFunction.Response rejected = toolFunction.apply(blank);
        assertEquals("关键词不能为空", rejected.getError());
        verify(goodsService, never()).searchGoodsAllPlatforms(any());
    }

    private CpsGoodsItem item(String id, String platform, String price, String rate, long sales, String coupon) {
        return CpsGoodsItem.builder().goodsId(id).title(id).platformCode(platform).vendorCode("dataoke")
                .actualPrice(new BigDecimal(price)).commissionRate(new BigDecimal(rate))
                .commissionAmount(new BigDecimal("8")).monthSales(sales).couponPrice(new BigDecimal(coupon)).build();
    }
}
