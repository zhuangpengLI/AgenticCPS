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
    @DisplayName("fetchActivities - 将好单库活动数字平台码归一化为系统平台码")
    void fetchActivities_normalizesNumericActivityPlatformCodes() {
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

        assertEquals(List.of("taobao", "jd", "pdd", "douyin", "meituan", "eleme", "local_life",
                "local_life", "taobao", "fliggy"), page.getList().stream()
                .map(CpsThirdPartyActivity::getPlatformCode)
                .toList());
        assertEquals("hdk:taobao:row-1", page.getList().get(0).getExternalActivityId());
        assertEquals("1-activity", page.getList().get(0).getPromotionActivityId());
        assertEquals("https://example.com/activity/row-1", page.getList().get(0).getJumpUrl());
        assertEquals("https://example.com/activity/row-1",
                page.getList().get(0).getExtraFields().get("activity_url"));
    }

    @Test
    @DisplayName("fetchActivities - 不使用请求平台覆盖好单库活动自身平台")
    void fetchActivities_doesNotOverrideActivityPlatformWithRequestPlatform() {
        when(hdkActivityClient.fetchCategories()).thenReturn(List.of(
                HdkActivityCategory.builder().catId(6).name("美团").build()));
        when(hdkActivityClient.fetchActivities(any())).thenReturn(HdkActivityPage.builder()
                .items(List.of(activity("6", "美团外卖节")))
                .build());

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(CpsThirdPartyActivityRequest.builder()
                .platformCode("taobao")
                .pageNo(1)
                .pageSize(20)
                .build(), null);

        assertEquals("meituan", page.getList().get(0).getPlatformCode());
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
