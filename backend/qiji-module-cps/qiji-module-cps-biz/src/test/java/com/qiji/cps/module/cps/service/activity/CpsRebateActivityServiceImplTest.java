package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityClient;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPromotionRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Mock
    private HdkActivityClient hdkActivityClient;

    @Mock
    private CpsAdzoneService adzoneService;

    @Mock
    private CpsTransferRecordMapper transferRecordMapper;

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
    @DisplayName("getEnabledActivityList - 隐藏历史遗留的不可转链好单库活动")
    void getEnabledActivityList_hidesUnsupportedHaodankuActivities() {
        CpsRebateActivityDO configuredMeituan = CpsRebateActivityDO.builder()
                .id(1L).activityName("运营配置美团").platformCode("meituan")
                .sourceType("configured").sort(1).status(1).build();
        CpsRebateActivityDO haodankuTaobao = CpsRebateActivityDO.builder()
                .id(2L).activityName("好单库淘宝").platformCode("taobao")
                .sourceType("haodanku").sort(2).status(1).build();
        CpsRebateActivityDO haodankuMeituan = CpsRebateActivityDO.builder()
                .id(3L).activityName("好单库美团").platformCode("meituan")
                .sourceType("haodanku").sort(3).status(1).build();
        List<CpsRebateActivityDO> activities = List.of(
                configuredMeituan, haodankuTaobao, haodankuMeituan);
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(activities);

        List<CpsRebateActivityDO> result = service.getEnabledActivityList();

        assertEquals(List.of(configuredMeituan, haodankuTaobao), result);
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
    @DisplayName("getActivityCenter - 按活动来源 API 供应商过滤")
    void getActivityCenter_filtersBySourceType() {
        CpsRebateActivityDO dataokeActivity = buildActivity(1L, "大淘客淘宝会场", "taobao", "CPS", 100, 1);
        dataokeActivity.setSourceType("dataoke");
        CpsRebateActivityDO haodankuActivity = buildActivity(2L, "好单库淘宝会场", "taobao", "CPS", 90, 2);
        haodankuActivity.setSourceType("haodanku");
        CpsRebateActivityDO configuredActivity = buildActivity(3L, "运营配置会场", "taobao", "CPS", 80, 3);
        configuredActivity.setSourceType("configured");
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(
                List.of(dataokeActivity, haodankuActivity, configuredActivity));
        when(platformService.getEnabledPlatformList()).thenReturn(List.of(
                CpsPlatformDO.builder().platformCode("taobao").platformName("淘宝").sort(1).build()));

        CpsRebateActivityCenterReqVO reqVO = new CpsRebateActivityCenterReqVO();
        reqVO.setSourceType("dataoke");
        reqVO.setPlatformCode("hot");
        reqVO.setBillingType("all");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CpsRebateActivityCenterRespVO result = service.getActivityCenter(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getCards().size());
        assertEquals("大淘客淘宝会场", result.getCards().getFirst().getActivityName());
        assertEquals("dataoke", result.getCards().getFirst().getSourceType());
        assertEquals(1L, result.getTabs().getFirst().getActivityCount().longValue());
    }

    @Test
    @DisplayName("getActivityCenter - 归一化历史好单库数字平台码")
    void getActivityCenter_normalizesLegacyHaodankuNumericPlatformCodes() {
        CpsRebateActivityDO jdActivity = buildActivity(1L, "京东外卖", "2", "CPS", 11, 1);
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(List.of(jdActivity));
        when(platformService.getEnabledPlatformList()).thenReturn(List.of(
                CpsPlatformDO.builder().platformCode("jd").platformName("京东").platformLogo("jd.png").sort(1).build()));

        CpsRebateActivityCenterReqVO reqVO = new CpsRebateActivityCenterReqVO();
        reqVO.setPlatformCode("jd");
        reqVO.setBillingType("all");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CpsRebateActivityCenterRespVO result = service.getActivityCenter(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("jd", result.getCards().get(0).getPlatformCode());
        assertEquals("京东", result.getCards().get(0).getPlatformName());
        assertEquals(1, result.getTabs().stream()
                .filter(tab -> "jd".equals(tab.getPlatformCode()))
                .findFirst()
                .orElseThrow()
                .getActivityCount());
        assertTrue(result.getTabs().stream().noneMatch(tab -> "2".equals(tab.getPlatformCode())));
    }

    @Test
    @DisplayName("getActivityCenter - 点击美团时返回好单库历史数字平台码活动")
    void getActivityCenter_matchesLegacyHaodankuMeituanPlatformCode() {
        CpsRebateActivityDO meituanActivity = buildActivity(1L, "美团外卖品牌活动", "6", "CPS", 21, 1);
        when(activityMapper.selectEnabledList(any(LocalDateTime.class))).thenReturn(List.of(meituanActivity));
        when(platformService.getEnabledPlatformList()).thenReturn(List.of(
                CpsPlatformDO.builder().platformCode("meituan").platformName("美团").platformLogo("meituan.png").sort(1).build()));

        CpsRebateActivityCenterReqVO reqVO = new CpsRebateActivityCenterReqVO();
        reqVO.setPlatformCode("meituan");
        reqVO.setBillingType("all");
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CpsRebateActivityCenterRespVO result = service.getActivityCenter(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getCards().size());
        assertEquals("meituan", result.getCards().get(0).getPlatformCode());
        assertEquals("美团", result.getCards().get(0).getPlatformName());
        assertEquals(1, result.getTabs().stream()
                .filter(tab -> "meituan".equals(tab.getPlatformCode()))
                .findFirst()
                .orElseThrow()
                .getActivityCount());
        assertTrue(result.getTabs().stream().noneMatch(tab -> "6".equals(tab.getPlatformCode())));
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

    @Test
    @DisplayName("generatePromotionContent - url 活动生成可复制活动推广文案")
    void generatePromotionContent_usesExternalUrlActivityLanding() {
        CpsRebateActivityDO activity = buildActivity(10L, "品牌U享礼金专场", "taobao", "CPS", 320, 1);
        activity.setJumpType("url");
        activity.setJumpUrl("https://uland.taobao.com/coupon/edetail?activityId=abc");
        activity.setShortDesc("每日10点抢百万单品红包");
        activity.setRebateDesc("最高返利78%");
        when(activityMapper.selectById(10L)).thenReturn(activity);

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(10L);
        reqVO.setAdzoneId("mm_123_456_789");

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        assertEquals("SUCCESS", result.getLinkStatus());
        assertEquals("EXTERNAL_PROMOTION", result.getLinkType());
        assertEquals("https://uland.taobao.com/coupon/edetail?activityId=abc", result.getPromotionUrl());
        assertEquals("mm_123_456_789", result.getAdzoneId());
        assertTrue(result.getPromotionContent().contains("品牌U享礼金专场"));
        assertTrue(result.getPromotionContent().contains("最高返利78%"));
        assertTrue(result.getPromotionContent().contains("https://uland.taobao.com/coupon/edetail?activityId=abc"));
        assertTrue(result.getPromotionContent().contains("推广位：mm_123_456_789"));
    }

    @Test
    @DisplayName("generatePromotionContent - search 活动生成商品广场落地链接")
    void generatePromotionContent_buildsGoodsSquareLandingForSearchActivity() {
        CpsRebateActivityDO activity = buildActivity(11L, "飞猪酒店特惠", "fliggy", "CPS", 120, 1);
        activity.setJumpType("search");
        activity.setSearchKeyword("酒店红包");
        activity.setTagText("天天特惠");
        when(activityMapper.selectById(11L)).thenReturn(activity);

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(11L);

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        assertEquals("INTERNAL_FALLBACK", result.getLinkStatus());
        assertEquals("INTERNAL_LANDING", result.getLinkType());
        assertEquals("/cps-ops/goods/square?platformCode=fliggy&keyword=%E9%85%92%E5%BA%97%E7%BA%A2%E5%8C%85&activityTag=%E5%A4%A9%E5%A4%A9%E7%89%B9%E6%83%A0",
                result.getPromotionUrl());
        assertFalse(result.getPromotionUrl().contains("localhost"));
        assertTrue(result.getPromotionContent().contains("飞猪酒店特惠"));
        assertTrue(result.getPromotionContent().contains("酒店红包"));
        assertTrue(result.getPromotionContent().contains(result.getPromotionUrl()));
    }

    @Test
    @DisplayName("generatePromotionContent - 大淘客活动使用官方活动会场转链")
    void generatePromotionContent_usesDtkOfficialActivityLink() {
        CpsRebateActivityDO activity = buildActivity(12L, "淘宝官方补贴", "taobao", "CPS", 88, 1);
        activity.setSourceType("dataoke");
        activity.setExternalActivityId("dtk:10001");
        activity.setJumpType("url");
        activity.setJumpUrl("https://uland.taobao.com/");
        when(activityMapper.selectById(12L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .defaultAdzoneId("mm_default")
                .build();
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkActivityVendorClient.generateActivityLink(any(CpsPromotionLinkRequest.class), eq(config)))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://s.click.taobao.com/abc")
                        .longUrl("https://s.click.taobao.com/abc-long")
                        .tpwd("￥ABC123￥")
                        .build());

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(12L);
        reqVO.setAdzoneId("mm_1_2_3");
        reqVO.setChannelTag("wechat_a");

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(dtkActivityVendorClient).generateActivityLink(captor.capture(), eq(config));
        assertEquals("10001", captor.getValue().getGoodsId());
        assertEquals("mm_1_2_3", captor.getValue().getAdzoneId());
        assertEquals("wechat_a", captor.getValue().getExternalId());
        assertEquals("SUCCESS", result.getLinkStatus());
        assertEquals("EXTERNAL_PROMOTION", result.getLinkType());
        assertEquals("https://s.click.taobao.com/abc", result.getPromotionUrl());
        assertEquals("￥ABC123￥", result.getTpwd());
        assertTrue(result.getPromotionContent().contains("https://s.click.taobao.com/abc"));
        assertTrue(result.getPromotionContent().contains("淘口令：￥ABC123￥"));
    }

    @Test
    @DisplayName("generatePromotionContent - 大淘客官方转链超时后明确返回失败")
    void generatePromotionContent_failsWhenDtkLinkUnavailable() {
        CpsRebateActivityDO activity = buildActivity(13L, "淘宝官方补贴", "taobao", "CPS", 88, 1);
        activity.setSourceType("dataoke");
        activity.setExternalActivityId("dtk:10001");
        activity.setJumpType("url");
        activity.setJumpUrl("https://uland.taobao.com/coupon/edetail?activityId=abc");
        when(activityMapper.selectById(13L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .defaultAdzoneId("mm_default")
                .build();
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkActivityVendorClient.generateActivityLink(any(CpsPromotionLinkRequest.class), eq(config)))
                .thenReturn(null);

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(13L);
        reqVO.setAdzoneId("mm_1_2_3");
        reqVO.setChannelTag("wechat_a");

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        assertEquals("FAILED", result.getLinkStatus());
        assertEquals("NONE", result.getLinkType());
        assertNull(result.getPromotionUrl());
        assertNull(result.getPromotionContent());
        assertEquals("mm_1_2_3", result.getAdzoneId());
        assertEquals("wechat_a", result.getChannelTag());
        assertTrue(result.getLinkMessage().contains("官方活动转链暂不可用"));
    }

    @Test
    @DisplayName("generatePromotionContent - 大淘客未配置供应商时不伪造成功链接")
    void generatePromotionContent_failsWhenDtkConfigMissing() {
        CpsRebateActivityDO activity = buildActivity(14L, "淘宝官方补贴", "taobao", "CPS", 88, 1);
        activity.setSourceType("dataoke");
        activity.setExternalActivityId("dtk:10001");
        when(activityMapper.selectById(14L)).thenReturn(activity);
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(null);

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(14L);

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        assertEquals("FAILED", result.getLinkStatus());
        assertEquals("NONE", result.getLinkType());
        assertNull(result.getPromotionUrl());
        assertFalse(String.valueOf(result.getPromotionContent()).contains("127.0.0.1"));
    }

    @Test
    @DisplayName("generatePromotionContent - 好单库淘宝活动使用官方会场转链且不误传渠道关系ID")
    void generatePromotionContent_usesHaodankuOfficialConferenceLink() {
        CpsRebateActivityDO activity = buildActivity(15L, "淘宝官方超级补贴会场", "taobao", "CPS", 66, 1);
        activity.setSourceType("haodanku");
        activity.setExternalActivityId("hdk:taobao:1677");
        activity.setPromotionActivityId("20150318020023228");
        activity.setVendorMetadata("{\"activity_url\":\"https://pages.tmall.com/wow/activity\"}");
        when(activityMapper.selectById(15L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("taobao")
                .appKey("api-key")
                .authToken("authorized-tb-name")
                .defaultAdzoneId("mm_default")
                .build();
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(config);
        when(hdkActivityClient.generateConferenceLink(any(CpsPromotionLinkRequest.class), eq(config),
                eq("淘宝官方超级补贴会场"))).thenReturn(CpsPromotionLinkResult.builder()
                .shortUrl("https://s.click.taobao.com/hdk")
                .tpwd("￥HDK123￥")
                .build());

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(15L);
        reqVO.setChannelTag("wechat_group_a");

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(hdkActivityClient).generateConferenceLink(captor.capture(), eq(config), eq("淘宝官方超级补贴会场"));
        assertEquals("20150318020023228", captor.getValue().getGoodsId());
        assertEquals("https://pages.tmall.com/wow/activity", captor.getValue().getItemLink());
        assertEquals("wechat_group_a", captor.getValue().getExternalId());
        assertNull(captor.getValue().getRelationId());
        assertEquals("SUCCESS", result.getLinkStatus());
        assertEquals("EXTERNAL_PROMOTION", result.getLinkType());
        assertEquals("https://s.click.taobao.com/hdk", result.getPromotionUrl());
        assertEquals("￥HDK123￥", result.getTpwd());
    }

    @Test
    @DisplayName("generatePromotionContent - 好单库淘宝缺少授权账号名时返回可操作提示")
    void generatePromotionContent_reportsMissingHaodankuTbName() {
        CpsRebateActivityDO activity = buildActivity(19L, "淘宝官方超级补贴会场", "taobao", "CPS", 66, 1);
        activity.setSourceType("haodanku");
        activity.setPromotionActivityId("20150318020023228");
        when(activityMapper.selectById(19L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("taobao")
                .appKey("api-key")
                .defaultAdzoneId("mm_default")
                .build();
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(config);

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(19L);

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        assertEquals("FAILED", result.getLinkStatus());
        assertTrue(result.getLinkMessage().contains("tb_name"));
        verify(hdkActivityClient, never()).generateConferenceLink(any(), any(), any());
    }

    @Test
    @DisplayName("generatePromotionContent - 会员淘宝会场只使用可信专属推广位关系")
    void generatePromotionContent_usesTrustedMemberRelationForHaodankuConference() {
        CpsRebateActivityDO activity = buildActivity(16L, "淘宝官方超级补贴会场", "taobao", "CPS", 66, 1);
        activity.setSourceType("haodanku");
        activity.setPromotionActivityId("20150318020023228");
        when(activityMapper.selectById(16L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("taobao")
                .appKey("api-key")
                .authToken("authorized-tb-name")
                .defaultAdzoneId("mm_default")
                .build();
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(config);
        when(adzoneService.getMemberAdzone("taobao", 1002L)).thenReturn(CpsAdzoneDO.builder()
                .adzoneId("mm_member_1002")
                .relationType("member")
                .relationId(1002L)
                .externalRelationId("relation-1002")
                .status(1)
                .build());
        when(hdkActivityClient.generateConferenceLink(any(CpsPromotionLinkRequest.class), eq(config),
                eq("淘宝官方超级补贴会场"))).thenReturn(CpsPromotionLinkResult.builder()
                .shortUrl("https://s.click.taobao.com/member-hdk")
                .build());

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(16L);
        reqVO.setAdzoneId("mm_untrusted_request");
        reqVO.setChannelTag("1002");

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO, 1002L);

        ArgumentCaptor<CpsPromotionLinkRequest> captor = ArgumentCaptor.forClass(CpsPromotionLinkRequest.class);
        verify(hdkActivityClient).generateConferenceLink(captor.capture(), eq(config),
                eq("淘宝官方超级补贴会场"));
        assertEquals("mm_member_1002", captor.getValue().getAdzoneId());
        assertEquals("relation-1002", captor.getValue().getRelationId());
        assertNull(captor.getValue().getSpecialId());
        assertEquals("MEMBER_TRACKED", result.getAttributionStatus());
        assertTrue(result.getAttributionMessage().contains("会员"));
    }

    @Test
    @DisplayName("generatePromotionContent - 会员闪购会场使用短 sid 并保存可信映射")
    void generatePromotionContent_tracksHaodankuElemeActivityWithOpaqueSid() {
        CpsRebateActivityDO activity = buildActivity(17L, "淘宝闪购天天领红包", "eleme", "CPS", 66, 1);
        activity.setSourceType("haodanku");
        activity.setPromotionActivityId("elm-activity-1");
        activity.setJumpUrl("https://market.m.taobao.com/app/eleme/activity");
        when(activityMapper.selectById(17L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("eleme")
                .appKey("api-key")
                .build();
        when(platformClientFactory.getVendorConfig("haodanku", "eleme")).thenReturn(config);
        when(transferRecordMapper.insert(any(CpsTransferRecordDO.class))).thenAnswer(invocation -> {
            CpsTransferRecordDO record = invocation.getArgument(0);
            record.setId(9001L);
            return 1;
        });
        when(hdkActivityClient.generateElemeActivityLink(any(CpsPromotionLinkRequest.class), eq(config), any()))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .shortUrl("https://s.click.ele.me/member-hdk")
                        .tpwd("闪购口令")
                        .build());

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(17L);

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO, 1002L);

        ArgumentCaptor<String> sidCaptor = ArgumentCaptor.forClass(String.class);
        verify(hdkActivityClient).generateElemeActivityLink(any(CpsPromotionLinkRequest.class), eq(config),
                sidCaptor.capture());
        String sid = sidCaptor.getValue();
        assertTrue(sid.matches("[A-Za-z0-9_]{12,15}"));
        assertFalse(sid.contains("1002"));
        ArgumentCaptor<CpsTransferRecordDO> recordCaptor = ArgumentCaptor.forClass(CpsTransferRecordDO.class);
        verify(transferRecordMapper).insert(recordCaptor.capture());
        assertEquals(1002L, recordCaptor.getValue().getMemberId());
        assertEquals("haodanku", recordCaptor.getValue().getVendorCode());
        assertEquals("eleme", recordCaptor.getValue().getPlatformCode());
        assertEquals(17L, recordCaptor.getValue().getActivityId());
        assertEquals("SID", recordCaptor.getValue().getAttributionType());
        assertEquals(sid, recordCaptor.getValue().getAttributionToken());
        assertEquals("MEMBER_TRACKED", result.getAttributionStatus());
        assertEquals("https://s.click.ele.me/member-hdk", result.getPromotionUrl());
    }

    @Test
    @DisplayName("generatePromotionContent - 闪购未单独配置时复用好单库淘宝账号")
    void generatePromotionContent_reusesHaodankuTaobaoAccountForEleme() {
        CpsRebateActivityDO activity = buildActivity(18L, "淘宝闪购天天领红包", "eleme", "CPS", 66, 1);
        activity.setSourceType("haodanku");
        activity.setPromotionActivityId("elm-activity-2");
        when(activityMapper.selectById(18L)).thenReturn(activity);
        CpsVendorConfig taobaoConfig = CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("taobao")
                .appKey("shared-api-key")
                .build();
        when(platformClientFactory.getVendorConfig("haodanku", "eleme")).thenReturn(null);
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(taobaoConfig);
        when(hdkActivityClient.generateElemeActivityLink(any(CpsPromotionLinkRequest.class), eq(taobaoConfig),
                isNull())).thenReturn(CpsPromotionLinkResult.builder()
                .shortUrl("https://s.click.ele.me/channel-hdk")
                .build());

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(18L);

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        verify(platformClientFactory).getVendorConfig("haodanku", "eleme");
        verify(platformClientFactory).getVendorConfig("haodanku", "taobao");
        assertEquals("SUCCESS", result.getLinkStatus());
        assertEquals("https://s.click.ele.me/channel-hdk", result.getPromotionUrl());
    }

    @Test
    @DisplayName("generatePromotionContent - 闪购活动无 H5 时使用官方应用唤起链接")
    void generatePromotionContent_usesOfficialSchemeWhenElemeH5Missing() {
        CpsRebateActivityDO activity = buildActivity(20L, "闪购品牌日 单单有福利", "eleme", "CPS", 66, 1);
        activity.setSourceType("haodanku");
        activity.setPromotionActivityId("12698");
        when(activityMapper.selectById(20L)).thenReturn(activity);
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("taobao")
                .appKey("shared-api-key")
                .build();
        when(platformClientFactory.getVendorConfig("haodanku", "eleme")).thenReturn(null);
        when(platformClientFactory.getVendorConfig("haodanku", "taobao")).thenReturn(config);
        when(hdkActivityClient.generateElemeActivityLink(any(CpsPromotionLinkRequest.class), eq(config), isNull()))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .tpwd("闪购品牌日官方淘口令")
                        .mobileUrl("eleme://miniapp/activity")
                        .build());

        CpsRebateActivityPromotionReqVO reqVO = new CpsRebateActivityPromotionReqVO();
        reqVO.setActivityId(20L);

        CpsRebateActivityPromotionRespVO result = service.generatePromotionContent(reqVO);

        assertEquals("SUCCESS", result.getLinkStatus());
        assertEquals("EXTERNAL_PROMOTION", result.getLinkType());
        assertEquals("eleme://miniapp/activity", result.getPromotionUrl());
        assertEquals("闪购品牌日官方淘口令", result.getTpwd());
        assertTrue(result.getPromotionContent().contains("eleme://miniapp/activity"));
        assertTrue(result.getPromotionContent().contains("闪购品牌日官方淘口令"));

        when(hdkActivityClient.generateElemeActivityLink(any(CpsPromotionLinkRequest.class), eq(config), isNull()))
                .thenReturn(CpsPromotionLinkResult.builder()
                        .mobileUrl("javascript:alert(1)")
                        .build());

        CpsRebateActivityPromotionRespVO rejected = service.generatePromotionContent(reqVO);

        assertEquals("FAILED", rejected.getLinkStatus());
        assertNull(rejected.getPromotionUrl());
    }

    @Test
    @DisplayName("createActivity - url 跳转缺少有效公网地址时拒绝保存")
    void createActivity_rejectsInvalidUrlTarget() {
        CpsRebateActivitySaveReqVO reqVO = buildReqVO();
        reqVO.setJumpType("url");
        reqVO.setJumpUrl("http://localhost/activity");
        reqVO.setSearchKeyword(null);

        assertThrows(ServiceException.class, () -> service.createActivity(reqVO));
        verify(activityMapper, never()).insert(any(CpsRebateActivityDO.class));
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
