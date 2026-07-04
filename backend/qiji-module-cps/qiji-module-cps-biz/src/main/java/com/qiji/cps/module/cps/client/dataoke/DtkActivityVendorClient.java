package com.qiji.cps.module.cps.client.dataoke;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyApiCategory;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 大淘客热门活动统一适配器。
 */
@Slf4j
@Component
public class DtkActivityVendorClient implements CpsThirdPartyActivityVendorClient {

    private static final int HTTP_TIMEOUT = 5000;
    private static final int ACTIVITY_LINK_HTTP_TIMEOUT = 10000;
    private static final String SOURCE_DATAOKE = "dataoke";
    private static final String EXTERNAL_PREFIX = "dtk:";
    private static final String OFFICIAL_ACTIVITY_PATH = "/category/get-tb-topic-list";
    private static final String ACTIVITY_LINK_PATH = "/tb-service/activity-link";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    public DtkActivityVendorClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.DATAOKE.getCode();
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    @Override
    public CpsThirdPartyPage<CpsThirdPartyActivity> fetchActivities(CpsThirdPartyActivityRequest request,
                                                                    CpsVendorConfig config) {
        int pageNo = defaultInt(request.getPageNo(), 1);
        int pageSize = defaultInt(request.getPageSize(), 20);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pageId", pageNo);
        params.put("pageSize", pageSize);
        params.put("type", 0);
        params.put("channelId", "0");
        params.put("version", "v1.2.0");

        JsonNode response = executeRequest(OFFICIAL_ACTIVITY_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                    .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                    .list(Collections.emptyList())
                    .total(0L)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .rawPayload(toRawPayload(response))
                    .build();
        }

        JsonNode data = response.path("data");
        JsonNode list = data.isArray() ? data : firstArray(data, "list", "activityList", "records", "data");
        List<CpsThirdPartyActivity> activities = new ArrayList<>();
        if (list != null && list.isArray()) {
            for (JsonNode item : list) {
                CpsThirdPartyActivity activity = parseActivity(item, request);
                if (activity != null) {
                    activities.add(activity);
                }
            }
        }
        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(activities)
                .total(firstLong(data, "totalCount", "total", "count", activities.size()))
                .pageNo(pageNo)
                .pageSize(pageSize)
                .nextPageId(String.valueOf(pageNo + 1))
                .rawPayload(toRawPayload(response))
                .build();
    }

    public CpsPromotionLinkResult generateActivityLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("promotionSceneId", request.getGoodsId());
        params.put("version", "v1.0.0");
        params.put("pid", firstText(request.getAdzoneId(), config.getDefaultAdzoneId()));
        params.put("relationId", request.getChannelId());
        params.put("unionId", request.getExternalId());

        JsonNode response = executeRequest(ACTIVITY_LINK_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsPromotionLinkResult.builder()
                    .rawPayload(toRawPayload(response))
                    .build();
        }

