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
 * 好单库-京东供应商客户端
 *
 * <p>通过好单库 API 对接京东联盟。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class HdkJdVendorClient extends AbstractHdkVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.JD.getCode();
    }

    @Override
    protected String getSearchApiPath() {
        // 好单库京东搜索接口：/jd_goods_search，返回 code=200
        return "/jd_goods_search";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("jd_user_id", firstNonBlank(config.getAuthToken(), getExtraConfig(config, "jd_user_id")));
        params.put("keyword", encodeKeywordOnce(request.getKeyword()));
        params.put("min_id", request.getPageNo());
        params.put("back", request.getPageSize());
        params.put("has_coupon", request.getHasCoupon() == null ? 0 : request.getHasCoupon());
        params.put("sort", convertSortType(request.getSortType()));
        if (request.getPriceLowerLimit() != null) {
            params.put("start_price", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("end_price", request.getPriceUpperLimit());
        }
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        // /jd_goods_search 实际返回字段（已通过API测试验证）：
        //   itemid/skuid → goodsId
        //   goodsname    → title
        //   itempic      → mainPic
        //   itemprice    → originalPrice
        //   itemendprice → actualPrice
        //   commissionshare (0.01 = 1%) → commissionRate (需×100)
        //   shopname     → shopName
        //   itemsale     → monthSales
        JsonNode data = response.path("data");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                BigDecimal price = parseDecimal(item, "itemprice");
                BigDecimal endPrice = parseDecimal(item, "itemendprice");
                // commissionshare 字段值为小数（如 0.01 = 1%），需转换为百分比
                BigDecimal commissionShare = parseDecimal(item, "commissionshare");
                BigDecimal commissionRate = commissionShare != null
                        ? commissionShare.multiply(BigDecimal.valueOf(100)) : null;
                goodsList.add(CpsGoodsItem.builder()
                        .goodsId(item.path("itemid").asText(null))
                        .platformCode(getPlatformCode())
                        .title(item.path("goodsname").asText(null))
                        .mainPic(item.path("itempic").asText(null))
                        .originalPrice(price)
                        .actualPrice(endPrice != null ? endPrice : price)
                        .commissionRate(commissionRate)
                        .shopName(item.path("shopname").asText(null))
                        .monthSales(item.path("itemsale").asLong(0))
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
        return "/unify_jditems_link";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("material_id", firstNonBlank(request.getItemLink(), request.getGoodsId()));
        params.put("subUnionId", firstNonBlank(request.getChannelId(), request.getExternalId()));
        params.put("proType", getExtraConfig(config, "proType"));
        params.put("weChatType", getExtraConfig(config, "weChatType"));
        params.put("scene_id", getExtraConfig(config, "scene_id"));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = hdkPayload(response);
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("shortURL").asText(null))
                .longUrl(data.path("clickURL").asText(null))
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/unify_jd_order_list";
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
                Integer rawStatus = firstInt(item, "order_status", "valid_code", "validCode");
                orders.add(CpsOrderDTO.builder()
                        .platformCode(getPlatformCode())
                        .platformOrderId(firstText(item, "trade_id", "order_id", "orderId"))
                        .parentOrderId(firstText(item, "trade_parent_id", "parent_order_id", "parentOrderId"))
                        .itemId(firstText(item, "item_id", "sku_id", "skuId", "goods_id", "goodsId"))
                        .itemTitle(firstText(item, "item_title", "goods_name", "goodsName", "sku_name", "skuName"))
                        .itemPic(firstText(item, "item_img", "item_pic", "goods_img", "image_url"))
                        .itemPrice(firstDecimal(item, "item_price", "sku_price", "goods_price", "price"))
                        .finalPrice(firstNonZeroDecimal(item, "pay_price", "actual_price", "estimate_cos_price", "order_amount"))
                        .commissionRate(firstNonZeroDecimal(item, "commission_rate", "commissionShare", "commission_share"))
                        .commissionAmount(firstNonZeroDecimal(item, "actual_money", "predict_money", "commission", "estimateFee", "estimate_fee", "actual_commission"))
                        .platformStatus(mapHdkOrderStatus(rawStatus))
                        .refundTag(Integer.valueOf(3).equals(rawStatus) ? 1 : 0)
                        .orderTime(firstText(item, "create_time", "order_time", "orderTime"))
                        .payTime(firstText(item, "paid_time", "pay_time", "finish_time"))
                        .settleTime(firstText(item, "earning_time", "settled_at", "settle_time"))
                        .adzoneId(firstText(item, "position_id", "adzone_id", "pid"))
                        .externalId(firstText(item, "sub_union_id", "subUnionId", "subunionid", "channel_code",
                                "position_id", "sid", "external_id", "externalId"))
                        .extraFields(selectedFields(item, "order_status", "valid_code", "settled_status", "sub_union_id",
                                "subUnionId", "channel_code", "refund_time", "fail_reason"))
                        .rawPayload(item.toString())
                        .build());
            }
        }
        return orders;
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "/jd_goods_search";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", encodeKeywordOnce("手机"));
        params.put("min_id", 1);
        params.put("back", 1);
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
