package com.qiji.cps.module.cps.client.dataoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSyncReqVO;
import com.qiji.cps.module.cps.service.selection.CpsSelectionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DtkSelectionLibraryClientTest {

    private static class TestDtkSelectionLibraryClient extends DtkSelectionLibraryClient {

        private final JsonNode response;
        private String requestedUrlOrPath;
        private Map<String, Object> requestedParams;

        TestDtkSelectionLibraryClient(JsonNode response) {
            this.response = response;
        }

        @Override
        protected JsonNode executeRequest(String urlOrPath, Map<String, Object> params, CpsVendorConfig config) {
            this.requestedUrlOrPath = urlOrPath;
            this.requestedParams = params;
            return response;
        }
    }

    @Test
    @DisplayName("fetchThemes - 默认使用爆品清单接口并生成 爆品商品_二级主题名")
    void fetchThemes_usesScenePalletListAsSelectionThemes() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "msg": "查询成功",
                  "data": [
                    {"id": 2, "name": "淘金币玩法"},
                    {"id": 3, "name": "红包签到"}
                  ]
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/open-api/scene-pallet", client.requestedUrlOrPath);
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals(2, page.getList().size());
        assertEquals("dtk:scene_pallet:2", page.getList().get(0).getExternalActivityId());
        assertEquals("爆品商品_淘金币玩法", page.getList().get(0).getActivityName());
        assertEquals("/open-api/goods/scene-pallet", page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals(2L, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("id"));
    }

    @Test
    @DisplayName("fetchThemeGoods - 按主题规则中的商品列表 URL 和可选参数拉商品")
    void fetchThemeGoods_usesRuleGoodsUrlAndParams() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "total": 1,
                    "list": [
                      {
                        "sign": "encrypted-goods-id",
                        "goodsLink": "https://uland.taobao.com/item/edetail?id=encrypted-goods-id",
                        "title": "沐浴露3瓶保湿补水家庭装",
                        "pic": "https://img.example/goods.jpg",
                        "originalPrice": 38.84,
                        "postRollPrice": 20.84,
                        "commission": 37.2,
                        "ticketPrice": 18,
                        "storeName": "澳宝化妆品旗舰店",
                        "sales": 8000
                      }
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setVendorCode("dataoke");
        rule.setPlatforms(List.of("taobao"));
        rule.setVendorThemeSource("SCENE_PALLET");
        rule.setExternalThemeName("淘金币玩法");
        rule.setGoodsListUrl("/open-api/goods/scene-pallet");
        rule.setGoodsListParams(Map.of("version", "v1.0.0", "id", 2, "sortType", 4));

        var goodsList = client.fetchThemeGoods(rule, 20, CpsVendorConfig.builder().build());

        assertEquals("/open-api/goods/scene-pallet", client.requestedUrlOrPath);
        assertEquals(2, client.requestedParams.get("id"));
        assertEquals(4, client.requestedParams.get("sortType"));
        assertEquals(30, client.requestedParams.get("page_size"));
        assertEquals(1, goodsList.size());
        assertEquals("encrypted-goods-id", goodsList.get(0).getGoodsId());
        assertEquals("沐浴露3瓶保湿补水家庭装", goodsList.get(0).getTitle());
        assertEquals(new BigDecimal("20.84"), goodsList.get(0).getActualPrice());
        assertEquals(new BigDecimal("18"), goodsList.get(0).getCouponPrice());
        assertEquals("澳宝化妆品旗舰店", goodsList.get(0).getShopName());
    }

    @Test
    @DisplayName("fetchThemes - 采集群列表生成 采集群_群名 并配置 group-goods 参数")
    void fetchThemes_usesCollectGroupListAndGroupGoodsParams() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"id": 12, "name": "官方精推采集1群", "avgGoodsNum": "30-50个"}
                    ],
                    "total": 1
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();
        syncReqVO.setSourceCode("COLLECT_GROUP");

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/api/collect-group", client.requestedUrlOrPath);
        assertEquals("采集群_官方精推采集1群", page.getList().get(0).getActivityName());
        assertEquals("dtk:collect_group:12", page.getList().get(0).getExternalActivityId());
        assertEquals("/api/group-goods", page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals(12L, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("group_id"));
    }

    @Test
    @DisplayName("fetchThemeGoods - 采集群商品列表使用 page/size 分页并规范 size")
    void fetchThemeGoods_usesGroupGoodsPaging() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"signId": "encrypted-sign", "productId": "product-1", "dTitle": "伊利纯牛奶", "couponPrice": "¥11.9"}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setVendorCode("dataoke");
        rule.setPlatforms(List.of("taobao"));
        rule.setVendorThemeSource("COLLECT_GROUP");
        rule.setExternalThemeName("官方精推采集1群");
        rule.setGoodsListUrl("/api/group-goods");
        rule.setGoodsListParams(Map.of("version", "v1.0.0", "group_id", 12, "sort", 0));

        var goodsList = client.fetchThemeGoods(rule, 30, CpsVendorConfig.builder().build());

        assertEquals("/api/group-goods", client.requestedUrlOrPath);
        assertEquals(12, client.requestedParams.get("group_id"));
        assertEquals(1, client.requestedParams.get("page"));
        assertEquals(50, client.requestedParams.get("size"));
        assertEquals(1, goodsList.size());
        assertEquals("product-1", goodsList.get(0).getGoodsId());
        assertEquals("伊利纯牛奶", goodsList.get(0).getTitle());
        assertEquals(new BigDecimal("11.9"), goodsList.get(0).getActualPrice());
    }

    @Test
    @DisplayName("fetchThemes - 热门活动生成 热门活动_活动名 并配置 activity-goods 参数")
    void fetchThemes_usesHotActivityListAndActivityGoodsParams() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": [
                    {"activityId": 32549, "activityName": "双11定金预售", "goodsCount": 67}
                  ]
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();
        syncReqVO.setSourceCode("HOT_ACTIVITY");

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/api/goods/activity/catalogue", client.requestedUrlOrPath);
        assertEquals("热门活动_双11定金预售", page.getList().get(0).getActivityName());
        assertEquals("dtk:hot_activity:32549", page.getList().get(0).getExternalActivityId());
        assertEquals("/api/goods/activity/goods-list", page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals(32549L, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("activityId"));
    }

    @Test
    @DisplayName("fetchThemeGoods - 活动商品列表使用 pageId/pageSize 分页")
    void fetchThemeGoods_usesActivityGoodsPaging() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"goodsId": "589284195570", "title": "夏季睡衣男冰丝短袖", "actualPrice": 28.5}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setVendorCode("dataoke");
        rule.setPlatforms(List.of("taobao"));
        rule.setVendorThemeSource("HOT_ACTIVITY");
        rule.setExternalThemeName("双11定金预售");
        rule.setGoodsListUrl("/api/goods/activity/goods-list");
        rule.setGoodsListParams(Map.of("version", "v1.0.0", "activityId", 32549));

        var goodsList = client.fetchThemeGoods(rule, 120, CpsVendorConfig.builder().build());

        assertEquals("/api/goods/activity/goods-list", client.requestedUrlOrPath);
        assertEquals(32549, client.requestedParams.get("activityId"));
        assertEquals(1, client.requestedParams.get("pageId"));
        assertEquals(100, client.requestedParams.get("pageSize"));
        assertEquals(1, goodsList.size());
        assertEquals("589284195570", goodsList.get(0).getGoodsId());
        assertEquals("夏季睡衣男冰丝短袖", goodsList.get(0).getTitle());
    }

    @Test
    @DisplayName("fetchThemes - 专辑列表生成 专辑_标题 并配置 album goods 参数")
    void fetchThemes_usesAlbumListAndAlbumGoodsParams() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"id": 48728, "title": "近2小时热卖单品", "goodsCount": 50}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();
        syncReqVO.setSourceCode("ALBUM");

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(2)
                        .pageSize(50)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/api/album/album-list", client.requestedUrlOrPath);
        assertEquals(2, client.requestedParams.get("pageId"));
        assertEquals(50, client.requestedParams.get("pageSize"));
        assertEquals("专辑_近2小时热卖单品", page.getList().get(0).getActivityName());
        assertEquals("dtk:album:48728", page.getList().get(0).getExternalActivityId());
        assertEquals("/api/album/goods-list", page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals(48728L, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("albumId"));
    }

    @Test
    @DisplayName("fetchThemeGoods - 单个专辑商品列表从 goodlist 读取商品")
    void fetchThemeGoods_usesAlbumGoodlist() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "albumId": 48728,
                    "goodlist": [
                      {"goodsId": "594036262947", "title": "法丽兹夹心饼干", "mainPic": "https://img.example/a.jpg", "actualPrice": 10.9}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setVendorCode("dataoke");
        rule.setPlatforms(List.of("taobao"));
        rule.setVendorThemeSource("ALBUM");
        rule.setExternalThemeName("近2小时热卖单品");
        rule.setGoodsListUrl("/api/album/goods-list");
        rule.setGoodsListParams(Map.of("version", "v1.0.0", "albumId", 48728));

        var goodsList = client.fetchThemeGoods(rule, 20, CpsVendorConfig.builder().build());

        assertEquals("/api/album/goods-list", client.requestedUrlOrPath);
        assertEquals(48728, client.requestedParams.get("albumId"));
        assertEquals(1, goodsList.size());
        assertEquals("594036262947", goodsList.get(0).getGoodsId());
        assertEquals("法丽兹夹心饼干", goodsList.get(0).getTitle());
    }

    @Test
    @DisplayName("fetchThemes - 品牌栏目生成 品牌_品牌名 并配置 brand goods 参数")
    void fetchThemes_usesBrandColumnAndBrandGoodsParams() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"brandId": "94385", "brandName": "仁和", "goodsCount": 12}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();
        syncReqVO.setSourceCode("BRAND_COLUMN");

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/api/delanys/brand/get-column-list", client.requestedUrlOrPath);
        assertEquals(1, client.requestedParams.get("cid"));
        assertEquals("品牌_仁和", page.getList().get(0).getActivityName());
        assertEquals("dtk:brand_column:94385", page.getList().get(0).getExternalActivityId());
        assertEquals("/api/delanys/brand/get-goods-list", page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals("94385", ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("brandId"));
    }

    @Test
    @DisplayName("fetchThemes - 直接商品列表接口生成单主题并保存商品参数")
    void fetchThemes_createsSingleThemeForDirectGoodsSource() {
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(null);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();
        syncReqVO.setSourceCode("HIGH_COMMISSION");
        syncReqVO.setGoodsListParamsJson("{\"cid\":6}");

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals(1, page.getList().size());
        assertEquals("dtk:high_commission:default", page.getList().get(0).getExternalActivityId());
        assertEquals("高佣精选", page.getList().get(0).getActivityName());
        assertEquals("/api/goods/singlePage/list-height-commission",
                page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals(6, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("cid"));
        assertEquals(3, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("sort"));
        assertEquals(null, client.requestedUrlOrPath);
    }

    @Test
    @DisplayName("fetchThemeGoods - 直接商品列表接口使用 pageId/pageSize 分页")
    void fetchThemeGoods_usesDirectGoodsPageIdPaging() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"goodsId": "612415107604", "title": "少儿在线学习编程课", "actualPrice": 5.1}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setVendorCode("dataoke");
        rule.setPlatforms(List.of("taobao"));
        rule.setVendorThemeSource("HIGH_COMMISSION");
        rule.setExternalThemeName("高佣精选");
        rule.setGoodsListUrl("/api/goods/singlePage/list-height-commission");
        rule.setGoodsListParams(Map.of("version", "v1.0.0", "sort", 3));

        var goodsList = client.fetchThemeGoods(rule, 20, CpsVendorConfig.builder().build());

        assertEquals("/api/goods/singlePage/list-height-commission", client.requestedUrlOrPath);
        assertEquals(1, client.requestedParams.get("pageId"));
        assertEquals(20, client.requestedParams.get("pageSize"));
        assertEquals(1, goodsList.size());
        assertEquals("612415107604", goodsList.get(0).getGoodsId());
    }

    @Test
    @DisplayName("fetchThemeGoods - 线报接口从 itemIds 和图文内容生成商品快照")
    void fetchThemeGoods_mapsTipOffGoodsSnapshot() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {
                        "itemIds": "DRRqPdZfDt5bp3a3ONuQp0Cjtn-D3G9b6AsP79PYkBBcn",
                        "contentCopy": "淘口令请转链",
                        "picUrls": "https://img.example/tip.jpg",
                        "urls": "https://uland.taobao.com/item/edetail?id=abc"
                      }
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setVendorCode("dataoke");
        rule.setPlatforms(List.of("taobao"));
        rule.setVendorThemeSource("TIP_OFF");
        rule.setExternalThemeName("线报");
        rule.setGoodsListUrl("/api/dels/spider/list-tip-off");
        rule.setGoodsListParams(Map.of("version", "v1.0.0", "platform", 0));

        var goodsList = client.fetchThemeGoods(rule, 20, CpsVendorConfig.builder().build());

        assertEquals("/api/dels/spider/list-tip-off", client.requestedUrlOrPath);
        assertEquals(1, client.requestedParams.get("pageId"));
        assertEquals(20, client.requestedParams.get("pageSize"));
        assertEquals("DRRqPdZfDt5bp3a3ONuQp0Cjtn-D3G9b6AsP79PYkBBcn", goodsList.get(0).getGoodsId());
        assertEquals("淘口令请转链", goodsList.get(0).getTitle());
        assertEquals("https://img.example/tip.jpg", goodsList.get(0).getMainPic());
    }

    @Test
    @DisplayName("fetchThemes - 细分类目合集生成细分类目主题并配置榜单参数")
    void fetchThemes_usesSubdivisionListAndRankParams() throws Exception {
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": 0,
                  "data": {
                    "list": [
                      {"id": 20, "name": "螺蛳粉", "totalNum": 23}
                    ]
                  }
                }
                """);
        TestDtkSelectionLibraryClient client = new TestDtkSelectionLibraryClient(response);
        CpsSelectionThemeSyncReqVO syncReqVO = new CpsSelectionThemeSyncReqVO();
        syncReqVO.setSourceCode("SUBDIVISION");

        var page = client.fetchThemes(syncReqVO,
                CpsThirdPartyActivityRequest.builder()
                        .platformCode("taobao")
                        .pageNo(1)
                        .pageSize(20)
                        .build(),
                CpsVendorConfig.builder().build());

        assertEquals("/api/subdivision/get-list", client.requestedUrlOrPath);
        assertEquals("细分类目_螺蛳粉", page.getList().get(0).getActivityName());
        assertEquals("/api/subdivision/get-rank-list", page.getList().get(0).getExtraFields().get("goodsListUrl"));
        assertEquals(20L, ((Map<?, ?>) page.getList().get(0).getExtraFields().get("goodsListParams")).get("subdivisionId"));
    }
}
