package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 好单库-拼多多供应商客户端
 *
 * <p>通过好单库 API 对接拼多多联盟。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class HdkPddVendorClient extends AbstractHdkVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.PDD.getCode();
    }

    @Override
    protected String getSearchApiPath() {
        // 好单库拼多多搜索接口：/pdd_goods_search (GET, v2)
        // 注意：该接口需要在好单库平台进行多多进宝授权，未授权时返回 code=0 错误
        return "/pdd_goods_search";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", encodeKeywordOnce(request.getKeyword()));
        params.put("min_id", request.getPageNo());
        params.put("limit", request.getPageSize());
        params.put("sort", convertSortType(request.getSortType()));
        params.put("is_coupon", request.getHasCoupon() == null ? 0 : request.getHasCoupon());
        if (request.getPriceLowerLimit() != null) {
            params.put("start_price", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("end_price", request.getPriceUpperLimit());
        }
        if (config.getDefaultAdzoneId() != null) {
            params.put("pid", config.getDefaultAdzoneId());
        }
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                BigDecimal price = parseDecimal(item, "min_normal_price");
                BigDecimal groupPrice = parseDecimal(item, "min_group_price");
                // PDD 价格单位为分
                BigDecimal priceYuan = price != null ? price.divide(BigDecimal.valueOf(100)) : null;
                BigDecimal groupPriceYuan = groupPrice != null ? groupPrice.divide(BigDecimal.valueOf(100)) : null;

                goodsList.add(CpsGoodsItem.builder()
                        .goodsId(item.path("goods_sign").asText(null))
                        .goodsSign(item.path("goods_sign").asText(null))
                        .platformCode(getPlatformCode())
                        .title(item.path("goods_name").asText(null))
                        .mainPic(item.path("goods_image_url").asText(null))
                        .originalPrice(priceYuan)
                        .actualPrice(groupPriceYuan)
                        .commissionRate(parseDecimal(item, "promotion_rate"))
                        .shopName(item.path("mall_name").asText(null))
                        .build());
            }
        }
        long total = response.path("total").asLong(-1);
        return CpsGoodsSearchResult.builder()
                .list(goodsList)
                .total(total)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "/unify_pdditems_link";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("itemid", firstNonBlank(request.getGoodsSign(), request.getGoodsId()));
        params.put("channel", firstNonBlank(request.getChannelId(), request.getExternalId()));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = hdkPayload(response);
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("short_url").asText(null))
                .longUrl(data.path("url").asText(null))
                .mobileUrl(data.path("mobile_url").asText(null))
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/unify_pdd_order_list";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("min_id", firstNonBlank(request.getPositionIndex(), String.valueOf(request.getPageNo())));
        params.put("back", request.getPageSize());
        params.put("date_type", request.getQueryType());
        params.put("state", request.getOrderStatus() == null ? 0 : request.getOrderStatus());
        params.put("start_date", toHdkUnixSeconds(request.getStartTime()));
        params.put("end_date", toHdkUnixSeconds(request.getEndTime()));
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                Integer rawStatus = firstInt(item, "order_status", "orderStatus");
                orders.add(CpsOrderDTO.builder()
                        .platformCode(getPlatformCode())
                        .platformOrderId(firstText(item, "trade_id", "order_sn", "orderSn", "order_id", "orderId"))
                        .parentOrderId(firstText(item, "trade_parent_id", "parent_order_sn", "parentOrderSn"))
                        .itemId(firstText(item, "goods_sign", "goods_id", "goodsId", "item_id", "itemId"))
                        .itemTitle(firstText(item, "item_title", "goods_name", "goodsName", "title"))
                        .itemPic(firstText(item, "item_img", "goods_thumbnail_url", "goods_image_url", "itempic"))
                        .itemPrice(firstDecimal(item, "goods_price", "goods_price_amount", "min_group_price", "item_price"))
                        .finalPrice(firstNonZeroDecimal(item, "pay_price", "order_amount", "orderAmount", "order_pay_amount"))
                        .commissionRate(firstNonZeroDecimal(item, "commission_rate", "promotion_rate", "promotionRate"))
                        .commissionAmount(firstNonZeroDecimal(item, "actual_money", "predict_money", "promotion_amount", "promotionAmount"))
                        .platformStatus(mapHdkOrderStatus(rawStatus))
                        .refundTag(Integer.valueOf(3).equals(rawStatus) ? 1 : 0)
                        .orderTime(firstText(item, "create_time", "order_create_time", "orderCreateTime"))
                        .payTime(firstText(item, "paid_time", "order_pay_time", "orderPayTime"))
                        .receiveTime(firstText(item, "order_receive_time", "receive_time", "confirm_time"))
                        .settleTime(firstText(item, "earning_time", "settled_at", "settle_time"))
                        .externalId(firstText(item, "channel_code", "custom_parameters", "customParameters",
                                "channel", "external_id", "externalId", "sid"))
                        .extraFields(selectedFields(item, "order_status", "settled_status", "channel_code",
                                "custom_parameters", "customParameters", "refund_time", "fail_reason", "price_compare_status"))
                        .rawPayload(item.toString())
                        .build());
            }
        }
        return orders;
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "/pdd_goods_search";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", encodeKeywordOnce("手机"));
        params.put("min_id", 1);
        params.put("limit", 10);
        return params;
    }

    private String encodeKeywordOnce(String keyword) {
        return keyword == null ? null : URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }

    private Integer convertSortType(Integer sortType) {
        return switch (sortType == null ? 0 : sortType) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 5;
            case 4 -> 6;
            default -> 0;
        };
    }

}
