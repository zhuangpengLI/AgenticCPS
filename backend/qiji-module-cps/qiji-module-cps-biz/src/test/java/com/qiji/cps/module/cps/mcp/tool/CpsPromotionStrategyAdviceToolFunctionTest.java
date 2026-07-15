package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionRequest;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionResponse;
import com.qiji.cps.module.cps.service.decision.CpsPurchaseDecisionService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsPromotionStrategyAdviceToolFunctionTest {

    @InjectMocks
    private CpsPromotionStrategyAdviceToolFunction toolFunction;

    @Mock
    private CpsPurchaseDecisionService purchaseDecisionService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    void apply_delegatesToPurchaseDecisionAndBuildsPromotionPlaybook() {
        CpsPromotionStrategyAdviceToolFunction.Request request =
                new CpsPromotionStrategyAdviceToolFunction.Request();
        request.setProductNeed("wireless mouse");
        request.setCampaignGoal("increase conversion");
        request.setTargetAudience("office users");
        request.setContentChannel("wechat");
        request.setBudgetMin(new BigDecimal("50.00"));
        request.setBudgetMax(new BigDecimal("200.00"));
        request.setPreferredPlatforms(List.of("jd", "taobao"));
        request.setGenerateLink(true);
        when(purchaseDecisionService.decide(any(), eq(100L))).thenReturn(CpsPurchaseDecisionResponse.builder()
                .summary("best value recommendation")
                .bestChoice(CpsPurchaseDecisionResponse.DecisionItem.builder()
                        .goodsId("goods-1")
                        .platformCode("jd")
                        .title("silent wireless mouse")
                        .actualPrice(new BigDecimal("99.00"))
                        .estimatedRebate(new BigDecimal("8.00"))
                        .promotionUrl("https://cps.example/s")
                        .reasons(List.of("budget match", "rebate friendly"))
                        .build())
                .alternatives(List.of())
                .risks(List.of("platform prices may change"))
                .hainaAvailable(false)
                .build());

        CpsPromotionStrategyAdviceToolFunction.Response response = toolFunction.apply(request,
                new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertNull(response.getError());
        assertEquals("goods-1", response.getRecommendedGoods().getGoodsId());
        assertTrue(response.getPromotionActions().stream().anyMatch(action -> action.contains("wechat")));
        assertTrue(response.getCopyAngles().stream().anyMatch(angle -> angle.contains("office users")));
        ArgumentCaptor<CpsPurchaseDecisionRequest> requestCaptor =
                ArgumentCaptor.forClass(CpsPurchaseDecisionRequest.class);
        verify(purchaseDecisionService).decide(requestCaptor.capture(), eq(100L));
        assertEquals("wireless mouse", requestCaptor.getValue().getNeed());
        assertEquals("increase conversion / office users / wechat", requestCaptor.getValue().getScenario());
        assertEquals(true, requestCaptor.getValue().getGenerateLink());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_promotion_strategy_advice", logCaptor.getValue().getToolName());
        assertEquals(1, logCaptor.getValue().getStatus());
    }

    @Test
    void apply_returnsValidationErrorForBlankProductNeed() {
        CpsPromotionStrategyAdviceToolFunction.Request request =
                new CpsPromotionStrategyAdviceToolFunction.Request();
        request.setProductNeed(" ");

        CpsPromotionStrategyAdviceToolFunction.Response response = toolFunction.apply(request,
                new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals("product_need is required", response.getError());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals(0, logCaptor.getValue().getStatus());
    }
}
