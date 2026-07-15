package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsGenerateLinkToolFunctionTest {

    @InjectMocks
    private CpsGenerateLinkToolFunction toolFunction;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    @DisplayName("apply - ToolContext 登录用户优先于 request.memberId 并写入成功审计")
    void apply_prefersTrustedToolContextMemberAndWritesAuditLog() {
        CpsGenerateLinkToolFunction.Request request = new CpsGenerateLinkToolFunction.Request();
        request.setPlatformCode("jd");
        request.setGoodsId("goods-1");
        request.setGoodsSign("sign-1");
        request.setMemberId(200L);
        request.setVendorCode("haodanku");

        when(goodsService.generatePromotionLink(eq("jd"), eq("goods-1"), eq("sign-1"), eq(100L), isNull(), eq("haodanku")))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://cps.example/s")
                        .actualPrice(new BigDecimal("88.00"))
                        .commissionAmount(new BigDecimal("6.00"))
                        .build());

        var response = toolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertNull(response.getError());
        assertEquals("https://cps.example/s", response.getShortUrl());
        verify(goodsService).generatePromotionLink("jd", "goods-1", "sign-1", 100L, null, "haodanku");
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_generate_link", logCaptor.getValue().getToolName());
        assertEquals(1, logCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("apply - 无 ToolContext 时忽略请求体伪造 memberId")
    void apply_ignoresUntrustedRequestMemberWithoutToolContext() {
        CpsGenerateLinkToolFunction.Request request = new CpsGenerateLinkToolFunction.Request();
        request.setPlatformCode("jd");
        request.setGoodsId("goods-1");
        request.setMemberId(999L);
        when(goodsService.generatePromotionLink(eq("jd"), eq("goods-1"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(CpsPromotionLinkResult.builder().shortUrl("https://cps.example/anonymous").build());

        var response = toolFunction.apply(request, null);

        assertNull(response.getError());
        verify(goodsService).generatePromotionLink("jd", "goods-1", null, null, null, null);
    }

    @Test
    @DisplayName("apply - 转链失败时写入失败审计且不暴露内部异常")
    void apply_writesFailureAuditLogWithSanitizedError() {
        CpsGenerateLinkToolFunction.Request request = new CpsGenerateLinkToolFunction.Request();
        request.setPlatformCode("jd");
        request.setGoodsId("goods-1");
        when(goodsService.generatePromotionLink(anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("SQL signature secret leaked"));

        var response = toolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals("转链失败，请稍后重试", response.getError());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_generate_link", logCaptor.getValue().getToolName());
        assertEquals(0, logCaptor.getValue().getStatus());
        assertEquals("IllegalStateException", logCaptor.getValue().getErrorMessage());
    }
}
