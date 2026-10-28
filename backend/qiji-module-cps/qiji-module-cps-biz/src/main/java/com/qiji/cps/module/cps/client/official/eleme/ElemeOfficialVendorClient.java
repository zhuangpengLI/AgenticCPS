package com.qiji.cps.module.cps.client.official.eleme;

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
 * 淘宝闪购联盟官方 API 供应商客户端（阿里本地生活 TOP OpenAPI）
 *
 * <p>对接阿里本地生活/淘宝闪购联盟官方开放平台 API：
 * <ul>
 *   <li>网关地址：https://eco.taobao.com/router/rest（预发环境：http://pre-gw.api.taobao.com/router/rest）</li>
 *   <li>官方活动查询与推广转链：alibaba.alsc.union.eleme.promotion.officialactivity.get</li>
 *   <li>单店推广店铺列表（导购选品）：alibaba.alsc.union.eleme.promotion.storepromotion.query</li>
 *   <li>单店推广单店铺查询与转链：alibaba.alsc.union.eleme.promotion.storepromotion.get</li>
 *   <li>媒体推广正向订单明细报表：alibaba.alsc.union.kbcpx.positive.order.get</li>
 *   <li>媒体推广逆向维权订单报表：alibaba.alsc.union.kbcpx.refund.order.get</li>
 *   <li>媒体推广反作弊风控订单报表：alibaba.alsc.union.kbcpx.punish.order.get</li>
 *   <li>会员管理（SID 溯源）：支持 sid 传参及订单回传，用于精确会员归因与营销返利</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class ElemeOfficialVendorClient extends AbstractOfficialVendorClient
        implements CpsThirdPartyActivityVendorClient {

    public static final String DEFAULT_BASE_URL = "https://eco.taobao.com/router/rest";
    public static final String SANDBOX_BASE_URL = "http://pre-gw.api.taobao.com/router/rest";

    public static final String API_OFFICIAL_ACTIVITY_GET = "alibaba.alsc.union.eleme.promotion.officialactivity.get";
    public static final String API_STORE_PROMOTION_QUERY = "alibaba.alsc.union.eleme.promotion.storepromotion.query";
    public static final String API_STORE_PROMOTION_GET = "alibaba.alsc.union.eleme.promotion.storepromotion.get";
    public static final String API_ORDER_DETAILS_GET = "alibaba.alsc.union.kbcpx.positive.order.get";
    public static final String API_ORDER_REFUND_GET = "alibaba.alsc.union.kbcpx.refund.order.get";
    public static final String API_ORDER_PUNISH_GET = "alibaba.alsc.union.kbcpx.punish.order.get";

    public static final String DEFAULT_ACTIVITY_ID = "10144"; // 淘宝闪购天天领红包

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HttpClient httpClient;

    public ElemeOfficialVendorClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ElemeOfficialVendorClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.ELEME.getCode();
    }

    @Override
    public Set<CpsVendorCapability> getCapabilities() {
        return EnumSet.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST,
                CpsVendorCapability.ACTIVITY_PULL
        );
    }

    @Override
    public CpsVendorConfigSchema getConfigSchema() {
        return new CpsVendorConfigSchema(List.of(
                CpsVendorConfigField.required("appKey", true),
                CpsVendorConfigField.required("appSecret", true),
                CpsVendorConfigField.required("defaultAdzoneId", false), // 阿里本地生活媒体推广位 PID
                CpsVendorConfigField.optional("apiBaseUrl", false),
                CpsVendorConfigField.optional("timeoutMs", false)
        ));
    }

    @Override
    public CpsVendorGovernancePolicy getGovernancePolicy() {
        return CpsVendorGovernancePolicy.builder()
                .timeoutMillis(5000)
                .rateLimitPerMinute(100) // 淘宝闪购联盟订单 API 限流 100 QPM
                .circuitBreakerOpenMillis(10000)
                .metricsEnabled(true)
                .maskedDiagnosticsEnabled(true)
                .build();
    }

    // ==================== 商品搜索 / 门店导购选品 ====================

    @Override
    protected String getSearchApiPath() {
        return API_STORE_PROMOTION_QUERY;
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> queryRequest = new LinkedHashMap<>();
        String pid = firstText(config.getDefaultAdzoneId(), "default_pid");
        queryRequest.put("pid", pid);

        if (StringUtils.hasText(request.getKeyword())) {
            queryRequest.put("keyword", request.getKeyword());
        }
        if (config.getExtraConfig() != null && config.getExtraConfig().get("session_id") != null) {
            queryRequest.put("session_id", config.getExtraConfig().get("session_id"));
        }
        if (config.getExtraConfig() != null && config.getExtraConfig().get("city_id") != null) {
            queryRequest.put("city_id", config.getExtraConfig().get("city_id"));
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query_request", toJsonString(queryRequest));
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode responseRoot, CpsGoodsSearchRequest request) {
        JsonNode resp = responseRoot.path("alibaba_alsc_union_eleme_promotion_storepromotion_query_response");
        if (resp.isMissingNode()) {
            resp = responseRoot;
        }
        JsonNode result = resp.path("result");
        JsonNode data = result.path("data");
        if (data.isMissingNode() && resp.has("data")) {
            data = resp.path("data");
        }

        JsonNode storeList = data.path("store_list");
        if (storeList.isMissingNode() && data.path("dataList").isArray()) {
            storeList = data.path("dataList");
        }

        String nextSessionId = firstText(data, "/session_id", "session_id", "/sessionId", "sessionId");
        boolean hasMore = data.has("has_more") ? data.path("has_more").asBoolean(true)
                : data.has("hasMore") ? data.path("hasMore").asBoolean(true) : false;

        List<CpsGoodsItem> items = new ArrayList<>();
        if (storeList.isArray()) {
            for (JsonNode store : storeList) {
                String shopId = firstText(store, "/shop_id", "shop_id", "/shopId", "shopId");
                String shopName = firstText(store, "/shop_name", "shop_name", "/shopName", "shopName", "/name", "name");
                String shopLogo = firstText(store, "/shop_logo", "shop_logo", "/shopLogo", "shopLogo", "/logo", "logo");
                BigDecimal minPrice = firstNonZeroDecimal(store, "/min_price", "min_price", "/minPrice", "minPrice", "/price", "price");
                BigDecimal commissionRate = firstNonZeroDecimal(store, "/commission_rate", "commission_rate", "/commissionRate", "commissionRate");

                items.add(CpsGoodsItem.builder()
                        .goodsId(shopId)
                        .title(shopName)
                        .mainPic(shopLogo)
                        .shopName(shopName)
                        .platformCode(getPlatformCode())
                        .vendorCode(getVendorCode())
                        .originalPrice(minPrice != null ? minPrice : BigDecimal.ZERO)
                        .actualPrice(minPrice != null ? minPrice : BigDecimal.ZERO)
                        .commissionRate(commissionRate != null ? commissionRate : BigDecimal.ZERO)
                        .categoryName(firstText(store, "/category_name", "category_name", "/categoryName", "categoryName"))
                        .rawPayload(store.toString())
                        .build());
            }
        }

        return CpsGoodsSearchResult.builder()
                .list(items)
                .total((long) items.size())
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .nextPageId(nextSessionId)
                .build();
    }

    // ==================== 推广转链（官方活动 / 单店推广） ====================

    @Override
    protected String getPromotionLinkApiPath() {
        return API_OFFICIAL_ACTIVITY_GET;
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        String pid = firstText(request.getAdzoneId(), config.getDefaultAdzoneId());
        String sid = request.getExternalId();

        // 判断是否为单店推广（goodsId 以 E 开头或带有门店标识）
        String shopId = extractShopId(request);
        boolean isStorePromotion = StringUtils.hasText(shopId);

        Map<String, Object> queryRequest = new LinkedHashMap<>();
        queryRequest.put("pid", pid);
        if (StringUtils.hasText(sid)) {
            // SID 长度建议不超过 100 字符
            queryRequest.put("sid", sid.length() > 100 ? sid.substring(0, 100) : sid);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        if (isStorePromotion) {
            queryRequest.put("shop_id", shopId);
            if (StringUtils.hasText(request.getGoodsId()) && !request.getGoodsId().equals(shopId)) {
                queryRequest.put("activity_id", request.getGoodsId());
            }
            queryRequest.put("include_wx_img", false);
            params.put("_api_method_", API_STORE_PROMOTION_GET);
        } else {
            String activityId = firstText(request.getGoodsId(), DEFAULT_ACTIVITY_ID);
            queryRequest.put("activity_id", activityId);
            params.put("_api_method_", API_OFFICIAL_ACTIVITY_GET);
        }

        params.put("query_request", toJsonString(queryRequest));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot) {
        JsonNode resp = responseRoot.path("alibaba_alsc_union_eleme_promotion_officialactivity_get_response");
        if (resp.isMissingNode()) {
            resp = responseRoot.path("alibaba_alsc_union_eleme_promotion_storepromotion_get_response");
        }
        if (resp.isMissingNode()) {
            resp = responseRoot;
        }

        JsonNode result = resp.path("result");
        JsonNode data = result.path("data");
        if (data.isMissingNode() && resp.has("data")) {
            data = resp.path("data");
        }

        Map<String, Object> extraFields = new LinkedHashMap<>();
        if (data.hasNonNull("wx_app_id")) {
            extraFields.put("wx_appid", data.path("wx_app_id").asText());
        }
        if (data.hasNonNull("wx_path")) {
            extraFields.put("wx_path", data.path("wx_path").asText());
        }
        if (data.hasNonNull("url_scheme")) {
            extraFields.put("url_scheme", data.path("url_scheme").asText());
        }
        if (data.hasNonNull("tb_open_url")) {
            extraFields.put("tb_open_url", data.path("tb_open_url").asText());
        }
        if (data.hasNonNull("shop_id")) {
            extraFields.put("shop_id", data.path("shop_id").asText());
        }
        if (data.hasNonNull("shop_name")) {
            extraFields.put("shop_name", data.path("shop_name").asText());
        }

        String shortUrl = firstText(data, "/short_url", "short_url", "/shortUrl", "shortUrl");
        String longUrl = firstText(data, "/click_url", "click_url", "/url", "url", "/clickUrl", "clickUrl");
        String tpwd = firstText(data, "/tb_command", "tb_command", "/tkl", "tkl", "/full_taobao_word", "full_taobao_word", "/tpwd", "tpwd");
        String mobileUrl = firstText(data, "/url_scheme", "url_scheme", "/tb_open_url", "tb_open_url", "/mobile_url", "mobile_url");

        return CpsPromotionLinkResult.builder()
                .shortUrl(shortUrl)
                .longUrl(longUrl)
                .tpwd(tpwd)
                .mobileUrl(mobileUrl)
                .extraFields(extraFields)
                .rawPayload(responseRoot.toString())
                .build();
    }

    // ==================== 订单明细查询 ====================

    @Override
    protected String getOrderQueryApiPath() {
        return API_ORDER_DETAILS_GET;
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        // date_type: 1-付款时间 2-创建时间 3-结算时间 4-更新时间 (推荐4-更新时间)
        int dateType = request.getQueryType() != null ? request.getQueryType() : 4;
        params.put("date_type", String.valueOf(dateType));

        String startTime = formatToDateTimeString(request.getStartTime());
        String endTime = formatToDateTimeString(request.getEndTime());
        params.put("start_date", startTime);
        if (StringUtils.hasText(endTime)) {
            params.put("end_date", endTime);
        }

        // biz_unit: 1-CPA, 2-CPS (固定为 2)
        params.put("biz_unit", "2");

        int pageSize = request.getPageSize() != null ? Math.min(request.getPageSize(), 50) : 50;
        int pageNo = request.getPageNo() != null ? request.getPageNo() : 1;
        params.put("page_size", String.valueOf(pageSize));
        params.put("page_number", String.valueOf(pageNo));

        String pid = firstText(config.getDefaultAdzoneId(), null);
        if (StringUtils.hasText(pid)) {
            params.put("pid", pid);
        }
        if (request.getOrderStatus() != null) {
            params.put("order_state", String.valueOf(request.getOrderStatus()));
        }

        params.put("order_channel", "taobao_shangou");
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode resp = responseRoot.path("alibaba_alsc_union_kbcpx_positive_order_get_response");
        if (resp.isMissingNode()) {
            resp = responseRoot;
        }

        JsonNode result = resp.path("result");
        JsonNode data = result.path("data");
        if (data.isMissingNode() && resp.has("data")) {
            data = resp.path("data");
        }

        JsonNode orderList = data.path("order_list");
        if (orderList.isMissingNode() && data.isArray()) {
            orderList = data;
        }
        if (!orderList.isArray()) {
            return orders;
        }

        for (JsonNode item : orderList) {
            String platformOrderId = firstText(item, "/biz_order_id", "biz_order_id", "/order_id", "order_id", "/trade_id", "trade_id");
            String parentOrderId = firstText(item, "/tb_trade_parent_id", "tb_trade_parent_id", "/trade_parent_id", "trade_parent_id");
            String itemId = firstText(item, "/item_id", "item_id", "/activity_id", "activity_id", "/shop_id", "shop_id");
            String itemTitle = firstText(item, "/item_title", "item_title", "/title", "title", "/activity_name", "activity_name", "/shop_name", "shop_name");
            String itemPic = firstText(item, "/item_pic", "item_pic", "/pic_url", "pic_url", "/shop_logo", "shop_logo");
            BigDecimal finalPrice = firstNonZeroDecimal(item, "/pay_amount", "pay_amount", "/pay_price", "pay_price", "/actual_amount", "actual_amount");
            BigDecimal commissionAmount = firstNonZeroDecimal(item, "/income", "income", "/settle", "settle", "/commission", "commission");

            Integer orderState = firstInt(item, "/order_state", "order_state", "/orderStatus", "orderStatus");
            Integer settleState = firstInt(item, "/settle_state", "settle_state", "/settled_status", "settled_status");
            Integer orderItemStatus = firstInt(item, "/order_item_status", "order_item_status");

            // 状态机转换：
            // order_state: 0-已失效, 1-已下单, 2-已付款, 4-已收货, 3-售中退款, 5-售后退款
            int mappedPlatformStatus;
            int refundTag = 0;
            if (orderState != null && orderState == 0) {
                mappedPlatformStatus = -1; // INVALID
            } else if (orderState != null && (orderState == 3 || orderState == 5)) {
                mappedPlatformStatus = -1;
                refundTag = 1;
            } else if (orderItemStatus != null && (orderItemStatus == 3 || orderItemStatus == 5)) {
                mappedPlatformStatus = -1;
                refundTag = 1;
            } else if (orderState != null && orderState == 1) {
                mappedPlatformStatus = 0; // CREATED
            } else if (orderState != null && orderState == 2) {
                mappedPlatformStatus = 1; // PAID
            } else if (orderState != null && orderState == 4) {
                mappedPlatformStatus = (settleState != null && settleState == 1) ? 3 : 2; // SETTLED 或 RECEIVED
            } else {
                mappedPlatformStatus = 0;
            }

            // sid 为核心会员追踪字段，必须提取到 externalId 用于会员自动归因
            String sid = firstText(item, "/sid", "sid", "/channel_code", "channel_code");

            Map<String, Object> extraFields = new LinkedHashMap<>();
            if (orderState != null) {
                extraFields.put("order_state", orderState);
            }
            if (settleState != null) {
                extraFields.put("settle_state", settleState);
            }
            if (orderItemStatus != null) {
                extraFields.put("order_item_status", orderItemStatus);
            }
            if (item.hasNonNull("flow_type")) {
                extraFields.put("flow_type", item.path("flow_type").asText());
            }

            orders.add(CpsOrderDTO.builder()
                    .platformCode(getPlatformCode())
                    .vendorCode(getVendorCode())
                    .platformOrderId(platformOrderId)
                    .parentOrderId(parentOrderId)
                    .itemId(itemId)
                    .itemTitle(itemTitle)
                    .itemPic(itemPic)
                    .finalPrice(finalPrice)
                    .commissionAmount(commissionAmount)
                    .platformStatus(mappedPlatformStatus)
                    .refundTag(refundTag)
                    .orderTime(firstText(item, "/create_time", "create_time", "/createTime", "createTime"))
                    .payTime(firstText(item, "/pay_time", "pay_time", "/paid_time", "paid_time", "/paidTime", "paidTime"))
                    .settleTime(firstText(item, "/settle_time", "settle_time", "/earning_time", "earning_time", "/settleTime", "settleTime"))
                    .externalId(sid)
                    .extraFields(extraFields)
                    .rawPayload(item.toString())
                    .build());
        }

        return orders;
    }

    // ==================== 连接测试 ====================

    @Override
    protected String getTestConnectionApiPath() {
        return API_OFFICIAL_ACTIVITY_GET;
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> queryRequest = new LinkedHashMap<>();
        queryRequest.put("pid", "test_pid");
        queryRequest.put("activity_id", DEFAULT_ACTIVITY_ID);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query_request", toJsonString(queryRequest));
        return params;
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams(CpsVendorConfig config) {
        String pid = firstText(config.getDefaultAdzoneId(), "test_pid");
        Map<String, Object> queryRequest = new LinkedHashMap<>();
        queryRequest.put("pid", pid);
        queryRequest.put("activity_id", DEFAULT_ACTIVITY_ID);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("query_request", toJsonString(queryRequest));
        return params;
    }

    // ==================== 官方活动拉取 (CpsThirdPartyActivityVendorClient) ====================

    @Override
    public CpsThirdPartyPage<CpsThirdPartyActivity> fetchActivities(CpsThirdPartyActivityRequest request,
                                                                   CpsVendorConfig config) {
        // 阿里本地生活淘宝闪购常驻在线官方推广活动列表
        List<CpsThirdPartyActivity> list = List.of(
                CpsThirdPartyActivity.builder()
                        .externalActivityId("eleme:official:10144")
                        .promotionActivityId("10144")
                        .activityName("淘宝闪购天天领红包")
                        .activityType("外卖闪购")
                        .platformCode(CpsPlatformCodeEnum.ELEME.getCode())
                        .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                        .mainPic("https://img.alicdn.com/imgextra/i3/O1CN01fUvOcm1fM8VwB8P3C_!!6000000003986-2-tps-750-375.png")
                        .shortDesc("淘宝闪购天天领大额红包，全国流量适用")
                        .rebateDesc("天天领大额红包，享闪购CPS高额返佣")
                        .billingType("CPS")
                        .tagText("全国流量")
                        .jumpType("url")
                        .supportsList(true)
                        .supportsPromotionLink(true)
                        .supportsOrders(true)
                        .supportsMiniProgram(true)
                        .supportsLocalLife(true)
                        .extraFields(Map.of("activityId", "10144", "trafficType", "全国流量"))
                        .build(),
                CpsThirdPartyActivity.builder()
                        .externalActivityId("eleme:official:10174")
                        .promotionActivityId("10174")
                        .activityName("浏览店铺做任务")
                        .activityType("任务领券")
                        .platformCode(CpsPlatformCodeEnum.ELEME.getCode())
                        .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                        .mainPic("https://img.alicdn.com/imgextra/i2/O1CN01s2Zc641H0H5k3Uo9m_!!6000000000699-2-tps-750-375.png")
                        .shortDesc("浏览店铺做任务领红包")
                        .rebateDesc("浏览品牌门店领任务红包")
                        .billingType("CPS")
                        .tagText("全国流量")
                        .jumpType("url")
                        .supportsList(true)
                        .supportsPromotionLink(true)
                        .supportsOrders(true)
                        .supportsMiniProgram(true)
                        .supportsLocalLife(true)
                        .extraFields(Map.of("activityId", "10174", "trafficType", "全国流量"))
                        .build(),
                CpsThirdPartyActivity.builder()
                        .externalActivityId("eleme:official:10183")
                        .promotionActivityId("10183")
                        .activityName("零售专属会场")
                        .activityType("超市零售")
                        .platformCode(CpsPlatformCodeEnum.ELEME.getCode())
                        .sourceType(CpsVendorCodeEnum.OFFICIAL.getCode())
                        .mainPic("https://img.alicdn.com/imgextra/i4/O1CN01dE6gY81XvD2wYf6Z6_!!6000000002985-2-tps-750-375.png")
                        .shortDesc("超市便利、生鲜水果零售专属会场")
                        .rebateDesc("超市便利、水果生鲜零售会场")
                        .billingType("CPS")
                        .tagText("全国流量")
                        .jumpType("url")
                        .supportsList(true)
                        .supportsPromotionLink(true)
                        .supportsOrders(true)
                        .supportsMiniProgram(true)
                        .supportsLocalLife(true)
                        .extraFields(Map.of("activityId", "10183", "trafficType", "全国流量"))
                        .build()
        );

        return CpsThirdPartyPage.<CpsThirdPartyActivity>builder()
                .list(list)
                .total((long) list.size())
                .pageNo(1)
                .pageSize(list.size())
                .build();
    }

    // ==================== HTTP 请求执行与响应判断 ====================

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        String method = path;
        if (params.containsKey("_api_method_")) {
            method = String.valueOf(params.remove("_api_method_"));
        }

        String baseUrl = resolveBaseUrl(config);
        String appKey = config.getAppKey();
        String appSecret = config.getAppSecret();

        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
            log.warn("[淘宝闪购官方] 未配置 appKey 或 appSecret，无法请求 TOP 网关");
            throw new CpsVendorException("淘宝闪购官方 API 配置缺少 appKey 或 appSecret");
        }

        Map<String, String> bizParams = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                bizParams.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        Map<String, String> signedParams = TaobaoTopSigner.buildSignedParams(method, appKey, appSecret, bizParams);
        String formBody = TaobaoTopSigner.buildFormUrlEncoded(signedParams);

        long timeoutMs = 5000L;
        if (config.getExtraConfig() != null && config.getExtraConfig().get("timeoutMs") != null) {
            try {
                timeoutMs = Long.parseLong(config.getExtraConfig().get("timeoutMs"));
            } catch (Exception ignored) {
            }
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("[淘宝闪购官方] HTTP 状态码异常: method={}, status={}, body={}",
                        method, response.statusCode(), response.body());
                return null;
            }

            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.error("[淘宝闪购官方] HTTP 请求调用异常: method={}, error={}", method, e.getMessage());
            throw new CpsVendorException("淘宝闪购官方 API 调用异常: " + e.getMessage(), e);
        }
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return false;
        }
        // TOP 响应中如果包含 error_response 则表示系统级或业务级错误
        if (root.has("error_response")) {
            JsonNode err = root.path("error_response");
            log.warn("[淘宝闪购官方] TOP 错误响应: code={}, msg={}, sub_code={}, sub_msg={}",
                    err.path("code").asText(), err.path("msg").asText(),
                    err.path("sub_code").asText(), err.path("sub_msg").asText());
            return false;
        }

        // 检查业务层 result.biz_code 或 success
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getKey().endsWith("_response")) {
                JsonNode respObj = entry.getValue();
                JsonNode result = respObj.path("result");
                if (!result.isMissingNode()) {
                    if (result.has("biz_code") && result.path("biz_code").asInt(0) != 0) {
                        log.warn("[淘宝闪购官方] 业务响应 biz_code 非0: code={}, msg={}",
                                result.path("biz_code").asText(), result.path("biz_msg").asText());
                        return false;
                    }
                    if (result.has("success") && !result.path("success").asBoolean(true)) {
                        log.warn("[淘宝闪购官方] 业务响应 success 为 false: msg={}",
                                result.path("biz_msg").asText());
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // ==================== 辅助方法 ====================

    private String resolveBaseUrl(CpsVendorConfig config) {
        if (config != null && StringUtils.hasText(config.getApiBaseUrl())) {
            return config.getApiBaseUrl();
        }
        return DEFAULT_BASE_URL;
    }

    private String extractShopId(CpsPromotionLinkRequest request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.hasText(request.getGoodsId())
                && (request.getGoodsId().startsWith("E") || request.getGoodsId().startsWith("e")
                || request.getGoodsId().length() > 8)) {
            return request.getGoodsId();
        }
        return null;
    }

    private String formatToDateTimeString(String timeStr) {
        if (!StringUtils.hasText(timeStr)) {
            return LocalDateTime.now(SHANGHAI_ZONE).format(DATE_TIME_FORMATTER);
        }
        // 如果是纯秒级时间戳
        if (timeStr.matches("\\d{10}")) {
            long epochSecond = Long.parseLong(timeStr);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), SHANGHAI_ZONE)
                    .format(DATE_TIME_FORMATTER);
        }
        // 如果是毫秒级时间戳
        if (timeStr.matches("\\d{13}")) {
            long epochMilli = Long.parseLong(timeStr);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), SHANGHAI_ZONE)
                    .format(DATE_TIME_FORMATTER);
        }
        return timeStr;
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("[淘宝闪购官方] 序列化请求对象异常: {}", e.getMessage());
            return "{}";
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String firstText(JsonNode root, String... pointers) {
        if (root == null || pointers == null) {
            return null;
        }
        for (String ptr : pointers) {
            JsonNode node = ptr.startsWith("/") ? root.at(ptr) : root.path(ptr);
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText())) {
                return node.asText();
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode root, String... pointers) {
        if (root == null || pointers == null) {
            return null;
        }
        for (String ptr : pointers) {
            JsonNode node = ptr.startsWith("/") ? root.at(ptr) : root.path(ptr);
            if (!node.isMissingNode() && !node.isNull() && (node.isInt() || node.isNumber())) {
                return node.asInt();
            }
        }
        return null;
    }

    private BigDecimal firstNonZeroDecimal(JsonNode root, String... pointers) {
        if (root == null || pointers == null) {
            return BigDecimal.ZERO;
        }
        for (String ptr : pointers) {
            JsonNode node = ptr.startsWith("/") ? root.at(ptr) : root.path(ptr);
            if (!node.isMissingNode() && !node.isNull()) {
                String text = node.asText();
                if (StringUtils.hasText(text)) {
                    try {
                        BigDecimal val = new BigDecimal(text);
                        if (val.compareTo(BigDecimal.ZERO) > 0) {
                            return val;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return BigDecimal.ZERO;
    }
}
