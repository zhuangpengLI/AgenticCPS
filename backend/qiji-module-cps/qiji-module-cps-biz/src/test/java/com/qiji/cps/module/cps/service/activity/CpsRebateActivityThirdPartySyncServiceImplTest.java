package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyApiCategory;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.client.jutuike.JutuikeUnionVendorClient;
import com.qiji.cps.module.cps.client.official.jd.JdOfficialVendorClient;
import com.qiji.cps.module.cps.client.official.meituan.MeituanOfficialVendorClient;
import com.qiji.cps.module.cps.client.csj.CsjDouyinVendorClient;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateActivityThirdPartySyncServiceImplTest {

    @InjectMocks
    private CpsRebateActivitySyncServiceImpl service;

    @Mock
    private HaodankuActivityVendorClient haodankuActivityVendorClient;

    @Mock
    private JutuikeUnionVendorClient jutuikeUnionVendorClient;

    @Mock
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Mock
    private MeituanOfficialVendorClient meituanOfficialVendorClient;

    @Mock
    private CsjDouyinVendorClient csjDouyinVendorClient;
    @Mock
    private JdOfficialVendorClient jdOfficialVendorClient;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private CpsRebateActivityMapper activityMapper;

    @Test
    @DisplayName("syncThirdPartyActivities - 按供应商统一适配拉取并同步聚推客活动")
    void syncThirdPartyActivities_insertsJutuikeActivity() {
        when(jutuikeUnionVendorClient.getVendorCode()).thenReturn("jutuike");
        when(jutuikeUnionVendorClient.getPlatformCode()).thenReturn("union");
        when(platformClientFactory.getVendorConfig("jutuike", "union")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("jutuike")
                .platformCode("union")
                .appKey("test-key")
                .apiBaseUrl("http://api.jutuike.com")
                .build());
        when(jutuikeUnionVendorClient.fetchActivities(any(), any())).thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(List.of(CpsThirdPartyActivity.builder()
                        .sourceType("jutuike")
                        .externalActivityId("jtk:7")
                        .activityName("美团外卖")
                        .activityType("美团")
                        .platformCode("meituan")
                        .mainPic("http://img.example/meituan.jpg")
                        .shortDesc("活动说明")
                        .rebateDesc("T+1")
                        .billingType("CPS")
                        .promotionCount(58)
                        .tagText("美团")
                        .jumpType("url")
                        .jumpUrl("https://example.com/activity")
                        .searchKeyword("美团外卖")
                        .startTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                        .endTime(LocalDateTime.of(2026, 12, 31, 23, 59))
                        .build()))
                .pageNo(1)
                .pageSize(20)
                .total(1L)
                .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("jutuike", "jtk:7")).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("jutuike")
                .platformCode("meituan")
                .pageSize(20)
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        CpsRebateActivityDO saved = captor.getValue();
        assertEquals(1, result.getInsertedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals("美团外卖", saved.getActivityName());
        assertEquals("美团", saved.getActivityType());
        assertEquals("meituan", saved.getPlatformCode());
        assertEquals("jtk:7", saved.getExternalActivityId());
        assertEquals("T+1", saved.getRebateDesc());
        assertEquals("url", saved.getJumpType());
        assertEquals("https://example.com/activity", saved.getJumpUrl());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - all 同步同时覆盖大淘客、好单库、聚推客、美团官方和穿山甲抖音")
    void syncThirdPartyActivities_allIncludesJutuike() {
        when(dtkActivityVendorClient.getVendorCode()).thenReturn("dataoke");
        when(dtkActivityVendorClient.getPlatformCode()).thenReturn("taobao");
        when(haodankuActivityVendorClient.getVendorCode()).thenReturn("haodanku");
        when(haodankuActivityVendorClient.getPlatformCode()).thenReturn("activity");
        when(jutuikeUnionVendorClient.getVendorCode()).thenReturn("jutuike");
        when(jutuikeUnionVendorClient.getPlatformCode()).thenReturn("union");
        when(meituanOfficialVendorClient.getVendorCode()).thenReturn("official");
        when(meituanOfficialVendorClient.getPlatformCode()).thenReturn("meituan");
        when(csjDouyinVendorClient.getVendorCode()).thenReturn("csj");
        when(csjDouyinVendorClient.getPlatformCode()).thenReturn("douyin");
        when(jdOfficialVendorClient.getVendorCode()).thenReturn("official");
        when(jdOfficialVendorClient.getPlatformCode()).thenReturn("jd");

        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("dtk-key")
                .appSecret("dtk-secret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build());
        when(platformClientFactory.getVendorConfig("haodanku", "activity")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("haodanku")
                .platformCode("activity")
                .appKey("hdk-key")
                .apiBaseUrl("https://www.haodanku.com/openapi")
                .build());
        when(platformClientFactory.getVendorConfig("jutuike", "union")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("jutuike")
                .platformCode("union")
                .appKey("jtk-key")
                .apiBaseUrl("http://api.jutuike.com")
                .build());
        when(platformClientFactory.getVendorConfig("official", "meituan")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("official")
                .platformCode("meituan")
                .appKey("mt-key")
                .appSecret("mt-secret")
                .apiBaseUrl("https://openapi.meituan.com")
                .build());
        when(platformClientFactory.getVendorConfig("csj", "douyin")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("csj")
                .platformCode("douyin")
                .appKey("csj-key")
                .appSecret("csj-secret")
                .apiBaseUrl("https://ecom.pangolin-sdk-toutiao.com")
                .build());
        when(platformClientFactory.getVendorConfig("official", "jd")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("official")
                .platformCode("jd")
                .appKey("jd-key")
                .appSecret("jd-secret")
                .build());

        when(dtkActivityVendorClient.fetchActivities(any(), any())).thenReturn(emptyPage());
        when(haodankuActivityVendorClient.fetchActivities(any(), any())).thenReturn(emptyPage());
        when(jutuikeUnionVendorClient.fetchActivities(any(), any())).thenReturn(emptyPage());
        when(meituanOfficialVendorClient.fetchActivities(any(), any())).thenReturn(emptyPage());
        when(csjDouyinVendorClient.fetchActivities(any(), any())).thenReturn(emptyPage());
        when(jdOfficialVendorClient.fetchActivities(any(), any())).thenReturn(emptyPage());

        service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("all")
                .pageSize(20)
                .maxPages(1)
                .build());

        verify(dtkActivityVendorClient).fetchActivities(any(), any());
        verify(haodankuActivityVendorClient).fetchActivities(any(), any());
        verify(jutuikeUnionVendorClient).fetchActivities(any(), any());
        verify(meituanOfficialVendorClient).fetchActivities(any(), any());
        verify(csjDouyinVendorClient).fetchActivities(any(), any());
        verify(jdOfficialVendorClient).fetchActivities(any(), any());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - 拉取并同步美团官方推广活动")
    void syncThirdPartyActivities_insertsMeituanOfficialActivity() {
        when(meituanOfficialVendorClient.getVendorCode()).thenReturn("official");
        when(meituanOfficialVendorClient.getPlatformCode()).thenReturn("meituan");
        when(platformClientFactory.getVendorConfig("official", "meituan")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("official")
                .platformCode("meituan")
                .appKey("mt-key")
                .appSecret("mt-secret")
                .apiBaseUrl("https://openapi.meituan.com")
                .build());
        when(meituanOfficialVendorClient.fetchActivities(any(), any())).thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(List.of(CpsThirdPartyActivity.builder()
                        .sourceType("official")
                        .externalActivityId("33")
                        .activityName("美团外卖天天神券")
                        .activityType("外卖神券")
                        .platformCode("meituan")
                        .mainPic("https://img.meituan.net/waimai.png")
                        .shortDesc("天天神券大额满减红包")
                        .rebateDesc("最高返 6%")
                        .billingType("CPS")
                        .promotionCount(100)
                        .tagText("美团外卖")
                        .jumpType("url")
                        .jumpUrl("https://click.meituan.com/t?sm=33")
                        .searchKeyword("美团外卖")
                        .build()))
                .pageNo(1)
                .pageSize(20)
                .total(1L)
                .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("official", "33")).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("official")
                .platformCode("meituan")
                .pageSize(20)
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        CpsRebateActivityDO saved = captor.getValue();
        assertEquals(1, result.getInsertedCount());
        assertEquals("美团外卖天天神券", saved.getActivityName());
        assertEquals("official", saved.getSourceType());
        assertEquals("meituan", saved.getPlatformCode());
        assertEquals("33", saved.getExternalActivityId());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - 拉取并同步穿山甲抖音官方推广活动")
    void syncThirdPartyActivities_insertsCsjDouyinActivity() {
        when(csjDouyinVendorClient.getVendorCode()).thenReturn("csj");
        when(csjDouyinVendorClient.getPlatformCode()).thenReturn("douyin");
        when(platformClientFactory.getVendorConfig("csj", "douyin")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("csj")
                .platformCode("douyin")
                .appKey("csj-key")
                .appSecret("csj-secret")
                .apiBaseUrl("https://ecom.pangolin-sdk-toutiao.com")
                .build());
        when(csjDouyinVendorClient.fetchActivities(any(), any())).thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(List.of(CpsThirdPartyActivity.builder()
                        .sourceType("csj")
                        .externalActivityId("1001")
                        .activityName("抖音电商超值购")
                        .activityType("官方大促")
                        .platformCode("douyin")
                        .mainPic("https://p3.douyinpic.com/chaozhigou.png")
                        .shortDesc("官方正品大额补贴")
                        .rebateDesc("最高返 15%")
                        .billingType("CPS")
                        .promotionCount(200)
                        .tagText("超值购")
                        .jumpType("url")
                        .jumpUrl("https://haohuo.jinritemai.com/views/material/index?material_id=1001")
                        .searchKeyword("超值购")
                        .build()))
                .pageNo(1)
                .pageSize(20)
                .total(1L)
                .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("csj", "1001")).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("csj")
                .platformCode("douyin")
                .pageSize(20)
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        CpsRebateActivityDO saved = captor.getValue();
        assertEquals(1, result.getInsertedCount());
        assertEquals("抖音电商超值购", saved.getActivityName());
        assertEquals("csj", saved.getSourceType());
        assertEquals("douyin", saved.getPlatformCode());
        assertEquals("1001", saved.getExternalActivityId());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - 支持京东官方活动同步")
    void syncThirdPartyActivities_insertsJdOfficialActivity() {
        when(jdOfficialVendorClient.getVendorCode()).thenReturn("official");
        when(jdOfficialVendorClient.getPlatformCode()).thenReturn("jd");
        when(platformClientFactory.getVendorConfig("official", "jd")).thenReturn(CpsVendorConfig.builder()
                .vendorCode("official")
                .platformCode("jd")
                .appKey("jd-key")
                .appSecret("jd-secret")
                .build());
        when(jdOfficialVendorClient.fetchActivities(any(), any())).thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(List.of(CpsThirdPartyActivity.builder()
                        .sourceType("official")
                        .externalActivityId("jd:official:307006")
                        .activityName("腕表大促品类日")
                        .activityType("营销会场")
                        .platformCode("jd")
                        .mainPic("https://img.jd.com/banner.png")
                        .shortDesc("五折品限时抢购")
                        .rebateDesc("京东官方高额佣金")
                        .billingType("CPS")
                        .jumpType("url")
                        .jumpUrl("https://pro.m.jd.com/mall/active/xxx/index.html")
                        .startTime(LocalDateTime.of(2026, 10, 1, 0, 0))
                        .endTime(LocalDateTime.of(2026, 11, 1, 0, 0))
                        .build()))
                .pageNo(1)
                .pageSize(20)
                .total(1L)
                .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("official", "jd:official:307006")).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("official")
                .platformCode("jd")
                .pageSize(20)
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        CpsRebateActivityDO saved = captor.getValue();
        assertEquals(1, result.getInsertedCount());
        assertEquals("腕表大促品类日", saved.getActivityName());
        assertEquals("official", saved.getSourceType());
        assertEquals("jd", saved.getPlatformCode());
        assertEquals("jd:official:307006", saved.getExternalActivityId());
    }

    private CpsThirdPartyPage<CpsThirdPartyActivity> emptyPage() {
        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(List.of())
                .pageNo(1)
                .pageSize(20)
                .total(0L)
                .build();
    }

}
