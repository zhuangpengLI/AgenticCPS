package com.qiji.cps.module.cps.mcp.tool;

import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.dal.dataobject.mcp.CpsMcpAccessLogDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.mcp.CpsMcpAccessLogMapper;
import com.qiji.cps.module.cps.service.goods.CpsGoodsService;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import com.qiji.cps.module.cps.service.selection.CpsSelectionThemeService;
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
class CpsSelectionThemeMcpToolFunctionTest {

    @InjectMocks
    private CpsListSelectionThemesToolFunction listToolFunction;

    @InjectMocks
    private CpsRecommendFromSelectionThemeToolFunction recommendToolFunction;

    @Mock
    private CpsSelectionThemeService selectionThemeService;

    @Mock
    private CpsGoodsService goodsService;

    @Mock
    private CpsMcpAccessLogMapper accessLogMapper;

    @Test
    @DisplayName("cps_list_selection_themes - 只返回已发布主题并写入审计")
    void listSelectionThemes_returnsPublishedThemesAndWritesAudit() {
        when(selectionThemeService.listPublishedThemes("618", null)).thenReturn(List.of(
                CpsSelectionThemeDO.builder()
                        .id(1L)
                        .themeCode("618_PRE")
                        .themeName("618预售")
                        .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                        .build()));

        CpsListSelectionThemesToolFunction.Request request = new CpsListSelectionThemesToolFunction.Request();
        request.setKeyword("618");
        var response = listToolFunction.apply(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getThemes().size());
        assertEquals("618_PRE", response.getThemes().get(0).getThemeCode());
        verify(accessLogMapper).insert(any(CpsMcpAccessLogDO.class));
    }

    @Test
    @DisplayName("cps_recommend_from_selection_theme - 默认只读不转链")
    void recommendFromSelectionTheme_isReadOnlyByDefault() {
        when(selectionThemeService.getTheme(1L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(1L)
                .themeCode("618_PRE")
                .themeName("618预售")
                .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                .build());
        when(selectionThemeService.listEnabledItems(1L)).thenReturn(List.of(buildItem()));

        CpsRecommendFromSelectionThemeToolFunction.Request request = new CpsRecommendFromSelectionThemeToolFunction.Request();
        request.setThemeId(1L);
        request.setLimit(10);
        var response = recommendToolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getItems().size());
        assertNull(response.getItems().get(0).getPromotionUrl());
        verify(goodsService, never()).generatePromotionLink(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("cps_recommend_from_selection_theme - 显式转链且有可信上下文时生成推广链接")
    void recommendFromSelectionTheme_generatesLinkWhenExplicitAndTrustedContext() {
        when(selectionThemeService.getTheme(1L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(1L)
                .themeCode("618_PRE")
                .themeName("618预售")
                .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                .build());
        when(selectionThemeService.listEnabledItems(1L)).thenReturn(List.of(buildItem()));
        when(goodsService.generatePromotionLink(eq("taobao"), eq("goods-1"), eq("sign-1"), eq(100L), any(), eq("dataoke")))
                .thenReturn(CpsPromotionLinkResult.builder().shortUrl("https://cps.example/s").build());

        CpsRecommendFromSelectionThemeToolFunction.Request request = new CpsRecommendFromSelectionThemeToolFunction.Request();
        request.setThemeId(1L);
        request.setGenerateLink(true);
        var response = recommendToolFunction.apply(request, new ToolContext(Map.of("LOGIN_USER_ID", 100L)));

        assertEquals("https://cps.example/s", response.getItems().get(0).getPromotionUrl());
        ArgumentCaptor<CpsMcpAccessLogDO> logCaptor = ArgumentCaptor.forClass(CpsMcpAccessLogDO.class);
        verify(accessLogMapper).insert(logCaptor.capture());
        assertEquals("cps_recommend_from_selection_theme", logCaptor.getValue().getToolName());
    }

    private CpsSelectionThemeItemDO buildItem() {
        return CpsSelectionThemeItemDO.builder()
                .id(10L)
                .themeId(1L)
                .platformCode("taobao")
                .vendorCode("dataoke")
                .goodsId("goods-1")
                .goodsSign("sign-1")
                .title("防晒霜")
                .actualPrice(new BigDecimal("49.90"))
                .commissionAmount(new BigDecimal("9.98"))
                .recommendScore(new BigDecimal("90"))
                .recommendReason("佣金高，活动匹配")
                .status(CpsSelectionConstants.ItemStatus.ENABLED)
                .build();
    }
}
