package com.qiji.cps.module.cps.client.official.jd;

import com.fasterxml.jackson.databind.JsonNode;
import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.GoodsReq;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.JFGoodsReq;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.RecommendGoodsReq;
import com.jd.open.api.sdk.domain.kplunion.OrderService.request.query.OrderRowReq;
import com.jd.open.api.sdk.domain.kplunion.OrderService.request.query.BonusOrderReq;
import com.jd.open.api.sdk.domain.kplunion.ChannelService.request.get.ChannelRelationGetReq;
import com.jd.open.api.sdk.domain.kplunion.UserService.request.get.PidReq;
import com.jd.open.api.sdk.domain.kplunion.promotioncommon.PromotionService.request.get.PromotionCodeReq;
import com.jd.open.api.sdk.request.kplunion.UnionOpenCouponQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsJingfenQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsRecommendQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenOrderRowQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenOrderBonusQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenChannelRelationGetRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPositionCreateRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPositionQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenUserPidGetRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPromotionBysubunionidGetRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPromotionByunionidGetRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPromotionCommonGetRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenCouponGiftGetRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenCouponGiftStopRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenStatisticsGiftcouponQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenStatisticsRedpacketQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenStatisticsPromotionQueryRequest;
import com.jd.open.api.sdk.response.kplunion.UnionOpenGoodsQueryResponse;
import com.jd.open.api.sdk.response.kplunion.UnionOpenOrderRowQueryResponse;
import com.jd.open.api.sdk.response.kplunion.UnionOpenPromotionCommonGetResponse;
import com.qiji.cps.module.cps.client.CpsCouponInfoVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsCouponInfo;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 京东联盟开放平台官方 Java SDK 客户端。 */
@Slf4j
@Component
public class JdOfficialVendorClient extends AbstractOfficialVendorClient
        implements CpsCouponInfoVendorClient, JdOfficialManagementClient, JdOfficialExtendedClient {

    /** SDK 的 JSON 解析器要求 JOS 路由以 routerjson 结尾。 */
    private static final String DEFAULT_API_BASE_URL = "https://api.jd.com/routerjson";
    private static final String GOODS_JINGFEN_API = "jd.union.open.goods.jingfen.query";
    private static final String GOODS_RECOMMEND_API = "jd.union.open.goods.recommend.query";
    private static final String COUPON_QUERY_API = "jd.union.open.coupon.query";
    private static final String PROMOTION_SOCIAL_API = "jd.union.open.promotion.bysubunionid.get";
    private static final String PROMOTION_TOOL_API = "jd.union.open.promotion.byunionid.get";
    private static final String BONUS_ORDER_API = "jd.union.open.order.bonus.query";
    private static final String POSITION_CREATE_API = "jd.union.open.position.create";
    private static final String POSITION_QUERY_API = "jd.union.open.position.query";
    private static final String PID_GET_API = "jd.union.open.user.pid.get";
    private static final String CHANNEL_RELATION_GET_API = "jd.union.open.channel.relation.get";
    private static final String GIFT_CREATE_API = "jd.union.open.coupon.gift.get";
    private static final String GIFT_STOP_API = "jd.union.open.coupon.gift.stop";
    private static final String GIFT_EFFECT_API = "jd.union.open.statistics.giftcoupon.query";
    private static final String RED_PACKET_EFFECT_API = "jd.union.open.statistics.redpacket.query";
    private static final String PROMOTION_EFFECT_API = "jd.union.open.statistics.promotion.query";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.JD.getCode();
    }

    @Override
    public java.util.Set<CpsVendorCapability> getCapabilities() {
        return java.util.Set.of(CpsVendorCapability.GOODS_SEARCH, CpsVendorCapability.SELECTION_LIBRARY,
                CpsVendorCapability.COUPON_QUERY,
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
                CpsVendorConfigField.optional("timeoutMs", false),
                CpsVendorConfigField.optional("promotionMode", false),
                CpsVendorConfigField.optional("includeBonusOrders", false),
                CpsVendorConfigField.optional("unionId", false)));
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
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        String mode = normalizeMode(request.getSearchMode());
        if ("jingfen".equals(mode)) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("eliteId", integer(extra(config, "jdEliteId"), 1));
            params.put("pageIndex", request.getPageNo());
            params.put("pageSize", request.getPageSize());
            params.put("pid", firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId()));
            JsonNode response = executeRequest(GOODS_JINGFEN_API, params, config);
            return response != null && isSuccessResponse(response)
                    ? parseSearchResponse(response, request) : buildEmptyResult(request);
        }
        if ("recommend".equals(mode)) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("itemId", request.getKeyword());
            params.put("skuId", parseLong(request.getKeyword(), null));
            params.put("keyword", request.getKeyword());
            params.put("sceneId", integer(extra(config, "jdRecommendSceneId"), 1));
            JsonNode response = executeRequest(GOODS_RECOMMEND_API, params, config);
            if (response == null || !isSuccessResponse(response)) {
                return buildEmptyResult(request);
            }
            List<Long> skuIds = recommendSkuIds(response);
            if (!skuIds.isEmpty()) {
                Map<String, Object> detailParams = buildSearchParams(request, config);
                detailParams.put("keyword", null);
                detailParams.put("skuIds", skuIds.toArray(Long[]::new));
                JsonNode detailResponse = executeRequest(getSearchApiPath(), detailParams, config);
                if (detailResponse != null && isSuccessResponse(detailResponse)) {
                    return parseSearchResponse(detailResponse, request);
                }
            }
            return parseRecommendResponse(response, request);
        }
        return super.searchGoods(request, config);
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

    private CpsGoodsSearchResult parseRecommendResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode recommendations = resultNode(response, "queryResult").at("/data/recommendSkuInfoList");
        List<CpsGoodsItem> items = new ArrayList<>();
        if (recommendations.isArray()) {
            for (JsonNode item : recommendations) {
                String goodsId = firstText(item.path("skuId"), item.path("itemId"));
                items.add(CpsGoodsItem.builder()
                        .goodsId(goodsId)
                        .platformCode(getPlatformCode())
                        .title(firstText(item.path("reason"), item.path("itemId"), item.path("skuId")))
                        .itemLink(goodsId == null ? null : "https://item.jd.com/" + goodsId + ".html")
                        .vendorCode(getVendorCode())
                        .source("jd-official:recommend")
                        .activityTag(item.path("type").asText(null))
                        .sellingPoint(item.path("reason").asText(null))
                        .rawPayload(item.toString())
                        .build());
            }
        }
        return CpsGoodsSearchResult.builder()
                .list(items)
                .total((long) items.size())
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    private List<Long> recommendSkuIds(JsonNode response) {
        JsonNode recommendations = resultNode(response, "queryResult").at("/data/recommendSkuInfoList");
        List<Long> skuIds = new ArrayList<>();
        if (recommendations.isArray()) {
            for (JsonNode item : recommendations) {
                Long skuId = firstLong(item.path("skuId"));
                if (skuId != null) {
                    skuIds.add(skuId);
                }
            }
        }
        return skuIds;
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "jd.union.open.promotion.common.get";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        JdPid pid = parsePid(firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId()));
        params.put("materialId", firstNonBlank(request.getItemLink(),
                request.getGoodsId() == null ? null : "https://item.jd.com/" + request.getGoodsId() + ".html"));
        params.put("siteId", pid.siteId());
        params.put("positionId", pid.positionId());
        params.put("subUnionId", firstNonBlank(request.getChannelId(), request.getExternalId()));
        params.put("pid", firstNonBlank(request.getAdzoneId(), config.getDefaultAdzoneId()));
        params.put("couponUrl", request.getCouponUrl());
        params.put("promotionMode", firstNonBlank(extra(config, "promotionMode"), "common"));
        params.put("unionId", firstNonBlank(extra(config, "unionId"), pid.unionId()));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = resultNode(response, "getResult").path("data");
        String clickUrl = firstText(data.path("shortURL"), data.path("clickURL"),
                data.path("weChatShortLink"));
        String command = data.path("jCommand").asText(null);
        if (clickUrl == null && command == null) {
            return null;
        }
        return CpsPromotionLinkResult.builder()
                .shortUrl(clickUrl)
                .longUrl(data.path("clickURL").asText(clickUrl))
                .extraFields(Map.of("jCommand", command == null ? "" : command,
                        "jShortCommand", data.path("jShortCommand").asText(""),
                        "weChatShortLink", data.path("weChatShortLink").asText("")))
                .rawPayload(response.toString())
                .build();
    }

    @Override
    public CpsCouponInfo queryCouponInfo(String content, CpsVendorConfig config) {
        if (content == null || content.isBlank()) {
            return null;
        }
        JsonNode response = executeRequest(COUPON_QUERY_API,
                Map.of("couponUrls", List.of(content.trim())), config);
        if (response == null || !isSuccessResponse(response)) {
            return null;
        }
        JsonNode item = resultNode(response, "queryResult").at("/data/0");
        if (item.isMissingNode()) {
            return null;
        }
        Long total = longValue(item.path("num"));
        Long remain = longValue(item.path("remainNum"));
        return CpsCouponInfo.builder()
                .couponId(item.path("link").asText(null))
                .couponLink(item.path("link").asText(null))
                .couponAmount(decimal(item.path("discount"), item.path("mzDiscount")))
                .couponConditions(decimal(item.path("quota")))
                .couponTotalNum(total)
                .couponRemainNum(remain)
                .couponReceiveNum(total != null && remain != null ? Math.max(0L, total - remain) : null)
                .couponStartTime(formatEpoch(firstLong(item.path("beginTime"), item.path("takeBeginTime"))))
                .couponEndTime(formatEpoch(firstLong(item.path("endTime"), item.path("takeEndTime"))))
                .build();
    }

    @Override
    public List<Position> createPositions(CreatePositionCommand command, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("unionId", command.unionId());
        params.put("key", command.key());
        params.put("unionType", command.unionType());
        params.put("type", command.type());
        params.put("siteId", command.siteId());
        params.put("names", command.names());
        JsonNode response = executeRequest(POSITION_CREATE_API, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return Collections.emptyList();
        }
        JsonNode data = resultNode(response, "createResult").path("data");
        JsonNode resultList = data.path("resultList");
        JsonNode pidMap = data.path("pid");
        List<Position> positions = new ArrayList<>();
        if (resultList.isObject()) {
            resultList.fields().forEachRemaining(entry -> positions.add(new Position(
                    entry.getValue().asText(), longValue(data.path("siteId")), entry.getKey(),
                    data.path("type").isNumber() ? data.path("type").asInt() : command.type(),
                    pidMap.path(entry.getKey()).asText(null))));
        }
        return positions;
    }

    @Override
    public PositionPage queryPositions(QueryPositionCommand command, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("unionId", command.unionId());
        params.put("key", command.key());
        params.put("unionType", command.unionType());
        params.put("pageIndex", command.pageNo());
        params.put("pageSize", command.pageSize());
        JsonNode response = executeRequest(POSITION_QUERY_API, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return new PositionPage(Collections.emptyList(), 0L, command.pageNo(), command.pageSize());
        }
        JsonNode data = resultNode(response, "queryResult").path("data");
        List<Position> positions = new ArrayList<>();
        JsonNode result = data.path("result");
        if (result.isArray()) {
            for (JsonNode item : result) {
                positions.add(new Position(item.path("id").asText(null), longValue(item.path("siteId")),
                        item.path("spaceName").asText(null), item.path("type").asInt(0),
                        item.path("pid").asText(null)));
            }
        }
        return new PositionPage(positions, data.path("total").asLong(positions.size()),
                data.path("pageNo").asInt(command.pageNo()), data.path("pageSize").asInt(command.pageSize()));
    }

    @Override
    public String getPid(PidCommand command, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("unionId", command.unionId());
        params.put("childUnionId", command.childUnionId());
        params.put("promotionType", command.promotionType());
        params.put("positionName", command.positionName());
        params.put("mediaName", command.mediaName());
        JsonNode response = executeRequest(PID_GET_API, params, config);
        return response != null && isSuccessResponse(response)
                ? resultNode(response, "getResult").path("data").asText(null) : null;
    }

    @Override
    public Long createChannelRelation(ChannelRelationCommand command, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("inviteCode", command.inviteCode());
        params.put("note", command.note());
        params.put("channelNote", command.channelNote());
        JsonNode response = executeRequest(CHANNEL_RELATION_GET_API, params, config);
        return response != null && isSuccessResponse(response)
                ? longValue(resultNode(response, "getResult").at("/data/channelId")) : null;
    }

    @Override
    public JsonNode createGiftCoupon(Map<String, Object> params, CpsVendorConfig config) {
        return executeRequest(GIFT_CREATE_API, params, config);
    }

    @Override
    public JsonNode stopGiftCoupon(Map<String, Object> params, CpsVendorConfig config) {
        return executeRequest(GIFT_STOP_API, params, config);
    }

    @Override
    public JsonNode queryGiftCouponEffect(Map<String, Object> params, CpsVendorConfig config) {
        return executeRequest(GIFT_EFFECT_API, params, config);
    }

    @Override
    public JsonNode queryRedPacketEffect(Map<String, Object> params, CpsVendorConfig config) {
        return executeRequest(RED_PACKET_EFFECT_API, params, config);
    }

    @Override
    public JsonNode queryPromotionEffect(Map<String, Object> params, CpsVendorConfig config) {
        return executeRequest(PROMOTION_EFFECT_API, params, config);
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
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request, CpsVendorConfig config) {
        CpsOrderPageResult regular = super.queryOrderPage(request, config);
        if (!Boolean.parseBoolean(extra(config, "includeBonusOrders"))) {
            return regular;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("optType", integer(extra(config, "jdBonusOptType"), 1));
        params.put("startTime", toEpochMillis(request.getStartTime()));
        params.put("endTime", toEpochMillis(request.getEndTime()));
        params.put("pageNo", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        JsonNode response = executeRequest(BONUS_ORDER_API, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return regular;
        }
        List<CpsOrderDTO> bonusOrders = parseBonusOrderResponse(response);
        List<CpsOrderDTO> merged = new ArrayList<>(regular.getItems());
        merged.addAll(bonusOrders);
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 50 : request.getPageSize();
        boolean bonusHasMore = bonusOrders.size() >= pageSize;
        boolean hasMore = regular.isHasMore() || bonusHasMore;
        return CpsOrderPageResult.page(merged, hasMore ? pageNo + 1 : null, hasMore);
    }

    private List<CpsOrderDTO> parseBonusOrderResponse(JsonNode response) {
        JsonNode data = resultNode(response, "queryResult").path("data");
        if (!data.isArray()) {
            return Collections.emptyList();
        }
        List<CpsOrderDTO> orders = new ArrayList<>();
        for (JsonNode item : data) {
            orders.add(CpsOrderDTO.builder()
                    .platformOrderId(firstText(item.path("id"), item.path("orderId")))
                    .parentOrderId(item.path("parentId").asText(null))
                    .platformCode(getPlatformCode())
                    .vendorCode(getVendorCode())
                    .itemId(firstText(item.path("skuId"), item.path("itemId")))
                    .itemTitle(item.path("skuName").asText(null))
                    .itemPrice(decimal(item.path("payPrice")))
                    .finalPrice(decimal(item.path("actualCosPrice"), item.path("estimateCosPrice")))
                    .commissionRate(decimal(item.path("commissionRate"), item.path("finalRate")))
                    .commissionAmount(decimal(item.path("actualBonusFee"), item.path("estimateBonusFee"),
                            item.path("actualFee"), item.path("estimateFee")))
                    .quantity(1)
                    .platformStatus(item.path("orderState").asInt(item.path("bonusState").asInt(0)))
                    .orderTime(item.path("orderTime").asText(null))
                    .settleTime(item.path("finishTime").asText(null))
                    .adzoneId(firstText(item.path("positionId"), item.path("pid")))
                    .externalId(item.path("subUnionId").asText(null))
                    .extraFields(Map.of("orderCategory", "BONUS"))
                    .rawPayload(item.toString())
                    .build());
        }
        return orders;
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
            if (GOODS_JINGFEN_API.equals(path)) {
                JFGoodsReq req = new JFGoodsReq();
                req.setEliteId(integer(params.get("eliteId"), 1));
                req.setPageIndex(integer(params.get("pageIndex"), 1));
                req.setPageSize(integer(params.get("pageSize"), 20));
                req.setPid(string(params.get("pid")));
                UnionOpenGoodsJingfenQueryRequest request = new UnionOpenGoodsJingfenQueryRequest();
                request.setGoodsReq(req);
                response = client.execute(request);
            } else if (GOODS_RECOMMEND_API.equals(path)) {
                RecommendGoodsReq req = new RecommendGoodsReq();
                req.setItemId(string(params.get("itemId")));
                req.setSkuId(longOrNull(params.get("skuId")));
                req.setKeyword(string(params.get("keyword")));
                req.setSceneId(integer(params.get("sceneId"), 1));
                UnionOpenGoodsRecommendQueryRequest request = new UnionOpenGoodsRecommendQueryRequest();
                request.setRecommendGoodsReq(req);
                response = client.execute(request);
            } else if (getSearchApiPath().equals(path)) {
                GoodsReq req = new GoodsReq();
                req.setKeyword(string(params.get("keyword")));
                req.setPageIndex(integer(params.get("pageIndex"), 1));
                req.setPageSize(integer(params.get("pageSize"), 20));
                req.setPid(string(params.get("pid")));
                if (params.get("skuIds") instanceof Long[] skuIds) {
                    req.setSkuIds(skuIds);
                }
                req.setPricefrom(decimal(params.get("pricefrom")) == null ? null : decimal(params.get("pricefrom")).doubleValue());
                req.setPriceto(decimal(params.get("priceto")) == null ? null : decimal(params.get("priceto")).doubleValue());
                req.setIsCoupon(integerOrNull(params.get("isCoupon")));
                UnionOpenGoodsQueryRequest request = new UnionOpenGoodsQueryRequest();
                request.setGoodsReqDTO(req);
                response = client.execute(request);
            } else if (COUPON_QUERY_API.equals(path)) {
                UnionOpenCouponQueryRequest request = new UnionOpenCouponQueryRequest();
                @SuppressWarnings("unchecked")
                List<String> couponUrls = (List<String>) params.get("couponUrls");
                request.setCouponUrls(couponUrls);
                response = client.execute(request);
            } else if (getPromotionLinkApiPath().equals(path)) {
                String mode = normalizeMode(string(params.get("promotionMode")));
                if ("social".equals(mode)) {
                    com.jd.open.api.sdk.domain.kplunion.promotionbysubunioni.PromotionService.request.get.PromotionCodeReq req =
                            new com.jd.open.api.sdk.domain.kplunion.promotionbysubunioni.PromotionService.request.get.PromotionCodeReq();
                    req.setMaterialId(string(params.get("materialId")));
                    req.setPositionId(parseLong(string(params.get("positionId")), 0L));
                    req.setSubUnionId(string(params.get("subUnionId")));
                    req.setPid(string(params.get("pid")));
                    req.setCouponUrl(string(params.get("couponUrl")));
                    UnionOpenPromotionBysubunionidGetRequest request = new UnionOpenPromotionBysubunionidGetRequest();
                    request.setPromotionCodeReq(req);
                    response = client.execute(request);
                } else if ("tool".equals(mode)) {
                    com.jd.open.api.sdk.domain.kplunion.promotionbyunionid.PromotionService.request.get.PromotionCodeReq req =
                            new com.jd.open.api.sdk.domain.kplunion.promotionbyunionid.PromotionService.request.get.PromotionCodeReq();
                    req.setMaterialId(string(params.get("materialId")));
                    req.setUnionId(longOrNull(params.get("unionId")));
                    req.setPositionId(longOrNull(params.get("positionId")));
                    req.setSubUnionId(string(params.get("subUnionId")));
                    req.setPid(string(params.get("pid")));
                    req.setCouponUrl(string(params.get("couponUrl")));
                    UnionOpenPromotionByunionidGetRequest request = new UnionOpenPromotionByunionidGetRequest();
                    request.setPromotionCodeReq(req);
                    response = client.execute(request);
                } else {
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
                }
            } else if (BONUS_ORDER_API.equals(path)) {
                BonusOrderReq req = new BonusOrderReq();
                req.setOptType(integer(params.get("optType"), 1));
                req.setStartTime(longOrNull(params.get("startTime")));
                req.setEndTime(longOrNull(params.get("endTime")));
                req.setPageNo(integer(params.get("pageNo"), 1));
                req.setPageSize(integer(params.get("pageSize"), 50));
                UnionOpenOrderBonusQueryRequest request = new UnionOpenOrderBonusQueryRequest();
                request.setOrderReq(req);
                response = client.execute(request);
            } else if (POSITION_CREATE_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.PositionService.request.create.PositionReq req =
                        new com.jd.open.api.sdk.domain.kplunion.PositionService.request.create.PositionReq();
                req.setUnionId(parseLong(string(params.get("unionId")), 0L));
                req.setKey(string(params.get("key")));
                req.setUnionType(integer(params.get("unionType"), 0));
                req.setType(integer(params.get("type"), 0));
                req.setSiteId(parseLong(string(params.get("siteId")), 0L));
                @SuppressWarnings("unchecked")
                List<String> names = (List<String>) params.get("names");
                req.setSpaceNameList(names == null ? new String[0] : names.toArray(String[]::new));
                UnionOpenPositionCreateRequest request = new UnionOpenPositionCreateRequest();
                request.setPositionReq(req);
                response = client.execute(request);
            } else if (POSITION_QUERY_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.PositionService.request.query.PositionReq req =
                        new com.jd.open.api.sdk.domain.kplunion.PositionService.request.query.PositionReq();
                req.setUnionId(parseLong(string(params.get("unionId")), 0L));
                req.setKey(string(params.get("key")));
                req.setUnionType(integer(params.get("unionType"), 0));
                req.setPageIndex(integer(params.get("pageIndex"), 1));
                req.setPageSize(integer(params.get("pageSize"), 20));
                UnionOpenPositionQueryRequest request = new UnionOpenPositionQueryRequest();
                request.setPositionReq(req);
                response = client.execute(request);
            } else if (PID_GET_API.equals(path)) {
                PidReq req = new PidReq();
                req.setUnionId(longOrNull(params.get("unionId")));
                req.setChildUnionId(longOrNull(params.get("childUnionId")));
                req.setPromotionType(integerOrNull(params.get("promotionType")));
                req.setPositionName(string(params.get("positionName")));
                req.setMediaName(string(params.get("mediaName")));
                UnionOpenUserPidGetRequest request = new UnionOpenUserPidGetRequest();
                request.setPidReq(req);
                response = client.execute(request);
            } else if (CHANNEL_RELATION_GET_API.equals(path)) {
                ChannelRelationGetReq req = new ChannelRelationGetReq();
                req.setInviteCode(string(params.get("inviteCode")));
                req.setNote(string(params.get("note")));
                req.setChannelNote(string(params.get("channelNote")));
                UnionOpenChannelRelationGetRequest request = new UnionOpenChannelRelationGetRequest();
                request.setChannelRelationGetReq(req);
                response = client.execute(request);
            } else if (GIFT_CREATE_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.CouponService.request.get.CreateGiftCouponReq req =
                        new com.jd.open.api.sdk.domain.kplunion.CouponService.request.get.CreateGiftCouponReq();
                req.setSkuMaterialId(string(params.get("skuMaterialId")));
                req.setDiscount(doubleOrNull(params.get("discount")));
                req.setAmount(integerOrNull(params.get("amount")));
                req.setReceiveStartTime(string(params.get("receiveStartTime")));
                req.setReceiveEndTime(string(params.get("receiveEndTime")));
                req.setEffectiveDays(integerOrNull(params.get("effectiveDays")));
                req.setIsSpu(integerOrNull(params.get("isSpu")));
                req.setExpireType(integerOrNull(params.get("expireType")));
                req.setUseStartTime(string(params.get("useStartTime")));
                req.setUseEndTime(string(params.get("useEndTime")));
                req.setShare(integerOrNull(params.get("share")));
                req.setContentMatch(integerOrNull(params.get("contentMatch")));
                req.setCouponTitle(string(params.get("couponTitle")));
                req.setContentMatchMedias(integerArray(params.get("contentMatchMedias")));
                req.setShowInMedias(integerOrNull(params.get("showInMedias")));
                req.setTargetType(integerOrNull(params.get("targetType")));
                req.setChildPromoters(string(params.get("childPromoters")));
                UnionOpenCouponGiftGetRequest request = new UnionOpenCouponGiftGetRequest();
                request.setCouponReq(req);
                response = client.execute(request);
            } else if (GIFT_STOP_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.CouponService.request.stop.StopGiftCouponReq req =
                        new com.jd.open.api.sdk.domain.kplunion.CouponService.request.stop.StopGiftCouponReq();
                req.setGiftCouponKey(string(params.get("giftCouponKey")));
                UnionOpenCouponGiftStopRequest request = new UnionOpenCouponGiftStopRequest();
                request.setCouponReq(req);
                response = client.execute(request);
            } else if (GIFT_EFFECT_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.StatisticsService.request.query.GiftCouponEffectDataReq req =
                        new com.jd.open.api.sdk.domain.kplunion.StatisticsService.request.query.GiftCouponEffectDataReq();
                req.setSkuId(longOrNull(params.get("skuId")));
                req.setGiftCouponKey(string(params.get("giftCouponKey")));
                req.setCreateTime(string(params.get("createTime")));
                req.setStartTime(string(params.get("startTime")));
                req.setKey(string(params.get("key")));
                req.setTargetType(integerOrNull(params.get("targetType")));
                req.setItemId(string(params.get("itemId")));
                UnionOpenStatisticsGiftcouponQueryRequest request = new UnionOpenStatisticsGiftcouponQueryRequest();
                request.setEffectDataReq(req);
                response = client.execute(request);
            } else if (RED_PACKET_EFFECT_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.StatisticsService.request.query.RedPacketEffectDataReq req =
                        new com.jd.open.api.sdk.domain.kplunion.StatisticsService.request.query.RedPacketEffectDataReq();
                req.setActId(longOrNull(params.get("actId")));
                req.setPositionId(longOrNull(params.get("positionId")));
                req.setStartDate(string(params.get("startDate")));
                req.setEndDate(string(params.get("endDate")));
                req.setPageIndex(integerOrNull(params.get("pageIndex")));
                req.setPageSize(integerOrNull(params.get("pageSize")));
                req.setKey(string(params.get("key")));
                req.setType(integerOrNull(params.get("type")));
                req.setChannelIds(longArray(params.get("channelIds")));
                UnionOpenStatisticsRedpacketQueryRequest request = new UnionOpenStatisticsRedpacketQueryRequest();
                request.setEffectDataReq(req);
                response = client.execute(request);
            } else if (PROMOTION_EFFECT_API.equals(path)) {
                com.jd.open.api.sdk.domain.kplunion.StatisticsService.request.query.PromotionEffectDataReq req =
                        new com.jd.open.api.sdk.domain.kplunion.StatisticsService.request.query.PromotionEffectDataReq();
                req.setSkuId(longOrNull(params.get("skuId")));
                req.setActivityUrl(string(params.get("activityUrl")));
                req.setTimeType(integerOrNull(params.get("timeType")));
                req.setDataType(integerOrNull(params.get("dataType")));
                req.setFields(string(params.get("fields")));
                req.setItemId(string(params.get("itemId")));
                UnionOpenStatisticsPromotionQueryRequest request = new UnionOpenStatisticsPromotionQueryRequest();
                request.setPromotionEffectDataReq(req);
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
        for (String name : List.of("queryResult", "getResult", "createResult")) {
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

    private Long firstLong(JsonNode... nodes) {
        return longValue(nodes);
    }

    private String formatEpoch(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return null;
        }
        long epochMillis = timestamp < 10_000_000_000L ? timestamp * 1000L : timestamp;
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    private Long toEpochMillis(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        } catch (java.time.format.DateTimeParseException ignored) {
            return parseLong(value, null);
        }
    }

    private Integer integer(Object value, int fallback) {
        Integer parsed = integerOrNull(value);
        return parsed == null ? fallback : parsed;
    }

    private Integer integerOrNull(Object value) {
        if (value == null) return null;
        try { return Integer.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private Double doubleOrNull(Object value) {
        if (value == null) return null;
        try { return Double.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private Integer[] integerArray(Object value) {
        if (value == null) return null;
        if (value instanceof Integer[] values) return values;
        if (value instanceof List<?> values) {
            return values.stream().map(this::integerOrNull).toArray(Integer[]::new);
        }
        return new Integer[]{integerOrNull(value)};
    }

    private Long[] longArray(Object value) {
        if (value == null) return null;
        if (value instanceof Long[] values) return values;
        if (value instanceof List<?> values) {
            return values.stream().map(this::longOrNull).toArray(Long[]::new);
        }
        return new Long[]{longOrNull(value)};
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longOrNull(Object value) {
        return value == null ? null : parseLong(String.valueOf(value), null);
    }

    private Long parseLong(String value, Long fallback) {
        try { return value == null ? fallback : Long.valueOf(value); } catch (NumberFormatException e) { return fallback; }
    }

    private String extra(CpsVendorConfig config, String key) {
        return config == null || config.getExtraConfig() == null ? null : config.getExtraConfig().get(key);
    }

    private String normalizeMode(String value) {
        return value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private JdPid parsePid(String value) {
        if (value == null || value.isBlank()) {
            return new JdPid(null, null, null);
        }
        String[] parts = value.trim().split("_");
        if (parts.length >= 3) {
            return new JdPid(parts[0], parts[1], parseLong(parts[2], null));
        }
        Long numeric = parseLong(value.trim(), null);
        return new JdPid(null, null, numeric);
    }

    private record JdPid(String unionId, String siteId, Long positionId) {
    }
}
