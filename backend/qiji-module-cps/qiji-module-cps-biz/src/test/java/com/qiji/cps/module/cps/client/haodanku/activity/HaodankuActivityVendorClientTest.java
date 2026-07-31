package com.qiji.cps.module.cps.client.haodanku.activity;

import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HaodankuActivityVendorClientTest {

    @InjectMocks
    private HaodankuActivityVendorClient client;

    @Mock
    private HdkActivityClient hdkActivityClient;

    @Test
    @DisplayName("fetchActivities - 仅保留已接入官方活动转链的平台")
    void fetchActivities_keepsOnlyPlatformsWithImplementedActivityConversion() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder().catId(111).name("热门").build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .items(List.of(
                        activity("1", "淘宝会场"),
                        activity("2", "京东外卖"),
                        activity("3", "拼多多"),
                        activity("4", "抖音商城"),
                        activity("6", "美团外卖"),
                        activity("7", "淘宝闪购"),
                        activity("8", "肯德基"),
                        activity("10", "酒店"),
                        activity("15", "88VIP"),
                        activity("16", "飞猪旅行")))
                .build());

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(CpsThirdPartyActivityRequest.builder()
                .pageNo(1)
                .pageSize(20)
                .build(), null);

        assertEquals(List.of("taobao", "eleme", "taobao"), page.getList().stream()
                .map(CpsThirdPartyActivity::getPlatformCode)
                .toList());
        assertEquals("hdk:taobao:row-1", page.getList().get(0).getExternalActivityId());
        assertEquals("1-activity", page.getList().get(0).getPromotionActivityId());
        assertEquals("https://example.com/activity/row-1", page.getList().get(0).getJumpUrl());
        assertEquals("https://example.com/activity/row-1",
                page.getList().get(0).getExtraFields().get("activity_url"));
    }

    @Test
    @DisplayName("fetchActivities - 请求平台只返回同平台且可转链的好单库活动")
    void fetchActivities_filtersByRequestedSupportedPlatform() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder().catId(6).name("美团").build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .items(List.of(
                        activity("1", "淘宝会场"),
                        activity("7", "淘宝闪购"),
                        activity("6", "美团外卖节")))
                .build());

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(CpsThirdPartyActivityRequest.builder()
                .platformCode("taobao")
                .pageNo(1)
                .pageSize(20)
                .build(), null);

        assertEquals(1, page.getList().size());
        assertEquals("taobao", page.getList().get(0).getPlatformCode());
    }

    private HdkActivityItem activity(String platform, String activityName) {
        return HdkActivityItem.builder()
                .id("row-1")
                .activityId(platform + "-activity")
                .activityUrl("https://example.com/activity/row-1")
                .activityName(activityName)
                .platform(platform)
                .promotionType("1")
                .build();
    }
}
