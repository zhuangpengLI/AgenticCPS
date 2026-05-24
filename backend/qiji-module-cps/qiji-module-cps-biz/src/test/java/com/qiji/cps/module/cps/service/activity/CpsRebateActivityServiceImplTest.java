package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateActivityServiceImplTest {

    @InjectMocks
    private CpsRebateActivityServiceImpl service;

    @Mock
    private CpsRebateActivityMapper activityMapper;

    @Mock
    private CpsPlatformService platformService;

    @Test
    @DisplayName("createActivity - 保存运营配置活动卡片")
    void createActivity_savesConfiguredActivityCard() {
        CpsRebateActivitySaveReqVO reqVO = buildReqVO();
        when(activityMapper.insert(any(CpsRebateActivityDO.class))).thenAnswer(invocation -> {
            CpsRebateActivityDO activity = invocation.getArgument(0);
            activity.setId(100L);
            return 1;
        });

        Long id = service.createActivity(reqVO);

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        assertEquals(100L, id);
        assertEquals("eleme", captor.getValue().getPlatformCode());
        assertEquals("外卖", captor.getValue().getActivityType());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("updateActivity - 活动不存在时拒绝更新")
    void updateActivity_rejectsMissingActivity() {
        CpsRebateActivitySaveReqVO reqVO = buildReqVO();
        reqVO.setId(404L);
        when(activityMapper.selectById(404L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.updateActivity(reqVO));

        verify(activityMapper, never()).updateById(any(CpsRebateActivityDO.class));
    }

    @Test
    @DisplayName("getEnabledActivityList - 只返回当前有效活动并保持排序")
    void getEnabledActivityList_usesMapperEnabledWindowQuery() {
        List<CpsRebateActivityDO> activities = List.of(
                CpsRebateActivityDO.builder().id(1L).activityName("饿了么").sort(1).status(1).build(),
                CpsRebateActivityDO.builder().id(2L).activityName("美团").sort(2).status(1).build());
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(activities);

        List<CpsRebateActivityDO> result = service.getEnabledActivityList();

        assertEquals(activities, result);
        verify(activityMapper).selectEnabledList(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getActivityCenter - 按平台类型关键词过滤，并按热门分页返回卡片")
    void getActivityCenter_filtersAndPaginatesHotCards() {
        CpsRebateActivityDO meituanCps = buildActivity(1L, "美团外卖节", "meituan", "CPS", 707, 2);
        meituanCps.setSearchKeyword("美团外卖");
        meituanCps.setJumpType("search");
        CpsRebateActivityDO meituanCpa = buildActivity(2L, "美团拉新红包", "meituan", "CPA", 1200, 1);
        CpsRebateActivityDO taobaoCps = buildActivity(3L, "淘宝闪购", "taobao", "CPS", 1325, 3);
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(
                List.of(meituanCps, meituanCpa, taobaoCps));
        when(platformService.getEnabledPlatformList()).thenReturn(List.of(
                CpsPlatformDO.builder().platformCode("meituan").platformName("美团").platformLogo("meituan.png").sort(1).build(),
                CpsPlatformDO.builder().platformCode("taobao").platformName("淘宝").platformLogo("taobao.png").sort(2).build()));

        CpsRebateActivityCenterReqVO reqVO = new CpsRebateActivityCenterReqVO();
        reqVO.setPlatformCode("meituan");
        reqVO.setBillingType("CPS");
        reqVO.setKeyword("外卖");
        reqVO.setSortMode("hot");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CpsRebateActivityCenterRespVO result = service.getActivityCenter(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getCards().size());
        CpsRebateActivityCenterRespVO.Card card = result.getCards().get(0);
        assertEquals("美团外卖节", card.getActivityName());
        assertEquals("meituan", card.getPlatformCode());
        assertEquals("美团", card.getPlatformName());
        assertEquals("meituan.png", card.getPlatformLogo());
        assertEquals("CPS", card.getBillingType());
        assertEquals(707, card.getPromotionCount());
        assertEquals("search", card.getJumpType());
        assertEquals("美团外卖", card.getSearchKeyword());
        assertEquals("configured", card.getSourceType());
    }

    @Test
    @DisplayName("getActivityCenter - 最新模式按上线时间优先返回，并保留 url 跳转字段")
    void getActivityCenter_sortsLatestAndKeepsUrlJump() {
        CpsRebateActivityDO older = buildActivity(1L, "旧活动", "taobao", "CPS", 100, 1);
        older.setStartTime(LocalDateTime.now().minusDays(2));
        CpsRebateActivityDO newer = buildActivity(2L, "新活动", "jd", "CPS+CPA", 80, 2);
        newer.setStartTime(LocalDateTime.now().minusHours(1));
        newer.setJumpType("url");
        newer.setJumpUrl("https://example.com/latest");
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(List.of(older, newer));
        when(platformService.getEnabledPlatformList()).thenReturn(List.of());

        CpsRebateActivityCenterReqVO reqVO = new CpsRebateActivityCenterReqVO();
        reqVO.setSortMode("latest");
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);

        CpsRebateActivityCenterRespVO result = service.getActivityCenter(reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getCards().size());
        assertEquals("新活动", result.getCards().get(0).getActivityName());
        assertEquals("CPS+CPA", result.getCards().get(0).getBillingType());
        assertEquals("url", result.getCards().get(0).getJumpType());
        assertEquals("https://example.com/latest", result.getCards().get(0).getJumpUrl());
    }

    private CpsRebateActivitySaveReqVO buildReqVO() {
        CpsRebateActivitySaveReqVO reqVO = new CpsRebateActivitySaveReqVO();
        reqVO.setActivityName("饿了么外卖红包");
        reqVO.setActivityType("外卖");
        reqVO.setPlatformCode("eleme");
        reqVO.setMainPic("https://example.com/eleme.png");
        reqVO.setShortDesc("天天领外卖红包");
        reqVO.setRebateDesc("下单后按活动规则返利");
        reqVO.setJumpType("search");
        reqVO.setSearchKeyword("饿了么");
        reqVO.setSort(1);
        reqVO.setStatus(1);
        return reqVO;
    }

    private CpsRebateActivityDO buildActivity(Long id, String name, String platformCode, String billingType,
                                             Integer promotionCount, Integer sort) {
        return CpsRebateActivityDO.builder()
                .id(id)
                .activityName(name)
                .activityType("外卖")
                .platformCode(platformCode)
                .mainPic("https://example.com/" + id + ".png")
                .shortDesc(name + "短描述")
                .rebateDesc("最高返利")
                .billingType(billingType)
                .promotionCount(promotionCount)
                .sourceType("configured")
                .tagText("热")
                .jumpType("none")
                .sort(sort)
                .status(1)
                .build();
    }

}
