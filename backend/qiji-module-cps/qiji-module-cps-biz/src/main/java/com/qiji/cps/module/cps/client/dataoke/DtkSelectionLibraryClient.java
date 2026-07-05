package com.qiji.cps.module.cps.client.dataoke;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSyncReqVO;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.service.selection.CpsSelectionRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

/**
 * 大淘客选品库/货盘类接口客户端。
 *
 * <p>这里刻意不复用官方活动接口；选品库主题必须来自可继续拉取商品列表的货盘/清单源。</p>
 */
@Slf4j
@Component
public class DtkSelectionLibraryClient {

    public static final String SOURCE_SCENE_PALLET = "SCENE_PALLET";
    public static final String SOURCE_COLLECT_GROUP = "COLLECT_GROUP";
    public static final String SOURCE_HOT_ACTIVITY = "HOT_ACTIVITY";
    public static final String SOURCE_ALBUM = "ALBUM";
    public static final String SOURCE_BRAND_COLUMN = "BRAND_COLUMN";
    public static final String SOURCE_SUBDIVISION = "SUBDIVISION";
    public static final String SOURCE_CUSTOM = "CUSTOM";

    private static final int HTTP_TIMEOUT = 10000;
    private static final String DEFAULT_BASE_URL = "https://openapi.dataoke.com/api";
    private static final String SOURCE_DATAOKE = "dataoke";
    private static final String EXTERNAL_PREFIX = "dtk:";

    private final ObjectMapper objectMapper;

    public DtkSelectionLibraryClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CpsThirdPartyPage<CpsThirdPartyActivity> fetchThemes(CpsSelectionThemeSyncReqVO syncReqVO,
                                                                CpsThirdPartyActivityRequest request,
                                                                CpsVendorConfig config) {
        SourceConfig sourceConfig = resolveSourceConfig(syncReqVO);
        sourceConfig = sourceConfig.withParams(
                mergeParams(sourceConfig.themeListParams(), syncReqVO.getThemeListParamsJson()),
                mergeParams(sourceConfig.goodsListParams(), syncReqVO.getGoodsListParamsJson()));
        if (sourceConfig.directGoodsTheme()) {
            CpsThirdPartyActivity theme = buildDirectGoodsTheme(sourceConfig, request);
            List<CpsThirdPartyActivity> list = keywordMatched(theme, request.getKeyword()) ? List.of(theme) : List.of();
            return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                    .list(list)
                    .total((long) list.size())
                    .pageNo(defaultInt(request.getPageNo(), 1))
                    .pageSize(defaultInt(request.getPageSize(), 20))
                    .build();
        }
        Map<String, Object> params = sourceConfig.themeListParams();
        applyThemePageParams(params, sourceConfig.themeListUrl(), request);
        JsonNode response = executeRequest(sourceConfig.themeListUrl(), params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                    .list(List.of())
                    .total(0L)
                    .pageNo(defaultInt(request.getPageNo(), 1))
                    .pageSize(defaultInt(request.getPageSize(), 20))
                    .rawPayload(toRawPayload(response))
                    .build();
        }

        JsonNode data = response.path("data");
        JsonNode list = resolveListNode(data);
        List<CpsThirdPartyActivity> themes = new ArrayList<>();
        if (list != null && list.isArray()) {
            for (JsonNode item : list) {
                CpsThirdPartyActivity theme = parseTheme(item, request, sourceConfig);
                if (theme != null && keywordMatched(theme, request.getKeyword())) {
                    themes.add(theme);
                }
            }
        }
        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .list(themes)
                .total(firstLong(data, "total", "totalCount", "count", themes.size()))
                .pageNo(defaultInt(request.getPageNo(), 1))
                .pageSize(defaultInt(request.getPageSize(), 20))
                .rawPayload(toRawPayload(response))
                .build();
    }

