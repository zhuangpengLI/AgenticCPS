package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsQueryExchangeStatusToolFunctionTest {

    @InjectMocks
    private CpsQueryExchangeStatusToolFunction toolFunction;
    @Mock
    private CpsRebateTokenExchangeService exchangeService;
    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void ownerCanQueryExchangeOrder() {
        CpsQueryExchangeStatusToolFunction.Request request = request("CPSX001");
        CpsRebateTokenExchangeOrderDO order = CpsRebateTokenExchangeOrderDO.builder()
                .exchangeOrderNo("CPSX001").memberId(100L).build();
        when(exchangeService.getExchangeOrder(100L, "CPSX001")).thenReturn(order);

        CpsRebateTokenExchangeOrderDO result = toolFunction.apply(
                request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals(100L, result.getMemberId());
        verify(exchangeService).getExchangeOrder(100L, "CPSX001");
    }

    @Test
    void anotherMemberCannotQueryExchangeOrder() {
        CpsQueryExchangeStatusToolFunction.Request request = request("CPSX001");
        when(exchangeService.getExchangeOrder(200L, "CPSX001"))
                .thenThrow(new IllegalStateException("exchange order does not belong to member"));

        assertThrows(IllegalStateException.class, () -> toolFunction.apply(
                request, new ToolContext(Map.of("LOGIN_USER_ID", 200L))));
    }

    @Test
    void missingTrustedContextIsRejectedBeforeQuery() {
        CpsQueryExchangeStatusToolFunction.Request request = request("CPSX001");

        assertThrows(IllegalStateException.class, () -> toolFunction.apply(request, new ToolContext(Map.of())));
        verify(exchangeService, never()).getExchangeOrder(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private CpsQueryExchangeStatusToolFunction.Request request(String orderNo) {
        CpsQueryExchangeStatusToolFunction.Request request = new CpsQueryExchangeStatusToolFunction.Request();
        request.setExchangeOrderNo(orderNo);
        return request;
    }
}
