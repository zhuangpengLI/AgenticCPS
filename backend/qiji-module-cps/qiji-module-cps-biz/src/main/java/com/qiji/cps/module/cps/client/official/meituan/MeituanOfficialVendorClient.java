package com.qiji.cps.module.cps.client.official.meituan;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 美团联盟官方 API 客户端。 */
@Slf4j
@Component
public class MeituanOfficialVendorClient extends AbstractOfficialVendorClient {

    static final String DEFAULT_BASE_URL = "https://media.meituan.com/cps_open/common/api/v1";
    private static final String HEADER_APP = "S-Ca-App";
    private static final String HEADER_TIMESTAMP = "S-Ca-Timestamp";
    private static final String HEADER_SIGNATURE = "S-Ca-Signature";
    private static final String HEADER_SIGNATURE_HEADERS = "S-Ca-Signature-Headers";
    private static final String HEADER_CONTENT_MD5 = "Content-MD5";
    private static final List<String> SIGNATURE_HEADERS = List.of(HEADER_APP, HEADER_TIMESTAMP);
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.MEITUAN.getCode();
    }

    @Override
    public Set<CpsVendorCapability> getCapabilities() {
        return EnumSet.of(CpsVendorCapability.GOODS_SEARCH, CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY, CpsVendorCapability.CONNECTION_TEST);
    }

    @Override protected String getSearchApiPath() { return "/query_coupon"; }
    @Override protected String getPromotionLinkApiPath() { return "/get_referral_link"; }
    @Override protected String getOrderQueryApiPath() { return "/query_order"; }
    @Override protected String getTestConnectionApiPath() { return "/query_coupon"; }

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
        } else {
            put(params, "productViewSign", request.getGoodsId());
        }
        put(params, "sid", firstText(request.getAdzoneId(), config == null ? null : config.getDefaultAdzoneId()));
        put(params, "linkType", integerConfig(config, "linkType", 1));
        put(params, "platform", integerConfig(config, "platform", 1));
        put(params, "bizLine", integerConfig(config, "bizLine", null));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        String longUrl = data.isTextual() ? data.asText() : firstText(response,
                "/referralLinkMap/1", "/referralLinkMap/2", "/data/referralLinkMap/1",
                "/data/referralLinkMap/2", "/data/link", "/data/url");
        String shortUrl = firstText(response, "/referralLinkMap/2", "/data/referralLinkMap/2");
        Map<String, Object> extra = new LinkedHashMap<>();
        if (data.isObject()) {
            data.fields().forEachRemaining(entry -> extra.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
        }
        put(extra, "skuViewId", firstText(response, "/skuViewId", "/data/skuViewId"));
        put(extra, "productViewSign", firstText(response, "/productViewSign", "/data/productViewSign"));
        return CpsPromotionLinkResult.builder().longUrl(longUrl).shortUrl(shortUrl)
                .extraFields(extra).rawPayload(toJson(response)).build();
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
        put(params, "limit", request.getPageSize());
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        JsonNode data = response.path("data");
        JsonNode list = data.path("dataList");
        if (!list.isArray()) {
            list = response.path("dataList");
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

    @Override protected Map<String, Object> buildTestConnectionParams() {
        return Map.of("searchText", "美食", "pageNo", 1, "pageSize", 1);
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        if (config == null || !StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            throw new CpsVendorException("美团官方 API 缺少 appKey 或 appSecret");
        }
        try {
            String baseUrl = StringUtils.hasText(config.getApiBaseUrl()) ? config.getApiBaseUrl() : DEFAULT_BASE_URL;
            String requestPath = path.startsWith("/") ? path : "/" + path;
            String url = baseUrl.replaceAll("/+$", "") + requestPath;
            String basePath = URI.create(baseUrl).getPath();
            String canonicalPath = (StringUtils.hasText(basePath) ? basePath.replaceAll("/+$", "") : "") + requestPath;
            String body = objectMapper.writeValueAsString(params == null ? Map.of() : params);
            String contentMd5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Content-Type", "application/json; charset=UTF-8");
            headers.put(HEADER_CONTENT_MD5, contentMd5);
            headers.put(HEADER_APP, config.getAppKey());
            headers.put(HEADER_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
            headers.put(HEADER_SIGNATURE_HEADERS, MeituanApiGatewaySigner.signatureHeaderValue(SIGNATURE_HEADERS));
            headers.put(HEADER_SIGNATURE, MeituanApiGatewaySigner.sign(config.getAppSecret(), "POST", canonicalPath,
                    headers, Map.of(), SIGNATURE_HEADERS));

            int timeoutMs = timeoutMs(config);
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .header("Accept", headers.get("Accept"))
                    .header("Content-Type", headers.get("Content-Type"))
                    .header(HEADER_CONTENT_MD5, contentMd5)
                    .header(HEADER_APP, headers.get(HEADER_APP))
                    .header(HEADER_TIMESTAMP, headers.get(HEADER_TIMESTAMP))
                    .header(HEADER_SIGNATURE_HEADERS, headers.get(HEADER_SIGNATURE_HEADERS))
                    .header(HEADER_SIGNATURE, headers.get(HEADER_SIGNATURE))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(timeoutMs)).build()
                    .send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("[美团联盟官方] HTTP状态异常: status={}", response.statusCode());
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (CpsVendorException e) {
            throw e;
        } catch (Exception e) {
            throw new CpsVendorException("美团官方 API 请求失败", e);
        }
    }

    @Override protected boolean isSuccessResponse(JsonNode root) {
        return root != null && root.path("code").asInt(-1) == 0;
    }

    private CpsGoodsItem mapGoods(JsonNode item) {
        JsonNode detail = item.path("couponPackDetail");
        BigDecimal original = decimal(detail, "originalPrice");
        BigDecimal actual = decimal(detail, "sellPrice");
        BigDecimal commissionRate = decimal(item.path("commissionInfo"), "commissionPercent");
        if (commissionRate != null) commissionRate = commissionRate.divide(BigDecimal.valueOf(100));
        return CpsGoodsItem.builder().goodsId(firstText(item, "/productViewSign", "/skuViewId", "/couponPackDetail/productViewSign", "/couponPackDetail/skuViewId"))
                .platformCode(getPlatformCode()).vendorCode(getVendorCode()).title(text(detail, "name"))
                .mainPic(text(detail, "headUrl")).originalPrice(original).actualPrice(actual)
                .couponPrice(original != null && actual != null ? original.subtract(actual) : null)
                .commissionRate(commissionRate).commissionAmount(decimal(item.path("commissionInfo"), "commission"))
                .monthSales(parseLong(text(detail, "saleVolume"))).brandName(text(item.path("brandInfo"), "brandName"))
                .categoryName(text(item, "categoryName")).couponStartTime(formatEpoch(detail.path("startTime")))
                .couponEndTime(formatEpoch(detail.path("endTime"))).source(getVendorCode())
                .extraFields(goodsExtra(item))
                .rawPayload(toJson(item)).build();
    }

    private CpsOrderDTO mapOrder(JsonNode item, String scrollId) {
        int status = item.path("status").asInt(0);
        if (status != 0) {
            status = switch (status) {
                case 2 -> 1;
                case 3, 6 -> 3;
                case 4, 5 -> -1;
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
        return CpsOrderDTO.builder().platformOrderId(firstText(item, "/orderId", "/orderViewId", "/itemOrderId"))
                .platformCode(getPlatformCode()).vendorCode(getVendorCode())
                .itemId(firstText(item, "/productId", "/productViewSign")).itemTitle(text(item, "productName"))
                .finalPrice(firstDecimal(item, "payPrice", "basicAmount")).commissionRate(rate).commissionAmount(profit)
                .quantity(1).platformStatus(status).orderTime(formatEpoch(item.path("payTime")))
                .payTime(formatEpoch(item.path("payTime"))).adzoneId(text(item, "sid"))
                .refundTag(status == -1 || status == 4 || status == 5 ? 1 : 0).nextPositionIndex(scrollId)
                .extraFields(orderExtra(item))
                .rawPayload(toJson(item)).build();
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
        try { return Math.max(500, Math.min(Integer.parseInt(raw), 30_000)); } catch (Exception ignored) { return 5_000; }
    }

    private Integer integerConfig(CpsVendorConfig config, String key, Integer fallback) {
        String value = configValue(config, key);
        try { return value == null ? fallback : Integer.valueOf(value); } catch (NumberFormatException e) { return fallback; }
    }

    private List<Integer> listConfig(CpsVendorConfig config, String key) {
        String raw = configValue(config, key);
        if (!StringUtils.hasText(raw)) return null;
        List<Integer> result = new ArrayList<>();
        for (String value : raw.split(",")) { try { result.add(Integer.valueOf(value.trim())); } catch (Exception ignored) { } }
        return result.isEmpty() ? null : result;
    }

    private String configValue(CpsVendorConfig config, String key) {
        return config == null || config.getExtraConfig() == null ? null : config.getExtraConfig().get(key);
    }

    private void put(Map<String, Object> map, String key, Object value) { if (value != null) map.put(key, value); }
    private String firstText(String first, String second) { return StringUtils.hasText(first) ? first : second; }
    private String text(JsonNode node, String field) { return node == null || node.path(field).isMissingNode() || node.path(field).isNull() ? null : node.path(field).asText(); }
    private BigDecimal decimal(JsonNode node, String field) { try { String value = text(node, field); return StringUtils.hasText(value) ? new BigDecimal(value) : null; } catch (Exception e) { return null; } }
    private BigDecimal firstDecimal(JsonNode node, String... fields) { for (String field : fields) { BigDecimal value = decimal(node, field); if (value != null) return value; } return null; }
    private long firstLong(JsonNode root, String... pointers) { for (String pointer : pointers) { JsonNode value = root.at(pointer); if (value.canConvertToLong()) return value.asLong(); } return 0L; }
    private Boolean firstBoolean(JsonNode root, String... pointers) { for (String pointer : pointers) { JsonNode value = root.at(pointer); if (value.isBoolean()) return value.asBoolean(); } return null; }
    private String firstText(JsonNode root, String... pointers) { for (String pointer : pointers) { JsonNode value = root.at(pointer); if (!value.isMissingNode() && !value.isNull() && StringUtils.hasText(value.asText())) return value.asText(); } return null; }
    private List<JsonNode> arrayNode(JsonNode node, String... fields) { for (String field : fields) { JsonNode value = node.path(field); if (value.isArray()) { List<JsonNode> list = new ArrayList<>(); value.forEach(list::add); return list; } } return List.of(); }
    private String toJson(JsonNode node) { try { return objectMapper.writeValueAsString(node); } catch (Exception e) { return node == null ? null : node.toString(); } }
    private Map<String, Object> goodsExtra(JsonNode item) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("platform", item.path("platform").asInt(1));
        if (!item.path("bizLine").isNull() && !item.path("bizLine").isMissingNode()) {
            extra.put("bizLine", item.path("bizLine").asInt());
        }
        return extra;
    }
    private Long parseLong(String value) { try { return value == null ? null : Long.parseLong(value.replaceAll("[^0-9]", "")); } catch (Exception e) { return null; } }
    private Long epochSeconds(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return java.time.LocalDateTime.parse(value, DATE_TIME).atZone(SHANGHAI).toEpochSecond();
        } catch (Exception e) {
            try { return Long.valueOf(value); } catch (NumberFormatException ignored) { return null; }
        }
    }
    private String formatEpoch(JsonNode node) { if (node == null || node.isMissingNode() || node.isNull()) return null; try { long value = node.asLong(); if (value <= 0) return null; if (value < 10_000_000_000L) value *= 1000; return Instant.ofEpochMilli(value).atZone(SHANGHAI).format(DATE_TIME); } catch (Exception e) { return node.asText(null); } }
}
