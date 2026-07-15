package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeRespVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.service.selection.CpsSelectionConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsMarketingServiceImplTest {

    @InjectMocks
    private AppCpsMarketingServiceImpl service;

    @Mock
    private CpsRebateActivityMapper activityMapper;
    @Mock
    private CpsSelectionThemeMapper themeMapper;
    @Mock
    private CpsSelectionThemeItemMapper themeItemMapper;

    @Test
    @DisplayName("getActivityCenter exposes only effective activity cards")
    void getActivityCenter_exposesOnlyEffectiveActivityCards() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 19, 0);
        service.setClockForTest(() -> now);
        when(activityMapper.selectEnabledList(now)).thenReturn(List.of(CpsRebateActivityDO.builder()
                .id(1L)
                .activityName("Summer CPS")
                .platformCode("taobao")
                .billingType("CPS")
                .status(1)
                .build()));
        AppCpsMarketingActivityReqVO reqVO = new AppCpsMarketingActivityReqVO();
        reqVO.setPlatformCode("taobao");

        List<AppCpsMarketingActivityRespVO> result = service.getActivityCenter(1001L, reqVO);

        assertEquals(1, result.size());
        assertEquals("Summer CPS", result.get(0).getActivityName());
        assertEquals("taobao", result.get(0).getPlatformCode());
    }

    @Test
    @DisplayName("getSelectionThemes exposes only published effective themes")
    void getSelectionThemes_exposesOnlyPublishedEffectiveThemes() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 19, 0);
        service.setClockForTest(() -> now);
        AppCpsMarketingSelectionThemeReqVO reqVO = new AppCpsMarketingSelectionThemeReqVO();
        reqVO.setKeyword("rank");
        when(themeMapper.selectPublishedList("rank", null)).thenReturn(List.of(
                CpsSelectionThemeDO.builder()
                        .id(1L)
                        .themeName("Rank Shelf")
                        .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                        .startTime(now.minusDays(1))
                        .endTime(now.plusDays(1))
                        .build(),
                CpsSelectionThemeDO.builder()
                        .id(2L)
                        .themeName("Expired")
                        .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                        .endTime(now.minusSeconds(1))
                        .build()));

        List<AppCpsMarketingSelectionThemeRespVO> result = service.getSelectionThemes(1001L, reqVO);

        assertEquals(1, result.size());
        assertEquals("Rank Shelf", result.get(0).getThemeName());
    }

    @Test
    @DisplayName("getSelectionThemeItems requires published parent and enabled items")
    void getSelectionThemeItems_requiresPublishedParentAndEnabledItems() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 19, 0);
        service.setClockForTest(() -> now);
        when(themeMapper.selectById(1L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(1L)
                .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                .startTime(now.minusDays(1))
                .endTime(now.plusDays(1))
                .build());
        when(themeItemMapper.selectEnabledListByThemeId(1L)).thenReturn(List.of(
                CpsSelectionThemeItemDO.builder()
                        .id(10L)
                        .themeId(1L)
                        .title("Enabled Goods")
                        .status(CpsSelectionConstants.ItemStatus.ENABLED)
                        .build()));

        List<AppCpsMarketingSelectionThemeItemRespVO> result = service.getSelectionThemeItems(1001L, 1L);

        assertEquals(1, result.size());
        assertEquals("Enabled Goods", result.get(0).getTitle());
    }

    @Test
    @DisplayName("getSelectionThemeItems hides unpublished parent themes")
    void getSelectionThemeItems_hidesUnpublishedParentThemes() {
        when(themeMapper.selectById(2L)).thenReturn(CpsSelectionThemeDO.builder()
                .id(2L)
                .status(CpsSelectionConstants.ThemeStatus.DRAFT)
                .build());

        List<AppCpsMarketingSelectionThemeItemRespVO> result = service.getSelectionThemeItems(1001L, 2L);

        assertTrue(result.isEmpty());
        verify(themeItemMapper, never()).selectEnabledListByThemeId(2L);
    }
}
