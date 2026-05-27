package com.qiji.cps.module.cps.service.activity;

import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyApiCategory;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.client.jutuike.JutuikeUnionVendorClient;
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

}
