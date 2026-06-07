package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionResponse;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionService;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsPurchaseDecisionToolFunctionTest {

    @InjectMocks
    private CpsPurchaseDecisionToolFunction toolFunction;

    @Mock
    private CpsPurchaseDecisionService purchaseDecisionService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    @DisplayName("apply - need 为空时返回错误并写入失败审计")
    void apply_rejectsBlankNeedAndWritesFailureAudit() {
        CpsPurchaseDecisionToolFunction.Request request = new CpsPurchaseDecisionToolFunction.Request();
        request.setNeed(" ");

        CpsPurchaseDecisionResponse response = toolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals("购买需求不能为空", response.getError());
        verify(purchaseDecisionService, never()).decide(any(), any());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_purchase_decision", logCaptor.getValue().getToolName());
        assertEquals(0, logCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("apply - 传递可信 ToolContext 会员身份并写入成功审计")
    void apply_passesTrustedMemberIdAndWritesSuccessAudit() {
        CpsPurchaseDecisionToolFunction.Request request = new CpsPurchaseDecisionToolFunction.Request();
        request.setNeed("给 iPhone 16 买一个防摔手机壳");
        request.setScenario("日常通勤");
        request.setBudgetMin(new BigDecimal("20.00"));
        request.setBudgetMax(new BigDecimal("150.00"));
        request.setDecisionMode("best_value");
        request.setGenerateLink(true);
        when(purchaseDecisionService.decide(any(), eq(100L))).thenReturn(CpsPurchaseDecisionResponse.builder()
                .summary("推荐购买")
                .hainaAvailable(true)
                .bestChoice(CpsPurchaseDecisionResponse.DecisionItem.builder()
                        .goodsId("goods-1")
                        .platformCode("jd")
                        .title("iPhone 16 防摔壳")
                        .actualPrice(new BigDecimal("49.00"))
                        .decisionScore(86)
                        .reasons(List.of("预算匹配"))
                        .build())
                .alternatives(List.of())
                .evidence(CpsPurchaseDecisionResponse.EvidenceVO.empty())
                .risks(List.of("价格和库存以电商平台实时页面为准"))
                .build());

        CpsPurchaseDecisionResponse response = toolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertNull(response.getError());
        assertEquals("goods-1", response.getBestChoice().getGoodsId());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_purchase_decision", logCaptor.getValue().getToolName());
        assertEquals(1, logCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("apply - 服务异常时返回通用错误且不泄漏底层异常")
    void apply_returnsSanitizedErrorWhenServiceThrows() {
        CpsPurchaseDecisionToolFunction.Request request = new CpsPurchaseDecisionToolFunction.Request();
        request.setNeed("给 iPhone 16 买一个防摔手机壳");
        when(purchaseDecisionService.decide(any(), eq(100L)))
                .thenThrow(new IllegalStateException("secret token leaked"));

        CpsPurchaseDecisionResponse response = toolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals("购买决策失败，请稍后重试", response.getError());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("IllegalStateException", logCaptor.getValue().getErrorMessage());
    }
}