        JsonNode data = response.path("data");
        String clickUrl = firstText(data, "click_url", "clickUrl");
        Map<String, Object> extraFields = new LinkedHashMap<>();
        putIfHasText(extraFields, "longTpwd", firstText(data, "longTpwd", "long_tpwd"));
        putIfHasText(extraFields, "pageName", firstText(data, "page_name", "pageName"));
        putIfHasText(extraFields, "terminalType", firstText(data, "terminal_type", "terminalType"));
        putIfHasText(extraFields, "pageStartTime", firstText(data, "page_start_time", "pageStartTime"));
        putIfHasText(extraFields, "pageEndTime", firstText(data, "page_end_time", "pageEndTime"));
        return CpsPromotionLinkResult.builder()
                .shortUrl(clickUrl)
                .longUrl(clickUrl)
                .tpwd(firstText(data, "Tpwd", "tpwd"))
                .extraFields(extraFields)
                .rawPayload(toRawPayload(data))
                .build();
    }

    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        Map<String, Object> allParams = new LinkedHashMap<>(params);
        injectSignParams(allParams, config);
        allParams.entrySet().removeIf(entry -> entry.getValue() == null);

        String fullUrl = buildUrlWithParams(config.getApiBaseUrl() + path, allParams);
        try {
            HttpResponse response = HttpRequest.get(fullUrl).timeout(resolveTimeout(path)).execute();
            String body = response.body();
            log.debug("[{}:{}] 活动请求: {} 响应: {}", getVendorCode(), getPlatformCode(), path, body);
            return unwrapResponse(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("[{}:{}] 活动请求异常: path={}", getVendorCode(), getPlatformCode(), path, e);
            return null;
        }
    }

    protected int resolveTimeout(String path) {
        return ACTIVITY_LINK_PATH.equals(path) ? ACTIVITY_LINK_HTTP_TIMEOUT : HTTP_TIMEOUT;
    }

    private void injectSignParams(Map<String, Object> params, CpsVendorConfig config) {
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf(new Random().nextInt(900000) + 100000);
        String signSource = String.format("appKey=%s&timer=%s&nonce=%s&key=%s",
                config.getAppKey(), timer, nonce, config.getAppSecret());
        params.put("appKey", config.getAppKey());
        params.put("timer", timer);
        params.put("nonce", nonce);
        params.put("signRan", DigestUtil.md5Hex(signSource).toUpperCase());
    }

    private boolean isSuccessResponse(JsonNode root) {
        JsonNode response = unwrapResponse(root);
        return response != null && "0".equals(response.path("code").asText());
    }

    private JsonNode unwrapResponse(JsonNode root) {
        if (root != null && root.has("status") && root.path("data").has("code")) {
            return root.path("data");
        }
        return root;
    }

    private CpsThirdPartyActivity parseActivity(JsonNode item, CpsThirdPartyActivityRequest request) {
        String promotionSceneId = firstText(item, "promotionSceneId", "promotion_scene_id",
                "promotionSceneID", "promotion_sceneId");
        if (!StringUtils.hasText(promotionSceneId)) {
            return null;
        }
        String vendorActivityId = firstText(item, "id", "activityId", "activityIdStr", "activity_id");
        String activityName = firstText(item, "activityName", "activityInfo", "name", "title", "activity_title");
        String activityType = firstText(item, "activityType", "activityLabel", "type", "tag", "label", "categoryName");
        String jumpUrl = firstText(item, "activityUrl", "activityLink", "url", "link", "h5");
        Map<String, Object> extraFields = toMap(item, "id", "activityId", "activityType", "activityLabel",
                "goodsCount", "activityGoodsNum");
        if (StringUtils.hasText(vendorActivityId)) {
            extraFields.put("legacyExternalActivityId", EXTERNAL_PREFIX + vendorActivityId);
        }
        return CpsThirdPartyActivity.builder()
                .sourceType(SOURCE_DATAOKE)
                .externalActivityId(EXTERNAL_PREFIX + promotionSceneId)
                .activityName(activityName)
                .activityType(firstText(activityType, "淘宝会场"))
                .platformCode(firstText(request.getPlatformCode(), CpsPlatformCodeEnum.TAOBAO.getCode()))
                .mainPic(firstText(item, "materialLink", "activityPic", "activityImg", "activityImage",
                        "mainPic", "banner", "image"))
                .shortDesc(firstText(item, "activityInfo", "activityDesc", "desc", "description", "introduce"))
                .rebateDesc(firstText(firstText(item, "rebateDesc", "commissionDesc", "commissionRate"), "以实际转链佣金为准"))
                .billingType("CPS")
                .promotionCount(firstInt(item, "goodsCount", "goodsNum", "promotionCount", "activityGoodsNum"))
                .tagText(firstText(activityType, "大淘客"))
                .jumpType(StringUtils.hasText(jumpUrl) ? "url" : "search")
                .jumpUrl(jumpUrl)
                .searchKeyword(firstText(activityName, request.getKeyword(), activityType))
                .startTime(parseTime(firstText(item, "startTime", "activityStartTime", "start_time")))
                .endTime(parseTime(firstText(item, "endTime", "activityEndTime", "end_time")))
                .extraFields(extraFields)
                .rawPayload(toRawPayload(item))
                .build();
    }

    private JsonNode firstArray(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                return value;
            }
        }
        return null;
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
            String value = node.path(fieldName).asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asInt(0);
            }
        }
        return 0;
    }

    private Long firstLong(JsonNode node, String fieldName1, String fieldName2, String fieldName3, int defaultValue) {
        for (String fieldName : List.of(fieldName1, fieldName2, fieldName3)) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asLong(defaultValue);
            }
        }
        return (long) defaultValue;
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            if (value.matches("\\d{13}")) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(value)), ZoneId.systemDefault());
            }
            if (value.matches("\\d{10}")) {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(value)), ZoneId.systemDefault());
            }
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
            }
            return LocalDateTime.parse(value.replace("T", " "), DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> toMap(JsonNode node, String... fieldNames) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                map.put(fieldName, value.asText());
            }
        }
        return map;
    }

    private void putIfHasText(Map<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }

    private String toRawPayload(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        StringBuilder queryStr = new StringBuilder();
        params.forEach((key, value) -> {
            if (value != null) {
                if (!queryStr.isEmpty()) {
                    queryStr.append("&");
                }
                queryStr.append(encodeQueryParam(key)).append("=").append(encodeQueryParam(value));
            }
        });
        return baseUrl + "?" + queryStr;
    }

    private String encodeQueryParam(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }
}
