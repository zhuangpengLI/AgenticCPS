package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CpsAnalyzeOrderTrendToolFunctionTest {

    @InjectMocks
    private CpsAnalyzeOrderTrendToolFunction toolFunction;
    @Mock
    private CpsOrderMapper orderMapper;
    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_aggregatesTrustedMemberOrdersByDayAndBuildsTrendInsights() {
        LocalDateTime now = LocalDateTime.now();
        when(orderMapper.selectRecentListByMemberId(eq(100L), any(), any(), eq(300))).thenReturn(List.of(
                order(now.minusDays(2), "20", "2", "1"),
                order(now.minusDays(2), "30", "3", "2"),
                order(now.minusDays(1), "80", "8", "6")));
        CpsAnalyzeOrderTrendToolFunction.Request request = new CpsAnalyzeOrderTrendToolFunction.Request();
        request.setDays(30);
        request.setGranularity("daily");

        CpsAnalyzeOrderTrendToolFunction.Response response = toolFunction.apply(request,
                new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertNull(response.getError());
        assertEquals(3, response.getAnalyzedOrders());
        assertEquals(new BigDecimal("130.00"), response.getTotalGmv());
        assertEquals(2, response.getPoints().size());
        assertEquals(new BigDecimal("25.00"), response.getPoints().get(0).getAverageOrderValue());
        assertTrue(response.getInsights().stream().anyMatch(value -> value.contains("最高的周期")));
        assertTrue(response.getDataLimitations().stream().anyMatch(value -> value.contains("最终结算")));
    }

    @Test
    void apply_defaultsLongRangeToWeeklyAndRejectsMissingTrustedContext() {
        CpsAnalyzeOrderTrendToolFunction.Request request = new CpsAnalyzeOrderTrendToolFunction.Request();
        request.setDays(90);
        when(orderMapper.selectRecentListByMemberId(eq(100L), any(), any(), eq(300))).thenReturn(List.of());

        CpsAnalyzeOrderTrendToolFunction.Response response = toolFunction.apply(request,
                new ToolContext(Map.of("LOGIN_USER_ID", 100L)));
        assertEquals("weekly", response.getGranularity());

        CpsAnalyzeOrderTrendToolFunction.Response rejected = toolFunction.apply(request, new ToolContext(Map.of()));
        assertTrue(rejected.getError().contains("可信会员"));
        verify(orderMapper, times(1)).selectRecentListByMemberId(eq(100L), any(), any(), eq(300));
    }

    private CpsOrderDO order(LocalDateTime createTime, String price, String estimated, String real) {
        CpsOrderDO order = CpsOrderDO.builder().finalPrice(new BigDecimal(price))
                .estimateRebate(new BigDecimal(estimated)).realRebate(new BigDecimal(real)).build();
        order.setCreateTime(createTime);
        return order;
    }
}
