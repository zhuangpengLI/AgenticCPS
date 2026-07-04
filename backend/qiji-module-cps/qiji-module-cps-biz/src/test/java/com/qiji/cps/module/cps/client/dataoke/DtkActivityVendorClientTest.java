package com.qiji.cps.module.cps.client.dataoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
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

        int testResolveTimeout(String path) {
            return resolveTimeout(path);
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
                        "id": 69,
                        "promotionSceneId": "20150318020023228",
                        "activityName": "618淘宝主会场",
                        "activityType": "淘宝会场",
                        "materialLink": "https://img.example/activity.jpg",
                        "activityInfo": "年中大促官方会场",
                        "activityLink": "https://uland.taobao.com/",
                        "goodsCount": 88,
                        "activityStartTime": "2026-06-01 00:00:00",
                        "activityEndTime": "2026-06-30 23:59:59"
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

        assertEquals("/category/get-tb-topic-list", client.requestedPath);
        assertEquals(2, client.requestedParams.get("pageId"));
        assertEquals(30, client.requestedParams.get("pageSize"));
        assertEquals(0, client.requestedParams.get("type"));
        assertEquals("0", client.requestedParams.get("channelId"));
        assertEquals("v1.2.0", client.requestedParams.get("version"));
        assertEquals(1, page.getList().size());
        CpsThirdPartyActivity activity = page.getList().get(0);
        assertEquals("dataoke", activity.getSourceType());
        assertEquals("dtk:20150318020023228", activity.getExternalActivityId());
        assertEquals("dtk:69", activity.getExtraFields().get("legacyExternalActivityId"));
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

    @Test
    @DisplayName("fetchActivities - 跳过没有 promotionSceneId 的大淘客活动，避免错误 id 落库")
    void fetchActivities_skipsDataokeActivityWithoutPromotionSceneId() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "list": [
                      {
                        "id": 119,
                        "activityId": "119",
                        "activityName": "缺少转链场景 ID 的活动",
                        "activityLink": "https://uland.taobao.com/"
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
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals(0, page.getList().size());
    }

    @Test
    @DisplayName("generateActivityLink - 调用大淘客官方活动会场转链接口")
    void generateActivityLink_usesOfficialActivityLinkApi() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "click_url": "https://s.click.taobao.com/abc",
                    "Tpwd": "￥ABC123￥",
                    "longTpwd": "￥LONG123￥",
                    "page_name": "淘宝官方会场"
                  }
                }
                """);
        TestDtkActivityVendorClient client = new TestDtkActivityVendorClient(response);
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("10001");
        request.setAdzoneId("mm_1_2_3");
        request.setChannelId("relation-1");
        request.setExternalId("union-1");

        CpsPromotionLinkResult result = client.generateActivityLink(request, CpsVendorConfig.builder().build());

        assertEquals("/tb-service/activity-link", client.requestedPath);
        assertEquals("10001", client.requestedParams.get("promotionSceneId"));
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals("mm_1_2_3", client.requestedParams.get("pid"));
        assertEquals("relation-1", client.requestedParams.get("relationId"));
        assertEquals("union-1", client.requestedParams.get("unionId"));
        assertEquals("https://s.click.taobao.com/abc", result.getShortUrl());
        assertEquals("https://s.click.taobao.com/abc", result.getLongUrl());
        assertEquals("￥ABC123￥", result.getTpwd());
        assertEquals("￥LONG123￥", result.getExtraFields().get("longTpwd"));
    }

    @Test
    @DisplayName("resolveTimeout - 官方活动转链接口使用更长超时等待大淘客响应")
    void resolveTimeout_usesLongerTimeoutForOfficialActivityLink() {
        TestDtkActivityVendorClient client = new TestDtkActivityVendorClient(null);

        assertEquals(10000, client.testResolveTimeout("/tb-service/activity-link"));
        assertEquals(5000, client.testResolveTimeout("/category/get-tb-topic-list"));
    }
}
