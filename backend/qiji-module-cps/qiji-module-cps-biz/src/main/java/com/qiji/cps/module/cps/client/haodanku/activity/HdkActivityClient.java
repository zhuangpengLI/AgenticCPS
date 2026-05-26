package com.qiji.cps.module.cps.client.haodanku.activity;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final int HTTP_TIMEOUT = 5000;

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

    private String firstText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = root.path(fieldName).asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
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