    public List<CpsGoodsSquareGoodsRespVO> fetchThemeGoods(CpsSelectionRule rule, int pullCount,
                                                           CpsVendorConfig config) {
        if (rule == null || !StringUtils.hasText(rule.getGoodsListUrl())) {
            return List.of();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        if (rule.getGoodsListParams() != null) {
            params.putAll(rule.getGoodsListParams());
        }
        applyPageParams(params, rule.getGoodsListUrl(), pullCount);
        JsonNode response = executeRequest(rule.getGoodsListUrl(), params, config);
        if (response == null || !isSuccessResponse(response)) {
            return List.of();
        }
        JsonNode list = resolveListNode(response.path("data"));
        List<CpsGoodsSquareGoodsRespVO> goodsList = new ArrayList<>();
        if (list != null && list.isArray()) {
            for (JsonNode item : list) {
                CpsGoodsSquareGoodsRespVO goods = parseGoods(item, rule);
                if (goods != null) {
                    goodsList.add(goods);
                }
                if (goodsList.size() >= pullCount) {
                    break;
                }
            }
        }
        return goodsList;
    }

    protected JsonNode executeRequest(String urlOrPath, Map<String, Object> params, CpsVendorConfig config) {
        Map<String, Object> allParams = new LinkedHashMap<>(params == null ? Map.of() : params);
        allParams.putIfAbsent("version", "v1.0.0");
        injectSignParams(allParams, config);
        allParams.entrySet().removeIf(entry -> entry.getValue() == null);

        String fullUrl = buildUrlWithParams(resolveUrl(urlOrPath, config), allParams);
        try {
            HttpResponse response = HttpRequest.get(fullUrl).timeout(HTTP_TIMEOUT).execute();
            String body = response.body();
            log.debug("[dataoke:selection] 请求: {} params={} 响应: {}", urlOrPath, params, body);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("[dataoke:selection] 请求异常: urlOrPath={}", urlOrPath, e);
            return null;
        }
    }

    private SourceConfig resolveSourceConfig(CpsSelectionThemeSyncReqVO reqVO) {
        String sourceCode = firstText(reqVO.getSourceCode(), SOURCE_SCENE_PALLET).toUpperCase();
        if (SOURCE_COLLECT_GROUP.equals(sourceCode)) {
            return new SourceConfig(
                    SOURCE_COLLECT_GROUP,
                    firstText(reqVO.getThemeNamePrefix(), "采集群"),
                    firstText(reqVO.getThemeListUrl(), "/api/collect-group"),
                    firstText(reqVO.getGoodsListUrl(), "/api/group-goods"),
                    "group_id",
                    mapOf("version", "v1.0.0", "platform", 0, "sort", 0),
                    mapOf("version", "v1.0.0", "sort", 0),
                    false
            );
        }
        if (SOURCE_HOT_ACTIVITY.equals(sourceCode)) {
            return new SourceConfig(
                    SOURCE_HOT_ACTIVITY,
                    firstText(reqVO.getThemeNamePrefix(), "热门活动"),
                    firstText(reqVO.getThemeListUrl(), "/api/goods/activity/catalogue"),
                    firstText(reqVO.getGoodsListUrl(), "/api/goods/activity/goods-list"),
                    "activityId",
                    mapOf("version", "v1.0.0"),
                    mapOf("version", "v1.0.0"),
                    false
            );
        }
        if (SOURCE_ALBUM.equals(sourceCode)) {
            return new SourceConfig(
                    SOURCE_ALBUM,
                    firstText(reqVO.getThemeNamePrefix(), "专辑"),
                    firstText(reqVO.getThemeListUrl(), "/api/album/album-list"),
                    firstText(reqVO.getGoodsListUrl(), "/api/album/goods-list"),
                    "albumId",
                    mapOf("version", "v1.0.0", "albumType", 0, "sort", 0),
                    mapOf("version", "v1.0.0"),
                    false
            );
        }
        if (SOURCE_BRAND_COLUMN.equals(sourceCode)) {
            return new SourceConfig(
                    SOURCE_BRAND_COLUMN,
                    firstText(reqVO.getThemeNamePrefix(), "品牌"),
                    firstText(reqVO.getThemeListUrl(), "/api/delanys/brand/get-column-list"),
                    firstText(reqVO.getGoodsListUrl(), "/api/delanys/brand/get-goods-list"),
                    "brandId",
                    mapOf("version", "v1.0.0", "cid", 1),
                    mapOf("version", "v1.0.0"),
                    false
            );
        }
        if (SOURCE_SUBDIVISION.equals(sourceCode)) {
            return new SourceConfig(
                    SOURCE_SUBDIVISION,
                    firstText(reqVO.getThemeNamePrefix(), "细分类目"),
                    firstText(reqVO.getThemeListUrl(), "/api/subdivision/get-list"),
                    firstText(reqVO.getGoodsListUrl(), "/api/subdivision/get-rank-list"),
                    "subdivisionId",
                    mapOf("version", "v1.0.0", "cid", 6),
                    mapOf("version", "v1.0.0"),
                    false
            );
        }
        SourceConfig directGoodsSource = resolveDirectGoodsSource(reqVO, sourceCode);
        if (directGoodsSource != null) {
            return directGoodsSource;
        }
        if (SOURCE_CUSTOM.equals(sourceCode)) {
            return new SourceConfig(
                    SOURCE_CUSTOM,
                    firstText(reqVO.getThemeNamePrefix(), "自定义货盘"),
                    firstText(reqVO.getThemeListUrl(), "/open-api/scene-pallet"),
                    firstText(reqVO.getGoodsListUrl(), "/open-api/goods/scene-pallet"),
                    "id",
                    mapOf("version", "v1.0.0"),
                    mapOf("version", "v1.0.0", "sortType", 4),
                    false
            );
        }
        return new SourceConfig(
                SOURCE_SCENE_PALLET,
                firstText(reqVO.getThemeNamePrefix(), "爆品商品"),
                firstText(reqVO.getThemeListUrl(), "/open-api/scene-pallet"),
                firstText(reqVO.getGoodsListUrl(), "/open-api/goods/scene-pallet"),
                "id",
                mapOf("version", "v1.0.0"),
                mapOf("version", "v1.0.0", "sortType", 4),
                false
        );
    }

    private SourceConfig resolveDirectGoodsSource(CpsSelectionThemeSyncReqVO reqVO, String sourceCode) {
        DirectGoodsPreset preset = directGoodsPresets().get(sourceCode);
        if (preset == null) {
            return null;
        }
        return new SourceConfig(
                sourceCode,
                firstText(reqVO.getThemeNamePrefix(), preset.themeName()),
                null,
                firstText(reqVO.getGoodsListUrl(), preset.goodsListUrl()),
                null,
                Map.of(),
                preset.goodsListParams(),
                true
        );
    }

    private Map<String, DirectGoodsPreset> directGoodsPresets() {
        Map<String, DirectGoodsPreset> presets = new LinkedHashMap<>();
        presets.put("TIP_OFF", new DirectGoodsPreset("线报", "/api/dels/spider/list-tip-off",
                mapOf("version", "v1.0.0", "platform", 0)));
        presets.put("HIGH_COMMISSION", new DirectGoodsPreset("高佣精选",
                "/api/goods/singlePage/list-height-commission", mapOf("version", "v1.0.0", "sort", 3)));
        presets.put("LIVE_RECOMMEND", new DirectGoodsPreset("热门主播力荐商品", "/api/live/goods-list",
                mapOf("version", "v1.0.0")));
        presets.put("HISTORY_LOW_PRICE", new DirectGoodsPreset("历史新低商品合集",
                "/api/goods/get-history-low-price-list", mapOf("version", "v1.0.0", "sort", 0)));
        presets.put("SUPER_DISCOUNT", new DirectGoodsPreset("折上折", "/api/goods/super-discount-goods",
                mapOf("version", "v1.0.0", "sort", 0)));
        presets.put("HALF_PRICE_DAY", new DirectGoodsPreset("每日低价抢购", "/api/goods/get-half-price-day",
                mapOf("version", "v1.0.0", "sessions", 1)));
        presets.put("DAILY_EXPLOSIVE", new DirectGoodsPreset("每日爆品推荐", "/api/goods/explosive-goods-list",
                mapOf("version", "v1.0.0", "PriceCid", 1)));
        presets.put("NINE_NINE", new DirectGoodsPreset("9.9包邮精选", "/api/goods/nine/op-goods-list",
                mapOf("version", "v1.0.0", "nineCid", -1)));
        presets.put("DDQ", new DirectGoodsPreset("咚咚抢", "/api/category/ddq-goods-list",
                mapOf("version", "v1.0.0")));
        presets.put("RANKING", new DirectGoodsPreset("各大榜单", "/api/goods/get-ranking-list",
                mapOf("version", "v1.0.0", "rankType", 1)));
        presets.put("FRIENDS_CIRCLE", new DirectGoodsPreset("朋友圈素材", "/api/goods/friends-circle-list",
                mapOf("version", "v1.0.0", "sort", 0)));
        presets.put("FEATURE_GOODS", new DirectGoodsPreset("特色货盘", "/open-api/goods/get-feature-goods",
                mapOf("version", "v1.0.0", "pallet_type", 1)));
        presets.put("COLLECT_EXPLOSIVE", new DirectGoodsPreset("采集爆品商品列表",
                "/open-api/goods/get-explosive-goods", mapOf("version", "v1.0.0")));
        presets.put("EXPLOSIVE_RADAR", new DirectGoodsPreset("爆品雷达", "/open-api/goods/radar",
                mapOf("version", "v1.0.0")));
        return presets;
    }

    private CpsThirdPartyActivity buildDirectGoodsTheme(SourceConfig sourceConfig,
                                                        CpsThirdPartyActivityRequest request) {
        Map<String, Object> extraFields = new LinkedHashMap<>();
        extraFields.put("vendorThemeSource", sourceConfig.sourceCode());
        extraFields.put("externalThemeId", "default");
        extraFields.put("externalThemeName", sourceConfig.themeNamePrefix());
        extraFields.put("themeListUrl", null);
        extraFields.put("themeListParams", Map.of());
        extraFields.put("goodsListUrl", sourceConfig.goodsListUrl());
        extraFields.put("goodsListParams", new LinkedHashMap<>(sourceConfig.goodsListParams()));
        return CpsThirdPartyActivity.builder()
                .sourceType(SOURCE_DATAOKE)
                .externalActivityId(EXTERNAL_PREFIX + sourceConfig.sourceCode().toLowerCase() + ":default")
                .activityName(sourceConfig.themeNamePrefix())
                .activityType(sourceConfig.themeNamePrefix())
                .platformCode(firstText(request.getPlatformCode(), CpsPlatformCodeEnum.TAOBAO.getCode()))
                .shortDesc("直接商品列表接口")
                .rebateDesc("以实际转链佣金为准")
                .billingType("CPS")
                .promotionCount(0)
                .tagText(sourceConfig.themeNamePrefix())
                .jumpType("none")
                .searchKeyword(sourceConfig.themeNamePrefix())
                .extraFields(extraFields)
                .build();
    }

    private CpsThirdPartyActivity parseTheme(JsonNode item, CpsThirdPartyActivityRequest request,
                                             SourceConfig sourceConfig) {
        String externalId = firstText(item, "id", "activityId", "albumId", "brandId", "groupId", "group_id",
                "scenePalletId", "palletId");
        String name = firstText(item, "name", "activityName", "albumName", "brandName", "groupName", "title");
        if (!StringUtils.hasText(externalId) || !StringUtils.hasText(name)) {
            return null;
        }
        String themeName = sourceConfig.themeNamePrefix() + "_" + name;
        Map<String, Object> themeListParams = new LinkedHashMap<>(sourceConfig.themeListParams());
        Map<String, Object> goodsListParams = new LinkedHashMap<>(sourceConfig.goodsListParams());
        goodsListParams.put(sourceConfig.goodsThemeIdParam(),
                coerceThemeId(sourceConfig.goodsThemeIdParam(), externalId));
        Map<String, Object> extraFields = new LinkedHashMap<>();
        extraFields.put("vendorThemeSource", sourceConfig.sourceCode());
        extraFields.put("externalThemeId", externalId);
        extraFields.put("externalThemeName", name);
        extraFields.put("themeListUrl", sourceConfig.themeListUrl());
        extraFields.put("themeListParams", themeListParams);
        extraFields.put("goodsListUrl", sourceConfig.goodsListUrl());
        extraFields.put("goodsListParams", goodsListParams);
        extraFields.put("raw", toMap(item));
        return CpsThirdPartyActivity.builder()
                .sourceType(SOURCE_DATAOKE)
                .externalActivityId(EXTERNAL_PREFIX + sourceConfig.sourceCode().toLowerCase() + ":" + externalId)
                .activityName(themeName)
                .activityType(sourceConfig.themeNamePrefix())
                .platformCode(firstText(request.getPlatformCode(), CpsPlatformCodeEnum.TAOBAO.getCode()))
                .mainPic(firstText(item, "pic", "mainPic", "image", "cover", "logo", "brandLogo", "headImg"))
                .shortDesc(firstText(item, "desc", "description", "brandFeatures", "circleTag", "materialsTag",
                        "contentType"))
                .rebateDesc("以实际转链佣金为准")
                .billingType("CPS")
                .promotionCount(firstInt(item, "goodsCount", "todayGoodsCount", "avgGoodsNum"))
                .tagText(sourceConfig.themeNamePrefix())
                .jumpType("none")
                .searchKeyword(name)
                .extraFields(extraFields)
                .rawPayload(toRawPayload(item))
                .build();
    }

    private CpsGoodsSquareGoodsRespVO parseGoods(JsonNode item, CpsSelectionRule rule) {
        String goodsId = firstText(item, "goodsId", "itemId", "productId", "itemIds", "gid", "signId", "sign",
                "goodsSignLast");
        if (!StringUtils.hasText(goodsId)) {
            return null;
        }
        CpsGoodsSquareGoodsRespVO goods = new CpsGoodsSquareGoodsRespVO();
        goods.setGoodsId(goodsId);
        goods.setGoodsSign(firstText(item, "sign", "signId", "goodsSign", "goodsSignLast"));
        goods.setPlatformCode(resolveFirst(rule.getPlatforms(), CpsPlatformCodeEnum.TAOBAO.getCode()));
        goods.setVendorCode(firstText(rule.getVendorCode(), SOURCE_DATAOKE));
        goods.setTitle(firstText(item, "title", "dTitle", "dtitle", "shortTitle", "contentCopy", "content",
                "recommendDesc"));
        goods.setMainPic(firstText(item, "pic", "mainPic", "image", "itemPic", "picUrls", "marketingMainPic"));
        goods.setOriginalPrice(firstDecimal(item, "originalPrice", "originPrice", "original_price", "marketPrice"));
        goods.setActualPrice(firstDecimal(item, "postRollPrice", "post_roll_price", "actualPrice", "couponPrice",
                "price"));
        goods.setCouponPrice(firstDecimal(item, "ticketPrice", "ticket_price", "couponAmount", "couponPriceValue"));
        goods.setCommissionRate(firstDecimal(item, "commission", "commissionRate"));
        goods.setMonthSales(firstLong(item, "monthSales", "sales", "month_sales", 0L));
        goods.setShopName(firstText(item, "storeName", "shopName", "sellerName"));
        goods.setItemLink(firstText(item, "goodsLink", "itemLink", "url", "urls", "link"));
        goods.setBrandName(firstText(item, "brandName", "brand"));
        goods.setSource("dataoke:" + firstText(rule.getVendorThemeSource(), SOURCE_CUSTOM));
        goods.setActivityTag(resolveActivityTag(item, rule));
        goods.setCategoryName(firstText(item, "categoryName", "cidName", "contentType"));
        goods.setCouponEndTime(firstText(item, "ticketEnd", "couponEndTime", "coupon_end_time"));
        goods.setRankTag(firstText(rule.getExternalThemeName(), rule.getVendorThemeSource()));
        goods.setSellingPoint(firstText(item, "desc", "sellingPoint", "contentCopy", "content", "dTitle"));
        return goods;
    }

    private void applyThemePageParams(Map<String, Object> params, String themeListUrl,
                                      CpsThirdPartyActivityRequest request) {
        if (themeListUrl != null && themeListUrl.contains("collect-group")) {
            params.putIfAbsent("page", defaultInt(request.getPageNo(), 1));
            params.putIfAbsent("size", normalizeGroupGoodsSize(defaultInt(request.getPageSize(), 20)));
            return;
        }
        if (themeListUrl != null && (themeListUrl.contains("album/album-list")
                || themeListUrl.contains("brand/get-column-list")
                || themeListUrl.contains("subdivision/get-list"))) {
            params.putIfAbsent("pageId", defaultInt(request.getPageNo(), 1));
            params.putIfAbsent("pageSize", normalizeThemePageSize(defaultInt(request.getPageSize(), 20), themeListUrl));
        }
    }

    private void applyPageParams(Map<String, Object> params, String goodsListUrl, int pullCount) {
        if (goodsListUrl != null && goodsListUrl.contains("group-goods")) {
            params.putIfAbsent("page", 1);
            params.putIfAbsent("size", normalizeGroupGoodsSize(pullCount));
            return;
        }
        if (goodsListUrl != null && goodsListUrl.contains("activity/goods-list")) {
            params.putIfAbsent("pageId", 1);
            params.putIfAbsent("pageSize", normalizeActivityGoodsPageSize(pullCount));
            return;
        }
        if (goodsListUrl != null && isPageIdPageSizeUrl(goodsListUrl)) {
            params.putIfAbsent("pageId", 1);
            params.putIfAbsent("pageSize", normalizeActivityGoodsPageSize(pullCount));
            return;
        }
        if (goodsListUrl != null && goodsListUrl.contains("get-feature-goods")) {
            params.putIfAbsent("page", 1);
            params.putIfAbsent("size", normalizeGroupGoodsSize(pullCount));
            return;
        }
        params.putIfAbsent("page", 1);
        params.putIfAbsent("page_size", normalizeDtkPageSize(pullCount));
    }

    private boolean isPageIdPageSizeUrl(String goodsListUrl) {
        return goodsListUrl.contains("brand/get-goods-list")
                || goodsListUrl.contains("singlePage/list-height-commission")
                || goodsListUrl.contains("live/goods-list")
                || goodsListUrl.contains("get-history-low-price-list")
                || goodsListUrl.contains("super-discount-goods")
                || goodsListUrl.contains("get-half-price-day")
                || goodsListUrl.contains("explosive-goods-list")
                || goodsListUrl.contains("nine/op-goods-list")
                || goodsListUrl.contains("get-ranking-list")
                || goodsListUrl.contains("friends-circle-list")
                || goodsListUrl.contains("list-tip-off");
    }

    private int normalizeThemePageSize(int pageSize, String themeListUrl) {
        int max = themeListUrl != null && themeListUrl.contains("brand/get-column-list") ? 50 : 100;
        if (pageSize <= 10) {
            return 10;
        }
        if (pageSize <= 20) {
            return 20;
        }
        if (pageSize <= 50) {
            return 50;
        }
        return max;
    }

    private int normalizeGroupGoodsSize(int pullCount) {
        if (pullCount <= 10) {
            return 10;
        }
        if (pullCount <= 20) {
            return 20;
        }
        if (pullCount <= 50) {
            return 50;
        }
        if (pullCount <= 100) {
            return 100;
        }
        return 200;
    }

    private int normalizeActivityGoodsPageSize(int pullCount) {
        if (pullCount <= 0) {
            return 100;
        }
        return Math.min(pullCount, 100);
    }

    private int normalizeDtkPageSize(int pullCount) {
        if (pullCount <= 30) {
            return 30;
        }
        if (pullCount <= 50) {
            return 50;
        }
        return 100;
    }

    private boolean keywordMatched(CpsThirdPartyActivity theme, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String text = firstText(theme.getActivityName(), "") + " " + firstText(theme.getActivityType(), "") + " "
                + firstText(theme.getSearchKeyword(), "");
        return text.contains(keyword.trim());
    }

    private Map<String, Object> mergeParams(Map<String, Object> defaults, String paramsJson) {
        Map<String, Object> params = new LinkedHashMap<>(defaults == null ? Map.of() : defaults);
        if (!StringUtils.hasText(paramsJson)) {
            return params;
        }
        try {
            Map<String, Object> custom = objectMapper.readValue(paramsJson, new TypeReference<>() {});
            if (custom != null) {
                params.putAll(custom);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("大淘客选品库参数 JSON 格式错误", e);
        }
        return params;
    }

    private JsonNode resolveListNode(JsonNode data) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            return null;
        }
        if (data.isArray()) {
            return data;
        }
        for (String fieldName : List.of("list", "goodlist", "goodsList", "records", "items", "data")) {
            JsonNode value = data.path(fieldName);
            if (value.isArray()) {
                return value;
            }
            if (value.isObject()) {
                JsonNode nested = firstArrayChild(value);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return firstArrayChild(data);
    }

    private JsonNode firstArrayChild(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (JsonNode child : node) {
            if (child.isArray()) {
                return child;
            }
            JsonNode nested = firstArrayChild(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private boolean isSuccessResponse(JsonNode root) {
        return root != null && ("0".equals(root.path("code").asText()) || "200".equals(root.path("status").asText()));
    }

    private void injectSignParams(Map<String, Object> params, CpsVendorConfig config) {
        if (config == null) {
            return;
        }
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf(new Random().nextInt(900000) + 100000);
        String signSource = String.format("appKey=%s&timer=%s&nonce=%s&key=%s",
                config.getAppKey(), timer, nonce, config.getAppSecret());
        params.put("appKey", config.getAppKey());
        params.put("timer", timer);
        params.put("nonce", nonce);
        params.put("signRan", DigestUtil.md5Hex(signSource).toUpperCase());
    }

    private String resolveUrl(String urlOrPath, CpsVendorConfig config) {
        if (StringUtils.hasText(urlOrPath) && urlOrPath.startsWith("http")) {
            return urlOrPath;
        }
        String path = StringUtils.hasText(urlOrPath) ? urlOrPath : "/open-api/scene-pallet";
        String baseUrl = config == null || !StringUtils.hasText(config.getApiBaseUrl())
                ? DEFAULT_BASE_URL : config.getApiBaseUrl();
        if (path.startsWith("/open-api")) {
            try {
                URI uri = URI.create(baseUrl);
                return uri.getScheme() + "://" + uri.getAuthority() + path;
            } catch (Exception ignored) {
                return "https://openapi.dataoke.com" + path;
            }
        }
        if (path.startsWith("/api")) {
            try {
                URI uri = URI.create(baseUrl);
                return uri.getScheme() + "://" + uri.getAuthority() + path;
            } catch (Exception ignored) {
                return "https://openapi.dataoke.com" + path;
            }
        }
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String buildUrlWithParams(String url, Map<String, Object> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((key, value) -> {
            if (value != null) {
                joiner.add(encodeQueryParam(key) + "=" + encodeQueryParam(value));
            }
        });
        String query = joiner.toString();
        if (!StringUtils.hasText(query)) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + query;
    }

    private String encodeQueryParam(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    private String resolveActivityTag(JsonNode item, CpsSelectionRule rule) {
        JsonNode activityInfo = item.path("activityInfo");
        if (activityInfo.isArray()) {
            List<String> names = new ArrayList<>();
            for (JsonNode activity : activityInfo) {
                String name = firstText(activity, "activityName", "name");
                if (StringUtils.hasText(name)) {
                    names.add(name);
                }
            }
            if (!names.isEmpty()) {
                return String.join(",", names);
            }
        }
        return firstText(rule.getExternalThemeName(), rule.getVendorThemeSource());
    }

    private Object coerceNumberOrString(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return value;
        }
    }

    private Object coerceThemeId(String paramName, String value) {
        return "brandId".equals(paramName) ? value : coerceNumberOrString(value);
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return Map.of();
        }
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }

    private String toRawPayload(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                continue;
            }
            String text = value.asText(null);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode node, String... fieldNames) {
        Long value = firstLong(node, fieldNames);
        return value == null ? null : value.intValue();
    }

    private Long firstLong(JsonNode node, String... fieldNames) {
        return firstLong(node, fieldNames, null);
    }

    private Long firstLong(JsonNode node, String[] fieldNames, Long defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        for (String fieldName : fieldNames) {
            String value = firstText(node, fieldName);
            if (StringUtils.hasText(value)) {
                String normalized = value.replaceAll("[^0-9-]", "");
                if (StringUtils.hasText(normalized)) {
                    try {
                        return Long.parseLong(normalized);
                    } catch (NumberFormatException ignored) {
                        // continue
                    }
                }
            }
        }
        return defaultValue;
    }

    private Long firstLong(JsonNode node, String firstField, String secondField, String thirdField, long defaultValue) {
        return firstLong(node, new String[]{firstField, secondField, thirdField}, defaultValue);
    }

    private BigDecimal firstDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = firstText(node, fieldName);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String normalized = value.replace("%", "").replace("¥", "").replace(",", "").trim();
            try {
                return new BigDecimal(normalized);
            } catch (NumberFormatException ignored) {
                // continue
            }
        }
        return null;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String resolveFirst(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private record DirectGoodsPreset(String themeName, String goodsListUrl, Map<String, Object> goodsListParams) {
    }

    private record SourceConfig(String sourceCode, String themeNamePrefix, String themeListUrl, String goodsListUrl,
                                String goodsThemeIdParam, Map<String, Object> themeListParams,
                                Map<String, Object> goodsListParams, boolean directGoodsTheme) {

        private SourceConfig withParams(Map<String, Object> themeListParams, Map<String, Object> goodsListParams) {
            return new SourceConfig(sourceCode, themeNamePrefix, themeListUrl, goodsListUrl, goodsThemeIdParam,
                    themeListParams, goodsListParams, directGoodsTheme);
        }
    }
}
