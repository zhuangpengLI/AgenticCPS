package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityCategory;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityClient;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityItem;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkActivityPage;
import com.qiji.cps.module.cps.client.haodanku.activity.HdkSecondaryCategory;
import com.qiji.cps.module.cps.client.jutuike.JutuikeUnionVendorClient;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.dal.mysql.activity.CpsRebateActivityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateActivitySyncServiceImplTest {

    @InjectMocks
    private CpsRebateActivitySyncServiceImpl service;

    @Mock
    private HdkActivityClient hdkActivityClient;

    @Mock
    private HaodankuActivityVendorClient haodankuActivityVendorClient;

    @Mock
    private JutuikeUnionVendorClient jutuikeUnionVendorClient;

    @Mock
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Mock
    private CpsPlatformClientFactory platformClientFactory;

    @Mock
    private CpsRebateActivityMapper activityMapper;

    @Test
    @DisplayName("syncHaodankuActivities - 按平台分类分页同步并插入好单库活动")
    void syncHaodankuActivities_insertsPlatformActivityFromCategoryList() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder().catId(7).name("淘宝闪购").build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .countPage(1)
                .itemCount(58)
                .items(List.of(HdkActivityItem.builder()
                        .id("1677")
                        .activityId("7")
                        .activityPic("http://img.example/eleme.jpg")
                        .activityName("淘宝闪购外卖节")
                        .activityLabel("外卖")
                        .activityUrl("https://example.com/activity")
                        .startTime("2025-12-23 11:08:05")
                        .endTime("2027-12-31 11:08:05")
                        .platform("7")
                        .describe("结算说明：</br>官方账号推广统一结算")
                        .commissionRate("预估红包3%、页面：0.1%")
                        .promotionNum("708")
                        .promotionType("1")
                        .build()))
                .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId(eq("haodanku"), anyString())).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncHaodankuActivities(CpsRebateActivitySyncRequest.builder()
                .platformCode("eleme")
                .pageSize(20)
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        CpsRebateActivityDO saved = captor.getValue();
        assertEquals(1, result.getInsertedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals("淘宝闪购外卖节", saved.getActivityName());
        assertEquals("淘宝闪购", saved.getActivityType());
        assertEquals("eleme", saved.getPlatformCode());
        assertEquals("http://img.example/eleme.jpg", saved.getMainPic());
        assertEquals("官方账号推广统一结算", saved.getShortDesc());
        assertEquals("预估红包3%、页面：0.1%", saved.getRebateDesc());
        assertEquals("CPS", saved.getBillingType());
        assertEquals(708, saved.getPromotionCount());
        assertEquals("haodanku", saved.getSourceType());
        assertEquals("hdk:eleme:1677", saved.getExternalActivityId());
        assertEquals("7", saved.getPromotionActivityId());
        assertTrue(saved.getVendorMetadata().contains("activity_url"));
        assertEquals("外卖", saved.getTagText());
        assertEquals("url", saved.getJumpType());
        assertEquals("https://example.com/activity", saved.getJumpUrl());
        assertEquals("淘宝闪购外卖节", saved.getSearchKeyword());
        assertEquals(LocalDateTime.of(2025, 12, 23, 11, 8, 5), saved.getStartTime());
        assertEquals(LocalDateTime.of(2027, 12, 31, 11, 8, 5), saved.getEndTime());
    }

    @Test
    @DisplayName("syncHaodankuActivities - 没有已接入活动转链的平台只计为跳过")
    void syncHaodankuActivities_skipsPlatformWithoutImplementedConversion() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder().catId(6).name("美团").build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .countPage(1)
                .items(List.of(HdkActivityItem.builder()
                        .id("meituan-row")
                        .activityId("meituan-activity")
                        .activityName("美团外卖节")
                        .platform("6")
                        .build()))
                .build());

        CpsRebateActivitySyncResult result = service.syncHaodankuActivities(CpsRebateActivitySyncRequest.builder()
                .maxPages(1)
                .build());

        assertEquals(0, result.getInsertedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
        verify(activityMapper, never()).insert(any(CpsRebateActivityDO.class));
        verify(activityMapper, never()).updateById(any(CpsRebateActivityDO.class));
    }

    @Test
    @DisplayName("syncHaodankuActivities - 相同转链活动ID的不同平台记录不会互相覆盖")
    void syncHaodankuActivities_keepsRowsWithSharedPromotionActivityIdDistinct() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder().catId(1).name("热门").build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .countPage(1)
                .items(List.of(
                        HdkActivityItem.builder().id("row-taobao").activityId("shared-activity")
                                .activityName("淘宝活动").platform("1").build(),
                        HdkActivityItem.builder().id("row-eleme").activityId("shared-activity")
                                .activityName("淘宝闪购活动").platform("7").build()))
                .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId(anyString(), anyString())).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncHaodankuActivities(CpsRebateActivitySyncRequest.builder()
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper, times(2)).insert(captor.capture());
        assertEquals(2, result.getInsertedCount());
        assertEquals(List.of("hdk:taobao:row-taobao", "hdk:eleme:row-eleme"), captor.getAllValues().stream()
                .map(CpsRebateActivityDO::getExternalActivityId)
                .toList());
        assertEquals(List.of("shared-activity", "shared-activity"), captor.getAllValues().stream()
                .map(CpsRebateActivityDO::getPromotionActivityId)
                .toList());
    }

    @Test
    @DisplayName("syncHaodankuActivities - 二级分类活动按外部活动 ID 幂等更新")
    void syncHaodankuActivities_updatesExistingSecondaryCategoryActivity() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder()
                        .catId(7)
                        .name("淘宝闪购")
                        .secondaryCategories(List.of(HdkSecondaryCategory.builder()
                                .secondaryCatId(71)
                                .name("外卖会场")
                                .build()))
                        .build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .countPage(1)
                .itemCount(1)
                .items(List.of(HdkActivityItem.builder()
                        .id("2000")
                        .activityId("eleme-activity-1")
                        .activityName("淘宝闪购大额券")
                        .platform("7")
                        .commissionRate("最高10%")
                        .promotionNum("99")
                        .promotionType("3")
                        .build()))
                .build());
        CpsRebateActivityDO existing = CpsRebateActivityDO.builder()
                .id(99L)
                .activityName("旧名称")
                .sourceType("haodanku")
                .externalActivityId("hdk:eleme:2000")
                .build();
        when(activityMapper.selectBySourceTypeAndExternalActivityId("haodanku", "hdk:eleme:2000"))
                .thenReturn(existing);

        CpsRebateActivitySyncResult result = service.syncHaodankuActivities(CpsRebateActivitySyncRequest.builder()
                .platformCode("eleme")
                .maxPages(1)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).updateById(captor.capture());
        verify(activityMapper, never()).insert(any(CpsRebateActivityDO.class));
        CpsRebateActivityDO updated = captor.getValue();
        assertEquals(0, result.getInsertedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(99L, updated.getId());
        assertEquals("淘宝闪购大额券", updated.getActivityName());
        assertEquals("外卖会场", updated.getActivityType());
        assertEquals("eleme", updated.getPlatformCode());
        assertEquals("CPS+CPA", updated.getBillingType());
        assertEquals("search", updated.getJumpType());
        assertEquals("eleme-activity-1", updated.getPromotionActivityId());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - all 跳过没有已接入活动转链的好单库平台")
    void syncThirdPartyActivities_allSkipsHaodankuPlatformWithoutImplementedConversion() {
        CpsVendorConfig dataokeConfig = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("app-key")
                .appSecret("app-secret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build();
        when(dtkActivityVendorClient.getVendorCode()).thenReturn("dataoke");
        when(dtkActivityVendorClient.getPlatformCode()).thenReturn("taobao");
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(dataokeConfig);
        when(dtkActivityVendorClient.fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(dataokeConfig)))
                .thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .list(List.of(CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:10001")
                                .activityName("大淘客618会场")
                                .activityType("淘宝会场")
                                .platformCode("taobao")
                                .billingType("CPS")
                                .jumpType("search")
                                .searchKeyword("618")
                                .build()))
                        .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("dataoke", "dtk:10001")).thenReturn(null);

        when(haodankuActivityVendorClient.getVendorCode()).thenReturn("haodanku");
        when(haodankuActivityVendorClient.fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(null)))
                .thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .list(List.of(CpsThirdPartyActivity.builder()
                                .sourceType("haodanku")
                                .externalActivityId("hdk:7")
                                .activityName("好单库外卖活动")
                                .activityType("外卖")
                                .platformCode("meituan")
                                .billingType("CPS")
                                .jumpType("url")
                                .jumpUrl("https://example.com/hdk")
                                .build()))
                        .build());
        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("all")
                .platformCode("taobao")
                .keyword("618")
                .maxPages(1)
                .pageSize(20)
                .build());

        assertEquals(1, result.getInsertedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
        verify(dtkActivityVendorClient).fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(dataokeConfig));
        verify(haodankuActivityVendorClient).fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(null));
        verify(activityMapper, never()).selectBySourceTypeAndExternalActivityId("haodanku", "hdk:7");
    }

    @Test
    @DisplayName("syncThirdPartyActivities - 大淘客活动按外部活动 ID 幂等落表")
    void syncThirdPartyActivities_insertsDataokeActivity() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("app-key")
                .appSecret("app-secret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build();
        when(dtkActivityVendorClient.getVendorCode()).thenReturn("dataoke");
        when(dtkActivityVendorClient.getPlatformCode()).thenReturn("taobao");
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkActivityVendorClient.fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(config)))
                .thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .list(List.of(CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:10001")
                                .activityName("大淘客618会场")
                                .activityType("淘宝会场")
                                .platformCode("taobao")
                                .mainPic("https://img.example/dtk.jpg")
                                .shortDesc("官方热门活动")
                                .rebateDesc("以实际转链佣金为准")
                                .billingType("CPS")
                                .promotionCount(120)
                                .tagText("大淘客")
                                .jumpType("search")
                                .searchKeyword("618")
                                .startTime(LocalDateTime.of(2026, 6, 1, 0, 0))
                                .endTime(LocalDateTime.of(2026, 6, 30, 23, 59))
                                .build()))
                        .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("dataoke", "dtk:10001")).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .keyword("618")
                .maxPages(1)
                .pageSize(20)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).insert(captor.capture());
        CpsRebateActivityDO saved = captor.getValue();
        assertEquals(1, result.getInsertedCount());
        assertEquals("大淘客618会场", saved.getActivityName());
        assertEquals("淘宝会场", saved.getActivityType());
        assertEquals("taobao", saved.getPlatformCode());
        assertEquals("dataoke", saved.getSourceType());
        assertEquals("dtk:10001", saved.getExternalActivityId());
        assertEquals("search", saved.getJumpType());
        assertEquals("618", saved.getSearchKeyword());
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), saved.getStartTime());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - 大淘客同步用 legacyExternalActivityId 修正旧的错误拓展 ID")
    void syncThirdPartyActivities_updatesLegacyDataokeActivityId() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("app-key")
                .appSecret("app-secret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build();
        CpsRebateActivityDO legacy = CpsRebateActivityDO.builder()
                .id(88L)
                .sourceType("dataoke")
                .externalActivityId("dtk:119")
                .activityName("旧活动")
                .build();
        when(dtkActivityVendorClient.getVendorCode()).thenReturn("dataoke");
        when(dtkActivityVendorClient.getPlatformCode()).thenReturn("taobao");
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkActivityVendorClient.fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(config)))
                .thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .list(List.of(CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:20150318020023228")
                                .activityName("淘宝秒杀")
                                .activityType("淘宝会场")
                                .platformCode("taobao")
                                .billingType("CPS")
                                .extraFields(Map.of("legacyExternalActivityId", "dtk:119"))
                                .build()))
                        .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId("dataoke", "dtk:20150318020023228"))
                .thenReturn(null);
        when(activityMapper.selectBySourceTypeAndExternalActivityId("dataoke", "dtk:119"))
                .thenReturn(legacy);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .maxPages(1)
                .pageSize(20)
                .build());

        ArgumentCaptor<CpsRebateActivityDO> captor = ArgumentCaptor.forClass(CpsRebateActivityDO.class);
        verify(activityMapper).updateById(captor.capture());
        verify(activityMapper, never()).insert(any(CpsRebateActivityDO.class));
        CpsRebateActivityDO updated = captor.getValue();
        assertEquals(0, result.getInsertedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(88L, updated.getId());
        assertEquals("dtk:20150318020023228", updated.getExternalActivityId());
    }

    @Test
    @DisplayName("syncThirdPartyActivities - 不传最大页数时同步到第三方最后一页")
    void syncThirdPartyActivities_withoutMaxPagesFetchesUntilLastPage() {
        CpsVendorConfig config = CpsVendorConfig.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .appKey("app-key")
                .appSecret("app-secret")
                .apiBaseUrl("https://openapi.dataoke.com/api")
                .build();
        when(dtkActivityVendorClient.getVendorCode()).thenReturn("dataoke");
        when(dtkActivityVendorClient.getPlatformCode()).thenReturn("taobao");
        when(platformClientFactory.getVendorConfig("dataoke", "taobao")).thenReturn(config);
        when(dtkActivityVendorClient.fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(config)))
                .thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .total(2L)
                        .pageNo(1)
                        .pageSize(1)
                        .list(List.of(CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:page-1")
                                .activityName("第一页活动")
                                .activityType("淘宝会场")
                                .platformCode("taobao")
                                .billingType("CPS")
                                .build()))
                        .build())
                .thenReturn(CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                        .total(2L)
                        .pageNo(2)
                        .pageSize(1)
                        .list(List.of(CpsThirdPartyActivity.builder()
                                .sourceType("dataoke")
                                .externalActivityId("dtk:page-2")
                                .activityName("第二页活动")
                                .activityType("淘宝会场")
                                .platformCode("taobao")
                                .billingType("CPS")
                                .build()))
                        .build());
        when(activityMapper.selectBySourceTypeAndExternalActivityId(anyString(), anyString())).thenReturn(null);

        CpsRebateActivitySyncResult result = service.syncThirdPartyActivities(CpsRebateActivitySyncRequest.builder()
                .vendorCode("dataoke")
                .platformCode("taobao")
                .pageSize(1)
                .build());

        assertEquals(2, result.getInsertedCount());
        verify(dtkActivityVendorClient, times(2)).fetchActivities(any(CpsThirdPartyActivityRequest.class), eq(config));
    }

}
