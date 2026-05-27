package com.qiji.cps.module.cps.client.jutuike;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.common.AbstractAggregatorVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyApiCategory;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聚推客联盟统一接口客户端。
 */
@Component
public class JutuikeUnionVendorClient extends AbstractAggregatorVendorClient
        implements CpsThirdPartyActivityVendorClient {

    private static final String SOURCE_JUTUIKE = "jutuike";
    private static final String EXTERNAL_PREFIX = "jtk:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.JUTUIKE.getCode();
    }

    @Override
    public String getPlatformCode() {
        return "union";
    }

    @Override
    protected Map<String, String> computeSignContext(Map<String, Object> params, CpsVendorConfig config) {
        return Collections.emptyMap();
    }

    @Override
    protected void injectSignParams(Map<String, Object> params, CpsVendorConfig config,
                                    Map<String, String> signContext) {
        params.put("apikey", config.getAppKey());
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return false;
        }
        int code = root.path("code").asInt(root.path("status").asInt(-1));
        return code == 0 || code == 1 || code == 200;
    }

    @Override
    public CpsThirdPartyPage<CpsThirdPartyActivity> fetchActivities(CpsThirdPartyActivityRequest request,
                                                                    CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", defaultInt(request.getPageNo(), 1));
        params.put("pageSize", defaultInt(request.getPageSize(), 20));
        params.put("cate_name", firstText(request.getCategoryName(), request.getKeyword()));
        JsonNode response = executeRequest("/union/act_list", params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                    .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                    .list(Collections.emptyList())
                    .total(0L)
                    .pageNo(defaultInt(request.getPageNo(), 1))
                    .pageSize(defaultInt(request.getPageSize(), 20))
                    .rawPayload(toRawPayload(response))
                    .build();
        }
        List<CpsThirdPartyActivity> activities = new ArrayList<>();
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                CpsThirdPartyActivity activity = parseActivity(item, request);
                if (activity != null) {
                    activities.add(activity);
                }
            }
        }
        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(activities)
                .total(response.path("total").asLong(activities.size()))
                .pageNo(defaultInt(request.getPageNo(), 1))
                .pageSize(defaultInt(request.getPageSize(), 20))
                .nextPageId(String.valueOf(defaultInt(request.getPageNo(), 1) + 1))
                .rawPayload(toRawPayload(response))
                .build();
    }

    @Override
    protected String getSearchApiPath() {
        return "/union/act_list";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("cate_name", request.getKeyword());
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode responseRoot, CpsGoodsSearchRequest request) {
        return CpsGoodsSearchResult.builder()
                .list(Collections.emptyList())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "/union/act";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("act_id", request.getGoodsId());
        params.put("sid", request.getExternalId());
        params.put("relation_flag_name", firstText(request.getChannelId(), request.getAdzoneId()));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = payload(response);
        return CpsPromotionLinkResult.builder()
                .shortUrl(firstText(data, "short_url", "shortUrl"))
                .longUrl(firstText(data, "url", "link", "h5"))
                .mobileUrl(firstText(data, "h5", "url", "link"))
                .extraFields(toMap(data, "sid", "relation_flag_name", "qrcode", "mini_path"))
                .rawPayload(toRawPayload(data))
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/union/orders";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start_time", request.getStartTime());
        params.put("end_time", request.getEndTime());
        params.put("query_type", request.getQueryType());
        params.put("status", request.getOrderStatus());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            return orders;
        }
        for (JsonNode item : data) {
            orders.add(parseOrder(item));
        }
        return orders;
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "/union/act_list";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", 1);
        params.put("pageSize", 1);
        return params;
    }

    private CpsThirdPartyActivity parseActivity(JsonNode item, CpsThirdPartyActivityRequest request) {
        String activityId = firstText(item, "act_id", "activity_id", "id");
        if (!StringUtils.hasText(activityId)) {
            return null;
        }
        String cateName = firstText(item, "cate_name", "category_name", "activity_type");
        return CpsThirdPartyActivity.builder()
                .sourceType(SOURCE_JUTUIKE)
                .externalActivityId(EXTERNAL_PREFIX + activityId)
                .activityName(firstText(item, "act_name", "activity_name", "name"))
                .activityType(cateName)
                .platformCode(firstText(request.getPlatformCode(), platformFromCategory(cateName)))
                .mainPic(firstText(item, "img", "poster", "main_pic"))
                .icon(firstText(item, "icon"))
                .shortDesc(firstText(item, "desc", "introduce"))
                .rebateDesc(firstText(item, "attribution_explain", "settlement_time", "note"))
                .billingType("CPS")
                .promotionCount(parseInt(firstText(item, "promotion_num", "goods_num")))
                .tagText(cateName)
                .jumpType("url")
                .jumpUrl(firstText(item, "url", "h5", "activity_url"))
                .searchKeyword(firstText(item, "act_name", "activity_name", "name"))
                .startTime(parseDateOrDateTime(firstText(item, "start_date", "start_time")))
                .endTime(parseDateOrDateTime(firstText(item, "end_date", "end_time")))
                .extraFields(toMap(item, "settlement_time", "note", "poster", "attribution_explain", "cate_name"))
                .rawPayload(toRawPayload(item))
                .build();
    }

    private CpsOrderDTO parseOrder(JsonNode item) {
        BigDecimal shareRate = parseDecimal(item, "jtk_share_rate");
        if (shareRate != null) {
            shareRate = shareRate.multiply(BigDecimal.valueOf(100));
        }
        return CpsOrderDTO.builder()
                .platformCode(platformFromBrandId(item.path("brand_id").asInt(-1)))
                .platformOrderId(firstText(item, "order_sn", "order_id"))
                .itemId(firstText(item, "act_id", "item_id", "goods_id"))
                .itemTitle(firstText(item, "order_title", "act_name", "item_title"))
                .itemPrice(parseDecimal(item, "order_price"))
                .finalPrice(parseDecimal(item, "pay_price"))
                .commissionRate(shareRate)
                .commissionAmount(parseDecimal(item, "jtk_share_fee", "share_fee", "profit"))
                .platformStatus(item.path("status").asInt(-1))
                .orderTime(firstText(item, "create_time"))
                .payTime(firstText(item, "pay_time"))
                .externalId(firstText(item, "sid", "relation_flag_name"))
                .refundTag(item.path("status").asInt(-1) == 4 ? 1 : 0)
                .extraFields(toMap(item, "act_name", "status_desc", "brand_id", "invalid_reason", "icon", "modified_time"))
                .rawPayload(toRawPayload(item))
                .build();
    }

    private JsonNode payload(JsonNode response) {
        JsonNode data = response == null ? null : response.path("data");
        return data != null && !data.isMissingNode() && !data.isNull() ? data : response;
    }

    private String platformFromBrandId(int brandId) {
        return switch (brandId) {
            case 1, 36 -> CpsPlatformCodeEnum.MEITUAN.getCode();
            case 2 -> "eleme";
            case 3 -> CpsPlatformCodeEnum.PDD.getCode();
            case 4 -> CpsPlatformCodeEnum.JD.getCode();
            case 14 -> CpsPlatformCodeEnum.VIP.getCode();
            case 22 -> CpsPlatformCodeEnum.DOUYIN.getCode();
            case 32 -> CpsPlatformCodeEnum.TAOBAO.getCode();
            default -> "local_life";
        };
    }

    private String platformFromCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        if (category.contains("美团")) {
            return CpsPlatformCodeEnum.MEITUAN.getCode();
        }
        if (category.contains("饿了么")) {
            return "eleme";
        }
        if (category.contains("京东")) {
            return CpsPlatformCodeEnum.JD.getCode();
        }
        if (category.contains("淘宝") || category.contains("电商")) {
            return CpsPlatformCodeEnum.TAOBAO.getCode();
        }
        if (category.contains("抖音")) {
            return CpsPlatformCodeEnum.DOUYIN.getCode();
        }
        return null;
    }

    private Integer defaultInt(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
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

    private LocalDateTime parseDateOrDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            BigDecimal value = parseDecimal(node, fieldName);
            if (value != null) {
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

    private Map<String, Object> toMap(JsonNode node, String... fieldNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (node == null) {
            return result;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                result.put(fieldName, value.isNumber() ? value.numberValue() : value.asText());
            }
        }
        return result;
    }

    private String toRawPayload(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }
}
