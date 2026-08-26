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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsAnalyzeOrderProfileToolFunctionTest {

    @InjectMocks
    private CpsAnalyzeOrderProfileToolFunction toolFunction;

    @Mock
    private CpsOrderMapper orderMapper;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_aggregatesTrustedMemberOrdersAndExcludesRequestedProducts() {
        when(orderMapper.selectRecentListByMemberId(eq(100L), any(), any(), eq(200))).thenReturn(List.of(
                order("a", "纸巾", "taobao", "9.90", "1.00", null),
                order("a", "纸巾", "taobao", "9.90", "1.00", "0.80"),
                order("b", "牛奶", "jd", "49.00", "3.00", "2.50")));
        CpsAnalyzeOrderProfileToolFunction.Request request = new CpsAnalyzeOrderProfileToolFunction.Request();
        request.setDays(30);
        request.setExcludeItemIds(List.of("b"));

        CpsAnalyzeOrderProfileToolFunction.Response response = toolFunction.apply(request,
                new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertNull(response.getError());
        assertEquals(2, response.getAnalyzedOrders());
        assertEquals(1, response.getExcludedOrders());
        assertEquals(new BigDecimal("19.80"), response.getGmv());
        assertEquals("0-10元", response.getPriceBandBreakdown().get(0).getName());
        assertEquals(2, response.getTopProducts().get(0).getOrderCount());
        assertTrue(response.getDataLimitations().stream().anyMatch(note -> note.contains("不会推断性别")));
    }

    @Test
    void apply_rejectsMissingTrustedMemberContext() {
        CpsAnalyzeOrderProfileToolFunction.Response response = toolFunction.apply(
                new CpsAnalyzeOrderProfileToolFunction.Request(), new ToolContext(Map.of()));

        assertTrue(response.getError().contains("可信会员"));
        verify(orderMapper, never()).selectRecentListByMemberId(any(), any(), any(), any(Integer.class));
    }

    @Test
    void apply_acceptsCommaSeparatedExclusionIdsFromFormCompatibilityField() {
        when(orderMapper.selectRecentListByMemberId(eq(100L), any(), any(), eq(200))).thenReturn(List.of(
                order("a", "纸巾", "taobao", "9.90", "1.00", null),
                order("b", "牛奶", "jd", "49.00", "3.00", "2.50"),
                order("c", "牙刷", "jd", "19.00", "1.00", null)));
        CpsAnalyzeOrderProfileToolFunction.Request request = new CpsAnalyzeOrderProfileToolFunction.Request();
        request.setExcludeItemIdsCsv("a， b\nc");

        CpsAnalyzeOrderProfileToolFunction.Response response = toolFunction.apply(request,
                new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals(0, response.getAnalyzedOrders());
        assertEquals(3, response.getExcludedOrders());
    }

    private CpsOrderDO order(String itemId, String title, String platform, String price,
                             String estimatedRebate, String realRebate) {
        return CpsOrderDO.builder().itemId(itemId).itemTitle(title).platformCode(platform)
                .finalPrice(new BigDecimal(price)).estimateRebate(new BigDecimal(estimatedRebate))
                .realRebate(realRebate == null ? null : new BigDecimal(realRebate)).build();
    }
}
