package com.qiji.cps.module.cps.client.dataoke;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.client.selection.CpsSearchAssistVendorClient;
import com.qiji.cps.module.cps.client.selection.CpsTaobaoSelectionVendorClient;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsCouponInfoVendorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 大淘客-淘宝供应商客户端
 *
 * <p>通过大淘客开放平台 API 对接淘宝联盟，迁移自 TaobaoPlatformClientAdapter 的业务逻辑。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class DtkTaobaoVendorClient extends AbstractDtkVendorClient
        implements CpsTaobaoSelectionVendorClient, CpsSearchAssistVendorClient, CpsCouponInfoVendorClient {

    private static final String PARSE_CONTENT_PATH = "/tb-service/parse-content";
    private static final String COUPON_INFO_PATH = "/dels/taobao/kit/coupon/get-coupon-info";
    private static final String SUPER_CATEGORY_PATH = "/category/get-super-category";
    private static final String HOT_KEYWORDS_PATH = "/category/get-top100";
    private static final String SEARCH_SUGGESTION_PATH = "/goods/search-suggestion";
    private static final String IMAGE_SEARCH_URL = "https://openapiv2.dataoke.com/open-api/goods/search-by-image";
    private static final String SEARCH_MODE_IMAGE = "dataoke_image";

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    // ==================== 商品搜索 ====================

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        if (SEARCH_MODE_IMAGE.equals(request.getSearchMode())) {
            return searchGoodsByImage(request, config);
        }
        return super.searchGoods(request, config);
    }

    @Override
    protected String getSearchApiPath() {
        return "/goods/get-dtk-search-goods";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyWords", request.getKeyword());
        params.put("pageId", String.valueOf(request.getPageNo()));
        params.put("pageSize", request.getPageSize());
        if (request.getSortType() != null) {
            params.put("sort", convertSortType(request.getSortType()));
        }
        if (request.getPriceLowerLimit() != null) {
            params.put("priceLowerLimit", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("priceUpperLimit", request.getPriceUpperLimit());
        }
        if (request.getHasCoupon() != null) {
            params.put("hasCoupon", request.getHasCoupon());
        }
        if (hasText(request.getCategoryId()) && !"0".equals(request.getCategoryId())) {
            params.put("cids", request.getCategoryId());
        }
        if (request.getMinCommissionRate() != null) {
            params.put("commissionRateLowerLimit", request.getMinCommissionRate());
        }
        if (request.getMinMonthSales() != null) {
            params.put("monthSalesLowerLimit", request.getMinMonthSales());
        }
        if (request.getCouponAmountMin() != null) {
            params.put("couponPriceLowerLimit", request.getCouponAmountMin());
        }
        if (request.getCouponPriceUpperLimit() != null) {
            params.put("couponPriceUpperLimit", request.getCouponPriceUpperLimit());
        }
        if (request.getHotRankMin() != null) {
            params.put("hotRankLowerLimit", request.getHotRankMin());
        }
        if (request.getCouponExpireDays() != null) {
            params.put("couponExpireDays", request.getCouponExpireDays());
        }
        if (Boolean.TRUE.equals(request.getJuhuasuanOnly())) {
            params.put("juHuaSuan", 1);
        }
        if (Boolean.TRUE.equals(request.getTaoqianggouOnly())) {
            params.put("taoQiangGou", 1);
        }
        if (Boolean.TRUE.equals(request.getTmallOnly())) {
            params.put("tmall", 1);
        }
        if (Boolean.TRUE.equals(request.getTchaoshiOnly())) {
            params.put("tchaoshi", 1);
        }
        if (Boolean.TRUE.equals(request.getGoldSellerOnly())) {
            params.put("goldSeller", 1);
        }
        if (Boolean.TRUE.equals(request.getBrandOnly())) {
            params.put("brand", 1);
        }
        if (Boolean.TRUE.equals(request.getHaitaoOnly())) {
            params.put("haitao", 1);
        }
        if (Boolean.TRUE.equals(request.getCommercialOnly())) {
            params.put("directCommissionType", 3);
        }
        if (Boolean.TRUE.equals(request.getPreSaleOnly())) {
            params.put("pre", 1);
        }
        if (Boolean.TRUE.equals(request.getFreeshipRemoteDistrict())) {
            params.put("freeshipRemoteDistrict", 1);
        }
        if (Boolean.TRUE.equals(request.getInspectedGoodsOnly())) {
            params.put("inspectedGoods", 1);
        }
        params.put("version", "v2.1.2");
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        JsonNode list = data.path("list");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (list.isArray()) {
            for (JsonNode item : list) {
                goodsList.add(parseTaobaoGoodsItem(item));
            }
        }
        long totalCount = data.path("totalCount").asLong(-1);
        return CpsGoodsSearchResult.builder()
                .list(goodsList)
                .total(totalCount)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    private CpsGoodsSearchResult searchGoodsByImage(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        if (!hasText(request.getImageBase64())) {
            return buildEmptyResult(request);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.0");
        params.put("img_base64", request.getImageBase64());
        JsonNode response = unwrapResponse(executePostRequest(IMAGE_SEARCH_URL, params, config));
        if (response == null || !isSuccessResponse(response)) {
            log.warn("[{}:{}] 图片搜商品失败: {}", getVendorCode(), getPlatformCode(), response);
            return buildEmptyResult(request);
        }
        JsonNode data = response.path("data");
        JsonNode list = data.isArray() ? data : data.path("list");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (list.isArray()) {
            for (JsonNode item : list) {
                goodsList.add(parseTaobaoGoodsItem(item));
            }
        } else if (data.isObject() && hasText(firstText(data, "goodsId", "id", "goodsSign"))) {
            goodsList.add(parseTaobaoGoodsItem(data));
        }
        return CpsGoodsSearchResult.builder()
                .list(goodsList)
                .total((long) goodsList.size())
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    // ==================== 推广转链 ====================

    @Override
    protected String getPromotionLinkApiPath() {
        return "/tb-service/get-privilege-link";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goodsId", request.getGoodsId());
        params.put("version", "v1.3.1");
        if (request.getAdzoneId() != null) {
            params.put("pid", request.getAdzoneId());
        }
        String configuredChannelId = firstNonBlankExtraConfig(getExtraConfig(config, "channelId"), getExtraConfig(config, "relationId"));
        String channelId = firstNonBlankExtraConfig(request.getRelationId(), request.getChannelId(), configuredChannelId);
        if (channelId != null) {
            params.put("channelId", channelId);
        }
        if (hasText(request.getSpecialId())) {
            params.put("specialId", request.getSpecialId());
        }
        if (request.getExternalId() != null) {
            params.put("externalId", request.getExternalId());
        }
        return params;
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        if (!hasText(request.getOriginalContent())) {
            return super.generatePromotionLink(request, config);
        }
        try {
            Map<String, Object> params = buildUniversalTransferParams(request);
            JsonNode response = executeRequest(PARSE_CONTENT_PATH, params, config);
            if (response == null || !isSuccessResponse(response)) {
                // 大淘客文档注明万能解析偶发失败时可重试一次；仅做一次有界重试。
                response = executeRequest(PARSE_CONTENT_PATH, params, config);
            }
            if (response == null || !isSuccessResponse(response)) {
                log.warn("[dataoke:taobao] 万能转链失败: upstreamStatus=REJECTED");
                return null;
            }
            CpsPromotionLinkResult result = parseUniversalPromotionLinkResponse(response);
            if (!hasPromotionMaterial(result)) {
                log.warn("[dataoke:taobao] 万能转链未返回推广素材: goodsId={}", request.getGoodsId());
                return null;
            }
            return result;
        } catch (Exception e) {
            log.error("[dataoke:taobao] 万能转链异常: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    private Map<String, Object> buildUniversalTransferParams(CpsPromotionLinkRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.0");
        params.put("content", request.getOriginalContent());
        if (hasText(request.getAdzoneId())) {
            params.put("pid", request.getAdzoneId());
        }
        if (hasText(request.getChannelId())) {
            params.put("channelId", request.getChannelId());
        }
        if (hasText(request.getSpecialId())) {
            params.put("specialId", request.getSpecialId());
        }
        return params;
    }

    private CpsPromotionLinkResult parseUniversalPromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        return CpsPromotionLinkResult.builder()
                .shortUrl(firstText(data, "shortUrl", "couponShortUrl", "cpsShortUrl", "cpsSuperedShortUrl"))
                .longUrl(firstText(data, "cpsLongUrl", "couponLongUrl", "cpsSuperedLongUrl", "originUrl"))
                .tpwd(firstText(data, "shortTpwd", "cpsFullTpwd", "cpsShortTpwd",
                        "couponShortTpwd", "couponLongTpwd"))
                .commissionRate(parseDecimal(data, "commissionRate", "maxCommissionRate"))
                .commissionAmount(parseDecimal(data, "commissionAmount"))
                .actualPrice(parseDecimal(data, "actualPrice", "price"))
                .couponInfo(firstText(data, "couponInfo"))
                .rawPayload(response.toString())
                .build();
    }

    private boolean hasPromotionMaterial(CpsPromotionLinkResult result) {
        return result != null && (hasText(result.getShortUrl()) || hasText(result.getLongUrl())
                || hasText(result.getTpwd()));
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("shortUrl").asText(null))
                .longUrl(data.path("itemUrl").asText(null))
                .tpwd(data.path("tpwd").asText(null))
                .couponInfo(data.path("couponInfo").asText(null))
                .commissionRate(parseDecimal(data, "maxCommissionRate"))
                .actualPrice(parseDecimal(data, "actualPrice"))
                .build();
    }

    @Override
    public CpsContentParseResult parseContent(CpsContentParseRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.0");
        params.put("content", request.getOriginalContent());

        JsonNode response = executeRequest(PARSE_CONTENT_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsContentParseResult.unsupported("PARSE_FAILED", parseFailureMessage(response));
        }

        JsonNode data = response.path("data");
        String goodsId = firstText(data, "goodsId", "itemId");
        if (!hasText(goodsId)) {
            return CpsContentParseResult.unsupported("PARSE_NO_GOODS_ID", "大淘客未解析出商品ID");
        }
        JsonNode originInfo = data.path("originInfo");
        String title = firstNonBlank(firstText(data, "itemName", "title", "dtitle", "name", "goodsName"),
                originInfo.path("title").asText(null));
        return CpsContentParseResult.builder()
                .supported(true)
                .goodsId(goodsId)
                .itemLink(firstText(data, "originUrl", "itemLink"))
                .title(title)
                .build();
    }

    private String parseFailureMessage(JsonNode response) {
        String msg = response == null ? null : response.path("msg").asText(null);
        return hasText(msg) ? msg : "大淘客万能解析失败，请检查口令或链接是否有效";
    }

    // ==================== 订单查询 ====================

    @Override
    protected String getOrderQueryApiPath() {
        return "/tb-service/get-order-details";
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        List<CpsOrderDTO> orders = super.queryOrders(request, config);
        applyOrderScene(request, orders);
        return orders;
    }

    @Override
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request, CpsVendorConfig config) {
        CpsOrderPageResult result = super.queryOrderPage(request, config);
        applyOrderScene(request, result.getItems());
        return result;
    }

    private void applyOrderScene(CpsOrderQueryRequest request, List<CpsOrderDTO> orders) {
        if (orders != null && request != null && request.getOrderScene() != null) {
            orders.forEach(order -> {
                if (order.getOrderScene() == null) {
                    order.setOrderScene(request.getOrderScene());
                }
            });
        }
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("queryType", request.getQueryType());
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("pageSize", request.getPageSize());
        params.put("version", "v1.0.0");
        if (request.getPositionIndex() != null) {
            params.put("positionIndex", request.getPositionIndex());
        }
        if (request.getOrderScene() != null) {
            params.put("orderScene", request.getOrderScene());
        }
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = response.path("data");
        JsonNode results = data.path("results");
        JsonNode items = results.path("publisher_order_dto");
        String nextPositionIndex = data.path("positionIndex").asText(null);

        if (items.isArray()) {
            for (JsonNode item : items) {
                orders.add(parseTaobaoOrder(item, nextPositionIndex));
            }
        }
        return orders;
    }

    // ==================== 连接测试 ====================

    @Override
    protected String getTestConnectionApiPath() {
        return SUPER_CATEGORY_PATH;
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.1.0");
        return params;
    }

    @Override
    public CpsCouponInfo queryCouponInfo(String content, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.0");
        params.put("content", content);

        JsonNode response = executeRequest(COUPON_INFO_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            log.warn("[{}:{}] query coupon info failed: {}", getVendorCode(), getPlatformCode(), response);
            return null;
        }
        JsonNode data = response.path("data");
        return CpsCouponInfo.builder()
                .couponId(firstText(data, "couponId", "coupon_id", "activityId", "activity_id"))
                .couponLink(firstText(data, "couponLink", "coupon_link", "couponUrl", "coupon_url"))
                .couponAmount(parseDecimal(data, "couponAmount", "coupon_amount", "couponMoney", "coupon_money"))
                .couponConditions(parseDecimal(data, "couponConditions", "coupon_conditions",
                        "couponCondition", "coupon_condition", "couponStartFee", "coupon_start_fee"))
                .couponTotalNum(parseLong(data, "couponTotalNum", "coupon_total_num"))
                .couponRemainNum(parseLong(data, "couponRemainNum", "coupon_remain_num",
                        "couponSurplusNum", "coupon_surplus_num"))
                .couponReceiveNum(parseLong(data, "couponReceiveNum", "coupon_receive_num",
                        "couponUseNum", "coupon_use_num"))
                .couponStartTime(firstText(data, "couponStartTime", "coupon_start_time"))
                .couponEndTime(firstText(data, "couponEndTime", "coupon_end_time"))
                .build();
    }

    @Override
    public CpsGoodsSelectionMeta getSelectionMeta(CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.1.0");
        JsonNode response = executeRequest(SUPER_CATEGORY_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsGoodsSelectionMeta.builder().metaSource(getVendorCode()).build();
        }
        return CpsGoodsSelectionMeta.builder()
                .categories(parseCategoryOptions(response.path("data")))
                .metaSource(getVendorCode())
                .build();
    }

    @Override
    public List<CpsGoodsSelectionOption> getHotKeywords(Integer type, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.1");
        params.put("type", type == null ? 1 : type);
        JsonNode response = executeRequest(HOT_KEYWORDS_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return Collections.emptyList();
        }
        JsonNode hotWords = response.path("data").path("hotWords");
        if (!hotWords.isArray()) {
            return Collections.emptyList();
        }
        List<CpsGoodsSelectionOption> result = new ArrayList<>();
        for (JsonNode item : hotWords) {
            String keyword = item.asText(null);
            if (hasText(keyword)) {
                result.add(CpsGoodsSelectionOption.of(keyword, keyword));
            }
        }
        return result;
    }

    @Override
    public List<CpsGoodsSelectionOption> suggestKeywords(String keyword, Integer type, CpsVendorConfig config) {
        if (!hasText(keyword)) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.2");
        params.put("keyWords", keyword);
        params.put("type", type == null ? 1 : type);
        JsonNode response = executeRequest(SEARCH_SUGGESTION_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return Collections.emptyList();
        }
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            return Collections.emptyList();
        }
        List<CpsGoodsSelectionOption> result = new ArrayList<>();
        for (JsonNode item : data) {
            String kw = firstText(item, "kw", "keyword", "word");
            if (!hasText(kw)) {
                continue;
            }
            long total = item.path("total").asLong(-1);
            String description = total >= 0 ? total + " 个商品" : null;
            result.add(CpsGoodsSelectionOption.builder()
                    .value(kw)
                    .label(kw)
                    .description(description)
                    .build());
        }
        return result;
    }

    // ==================== 私有方法 ====================

    private CpsGoodsItem parseTaobaoGoodsItem(JsonNode item) {
        return CpsGoodsItem.builder()
                .goodsId(item.path("goodsId").asText(null))
                .platformCode(getPlatformCode())
                .title(item.path("title").asText(null))
                .mainPic(item.path("mainPic").asText(null))
                .originalPrice(parseDecimal(item, "originalPrice"))
                .actualPrice(parseDecimal(item, "actualPrice"))
                .couponPrice(parseDecimal(item, "couponPrice"))
                .couponConditions(parseDecimal(item, "couponConditions"))
                .couponTotalNum(parseLong(item, "couponTotalNum", "coupon_total_num"))
                .couponRemainNum(parseLong(item, "couponRemainNum", "couponRemainCount", "coupon_remain_num"))
                .couponReceiveNum(parseLong(item, "couponReceiveNum", "coupon_receive_num"))
                .commissionRate(parseDecimal(item, "commissionRate"))
                .commissionAmount(parseCommissionAmount(item))
                .monthSales(item.path("monthSales").asLong(0))
                .shopName(item.path("shopName").asText(null))
                .shopType(item.path("shopType").asInt(0))
                .itemLink(item.path("itemLink").asText(null))
                .goodsSign(firstText(item, "goodsSign", "sign"))
                .brandName(item.path("brandName").asText(null))
                .vendorCode(getVendorCode())
                .source("大淘客")
                .activityTag(firstText(item, "activityType", "activityTag", "marketingTag"))
                .categoryName(firstText(item, "cidName", "categoryName", "subcidName"))
                .couponEndTime(firstText(item, "couponEndTime", "couponEndTimeStr"))
                .couponStartTime(firstText(item, "couponStartTime", "couponStartTimeStr"))
                .rankTag(firstText(item, "ranking", "rankTag"))
                .sellingPoint(firstText(item, "desc", "marketingMainPic", "dtitle"))
                .build();
    }

    private BigDecimal parseCommissionAmount(JsonNode item) {
        BigDecimal actualPrice = parseDecimal(item, "actualPrice");
        BigDecimal commissionRate = parseDecimal(item, "commissionRate");
        if (actualPrice == null || commissionRate == null) {
            return parseDecimal(item, "commissionAmount", "commission");
        }
        return actualPrice.multiply(commissionRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    private CpsOrderDTO parseTaobaoOrder(JsonNode item, String nextPositionIndex) {
        int tkStatus = item.path("tk_status").asInt(-1);
        String specialId = firstText(item, "special_id", "specialId");
        String relationId = firstText(item, "relation_id", "relationId");
        Map<String, Object> extraFields = new LinkedHashMap<>();
        putIfHasText(extraFields, "specialId", specialId);
        putIfHasText(extraFields, "relationId", relationId);
        putIfHasText(extraFields, "rawTkStatus", String.valueOf(tkStatus));
        return CpsOrderDTO.builder()
                .platformCode(getPlatformCode())
                .platformOrderId(item.path("trade_id").asText(null))
                .parentOrderId(item.path("trade_parent_id").asText(null))
                .itemId(item.path("item_id").asText(null))
                .itemTitle(item.path("item_title").asText(null))
                .itemPic(item.path("item_img").asText(null))
                .itemPrice(firstDecimal(item, "item_price", "auction_price", "itemPrice"))
                .finalPrice(firstNonZeroDecimal(item, "pay_price", "alipay_total_price", "payPrice", "alipayTotalPrice"))
                .commissionRate(firstNonZeroDecimal(item, "total_commission_rate", "pub_share_rate", "commission_rate", "pubShareRate"))
                .commissionAmount(firstNonZeroDecimal(item, "pub_share_fee", "pub_share_pre_fee", "pubShareFee", "pubSharePreFee", "commission"))
                .quantity(item.path("item_num").asInt(1))
                .platformStatus(mapTaobaoTkStatus(tkStatus))
                .orderTime(item.path("tk_create_time").asText(null))
                .payTime(item.path("tk_paid_time").asText(null))
                .receiveTime(firstText(item, "tb_deposit_time", "tk_deposit_time", "confirm_receipt_time"))
                .settleTime(firstText(item, "tk_earning_time", "earning_time", "settle_time"))
                .adzoneId(item.path("adzone_id").asText(null))
                .externalId(firstText(item, "external_id", "externalId"))
                .specialId(specialId)
                .relationId(relationId)
                .orderScene(parseInteger(item, "order_scene", "orderScene"))
                .refundTag(item.path("refund_tag").asInt(0))
                .nextPositionIndex(nextPositionIndex)
                .extraFields(extraFields)
                .build();
    }

    private int mapTaobaoTkStatus(int tkStatus) {
        return switch (tkStatus) {
            case 12 -> 1; // 已付款
            case 14 -> 2; // 已收货/订单成功
            case 3 -> 3;  // 已结算
            case 13 -> -1; // 已失效
            default -> tkStatus;
        };
    }

    private BigDecimal firstNonZeroDecimal(JsonNode item, String... fieldNames) {
        BigDecimal first = null;
        for (String fieldName : fieldNames) {
            BigDecimal value = parseDecimal(item, fieldName);
            if (value == null) {
                continue;
            }
            if (first == null) {
                first = value;
            }
            if (value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return first;
    }

    private BigDecimal firstDecimal(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            BigDecimal value = parseDecimal(item, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (hasText(value)) {
            target.put(key, value);
        }
    }

    private String convertSortType(Integer sortType) {
        return switch (sortType) {
            case 1 -> "2";  // 销量降序
            case 2 -> "6";  // 价格升序
            case 3 -> "5";  // 价格降序
            case 4 -> "4";  // 佣金率降序
            case 5 -> "3";  // 领券量降序
            case 6 -> "1";  // 最新上架
            default -> "0"; // 综合排序
        };
    }

    private List<CpsGoodsSelectionOption> parseCategoryOptions(JsonNode data) {
        List<CpsGoodsSelectionOption> options = new ArrayList<>();
        if (data == null || data.isMissingNode() || data.isNull()) {
            return options;
        }
        JsonNode list = data.isArray() ? data : firstArray(data, "list", "categories", "data");
        if (list == null || !list.isArray()) {
            return options;
        }
        for (JsonNode item : list) {
            String value = firstText(item, "cid", "cids", "id");
            String label = firstText(item, "cname", "name", "label");
            if (hasText(value) && hasText(label)) {
                options.add(CpsGoodsSelectionOption.of(value, label));
            }
        }
        return options;
    }

    private JsonNode firstArray(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText(null);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
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

    private Long parseLong(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            try {
                return Long.valueOf(value.asText());
            } catch (Exception ignored) {
                // Try the next known alias.
            }
        }
        return null;
    }

    private Integer parseInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            try {
                return Integer.valueOf(value.asText());
            } catch (Exception ignored) {
                // Try the next known alias.
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
