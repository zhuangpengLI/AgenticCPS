package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsExplainRebateToolFunctionTest {

    @InjectMocks
    private CpsExplainRebateToolFunction toolFunction;

    @Mock
    private CpsGetRebateSummaryToolFunction rebateSummaryToolFunction;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_combinesStaticRulesWithTrustedMemberSummary() {
        CpsExplainRebateToolFunction.Request request = new CpsExplainRebateToolFunction.Request();
        request.setQuestion("how is my rebate calculated?");
        request.setRecentCount(3);
        ToolContext toolContext = new ToolContext(Map.of("LOGIN_USER_ID", 100L));
        when(rebateSummaryToolFunction.apply(any(), same(toolContext))).thenReturn(
                new CpsGetRebateSummaryToolFunction.Response(
                        new BigDecimal("12.50"),
                        new BigDecimal("3.00"),
                        new BigDecimal("50.00"),
                        new BigDecimal("8.00"),
                        "normal",
                        List.of(),
                        null));

        CpsExplainRebateToolFunction.Response response = toolFunction.apply(request, toolContext);

        assertNull(response.getError());
        assertTrue(response.getAnswer().contains("available balance is 12.50"));
        assertEquals(new BigDecimal("12.50"), response.getAccountSummary().getAvailableBalance());
        assertTrue(response.getCalculationPriority().get(0).contains("member personal"));
        assertTrue(response.getSettlementSteps().stream().anyMatch(step -> step.contains("freeze")));
        ArgumentCaptor<CpsGetRebateSummaryToolFunction.Request> summaryCaptor =
                ArgumentCaptor.forClass(CpsGetRebateSummaryToolFunction.Request.class);
        verify(rebateSummaryToolFunction).apply(summaryCaptor.capture(), same(toolContext));
        assertEquals(3, summaryCaptor.getValue().getRecentCount());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_explain_rebate", logCaptor.getValue().getToolName());
        assertEquals(1, logCaptor.getValue().getStatus());
    }

    @Test
    void apply_returnsSummaryErrorWhenTrustedMemberContextIsMissing() {
        CpsExplainRebateToolFunction.Request request = new CpsExplainRebateToolFunction.Request();
        request.setQuestion("why is my rebate not available?");
        when(rebateSummaryToolFunction.apply(any(), any())).thenReturn(
                new CpsGetRebateSummaryToolFunction.Response(
                        null, null, null, null, null, List.of(), "missing login context"));

        CpsExplainRebateToolFunction.Response response = toolFunction.apply(request, new ToolContext(Map.of()));

        assertEquals("missing login context", response.getError());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals(0, logCaptor.getValue().getStatus());
    }
}
