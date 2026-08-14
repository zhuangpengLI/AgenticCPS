package com.qiji.cps.module.cps.client.official.jd;

import com.fasterxml.jackson.databind.JsonNode;
import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.GoodsReq;
import com.jd.open.api.sdk.domain.kplunion.OrderService.request.query.OrderRowReq;
import com.jd.open.api.sdk.domain.kplunion.promotioncommon.PromotionService.request.get.PromotionCodeReq;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenOrderRowQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPromotionCommonGetRequest;
import com.jd.open.api.sdk.response.kplunion.UnionOpenGoodsQueryResponse;
import com.jd.open.api.sdk.response.kplunion.UnionOpenOrderRowQueryResponse;
import com.jd.open.api.sdk.response.kplunion.UnionOpenPromotionCommonGetResponse;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 京东联盟开放平台官方 Java SDK 客户端。 */
@Slf4j
@Component
public class JdOfficialVendorClient extends AbstractOfficialVendorClient {

    /** SDK 的 JSON 解析器要求 JOS 路由以 routerjson 结尾。 */
    private static final String DEFAULT_API_BASE_URL = "https://api.jd.com/routerjson";

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.JD.getCode();
    }

    @Override
    public java.util.Set<CpsVendorCapability> getCapabilities() {
        return java.util.Set.of(CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK, CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST);
    }

    @Override
    public CpsVendorConfigSchema getConfigSchema() {
        return new CpsVendorConfigSchema(List.of(
                CpsVendorConfigField.required("appKey", true),
                CpsVendorConfigField.required("appSecret", true),
                CpsVendorConfigField.optional("apiBaseUrl", false),
                CpsVendorConfigField.optional("authToken", true),
                CpsVendorConfigField.optional("defaultAdzoneId", false),
                CpsVendorConfigField.optional("timeoutMs", false)));
    }

    @Override
    protected String getSearchApiPath() {
        return "jd.union.open.goods.query";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("pageIndex", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("pid", firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId()));
        params.put("pricefrom", request.getPriceLowerLimit());
        params.put("priceto", request.getPriceUpperLimit());
        params.put("isCoupon", request.getHasCoupon());
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode result = resultNode(response, "queryResult");
        JsonNode data = result.path("data");
        List<CpsGoodsItem> items = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                JsonNode price = item.path("priceInfo");
                JsonNode commission = item.path("commissionInfo");
                JsonNode image = item.path("imageInfo");
                JsonNode shop = item.path("shopInfo");
                JsonNode coupon = item.at("/couponInfo/couponList/0");
                String mainPic = firstText(image.at("/imageList/0/url"), image.path("whiteImage"));
                BigDecimal originalPrice = decimal(price.path("price"));
                BigDecimal actualPrice = decimal(price.path("lowestCouponPrice"));
                items.add(CpsGoodsItem.builder()
                        .goodsId(firstText(item.path("skuId"), item.path("itemId")))
                        .platformCode(getPlatformCode())
                        .title(item.path("skuName").asText(null))
                        .mainPic(mainPic)
                        .originalPrice(originalPrice)
                        .actualPrice(actualPrice != null ? actualPrice : originalPrice)
                        .couponPrice(decimal(coupon.path("discount")))
                        .couponConditions(decimal(coupon.path("quota")))
                        .commissionRate(decimal(commission.path("commissionShare")))
                        .commissionAmount(decimal(commission.path("commission")))
                        .monthSales(longValue(item.path("inOrderCount30DaysSku"), item.path("inOrderCount30Days")))
                        .shopName(shop.path("shopName").asText(null))
                        .shopType(item.path("isJdSale").asInt(0))
                        .itemLink(item.path("materialUrl").asText(null))
                        .brandName(item.path("brandName").asText(null))
                        .vendorCode(getVendorCode())
                        .source("jd-official")
                        .rawPayload(item.toString())
                        .build());
            }
        }
        return CpsGoodsSearchResult.builder()
                .list(items)
                .total(result.path("totalCount").asLong(items.size()))
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "jd.union.open.promotion.common.get";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("materialId", firstNonBlank(request.getItemLink(),
                request.getGoodsId() == null ? null : "https://item.jd.com/" + request.getGoodsId() + ".html"));
        params.put("siteId", firstNonBlank(config.getDefaultAdzoneId(), request.getAdzoneId()));
        params.put("positionId", parseLong(request.getAdzoneId(), 0L));
        params.put("subUnionId", firstNonBlank(request.getChannelId(), request.getExternalId()));
        params.put("pid", firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId()));
        params.put("couponUrl", request.getCouponUrl());
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = resultNode(response, "getResult").path("data");
        String clickUrl = data.path("clickURL").asText(null);
        String command = data.path("jCommand").asText(null);
        if (clickUrl == null && command == null) {
            return null;
        }
        return CpsPromotionLinkResult.builder()
                .shortUrl(clickUrl)
                .longUrl(clickUrl)
                .extraFields(Map.of("jCommand", command == null ? "" : command))
                .rawPayload(response.toString())
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "jd.union.open.order.row.query";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pageIndex", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("type", request.getQueryType());
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("key", request.getPositionIndex());
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        JsonNode result = resultNode(response, "queryResult");
        JsonNode data = result.path("data");
        if (!data.isArray()) {
            return Collections.emptyList();
        }
        List<CpsOrderDTO> orders = new ArrayList<>();
        for (JsonNode item : data) {
            JsonNode goods = item.path("goodsInfo");
            orders.add(CpsOrderDTO.builder()
                    .platformOrderId(firstText(item.path("id"), item.path("orderId")))
                    .parentOrderId(item.path("parentId").asText(null))
                    .platformCode(getPlatformCode())
                    .vendorCode(getVendorCode())
                    .itemId(firstText(item.path("skuId"), item.path("itemId")))
                    .itemTitle(item.path("skuName").asText(null))
                    .itemPic(goods.path("imageUrl").asText(null))
                    .itemPrice(decimal(item.path("price")))
                    .finalPrice(decimal(item.path("actualCosPrice"), item.path("price")))
                    .commissionRate(decimal(item.path("commissionRate")))
                    .commissionAmount(decimal(item.path("actualFee"), item.path("estimateFee")))
                    .quantity(item.path("skuNum").asInt(0))
                    .platformStatus(item.path("validCode").asInt(0))
                    .orderTime(item.path("orderTime").asText(null))
                    .payTime(item.path("orderPayTime").asText(null))
                    .settleTime(item.path("finishTime").asText(null))
                    .adzoneId(firstText(item.path("positionId"), item.path("pid")))
                    .externalId(item.path("subUnionId").asText(null))
                    .refundTag(item.path("skuReturnNum").asInt(0) > 0 ? 1 : 0)
                    .rawPayload(item.toString())
                    .build());
        }
        return orders;
    }

    @Override
    protected CpsOrderPageResult resolveOrderPageResult(JsonNode response, CpsOrderQueryRequest request,
                                                        List<CpsOrderDTO> orders) {
        JsonNode result = resultNode(response, "queryResult");
        JsonNode hasMore = result.path("hasMore");
        if (hasMore.isBoolean()) {
            int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
            return CpsOrderPageResult.page(orders, hasMore.asBoolean() ? pageNo + 1 : null,
                    hasMore.asBoolean());
        }
        return super.resolveOrderPageResult(response, request, orders);
    }

    @Override
    protected String getTestConnectionApiPath() {
        return getSearchApiPath();
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "手机");
        params.put("pageIndex", 1);
        params.put("pageSize", 1);
        return params;
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        try {
            JdClient client = createClient(config);
            Object response;
            if (getSearchApiPath().equals(path)) {
                GoodsReq req = new GoodsReq();
                req.setKeyword(string(params.get("keyword")));
                req.setPageIndex(integer(params.get("pageIndex"), 1));
                req.setPageSize(integer(params.get("pageSize"), 20));
                req.setPid(string(params.get("pid")));
                req.setPricefrom(decimal(params.get("pricefrom")) == null ? null : decimal(params.get("pricefrom")).doubleValue());
                req.setPriceto(decimal(params.get("priceto")) == null ? null : decimal(params.get("priceto")).doubleValue());
                req.setIsCoupon(integerOrNull(params.get("isCoupon")));
                UnionOpenGoodsQueryRequest request = new UnionOpenGoodsQueryRequest();
                request.setGoodsReqDTO(req);
                response = client.execute(request);
            } else if (getPromotionLinkApiPath().equals(path)) {
                PromotionCodeReq req = new PromotionCodeReq();
                req.setMaterialId(string(params.get("materialId")));
                req.setSiteId(string(params.get("siteId")));
                req.setPositionId(parseLong(string(params.get("positionId")), 0L));
                req.setSubUnionId(string(params.get("subUnionId")));
                req.setPid(string(params.get("pid")));
                req.setCouponUrl(string(params.get("couponUrl")));
                UnionOpenPromotionCommonGetRequest request = new UnionOpenPromotionCommonGetRequest();
                request.setPromotionCodeReq(req);
                response = client.execute(request);
            } else if (getOrderQueryApiPath().equals(path)) {
                OrderRowReq req = new OrderRowReq();
                req.setPageIndex(integer(params.get("pageIndex"), 1));
                req.setPageSize(integer(params.get("pageSize"), 50));
                req.setType(integer(params.get("type"), 1));
                req.setStartTime(string(params.get("startTime")));
                req.setEndTime(string(params.get("endTime")));
                req.setKey(string(params.get("key")));
                UnionOpenOrderRowQueryRequest request = new UnionOpenOrderRowQueryRequest();
                request.setOrderReq(req);
                response = client.execute(request);
            } else {
                throw new IllegalArgumentException("Unsupported JD API path: " + path);
            }
            return objectMapper.valueToTree(response);
        } catch (Exception e) {
            log.warn("[京东联盟官方] SDK 请求失败: path={}, type={}", path, e.getClass().getSimpleName());
            return null;
        }
    }

    protected JdClient createClient(CpsVendorConfig config) {
        String baseUrl = firstNonBlank(config.getApiBaseUrl(), DEFAULT_API_BASE_URL);
        return new DefaultJdClient(baseUrl, config.getAuthToken(), config.getAppKey(), config.getAppSecret());
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        if (root == null) {
            return false;
        }
        String code = root.path("code").asText(null);
        if ("0".equals(code) || "200".equals(code)) {
            return true;
        }
        for (String name : List.of("queryResult", "getResult")) {
            JsonNode result = root.path(name);
            if (result.path("code").asInt(-1) == 0 || result.path("code").asInt(-1) == 200) {
                return true;
            }
        }
        return false;
    }

    private JsonNode resultNode(JsonNode root, String name) {
        JsonNode result = root.path(name);
        return result.isMissingNode() ? root : result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                try {
                    return new BigDecimal(node.asText());
                } catch (NumberFormatException ignored) {
                    // try next representation
                }
            }
        }
        return null;
    }

    private BigDecimal decimal(Object value) {
        return value == null ? null : decimal(objectMapper.valueToTree(value));
    }

    private Long longValue(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && node.canConvertToLong()) {
                return node.asLong();
            }
        }
        return null;
    }

    private Integer integer(Object value, int fallback) {
        Integer parsed = integerOrNull(value);
        return parsed == null ? fallback : parsed;
    }

    private Integer integerOrNull(Object value) {
        if (value == null) return null;
        try { return Integer.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLong(String value, long fallback) {
        try { return value == null ? fallback : Long.valueOf(value); } catch (NumberFormatException e) { return fallback; }
    }
}
