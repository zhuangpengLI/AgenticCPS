package com.qiji.cps.module.cps.client.csj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.CpsVendorGovernancePolicy;
import com.qiji.cps.module.cps.client.common.AbstractApiVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 穿山甲电商联盟（CSJ Pangolin E-commerce）抖音电商 CPS 供应商客户端
 *
 * <p>对接穿山甲电商联盟开放接口（如《订单查询接口》28733）：
 * <ul>
 *   <li>网关地址：https://ecom.pangolin-sdk-toutiao.com（支持 http/https）</li>
 *   <li>订单查询接口：/order/search（支持分页拉取与按订单状态流转对账）</li>
 *   <li>商品搜索接口：/product/search</li>
 *   <li>商品转链接口：/product/link</li>
 *   <li>认证与签名：基于 MD5 签名机制与 data 紧凑 JSON 报文封装</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class CsjDouyinVendorClient extends AbstractApiVendorClient {

    private static final String DEFAULT_BASE_URL = "https://ecom.pangolin-sdk-toutiao.com";
    private static final String ORDER_SEARCH_PATH = "/order/search";
    private static final String PRODUCT_SEARCH_PATH = "/product/search";
    private static final String PRODUCT_LINK_PATH = "/product/link";

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public CsjDouyinVendorClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public CsjDouyinVendorClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.CSJ.getCode();
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.DOUYIN.getCode();
    }

    @Override
    public String getVendorType() {
        return "aggregator";
    }

    @Override
    protected boolean isSuccessResponse(JsonNode response) {
        return response != null && response.path("code").asInt(-1) == 0;
    }

    @Override
    public CpsVendorGovernancePolicy getGovernancePolicy() {
        return CpsVendorGovernancePolicy.builder()
                .timeoutMillis(5000)
                .rateLimitPerMinute(120)
                .circuitBreakerOpenMillis(10000)
                .metricsEnabled(true)
                .build();
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
                CpsVendorConfigField.optional("defaultAdzoneId", false),
                CpsVendorConfigField.optional("apiBaseUrl", false),
                CpsVendorConfigField.optional("timeoutMs", false)
        ));
    }

    // ==================== 模板方法实现 ====================

    @Override
    protected String getSearchApiPath() {
        return PRODUCT_SEARCH_PATH;
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> dataMap = new LinkedHashMap<>();
        if (StringUtils.hasText(request.getKeyword())) {
            dataMap.put("keyword", request.getKeyword().trim());
        }
        int page = request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1;
        int size = request.getPageSize() != null && request.getPageSize() > 0 ? Math.min(request.getPageSize(), 50) : 20;
        dataMap.put("page", page);
        dataMap.put("size", size);
        if (request.getPriceLowerLimit() != null) {
            dataMap.put("min_price", request.getPriceLowerLimit().multiply(BigDecimal.valueOf(100)).longValue());
        }
        if (request.getPriceUpperLimit() != null) {
            dataMap.put("max_price", request.getPriceUpperLimit().multiply(BigDecimal.valueOf(100)).longValue());
        }
        return dataMap;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        int page = request.getPageNo() != null && request.getPageNo() > 0 ? request.getPageNo() : 1;
        int size = request.getPageSize() != null && request.getPageSize() > 0 ? Math.min(request.getPageSize(), 50) : 20;
        return parseGoodsSearchResult(response, page, size);
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return PRODUCT_LINK_PATH;
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> dataMap = new LinkedHashMap<>();
        if (StringUtils.hasText(request.getGoodsId())) {
            dataMap.put("product_id", request.getGoodsId().trim());
        } else if (StringUtils.hasText(request.getItemLink())) {
            dataMap.put("product_url", request.getItemLink().trim());
        }
        String customInfo = StringUtils.hasText(request.getExternalId()) ? request.getExternalId() : request.getAdzoneId();
        if (StringUtils.hasText(customInfo)) {
            dataMap.put("custom_info", customInfo.trim());
        }
        if (StringUtils.hasText(request.getAdzoneId())) {
            dataMap.put("pid", request.getAdzoneId().trim());
        }
        return dataMap;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        String clickUrl = firstText(data, "/click_url", "/promotion_url", "/url", "/short_url");
        String shortUrl = firstText(data, "/short_url", "/short_click_url");
        String deepLink = firstText(data, "/deep_link", "/schema", "/open_url");
        String dyPassword = firstText(data, "/dy_password", "/token", "/command");

        return CpsPromotionLinkResult.builder()
                .shortUrl(shortUrl)
                .longUrl(clickUrl)
                .tpwd(dyPassword)
                .mobileUrl(deepLink)
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return ORDER_SEARCH_PATH;
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        return buildOrderSearchData(request, config);
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        JsonNode data = response.path("data");
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode orderArray = firstArray(data, "orders", "order_list", "list");
        if (orderArray == null && data.isArray()) {
            orderArray = data;
        }
        if (orderArray == null) {
            orderArray = firstArray(response, "orders", "order_list");
        }
        String nextCursor = firstText(data, "/cursor", "/next_cursor", "/next_page");
        if (orderArray != null && orderArray.isArray()) {
            for (JsonNode item : orderArray) {
                orders.add(mapOrder(item, nextCursor));
            }
        }
        return orders;
    }

    @Override
    protected String getTestConnectionApiPath() {
        return ORDER_SEARCH_PATH;
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("size", 1);
        dataMap.put("cursor", "0");
        dataMap.put("start_time", now - 3600);
        dataMap.put("end_time", now);
        dataMap.put("order_type", 2);
        return dataMap;
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        return executePost(path, params, config);
    }

    // ==================== 业务接口实现 ====================

    @Override
    public boolean testConnection(CpsVendorConfig config) {
        String appId = resolveAppId(config);
        String secureKey = resolveSecureKey(config);
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(secureKey)) {
            log.warn("[穿山甲CSJ客户端] 测试连接失败：appId 或 secureKey 未配置");
            return false;
        }

        try {
            Map<String, Object> dataMap = buildTestConnectionParams();
            JsonNode response = executeRequest(ORDER_SEARCH_PATH, dataMap, config);
            int code = response.path("code").asInt(-1);
            if (code == 0) {
                log.info("[穿山甲CSJ客户端] 测试连接成功 [appId={}]", appId);
                return true;
            }
            log.warn("[穿山甲CSJ客户端] 测试连接失败，上游返回 code={}, message={}",
                    code, response.path("message").asText());
            return false;
        } catch (Exception e) {
            log.warn("[穿山甲CSJ客户端] 测试连接异常: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> dataMap = buildOrderSearchData(request, config);
        JsonNode response = executeRequest(ORDER_SEARCH_PATH, dataMap, config);

        int code = response.path("code").asInt(-1);
        if (code != 0) {
            String msg = response.path("message").asText("上游请求失败");
            throw CpsVendorException.upstreamRejected(getVendorCode(), getPlatformCode(),
                    CpsVendorCapability.ORDER_QUERY, String.valueOf(code), msg);
        }

        List<CpsOrderDTO> orders = parseOrderQueryResponse(response);
        JsonNode data = response.path("data");
        String nextCursor = firstText(data, "/cursor", "/next_cursor", "/next_page");
        Boolean hasMore = firstBoolean(data, "/has_more", "/hasMore");

        if (nextCursor != null && (hasMore == null || hasMore)) {
            return CpsOrderPageResult.cursor(orders, nextCursor, true);
        }
        return CpsOrderPageResult.page(orders, null, false);
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        return queryOrderPage(request, config).getItems();
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> buildOrderSearchData(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        int size = request.getPageSize() != null && request.getPageSize() > 0 ? Math.min(request.getPageSize(), 100) : 50;
        data.put("size", size);

        if (StringUtils.hasText(request.getPositionIndex())) {
            data.put("cursor", request.getPositionIndex().trim());
        } else if (request.getPageNo() != null && request.getPageNo() > 1) {
            data.put("cursor", String.valueOf((request.getPageNo() - 1) * size));
        } else {
            data.put("cursor", "0");
        }

        long now = Instant.now().getEpochSecond();
        long startTime = parseEpochSecond(request.getStartTime(), now - 86400);
        long endTime = parseEpochSecond(request.getEndTime(), now);
        data.put("start_time", startTime);
        data.put("end_time", endTime);

        // 订单查询类型：1 按照下单支付时间，2 按照更新时间（默认2用于状态变更对账）
        int orderType = (request.getQueryType() != null && request.getQueryType() == 1) ? 1 : 2;
        data.put("order_type", orderType);

        return data;
    }

    private JsonNode executePost(String path, Map<String, Object> dataMap, CpsVendorConfig config) {
        String appId = resolveAppId(config);
        String secureKey = resolveSecureKey(config);
        String baseUrl = resolveBaseUrl(config);

        if (!StringUtils.hasText(appId) || !StringUtils.hasText(secureKey)) {
            throw new CpsVendorException("CSJ Pangolin configuration missing appId or secureKey");
        }

        Map<String, Object> envelope = CsjOpenApiSigner.buildEnvelope(appId, secureKey, dataMap);
        String requestJson = CsjOpenApiSigner.compactJson(envelope);

        int timeoutMs = resolveTimeoutMs(config);
        String fullUrl = baseUrl.replaceAll("/+$", "") + path;

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (httpResponse.statusCode() >= 400) {
                throw new CpsVendorException("CSJ HTTP error: " + httpResponse.statusCode() + ", body: " + httpResponse.body());
            }
            return OBJECT_MAPPER.readTree(httpResponse.body());
        } catch (CpsVendorException e) {
            throw e;
        } catch (Exception e) {
            throw new CpsVendorException("Failed to invoke CSJ endpoint " + fullUrl + ": " + e.getMessage(), e);
        }
    }

    private CpsOrderDTO mapOrder(JsonNode item, String nextCursor) {
        String platformOrderId = firstText(item, "/order_id", "/orderId", "/id");
        String parentOrderId = firstText(item, "/parent_order_id", "/parentOrderId");
        String itemId = firstText(item, "/product_id", "/productId", "/goods_id");
        String itemTitle = firstText(item, "/product_name", "/productName", "/title");
        String itemPic = firstText(item, "/product_img", "/productImg", "/cover");

        BigDecimal finalPrice = toYuan(item, "total_pay_amount", "pay_amount", "pay_goods_amount", "payGoodsAmount");
        BigDecimal itemPrice = toYuan(item, "item_price", "price", "goods_price");
        if (itemPrice == null && finalPrice != null) {
            itemPrice = finalPrice;
        }

        BigDecimal commissionAmount = toYuan(item, "commission", "estimated_commission", "estimatedCommission", "cos_fee");
        BigDecimal commissionRate = parseCommissionRate(item, "commission_rate", "commissionRate", "cos_ratio");

        int quantity = firstInt(item, 1, "/item_num", "/itemNum", "/num");

        // 订单状态映射
        int rawStatus = parseOrderStatus(item);
        int refundTag = isRefunded(item, rawStatus) ? 1 : 0;

        String orderTime = formatDateTime(item, "order_time", "create_time");
        String payTime = formatDateTime(item, "pay_success_time", "pay_time");
        String settleTime = formatDateTime(item, "settle_time", "settled_time");

        String pid = firstText(item, "/pid", "/adzone_id");
        String externalId = firstText(item, "/custom_info", "/external_info", "/external_id");

        Map<String, Object> extraFields = new LinkedHashMap<>();
        if (item.has("flow_point")) {
            extraFields.put("flowPoint", item.path("flow_point").asText());
        }
        if (item.has("shop_name")) {
            extraFields.put("shopName", item.path("shop_name").asText());
        }

        return CpsOrderDTO.builder()
                .platformOrderId(platformOrderId)
                .parentOrderId(parentOrderId)
                .platformCode(getPlatformCode())
                .vendorCode(getVendorCode())
                .itemId(itemId)
                .itemTitle(itemTitle)
                .itemPic(itemPic)
                .itemPrice(itemPrice)
                .finalPrice(finalPrice)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .quantity(quantity)
                .platformStatus(rawStatus)
                .orderTime(orderTime)
                .payTime(payTime)
                .settleTime(settleTime)
                .adzoneId(pid)
                .externalId(externalId)
                .refundTag(refundTag)
                .nextPositionIndex(nextCursor)
                .extraFields(extraFields)
                .rawPayload(item.toString())
                .build();
    }

    private int parseOrderStatus(JsonNode item) {
        String flowPoint = firstText(item, "/flow_point", "/flowPoint", "/status_desc");
        if (StringUtils.hasText(flowPoint)) {
            String upper = flowPoint.toUpperCase(Locale.ROOT);
            if (upper.contains("PAY") || upper.contains("支付")) {
                return 1; // PAID
            }
            if (upper.contains("CONFIRM") || upper.contains("RECEIVE") || upper.contains("收货")) {
                return 2; // RECEIVED
            }
            if (upper.contains("SETTLE") || upper.contains("结算")) {
                return 3; // SETTLED
            }
            if (upper.contains("REFUND") || upper.contains("退款") || upper.contains("INVALID") || upper.contains("失效")) {
                return -1; // INVALID
            }
        }

        int status = item.path("status").asInt(item.path("order_status").asInt(0));
        return switch (status) {
            case 1 -> 1;  // 已支付
            case 2 -> 2;  // 已收货
            case 3 -> 3;  // 已结算
            case 4, 5, -1 -> -1; // 退款/失效
            default -> status;
        };
    }

    private boolean isRefunded(JsonNode item, int status) {
        if (status == -1) {
            return true;
        }
        String flowPoint = firstText(item, "/flow_point", "/flowPoint");
        if (StringUtils.hasText(flowPoint) && (flowPoint.toUpperCase(Locale.ROOT).contains("REFUND") || flowPoint.contains("退款"))) {
            return true;
        }
        return item.path("refund_status").asInt(0) > 0;
    }

    private CpsGoodsSearchResult parseGoodsSearchResult(JsonNode response, int page, int size) {
        JsonNode data = response.path("data");
        JsonNode listNode = firstArray(data, "products", "goods_list", "list");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (listNode != null && listNode.isArray()) {
            for (JsonNode node : listNode) {
                String goodsId = firstText(node, "/product_id", "/id");
                String title = firstText(node, "/product_name", "/title");
                String cover = firstText(node, "/product_img", "/cover");
                BigDecimal price = toYuan(node, "price", "origin_price");
                BigDecimal finalPrice = toYuan(node, "pay_price", "discount_price", "price");
                BigDecimal commissionAmount = toYuan(node, "commission", "estimated_commission");
                BigDecimal commissionRate = parseCommissionRate(node, "commission_rate", "cos_ratio");

                goodsList.add(CpsGoodsItem.builder()
                        .platformCode(getPlatformCode())
                        .vendorCode(getVendorCode())
                        .goodsId(goodsId)
                        .title(title)
                        .mainPic(cover)
                        .originalPrice(price)
                        .actualPrice(finalPrice != null ? finalPrice : price)
                        .commissionRate(commissionRate)
                        .commissionAmount(commissionAmount)
                        .itemLink(firstText(node, "/product_url", "/detail_url"))
                        .source(getVendorCode())
                        .build());
            }
        }
        long total = firstInt(data, goodsList.size(), "/total", "/total_count");
        return CpsGoodsSearchResult.builder()
                .list(goodsList)
                .total(total)
                .pageNo(page)
                .pageSize(size)
                .build();
    }

    private String resolveAppId(CpsVendorConfig config) {
        if (config == null) return null;
        if (StringUtils.hasText(config.getAppKey())) return config.getAppKey().trim();
        if (config.getExtraConfig() != null) {
            String val = config.getExtraConfig().get("appId");
            if (StringUtils.hasText(val)) return val.trim();
            val = config.getExtraConfig().get("app_id");
            if (StringUtils.hasText(val)) return val.trim();
        }
        return null;
    }

    private String resolveSecureKey(CpsVendorConfig config) {
        if (config == null) return null;
        if (StringUtils.hasText(config.getAppSecret())) return config.getAppSecret().trim();
        if (config.getExtraConfig() != null) {
            String val = config.getExtraConfig().get("secureKey");
            if (StringUtils.hasText(val)) return val.trim();
            val = config.getExtraConfig().get("secure_key");
            if (StringUtils.hasText(val)) return val.trim();
        }
        return null;
    }

    private int resolveTimeoutMs(CpsVendorConfig config) {
        if (config != null && config.getExtraConfig() != null) {
            String val = config.getExtraConfig().get("timeoutMs");
            if (StringUtils.hasText(val)) {
                try {
                    return Integer.parseInt(val.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 5000;
    }

    private String resolveBaseUrl(CpsVendorConfig config) {
        if (config != null && StringUtils.hasText(config.getApiBaseUrl())) {
            return config.getApiBaseUrl().trim();
        }
        return DEFAULT_BASE_URL;
    }

    private BigDecimal toYuan(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = item.path(fieldName);
            if (node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                long val = node.asLong();
                return BigDecimal.valueOf(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            if (node.isTextual() && StringUtils.hasText(node.asText())) {
                try {
                    String str = node.asText().trim();
                    if (str.contains(".")) {
                        return new BigDecimal(str).setScale(2, RoundingMode.HALF_UP);
                    }
                    long val = Long.parseLong(str);
                    return BigDecimal.valueOf(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private BigDecimal parseCommissionRate(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = item.path(fieldName);
            if (node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                double val = node.asDouble();
                if (val > 100) {
                    return BigDecimal.valueOf(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
                return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
            }
            if (node.isTextual() && StringUtils.hasText(node.asText())) {
                try {
                    double val = Double.parseDouble(node.asText().trim());
                    if (val > 100) {
                        return BigDecimal.valueOf(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    }
                    return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private long parseEpochSecond(Object timeObj, long fallback) {
        if (timeObj == null) {
            return fallback;
        }
        if (timeObj instanceof Number n) {
            long val = n.longValue();
            return val > 1_000_000_000_000L ? val / 1000 : val;
        }
        if (timeObj instanceof String s && StringUtils.hasText(s)) {
            try {
                if (s.matches("^\\d+$")) {
                    long val = Long.parseLong(s);
                    return val > 1_000_000_000_000L ? val / 1000 : val;
                }
                return java.time.LocalDateTime.parse(s, DATE_TIME_FORMATTER)
                        .atZone(SHANGHAI_ZONE)
                        .toEpochSecond();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private String formatDateTime(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = item.path(fieldName);
            if (node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                long val = node.asLong();
                Instant instant = val > 1_000_000_000_000L ? Instant.ofEpochMilli(val) : Instant.ofEpochSecond(val);
                return DATE_TIME_FORMATTER.format(instant.atZone(SHANGHAI_ZONE));
            }
            if (node.isTextual() && StringUtils.hasText(node.asText())) {
                String str = node.asText().trim();
                if (str.matches("^\\d+$")) {
                    long val = Long.parseLong(str);
                    Instant instant = val > 1_000_000_000_000L ? Instant.ofEpochMilli(val) : Instant.ofEpochSecond(val);
                    return DATE_TIME_FORMATTER.format(instant.atZone(SHANGHAI_ZONE));
                }
                return str;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... jsonPointers) {
        for (String pointer : jsonPointers) {
            JsonNode target = pointer.startsWith("/") ? node.at(pointer) : node.path(pointer);
            if (!target.isMissingNode() && !target.isNull() && StringUtils.hasText(target.asText())) {
                return target.asText();
            }
        }
        return null;
    }

    private int firstInt(JsonNode node, int defaultValue, String... jsonPointers) {
        for (String pointer : jsonPointers) {
            JsonNode target = pointer.startsWith("/") ? node.at(pointer) : node.path(pointer);
            if (target.isNumber()) {
                return target.asInt();
            }
            if (target.isTextual() && StringUtils.hasText(target.asText())) {
                try {
                    return Integer.parseInt(target.asText().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultValue;
    }

    private JsonNode firstArray(JsonNode node, String... fieldNames) {
        if (node == null) return null;
        for (String fieldName : fieldNames) {
            JsonNode target = node.path(fieldName);
            if (target.isArray()) {
                return target;
            }
        }
        return null;
    }

    private Boolean firstBoolean(JsonNode node, String... jsonPointers) {
        if (node == null) return null;
        for (String pointer : jsonPointers) {
            JsonNode target = pointer.startsWith("/") ? node.at(pointer) : node.path(pointer);
            if (target.isBoolean()) {
                return target.asBoolean();
            }
            if (target.isTextual() && StringUtils.hasText(target.asText())) {
                return Boolean.parseBoolean(target.asText().trim());
            }
        }
        return null;
    }
}
