package com.qiji.cps.module.cps.client.haodanku.activity;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HdkActivityClient {

    private static final String BASE_URL = "https://www.haodanku.com/openapi";
    private static final String CONFERENCE_URL = "https://v2.api.haodanku.com/createConference_code";
    private static final String ELEME_ACTIVITY_URL = "https://v3.api.haodanku.com/elm_activity_ratesurl";
    private static final int HTTP_TIMEOUT = 5000;
    private static final int CONFERENCE_HTTP_TIMEOUT = 10000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<HdkActivityCategory> fetchCategories() {
        JsonNode root = get("/activity_list_cat", Map.of());
        if (!isSuccess(root)) {
            log.warn("[HdkActivityClient] 拉取活动分类失败: {}", root);
            return List.of();
        }
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            return List.of();
        }
        List<HdkActivityCategory> categories = new ArrayList<>();
        for (JsonNode item : data) {
            categories.add(HdkActivityCategory.builder()
                    .catId(parseInt(item.path("cat_id").asText(null)))
                    .name(item.path("name").asText(null))
                    .icon(item.path("icon").asText(null))
                    .secondaryCategories(parseSecondaryCategories(item.path("secondary_category")))
                    .build());
        }
        return categories;
    }

    public HdkActivityPage fetchActivities(HdkActivityListRequest request) {
        JsonNode root = get("/activity_list", buildListParams(request));
        if (!isSuccess(root)) {
            log.warn("[HdkActivityClient] 拉取活动列表失败: params={}, response={}", request, root);
            return HdkActivityPage.builder().build();
        }
        List<HdkActivityItem> items = new ArrayList<>();
        JsonNode data = root.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                items.add(HdkActivityItem.builder()
                        .id(item.path("id").asText(null))
                        .activityId(item.path("activity_id").asText(null))
                        .activityUrl(item.path("activity_url").asText(null))
                        .activityPic(item.path("activity_pic").asText(null))
                        .activityName(item.path("activity_name").asText(null))
                        .activityLabel(item.path("activity_label").asText(null))
                        .startTime(item.path("start_time").asText(null))
                        .endTime(item.path("end_time").asText(null))
                        .platform(item.path("platform").asText(null))
                        .describe(item.path("describe").asText(null))
                        .commissionRate(item.path("commission_rate").asText(null))
                        .promotionNum(item.path("promotion_num").asText(null))
                        .promotionType(item.path("promotion_type").asText(null))
                        .activityDate(item.path("activity_date").asText(null))
                        .isChannel(item.path("is_channel").isMissingNode() ? null : item.path("is_channel").asInt())
                        .build());
            }
        }
        return HdkActivityPage.builder()
                .items(items)
                .countPage(root.path("count_page").asInt(1))
                .itemCount(parseInt(firstText(root, "itme_count", "item_count")))
                .build();
    }

    public CpsPromotionLinkResult generateConferenceLink(CpsPromotionLinkRequest request, CpsVendorConfig config,
                                                          String activityTitle) {
        if (request == null || config == null || !StringUtils.hasText(config.getAppKey())) {
            return null;
        }
        String pid = firstText(request.getAdzoneId(), config.getDefaultAdzoneId());
        String activityId = request.getGoodsId();
        String activityUrl = request.getItemLink();
        String tbName = firstText(config.getAuthToken(), getExtraConfig(config, "tb_name"));
        String title = normalizeTitle(activityTitle);
        boolean useActivityId = StringUtils.hasText(activityId);
        if (!StringUtils.hasText(pid)
                || !StringUtils.hasText(tbName)
                || (!StringUtils.hasText(activityId) && !StringUtils.hasText(activityUrl))
                || title.length() <= 8) {
            return null;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("apikey", config.getAppKey());
        params.put(useActivityId ? "activity_id" : "activity_url", useActivityId ? activityId : activityUrl);
        params.put("pid", pid);
        params.put("tb_name", tbName);
        params.put("title", title);
        params.put("relation_id", request.getRelationId());
        params.entrySet().removeIf(entry -> entry.getValue() == null
                || entry.getValue() instanceof String value && !StringUtils.hasText(value));

        JsonNode root = executeConferenceRequest(params);
        if (!isConferenceSuccess(root)) {
            return null;
        }
        JsonNode data = root.path("data").isObject() ? root.path("data") : root;
        String url = firstText(data, "url", "click_url", "short_url");
        if (!isHttpUrl(url)) {
            return null;
        }
        return CpsPromotionLinkResult.builder()
                .shortUrl(url)
                .longUrl(url)
                .tpwd(firstText(data, "tao_code", "taoCode", "tpwd"))
                .rawPayload(toJson(root))
                .build();
    }

    public CpsPromotionLinkResult generateElemeActivityLink(CpsPromotionLinkRequest request, CpsVendorConfig config,
                                                             String sid) {
        if (request == null || config == null || !StringUtils.hasText(config.getAppKey())
                || StringUtils.hasText(sid) && !isValidSid(sid)) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("apikey", config.getAppKey());
        params.put("activity_id", request.getGoodsId());
        params.put("sid", sid);
        params.put("link", request.getItemLink());
        params.entrySet().removeIf(entry -> entry.getValue() == null
                || entry.getValue() instanceof String value && !StringUtils.hasText(value));

        JsonNode root = executeElemeActivityRequest(params);
        if (!isConferenceSuccess(root)) {
            return null;
        }
        JsonNode data = root.path("data").isObject() ? root.path("data") : root;
        String shortUrl = firstText(data, "h5_short_link", "h5ShortLink");
        String longUrl = firstText(data, "h5_url", "h5Url");
        String tpwd = firstText(data, "full_taobao_word", "fullTaobaoWord");
        String elemeSchemeUrl = firstText(data, "ele_scheme_url", "eleSchemeUrl");
        String taobaoSchemeUrl = firstText(data, "tb_scheme_url", "tbSchemeUrl");
        String alipayMiniUrl = firstText(data, "alipay_mini_url", "alipayMiniUrl");
        String miniQrcode = firstText(data, "mini_qrcode", "miniQrcode");
        if (!hasElemePromotionMaterial(shortUrl, longUrl, tpwd, elemeSchemeUrl,
                taobaoSchemeUrl, alipayMiniUrl, miniQrcode)) {
            return null;
        }
        Map<String, Object> extraFields = new LinkedHashMap<>();
        extraFields.put("miniQrcode", miniQrcode);
        extraFields.put("wxAppId", firstText(data, "wx_appid", "wxAppId"));
        extraFields.put("wxPath", firstText(data, "wx_path", "wxPath"));
        extraFields.put("taobaoSchemeUrl", taobaoSchemeUrl);
        extraFields.put("alipayMiniUrl", alipayMiniUrl);
        extraFields.entrySet().removeIf(entry -> entry.getValue() == null);
        return CpsPromotionLinkResult.builder()
                .shortUrl(shortUrl)
                .longUrl(longUrl)
                .tpwd(tpwd)
                .mobileUrl(elemeSchemeUrl)
                .extraFields(extraFields)
                .rawPayload(toJson(root))
                .build();
    }

    protected JsonNode executeConferenceRequest(Map<String, Object> params) {
        try {
            HttpRequest request = HttpRequest.post(CONFERENCE_URL).timeout(CONFERENCE_HTTP_TIMEOUT);
            params.forEach(request::form);
            return objectMapper.readTree(request.execute().body());
        } catch (Exception e) {
            log.warn("[HdkActivityClient] 淘宝会场转链请求异常", e);
            return null;
        }
    }

    protected JsonNode executeElemeActivityRequest(Map<String, Object> params) {
        try {
            HttpRequest request = HttpRequest.post(ELEME_ACTIVITY_URL).timeout(CONFERENCE_HTTP_TIMEOUT);
            params.forEach(request::form);
            return objectMapper.readTree(request.execute().body());
        } catch (Exception e) {
            log.warn("[HdkActivityClient] 淘宝闪购会场转链请求异常", e);
            return null;
        }
    }

    private JsonNode get(String path, Map<String, Object> params) {
        try {
            HttpRequest request = HttpRequest.get(BASE_URL + path)
                    .timeout(HTTP_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://www.haodanku.com/openapi/activity")
                    .header("X-Requested-With", "XMLHttpRequest");
            params.forEach((key, value) -> {
                if (value != null) {
                    request.form(key, value);
                }
            });
            HttpResponse response = request.execute();
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.warn("[HdkActivityClient] 请求异常: path={}, params={}", path, params, e);
            return null;
        }
    }

    private Map<String, Object> buildListParams(HdkActivityListRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("p", request.getPageNo() == null ? 1 : request.getPageNo());
        params.put("keyword", request.getKeyword());
        params.put("cat_id", request.getCatId());
        params.put("promotion_type", request.getPromotionType() == null ? 0 : request.getPromotionType());
        params.put("secondary_cat_id", request.getSecondaryCatId());
        params.put("order", request.getOrder() == null ? 1 : request.getOrder());
        return params;
    }

    private List<HdkSecondaryCategory> parseSecondaryCategories(JsonNode data) {
        if (!data.isArray()) {
            return List.of();
        }
        List<HdkSecondaryCategory> result = new ArrayList<>();
        for (JsonNode item : data) {
            result.add(HdkSecondaryCategory.builder()
                    .secondaryCatId(parseInt(item.path("secondary_cat_id").asText(null)))
                    .name(item.path("name").asText(null))
                    .build());
        }
        return result;
    }

    private boolean isSuccess(JsonNode root) {
        return root != null && root.path("code").asInt(-1) == 200;
    }

    private boolean isConferenceSuccess(JsonNode root) {
        int code = root == null ? -1 : root.path("code").asInt(-1);
        return code == 1 || code == 200;
    }

    private String firstText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = root.path(fieldName).asText(null);
            if (StringUtils.hasText(value)) {
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

    private String getExtraConfig(CpsVendorConfig config, String key) {
        return config.getExtraConfig() == null ? null : config.getExtraConfig().get(key);
    }

    private String normalizeTitle(String value) {
        String title = StringUtils.hasText(value) ? value.trim() : "官方联盟活动推广会场";
        return title.length() > 8 ? title : title + "官方活动推广";
    }

    private boolean isHttpUrl(String value) {
        return StringUtils.hasText(value)
                && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private boolean hasElemePromotionMaterial(String shortUrl, String longUrl, String tpwd,
                                               String elemeSchemeUrl, String taobaoSchemeUrl,
                                               String alipayMiniUrl, String miniQrcode) {
        return isHttpUrl(shortUrl)
                || isHttpUrl(longUrl)
                || StringUtils.hasText(tpwd)
                || hasScheme(elemeSchemeUrl, "eleme")
                || hasScheme(taobaoSchemeUrl, "tbopen")
                || hasScheme(alipayMiniUrl, "alipays")
                || isHttpUrl(miniQrcode);
    }

    private boolean hasScheme(String value, String scheme) {
        return StringUtils.hasText(value) && value.regionMatches(true, 0, scheme + "://", 0,
                scheme.length() + 3);
    }

    private boolean isValidSid(String value) {
        return StringUtils.hasText(value) && value.matches("[A-Za-z0-9_]{1,15}");
    }

    private String toJson(JsonNode value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

}
