package com.qiji.cps.module.cps.client.official.meituan;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.CpsVendorGovernancePolicy;
import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 美团联盟官方 API 客户端（基于美团开放平台与 Shepherd API 网关签名接入）
 *
 * <p>核心接入接口：
 * <ul>
 *   <li>商品查询（到家/到店/外卖）：/query_coupon</li>
 *   <li>推广链接生成（活动/商品/长短链/小程序）：/get_referral_link</li>
 *   <li>订单明细查询（按支付/更新时间分批滚动拉取）：/query_order</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class MeituanOfficialVendorClient extends AbstractOfficialVendorClient
        implements CpsThirdPartyActivityVendorClient {

    static final String DEFAULT_BASE_URL = "https://media.meituan.com/cps_open/common/api/v1";
    static final String IPC_BASE_URL = "https://media.meituan.com";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient;

    public MeituanOfficialVendorClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public MeituanOfficialVendorClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.MEITUAN.getCode();
    }

    @Override
    public Set<CpsVendorCapability> getCapabilities() {
        return EnumSet.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST
        );
    }

    @Override
    public CpsVendorConfigSchema getConfigSchema() {
        return new CpsVendorConfigSchema(List.of(
                CpsVendorConfigField.required("appKey", true),
                CpsVendorConfigField.required("appSecret", true),
                CpsVendorConfigField.optional("apiBaseUrl", false),
                CpsVendorConfigField.optional("defaultAdzoneId", false),
                CpsVendorConfigField.optional("timeoutMs", false),
                CpsVendorConfigField.optional("platform", false),
                CpsVendorConfigField.optional("bizLine", false),
                CpsVendorConfigField.optional("cityId", false),
                CpsVendorConfigField.optional("linkType", false),
                CpsVendorConfigField.optional("businessLine", false),
                CpsVendorConfigField.optional("tradeType", false),
                CpsVendorConfigField.optional("cookie", false)
        ));
    }

    @Override
    public CpsVendorGovernancePolicy getGovernancePolicy() {
        return CpsVendorGovernancePolicy.builder()
                .timeoutMillis(5000)
                .rateLimitPerMinute(120)
                .circuitBreakerOpenMillis(10000)
                .metricsEnabled(true)
                .maskedDiagnosticsEnabled(true)
                .build();
    }

    @Override
    protected String getSearchApiPath() {
        return "/query_coupon";
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "/get_referral_link";
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/query_order";
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "/query_coupon";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        put(params, "searchText", request.getKeyword());
        put(params, "pageNo", request.getPageNo());
        put(params, "pageSize", request.getPageSize());
        put(params, "platform", integerConfig(config, "platform", 1));
        put(params, "bizLine", integerConfig(config, "bizLine", null));
        put(params, "cityId", configValue(config, "cityId"));
        put(params, "sortField", request.getSortType() == null || request.getSortType() == 0 ? null
                : request.getSortType() == 1 ? 2 : request.getSortType() == 2 ? 1 : 3);
        if (request.getPriceLowerLimit() != null) {
            put(params, "priceMin", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            put(params, "priceMax", request.getPriceUpperLimit());
        }
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        List<CpsGoodsItem> items = new ArrayList<>();
        for (JsonNode item : arrayNode(data, "dataList", "list", "items")) {
            items.add(mapGoods(item));
        }
        if (data.isArray()) {
            items.clear();
            data.forEach(item -> items.add(mapGoods(item)));
        }
        long total = firstLong(response, "/total", "/data/total", "/data/totalCount");
        return CpsGoodsSearchResult.builder().list(items).total(total > 0 ? total : items.size())
                .nextPageId(firstText(response, "/searchId", "/data/searchId"))
                .pageNo(request.getPageNo()).pageSize(request.getPageSize()).build();
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (StringUtils.hasText(request.getItemLink()) || StringUtils.hasText(request.getOriginalContent())) {
            put(params, "text", StringUtils.hasText(request.getItemLink())
                    ? request.getItemLink() : request.getOriginalContent());
        } else if (StringUtils.hasText(request.getGoodsId())) {
            String goodsId = request.getGoodsId().trim();
            if (goodsId.startsWith("act:") || goodsId.startsWith("act_") || goodsId.startsWith("activity:")) {
                String actId = goodsId.substring(goodsId.indexOf(':') > 0 ? goodsId.indexOf(':') + 1 : goodsId.indexOf('_') + 1);
                put(params, "actId", actId);
            } else {
                put(params, "productViewSign", goodsId);
            }
        }
        put(params, "sid", firstText(request.getAdzoneId(), firstText(request.getExternalId(), config == null ? null : config.getDefaultAdzoneId())));
        put(params, "linkType", integerConfig(config, "linkType", 1));
        put(params, "platform", integerConfig(config, "platform", 1));
        put(params, "bizLine", integerConfig(config, "bizLine", null));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        String longUrl = data.isTextual() ? data.asText() : firstText(response,
                "/referralLinkMap/1", "/data/referralLinkMap/1", "/data/link", "/data/url");
        String shortUrl = firstText(response, "/referralLinkMap/2", "/data/referralLinkMap/2");
        if (!StringUtils.hasText(shortUrl) && StringUtils.hasText(longUrl)) {
            shortUrl = longUrl;
        } else if (!StringUtils.hasText(longUrl) && StringUtils.hasText(shortUrl)) {
            longUrl = shortUrl;
        }
        String mobileUrl = firstText(response, "/referralLinkMap/3", "/data/referralLinkMap/3");
        String wxMiniPath = firstText(response, "/referralLinkMap/4", "/data/referralLinkMap/4");
        String tpwd = firstText(response, "/referralLinkMap/5", "/data/referralLinkMap/5");

        Map<String, Object> extra = new LinkedHashMap<>();
        if (data.isObject()) {
            data.fields().forEachRemaining(entry -> extra.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
        }
        put(extra, "skuViewId", firstText(response, "/skuViewId", "/data/skuViewId"));
        put(extra, "productViewSign", firstText(response, "/productViewSign", "/data/productViewSign"));
        if (StringUtils.hasText(wxMiniPath)) {
            put(extra, "wxMiniPath", wxMiniPath);
        }
        if (StringUtils.hasText(mobileUrl)) {
            put(extra, "deeplink", mobileUrl);
        }
        return CpsPromotionLinkResult.builder()
                .longUrl(longUrl)
                .shortUrl(shortUrl)
                .mobileUrl(mobileUrl)
                .tpwd(tpwd)
                .extraFields(extra)
                .rawPayload(toJson(response))
                .build();
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        put(params, "platform", integerConfig(config, "platform", 1));
        put(params, "businessLine", listConfig(config, "businessLine"));
        put(params, "queryTimeType", request.getQueryType() != null && request.getQueryType() == 4 ? 2 : 1);
        put(params, "tradeType", integerConfig(config, "tradeType", 1));
        put(params, "searchType", 2);
        put(params, "scrollId", request.getPositionIndex());
        put(params, "startTime", epochSeconds(request.getStartTime()));
        put(params, "endTime", epochSeconds(request.getEndTime()));
        put(params, "page", 1);
        put(params, "limit", request.getPageSize() != null ? Math.min(request.getPageSize(), 100) : 20);
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        JsonNode data = response.path("data");
        JsonNode list = data.path("dataList");
        if (!list.isArray()) {
            list = response.path("dataList");
        }
        if (!list.isArray() && data.isArray()) {
            list = data;
        }
        List<CpsOrderDTO> orders = new ArrayList<>();
        if (list.isArray()) {
            list.forEach(item -> orders.add(mapOrder(item, firstText(response, "/scrollId", "/data/scrollId"))));
        }
        return orders;
    }

    @Override
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request, CpsVendorConfig config) {
        JsonNode response = executeRequest(getOrderQueryApiPath(), buildOrderQueryParams(request, config), config);
        if (response == null || !isSuccessResponse(response)) {
            throw new CpsVendorException("CPS vendor order query failed [official:meituan]: upstream rejected request");
        }
        List<CpsOrderDTO> orders = parseOrderQueryResponse(response);
        String scrollId = firstText(response, "/scrollId", "/data/scrollId");
        Boolean hasMore = firstBoolean(response, "/hasNext", "/data/hasNext");
        if (scrollId != null && (hasMore == null || hasMore)) {
            return CpsOrderPageResult.cursor(orders, scrollId, true);
        }
        return CpsOrderPageResult.page(orders, null, false);
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        JsonNode response = executeRequest(getOrderQueryApiPath(), buildOrderQueryParams(request, config), config);
        if (response == null || !isSuccessResponse(response)) {
            log.warn("[美团联盟官方] 订单批量查询失败");
            return Collections.emptyList();
        }
        return parseOrderQueryResponse(response);
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        return Map.of("searchText", "美食", "pageNo", 1, "pageSize", 1);
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        if (config == null || !StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            throw new CpsVendorException("美团官方 API 缺少 appKey 或 appSecret");
        }
        try {
            String baseUrl = StringUtils.hasText(config.getApiBaseUrl()) ? config.getApiBaseUrl().trim() : DEFAULT_BASE_URL;
            URI baseUri = URI.create(baseUrl);
            String basePath = baseUri.getPath() != null ? baseUri.getPath().replaceAll("/+$", "") : "";
            String requestPath = path.startsWith("/") ? path : "/" + path;
            String fullPath = requestPath.startsWith(basePath) ? requestPath : basePath + requestPath;
            String url = baseUri.getScheme() + "://" + baseUri.getAuthority() + fullPath;
            String canonicalPath = fullPath;

            String body = objectMapper.writeValueAsString(params == null ? Map.of() : params);
            Map<String, String> signedHeaders = MeituanApiGatewaySigner.buildSignedHeaders(
                    config.getAppKey(), config.getAppSecret(), "POST", canonicalPath,
                    Map.of(), Map.of(), body);

            int timeoutMs = timeoutMs(config);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            for (Map.Entry<String, String> entry : signedHeaders.entrySet()) {
                if (StringUtils.hasText(entry.getValue())) {
                    reqBuilder.header(entry.getKey(), entry.getValue());
                }
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[美团联盟官方] HTTP状态异常: status={}, body={}", response.statusCode(), response.body());
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (CpsVendorException e) {
            throw e;
        } catch (Exception e) {
            throw new CpsVendorException("美团官方 API 请求失败", e);
        }
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        return root != null && root.path("code").asInt(-1) == 0;
    }

    private CpsGoodsItem mapGoods(JsonNode item) {
        JsonNode detail = item.path("couponPackDetail");
        BigDecimal original = decimal(detail, "originalPrice");
        BigDecimal actual = decimal(detail, "sellPrice");
        BigDecimal commissionRate = decimal(item.path("commissionInfo"), "commissionPercent");
        if (commissionRate != null) commissionRate = commissionRate.divide(BigDecimal.valueOf(100));
        return CpsGoodsItem.builder()
                .goodsId(firstText(item, "/productViewSign", "/skuViewId", "/couponPackDetail/productViewSign", "/couponPackDetail/skuViewId"))
                .platformCode(getPlatformCode())
                .vendorCode(getVendorCode())
                .title(text(detail, "name"))
                .mainPic(text(detail, "headUrl"))
                .originalPrice(original)
                .actualPrice(actual)
                .couponPrice(original != null && actual != null ? original.subtract(actual) : null)
                .commissionRate(commissionRate)
                .commissionAmount(decimal(item.path("commissionInfo"), "commission"))
                .monthSales(parseLong(text(detail, "saleVolume")))
                .brandName(text(item.path("brandInfo"), "brandName"))
                .categoryName(text(item, "categoryName"))
                .couponStartTime(formatEpoch(detail.path("startTime")))
                .couponEndTime(formatEpoch(detail.path("endTime")))
                .source(getVendorCode())
                .extraFields(goodsExtra(item))
                .rawPayload(toJson(item))
                .build();
    }

    private CpsOrderDTO mapOrder(JsonNode item, String scrollId) {
        int status = item.path("status").asInt(0);
        if (status != 0) {
            status = switch (status) {
                case 2 -> 1;
                case 3, 6, 7 -> 3;
                case 4, 5, 8 -> -1;
                default -> status;
            };
        }
        if (status == 0) {
            status = switch (item.path("couponStatus").asInt(0)) {
                case 1 -> 1;
                case 2 -> 2;
                case 3 -> 3;
                case 4 -> -1;
                default -> 0;
            };
        }
        BigDecimal profit = firstDecimal(item, "profit", "cpaProfit", "couponFee");
        BigDecimal rate = decimal(item, "commissionRate");
        if (rate != null) rate = rate.divide(BigDecimal.valueOf(100));

        JsonNode orderTimeNode = !item.path("orderTime").isMissingNode() && !item.path("orderTime").isNull()
                ? item.path("orderTime") : item.path("payTime");
        JsonNode payTimeNode = item.path("payTime");
        int quantity = item.path("quantity").asInt(1);
        if (quantity <= 0) {
            quantity = 1;
        }

        return CpsOrderDTO.builder()
                .platformOrderId(firstText(item, "/orderId", "/orderViewId", "/itemOrderId"))
                .platformCode(getPlatformCode())
                .vendorCode(getVendorCode())
                .itemId(firstText(item, "/productId", "/productViewSign"))
                .itemTitle(firstText(item, "/productName", "/itemName", "/title"))
                .finalPrice(firstDecimal(item, "payPrice", "basicAmount", "actualPrice"))
                .commissionRate(rate)
                .commissionAmount(profit)
                .quantity(quantity)
                .platformStatus(status)
                .orderTime(formatEpoch(orderTimeNode))
                .payTime(formatEpoch(payTimeNode))
                .adzoneId(text(item, "sid"))
                .refundTag(status == -1 || status == 4 || status == 5 || status == 8 ? 1 : 0)
                .nextPositionIndex(scrollId)
                .extraFields(orderExtra(item))
                .rawPayload(toJson(item))
                .build();
    }

    private Map<String, Object> orderExtra(JsonNode item) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("businessLine", item.path("businessLine").asInt());
        extra.put("tradeType", item.path("tradeType").asInt());
        String productViewSign = text(item, "productViewSign");
        if (StringUtils.hasText(productViewSign)) {
            extra.put("productViewSign", productViewSign);
        }
        String updateTime = formatEpoch(item.path("updateTime"));
        if (updateTime != null) {
            extra.put("updateTime", updateTime);
        }
        return extra;
    }

    private int timeoutMs(CpsVendorConfig config) {
        String raw = config.getExtraConfig() == null ? null : config.getExtraConfig().get("timeoutMs");
        try {
            return Math.max(500, Math.min(Integer.parseInt(raw), 30_000));
        } catch (Exception ignored) {
            return 5_000;
        }
    }

    private Integer integerConfig(CpsVendorConfig config, String key, Integer fallback) {
        String value = configValue(config, key);
        try {
            return value == null ? fallback : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private List<Integer> listConfig(CpsVendorConfig config, String key) {
        String raw = configValue(config, key);
        if (!StringUtils.hasText(raw)) return null;
        List<Integer> result = new ArrayList<>();
        for (String value : raw.split(",")) {
            try {
                result.add(Integer.valueOf(value.trim()));
            } catch (Exception ignored) {
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String configValue(CpsVendorConfig config, String key) {
        return config == null || config.getExtraConfig() == null ? null : config.getExtraConfig().get(key);
    }

    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        return node == null || node.path(field).isMissingNode() || node.path(field).isNull() ? null : node.path(field).asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        try {
            String value = text(node, field);
            return StringUtils.hasText(value) ? new BigDecimal(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal firstDecimal(JsonNode node, String... fields) {
        for (String field : fields) {
            BigDecimal value = decimal(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private long firstLong(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode value = root.at(pointer);
            if (value.canConvertToLong()) return value.asLong();
        }
        return 0L;
    }

    private Boolean firstBoolean(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode value = root.at(pointer);
            if (value.isBoolean()) return value.asBoolean();
        }
        return null;
    }

    private String firstText(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode value = root.at(pointer);
            if (!value.isMissingNode() && !value.isNull() && StringUtils.hasText(value.asText())) return value.asText();
        }
        return null;
    }

    private List<JsonNode> arrayNode(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                value.forEach(list::add);
                return list;
            }
        }
        return List.of();
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return node == null ? null : node.toString();
        }
    }

    private Map<String, Object> goodsExtra(JsonNode item) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("platform", item.path("platform").asInt(1));
        if (!item.path("bizLine").isNull() && !item.path("bizLine").isMissingNode()) {
            extra.put("bizLine", item.path("bizLine").asInt());
        }
        return extra;
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Long epochSeconds(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return java.time.LocalDateTime.parse(value, DATE_TIME).atZone(SHANGHAI).toEpochSecond();
        } catch (Exception e) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private String formatEpoch(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        try {
            long value = node.asLong();
            if (value <= 0) return null;
            if (value < 10_000_000_000L) value *= 1000;
            return Instant.ofEpochMilli(value).atZone(SHANGHAI).format(DATE_TIME);
        } catch (Exception e) {
            return node.asText(null);
        }
    }

    private static final List<CpsThirdPartyActivity> OFFICIAL_MEITUAN_ACTIVITIES = List.of(
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:866")
                    .promotionActivityId("866")
                    .activityName("【限定城市】粉丝应援，美妆专场")
                    .activityType("美妆个护")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/b7d0c325c889f029b3f3607faeb31681132501.jpg")
                    .shortDesc("星妆应援，粉丝专属优惠价格")
                    .rebateDesc("红包归因1.3%，页面归因0.4%")
                    .billingType("CPS")
                    .promotionCount(8600)
                    .tagText("美妆专场")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&actId=866")
                    .searchKeyword("粉丝应援美妆")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("866", "美妆个护", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:855")
                    .promotionActivityId("855")
                    .activityName("【美团闪购】-精选商家专场")
                    .activityType("商超生鲜")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/efefc824b711b0df5c7b9d703daf87c2146349.jpg")
                    .shortDesc("闪购水果生鲜、超市速达，领神券再下单！")
                    .rebateDesc("会场内商家券0%-8% 页面0%-6%")
                    .billingType("CPS")
                    .promotionCount(15800)
                    .tagText("精选商家")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&actId=855")
                    .searchKeyword("闪购精选商家")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("855", "商超生鲜", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:852")
                    .promotionActivityId("852")
                    .activityName("【美团闪购】-开学季专场")
                    .activityType("商超生鲜")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/f34299e8f679c3cb7e9f8d652f3311f0113094.jpg")
                    .shortDesc("领59-15神券，开学好物，六折备齐！")
                    .rebateDesc("日百红包2% 页面2%，数码红包0.1%")
                    .billingType("CPS")
                    .promotionCount(9200)
                    .tagText("开学季")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&actId=852")
                    .searchKeyword("开学季专场")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("852", "商超生鲜", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:850")
                    .promotionActivityId("850")
                    .activityName("【美团闪购】-神价频道页")
                    .activityType("商超生鲜")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/efefc824b711b0df5c7b9d703daf87c2146349.jpg")
                    .shortDesc("最高享5元无门槛神价频道红包！")
                    .rebateDesc("爆品专区3%，其他券2%，页面1%")
                    .billingType("CPS")
                    .promotionCount(18600)
                    .tagText("神价频道")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&actId=850")
                    .searchKeyword("神价频道页")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("850", "商超生鲜", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:33")
                    .promotionActivityId("33")
                    .activityName("美团外卖天天神券")
                    .activityType("外卖餐饮")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p1.meituan.net/marketingcpsmedia/9e4470f4ed627e3b9001a6155ae3bd1490318.jpg")
                    .shortDesc("美团外卖红包天天领，最高可领66元外卖神券，每日限领一次")
                    .rebateDesc("最高6%返利")
                    .billingType("CPS")
                    .promotionCount(12850)
                    .tagText("外卖")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=waimai")
                    .searchKeyword("美团外卖")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("33", "外卖餐饮", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:104")
                    .promotionActivityId("104")
                    .activityName("美团外卖拼好饭")
                    .activityType("外卖餐饮")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/293e41a6f8c9546e8c1fdfac5573dec3122594.png")
                    .shortDesc("外卖拼单低至9.9元包邮，无套路免配送费，打工人干饭神器")
                    .rebateDesc("最高5%返利")
                    .billingType("CPS")
                    .promotionCount(11200)
                    .tagText("拼好饭")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=pinhaofan")
                    .searchKeyword("拼好饭")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("104", "外卖餐饮", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:115")
                    .promotionActivityId("115")
                    .activityName("美团外卖下午茶夜宵专区")
                    .activityType("外卖餐饮")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p1.meituan.net/marketingcpsmedia/a8e03e67c00e1da5e8cfc2eb0dcfd54e125890.jpg")
                    .shortDesc("奶茶咖啡、烧烤小龙虾限时满减，专属夜宵红包天天送")
                    .rebateDesc("最高6%返利")
                    .billingType("CPS")
                    .promotionCount(7890)
                    .tagText("下午茶")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=afternoon")
                    .searchKeyword("下午茶夜宵")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("115", "外卖餐饮", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:101")
                    .promotionActivityId("101")
                    .activityName("美团品牌餐饮神券节")
                    .activityType("外卖餐饮")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/b7d0c325c889f029b3f3607faeb31681132501.jpg")
                    .shortDesc("大牌餐饮联合让利，肯德基、麦当劳、星巴克大额神券狂欢")
                    .rebateDesc("最高8%返利")
                    .billingType("CPS")
                    .promotionCount(9430)
                    .tagText("大牌餐饮")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=brandfest")
                    .searchKeyword("神券节")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("101", "外卖餐饮", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:4")
                    .promotionActivityId("4")
                    .activityName("美团闪购商超红包天天领")
                    .activityType("商超生鲜")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/efefc824b711b0df5c7b9d703daf87c2146349.jpg")
                    .shortDesc("美团闪购超市便利生鲜大额红包天天领，最高领33元，最快30分钟送达")
                    .rebateDesc("最高4.5%返利")
                    .billingType("CPS")
                    .promotionCount(9680)
                    .tagText("闪购")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=shangou")
                    .searchKeyword("美团闪购")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("4", "商超生鲜", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:43")
                    .promotionActivityId("43")
                    .activityName("小象超市(美团买菜)专享券")
                    .activityType("即时零售")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/f34299e8f679c3cb7e9f8d652f3311f0113094.jpg")
                    .shortDesc("小象超市(原美团买菜)每日专享补贴，最快30分钟送达")
                    .rebateDesc("最高4%返利")
                    .billingType("CPS")
                    .promotionCount(5200)
                    .tagText("生鲜")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=maicai")
                    .searchKeyword("小象超市")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("43", "即时零售", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:22")
                    .promotionActivityId("22")
                    .activityName("美团优选今日特惠")
                    .activityType("社区生鲜")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/d3ddbe27664ab7e6a0ea20b815c2f6a0142667.jpg")
                    .shortDesc("美团优选次日自提，新鲜蔬菜水果爆品秒杀，每日更新")
                    .rebateDesc("最高10%返利")
                    .billingType("CPS")
                    .promotionCount(7510)
                    .tagText("优选")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=youxuan")
                    .searchKeyword("美团优选")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("22", "社区生鲜", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:111")
                    .promotionActivityId("111")
                    .activityName("美团买药天天满减")
                    .activityType("医疗健康")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/c4d87bf898442c554e207908b8b9a9f2134560.jpg")
                    .shortDesc("24小时急用药送药上门，家庭常备药立减优惠，专属健康红包")
                    .rebateDesc("最高5%返利")
                    .billingType("CPS")
                    .promotionCount(4300)
                    .tagText("买药")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=maiyao")
                    .searchKeyword("美团买药")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("111", "医疗健康", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:2")
                    .promotionActivityId("2")
                    .activityName("吃喝玩乐 每日福利")
                    .activityType("到店餐饮")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p1.meituan.net/marketingcpsmedia/82ee55b87a75745c73c6f2f472b0c543121899.png")
                    .shortDesc("美团到店美食团购、大牌秒杀3折起，每周五还可抽大额神券")
                    .rebateDesc("最高8%返利")
                    .billingType("CPS")
                    .promotionCount(8420)
                    .tagText("美食")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=food")
                    .searchKeyword("美团美食")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("2", "到店餐饮", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:10")
                    .promotionActivityId("10")
                    .activityName("美团到店餐饮大牌特惠")
                    .activityType("到店餐饮")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/5df218080c38865adca035db2bb31980145620.jpg")
                    .shortDesc("精选堂食代金券低至5折，大牌正餐工作日聚餐优选")
                    .rebateDesc("最高7%返利")
                    .billingType("CPS")
                    .promotionCount(6150)
                    .tagText("堂食")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=tangshi")
                    .searchKeyword("堂食特惠")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("10", "到店餐饮", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:23")
                    .promotionActivityId("23")
                    .activityName("美团丽人美发美甲SPA")
                    .activityType("到店综合")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/12ea07c08a9c2e08df0983bf98bcefa1124560.jpg")
                    .shortDesc("美发剪烫染、美甲美睫、美容SPA大额代金券限时秒杀")
                    .rebateDesc("最高9%返利")
                    .billingType("CPS")
                    .promotionCount(5380)
                    .tagText("丽人")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=liren")
                    .searchKeyword("美团丽人")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("23", "到店综合", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:112")
                    .promotionActivityId("112")
                    .activityName("美团休闲娱乐特惠")
                    .activityType("休闲娱乐")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p1.meituan.net/marketingcpsmedia/84ee35b87a75745c73c6f2f472b0c543134567.jpg")
                    .shortDesc("KTV轰趴、洗浴汗蒸、密室桌游周末聚会专属折扣券")
                    .rebateDesc("最高8%返利")
                    .billingType("CPS")
                    .promotionCount(4920)
                    .tagText("休闲")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=xiuxian")
                    .searchKeyword("休闲娱乐")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("112", "休闲娱乐", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:8")
                    .promotionActivityId("8")
                    .activityName("美团电影特惠观影")
                    .activityType("电影演出")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/79bfa50e972f65a78129df62a67e1234125890.jpg")
                    .shortDesc("院线热映大片在线特惠选座，大额满减券天天领")
                    .rebateDesc("最高6%返利")
                    .billingType("CPS")
                    .promotionCount(6800)
                    .tagText("电影")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=movie")
                    .searchKeyword("美团电影")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("8", "电影演出", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:7")
                    .promotionActivityId("7")
                    .activityName("美团酒店门票度假狂欢")
                    .activityType("酒旅出行")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/e800ff3136dfcba36c5a8a1f3689aabe241160.png")
                    .shortDesc("特惠酒店民宿低至5折起，周边游门票限时减，周末出游优选")
                    .rebateDesc("最高5%返利")
                    .billingType("CPS")
                    .promotionCount(6300)
                    .tagText("酒旅")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=hotel")
                    .searchKeyword("美团酒店")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("7", "酒旅出行", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:12")
                    .promotionActivityId("12")
                    .activityName("美团门票景区周边游")
                    .activityType("景区门票")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/35d4fa0e972f65a78129df62a67e5678123450.jpg")
                    .shortDesc("全国热门5A景区、主题乐园门票立减，亲子游度假首选")
                    .rebateDesc("最高5%返利")
                    .billingType("CPS")
                    .promotionCount(5720)
                    .tagText("门票")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=menpiao")
                    .searchKeyword("景区门票")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("12", "景区门票", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:51")
                    .promotionActivityId("51")
                    .activityName("美团打车通勤福利")
                    .activityType("打车出行")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p1.meituan.net/marketingcpsmedia/755ed8475934cd8a843e57088752160e180396.jpg")
                    .shortDesc("美团打车立减大额立减券天天领，通勤出行安全便捷")
                    .rebateDesc("最高5%返利")
                    .billingType("CPS")
                    .promotionCount(4890)
                    .tagText("出行")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=taxi")
                    .searchKeyword("美团打车")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("51", "打车出行", true))
                    .build(),
            CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:52")
                    .promotionActivityId("52")
                    .activityName("美团单车畅骑月卡")
                    .activityType("绿色出行")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic("https://p0.meituan.net/marketingcpsmedia/52ddbe27664ab7e6a0ea20b815c2f6a0142667.jpg")
                    .shortDesc("美团单车/电单车骑行立减券，周卡月卡折扣天天享")
                    .rebateDesc("最高6%返利")
                    .billingType("CPS")
                    .promotionCount(4120)
                    .tagText("骑车")
                    .jumpType("url")
                    .jumpUrl("https://click.meituan.com/t?t=1&c=1&p=bike")
                    .searchKeyword("美团单车")
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(buildMeituanExtraFields("52", "绿色出行", true))
                    .build()
    );

    @Override
    public CpsThirdPartyPage<CpsThirdPartyActivity> fetchActivities(CpsThirdPartyActivityRequest request,
                                                                    CpsVendorConfig config) {
        int pageNo = request != null && request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1;
        int pageSize = request != null && request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 20;
        String keyword = request != null ? firstText(request.getKeyword(), request.getCategoryName()) : null;

        // 1. 若配置了美团联盟后台 Cookie，尝试直连物料中心接口拉取最新在线活动
        String cookie = configValue(config, "cookie");
        if (StringUtils.hasText(cookie)) {
            try {
                CpsThirdPartyPage<CpsThirdPartyActivity> onlinePage = fetchOnlinePcActivities(request, cookie, pageNo, pageSize, keyword);
                if (onlinePage != null && onlinePage.getList() != null && !onlinePage.getList().isEmpty()) {
                    return onlinePage;
                }
            } catch (Exception e) {
                log.warn("[美团联盟官方] 调用物料中心 /ipc/pcActivityList 在线拉取失败，降级为内置官方活动: {}", e.getMessage());
            }
        }

        // 2. 默认或降级：使用系统全面内置的美团官方物料中心活动列表
        List<CpsThirdPartyActivity> filtered = new ArrayList<>();
        for (CpsThirdPartyActivity activity : OFFICIAL_MEITUAN_ACTIVITIES) {
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim();
                boolean match = activity.getActivityName().contains(kw)
                        || (activity.getActivityType() != null && activity.getActivityType().contains(kw))
                        || (activity.getTagText() != null && activity.getTagText().contains(kw))
                        || (activity.getSearchKeyword() != null && activity.getSearchKeyword().contains(kw));
                if (!match) {
                    continue;
                }
            }
            filtered.add(activity);
        }

        // 3. 若配置了官方 AppKey/AppSecret，通过官方开放 API /query_coupon 动态补充卡券活动
        if (config != null && StringUtils.hasText(config.getAppKey()) && StringUtils.hasText(config.getAppSecret())) {
            try {
                CpsGoodsSearchRequest searchReq = new CpsGoodsSearchRequest();
                searchReq.setKeyword(StringUtils.hasText(keyword) ? keyword : "美食");
                searchReq.setPageNo(pageNo);
                searchReq.setPageSize(Math.min(pageSize, 10));
                CpsGoodsSearchResult searchResult = searchGoods(searchReq, config);
                if (searchResult != null && searchResult.getList() != null) {
                    for (CpsGoodsItem item : searchResult.getList()) {
                        if (item.getGoodsId() != null && !item.getGoodsId().isEmpty()) {
                            filtered.add(mapCouponToActivity(item));
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("[美团联盟官方] 拉取动态优惠券活动跳过: {}", e.getMessage());
            }
        }

        int total = filtered.size();
        int fromIndex = Math.min((pageNo - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<CpsThirdPartyActivity> pageList = fromIndex < toIndex ? filtered.subList(fromIndex, toIndex) : Collections.emptyList();

        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(pageList)
                .total((long) total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .nextPageId(toIndex < total ? String.valueOf(pageNo + 1) : null)
                .build();
    }

    CpsThirdPartyPage<CpsThirdPartyActivity> fetchOnlinePcActivities(CpsThirdPartyActivityRequest request,
                                                                     String cookie,
                                                                     int pageNo,
                                                                     int pageSize,
                                                                     String keyword) throws Exception {
        // 请求美团联盟后台“物料推广 > 活动推广”真实接口 /ipc/pcQueryMaterialList
        String url = IPC_BASE_URL + "/ipc/pcQueryMaterialList";
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("activityId", "");
        bodyMap.put("materialType", 1);
        bodyMap.put("pageNum", pageNo);
        bodyMap.put("pageSize", pageSize);

        String jsonBody = objectMapper.writeValueAsString(bodyMap);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Origin", "https://media.meituan.com")
                .header("Referer", "https://media.meituan.com/pc/index.html")
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(6))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 || !StringUtils.hasText(response.body())) {
            log.warn("[美团联盟官方] 请求物料中心 /ipc/pcQueryMaterialList 状态异常: {}", response.statusCode());
            return null;
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        int code = rootNode.path("code").asInt(-1);
        if (code != 0) {
            log.warn("[美团联盟官方] 请求物料中心 /ipc/pcQueryMaterialList 失败: code={}, msg={}",
                    code, rootNode.path("msg").asText());
            return null;
        }

        JsonNode dataNode = rootNode.path("data");
        JsonNode listNode = dataNode.path("list");
        if (!listNode.isArray() && dataNode.path("activityList").isArray()) {
            listNode = dataNode.path("activityList");
        }
        if (!listNode.isArray() || listNode.isEmpty()) {
            return null;
        }

        List<CpsThirdPartyActivity> activities = new ArrayList<>();
        for (JsonNode item : listNode) {
            String actId = firstNonBlank(
                    item.path("materialId").asText(null),
                    item.path("activityId").asText(null)
            );
            if (!StringUtils.hasText(actId)) {
                continue;
            }
            String actName = firstNonBlank(
                    item.path("materialName").asText(null),
                    item.path("activityName").asText(null),
                    "美团活动"
            );
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim();
                if (!actName.contains(kw) && !item.path("materialDesc").asText("").contains(kw)) {
                    continue;
                }
            }
            String actPic = firstNonBlank(
                    item.path("listPic").asText(null),
                    item.path("activityPic").asText(null),
                    ""
            );
            String commissionRate = firstNonBlank(
                    item.path("incomeRate").asText(null),
                    item.path("commissionRate").asText(null),
                    "官方返利"
            );
            String actDesc = firstNonBlank(
                    item.path("materialDesc").asText(null),
                    item.path("activityDesc").asText(null),
                    actName
            );
            long startTime = item.path("validStart").asLong(item.path("activityStartTime").asLong(0));
            long endTime = item.path("validEnd").asLong(item.path("activityEndTime").asLong(0));
            String pcJumpLink = item.path("pcJumpLink").asText(null);

            Map<String, Object> extra = buildMeituanExtraFields(actId, "官方活动", true);
            extra.put("onlineSource", "ipc_pcQueryMaterialList");
            if (StringUtils.hasText(pcJumpLink)) {
                extra.put("pcJumpLink", pcJumpLink);
            }

            activities.add(CpsThirdPartyActivity.builder()
                    .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                    .externalActivityId("mt:act:" + actId)
                    .promotionActivityId(actId)
                    .activityName(actName)
                    .activityType("官方推广")
                    .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                    .mainPic(actPic)
                    .shortDesc(actDesc)
                    .rebateDesc(commissionRate)
                    .billingType("CPS")
                    .promotionCount(1000)
                    .tagText("官方物料")
                    .jumpType("url")
                    .jumpUrl(StringUtils.hasText(pcJumpLink) ? pcJumpLink : "https://click.meituan.com/t?t=1&c=1&actId=" + actId)
                    .searchKeyword(actName)
                    .supportsList(true)
                    .supportsPromotionLink(true)
                    .supportsOrders(true)
                    .supportsMiniProgram(true)
                    .supportsLocalLife(true)
                    .extraFields(extra)
                    .startTime(startTime > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime < 10_000_000_000L ? startTime * 1000 : startTime), SHANGHAI) : null)
                    .endTime(endTime > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime < 10_000_000_000L ? endTime * 1000 : endTime), SHANGHAI) : null)
                    .build());
        }

        long total = dataNode.path("total").asLong(activities.size());
        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .category(CpsThirdPartyApiCategory.ACTIVITY_PULL)
                .list(activities)
                .total(total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .nextPageId((long) pageNo * pageSize < total ? String.valueOf(pageNo + 1) : null)
                .build();
    }

    private static Map<String, Object> buildMeituanExtraFields(String actId, String category, boolean supportsLocalLife) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("row_id", "mt:act:" + actId);
        extra.put("activity_id", actId);
        extra.put("platform", CpsPlatformCodeEnum.MEITUAN.getCode());
        extra.put("category", category);
        extra.put("supportsList", true);
        extra.put("supportsPromotionLink", true);
        extra.put("supportsOrders", true);
        extra.put("supportsMiniProgram", true);
        extra.put("supportsLocalLife", supportsLocalLife);
        return extra;
    }

    private CpsThirdPartyActivity mapCouponToActivity(CpsGoodsItem item) {
        String goodsId = item.getGoodsId();
        String rebateDesc = item.getCommissionRate() != null
                ? item.getCommissionRate().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%"
                : "预估返利";
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("row_id", "mt:coupon:" + goodsId);
        extra.put("product_view_sign", goodsId);
        extra.put("platform", CpsPlatformCodeEnum.MEITUAN.getCode());
        extra.put("supportsList", true);
        extra.put("supportsPromotionLink", true);
        extra.put("supportsOrders", true);
        extra.put("supportsMiniProgram", true);
        extra.put("supportsLocalLife", true);

        return CpsThirdPartyActivity.builder()
                .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                .externalActivityId("mt:coupon:" + goodsId)
                .promotionActivityId("coupon:" + goodsId)
                .activityName(item.getTitle() != null ? item.getTitle() : "美团精选卡券活动")
                .activityType(item.getCategoryName() != null ? item.getCategoryName() : "卡券优惠")
                .platformCode(CpsPlatformCodeEnum.MEITUAN.getCode())
                .mainPic(item.getMainPic())
                .shortDesc(item.getTitle())
                .rebateDesc(rebateDesc)
                .billingType("CPS")
                .promotionCount(item.getMonthSales() != null ? item.getMonthSales().intValue() : 100)
                .tagText("卡券")
                .jumpType("url")
                .jumpUrl("")
                .searchKeyword(item.getTitle())
                .supportsList(true)
                .supportsPromotionLink(true)
                .supportsOrders(true)
                .supportsMiniProgram(true)
                .supportsLocalLife(true)
                .extraFields(extra)
                .build();
    }
}
