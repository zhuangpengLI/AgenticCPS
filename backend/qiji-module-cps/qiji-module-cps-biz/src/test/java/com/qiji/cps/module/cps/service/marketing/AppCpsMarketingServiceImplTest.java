package com.qiji.cps.module.cps.service.marketing;

import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingActivityCardRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeReqVO;
import com.qiji.cps.module.cps.controller.app.marketing.vo.AppCpsMarketingSelectionThemeRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivityService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
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
    @Mock
    private CpsRebateActivityService activityService;
    @Mock
    private CpsPlatformService platformService;

    @Test
    @DisplayName("getActivitiesByIds preserves request order and omits unavailable activities")
    void getActivitiesByIds_preservesRequestOrderAndOmitsUnavailableActivities() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 12, 0);
        service.setClockForTest(() -> now);
        CpsRebateActivityDO first = CpsRebateActivityDO.builder()
                .id(1L)
                .activityName("First")
                .platformCode("taobao")
                .status(1)
                .startTime(now.minusHours(1))
                .endTime(now.plusHours(1))
                .build();
        CpsRebateActivityDO third = CpsRebateActivityDO.builder()
                .id(3L)
                .activityName("Third")
                .platformCode("jd")
                .status(1)
                .build();
        CpsRebateActivityDO disabled = CpsRebateActivityDO.builder()
                .id(2L)
                .activityName("Disabled")
                .status(0)
                .build();
        CpsRebateActivityDO future = CpsRebateActivityDO.builder()
                .id(4L)
                .activityName("Future")
                .status(1)
                .startTime(now.plusSeconds(1))
                .build();
        CpsRebateActivityDO expired = CpsRebateActivityDO.builder()
                .id(5L)
                .activityName("Expired")
                .status(1)
                .endTime(now.minusSeconds(1))
                .build();
        when(activityMapper.selectByIds(List.of(3L, 2L, 4L, 5L, 9L, 1L)))
                .thenReturn(List.of(first, disabled, third, future, expired));
        when(platformService.getPlatformByCode("taobao"))
                .thenReturn(CpsPlatformDO.builder().platformCode("taobao").platformName("淘宝").build());
        when(platformService.getPlatformByCode("jd"))
                .thenReturn(CpsPlatformDO.builder().platformCode("jd").platformName("京东").build());

        List<AppCpsMarketingActivityCardRespVO> result =
                service.getActivitiesByIds(List.of(3L, 2L, 4L, 5L, 9L, 1L, 3L));

        assertEquals(List.of(3L, 1L), result.stream().map(AppCpsMarketingActivityCardRespVO::getId).toList());
        assertEquals(List.of("京东", "淘宝"),
                result.stream().map(AppCpsMarketingActivityCardRespVO::getPlatformName).toList());
        verify(activityMapper).selectByIds(List.of(3L, 2L, 4L, 5L, 9L, 1L));
        verify(activityService, never()).decorateActivityCapabilities(any(), any());
        verify(activityService, never()).generatePromotionContent(any(), any());
    }

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
                .sourceType("jutuike")
                .status(1)
                .build()));
        doAnswer(invocation -> {
            AppCpsMarketingActivityRespVO target = invocation.getArgument(1);
            target.setSupportsPromotionLink(true);
            target.setSupportsOrders(true);
            target.setSupportsLocalLife(true);
            return null;
        }).when(activityService).decorateActivityCapabilities(any(CpsRebateActivityDO.class),
                any(AppCpsMarketingActivityRespVO.class));
        when(activityService.generatePromotionContent(any(), org.mockito.ArgumentMatchers.eq(1001L)))
                .thenReturn(CpsRebateActivityPromotionRespVO.builder()
                        .linkStatus("SUCCESS")
                        .linkType("EXTERNAL_PROMOTION")
                        .attributionStatus("MEMBER_TRACKED")
                        .promotionUrl("https://s.example/member-entry")
                        .build());
        AppCpsMarketingActivityReqVO reqVO = new AppCpsMarketingActivityReqVO();
        reqVO.setPlatformCode("taobao");

        List<AppCpsMarketingActivityRespVO> result = service.getActivityCenter(1001L, reqVO);

        assertEquals(1, result.size());
        assertEquals("Summer CPS", result.get(0).getActivityName());
        assertEquals("taobao", result.get(0).getPlatformCode());
        assertTrue(result.get(0).getSupportsPromotionLink());
        assertTrue(result.get(0).getSupportsOrders());
        assertTrue(result.get(0).getSupportsLocalLife());
        assertEquals("SUCCESS", result.get(0).getLinkStatus());
        assertEquals("MEMBER_TRACKED", result.get(0).getAttributionStatus());
        assertEquals("https://s.example/member-entry", result.get(0).getPromotionUrl());
        verify(activityService).decorateActivityCapabilities(any(CpsRebateActivityDO.class),
                any(AppCpsMarketingActivityRespVO.class));
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
