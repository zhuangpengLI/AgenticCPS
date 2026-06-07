package com.qiji.cps.module.cps.client.dataoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DtkActivityVendorClientTest {

    private static class TestDtkActivityVendorClient extends DtkActivityVendorClient {

        private final JsonNode response;

        private String requestedPath;
        private Map<String, Object> requestedParams;

        TestDtkActivityVendorClient(JsonNode response) {
            this.response = response;
        }

        @Override
        protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
            this.requestedPath = path;
            this.requestedParams = params;
            return response;
        }
    }

    @Test
    @DisplayName("fetchActivities - 解析大淘客热门活动为统一活动对象")
    void fetchActivities_parsesActivityCatalogue() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "list": [
                      {
                        "activityId": "10001",
                        "activityName": "618淘宝主会场",
                        "activityType": "淘宝会场",
                        "activityPic": "https://img.example/activity.jpg",
                        "activityDesc": "年中大促官方会场",
                        "activityUrl": "https://uland.taobao.com/",
                        "goodsCount": 88,
                        "startTime": "2026-06-01 00:00:00",
                        "endTime": "2026-06-30 23:59:59"
                      }
                    ],
                    "totalCount": 1
                  }
                }
                """);
        TestDtkActivityVendorClient client = new TestDtkActivityVendorClient(response);

        CpsThirdPartyPage<CpsThirdPartyActivity> page = client.fetchActivities(
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .keyword("618")
                        .pageNo(2)
                        .pageSize(30)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/goods/activity/catalogue", client.requestedPath);
        assertEquals(2, client.requestedParams.get("pageId"));
        assertEquals(30, client.requestedParams.get("pageSize"));
        assertEquals("v1.2.0", client.requestedParams.get("version"));
        assertEquals(1, page.getList().size());
        CpsThirdPartyActivity activity = page.getList().get(0);
        assertEquals("dataoke", activity.getSourceType());
        assertEquals("dtk:10001", activity.getExternalActivityId());
        assertEquals("618淘宝主会场", activity.getActivityName());
        assertEquals("淘宝会场", activity.getActivityType());
        assertEquals("taobao", activity.getPlatformCode());
        assertEquals("https://img.example/activity.jpg", activity.getMainPic());
        assertEquals("年中大促官方会场", activity.getShortDesc());
        assertEquals("以实际转链佣金为准", activity.getRebateDesc());
        assertEquals("CPS", activity.getBillingType());
        assertEquals(88, activity.getPromotionCount());
        assertEquals("url", activity.getJumpType());
        assertEquals("https://uland.taobao.com/", activity.getJumpUrl());
        assertEquals("618淘宝主会场", activity.getSearchKeyword());
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), activity.getStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 30, 23, 59, 59), activity.getEndTime());
    }
}
