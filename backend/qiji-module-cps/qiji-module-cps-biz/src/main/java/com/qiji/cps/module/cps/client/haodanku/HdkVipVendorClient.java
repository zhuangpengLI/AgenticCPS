package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 好单库唯品会供应商客户端。 */
@Component
public class HdkVipVendorClient extends AbstractHdkVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.VIP.getCode();
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        return root != null && root.path("code").asInt(-1) == 200;
    }

    @Override
    protected String resolveApiBaseUrl(CpsVendorConfig config) {
        String baseUrl = super.resolveApiBaseUrl(config);
        return baseUrl == null ? null : baseUrl.replace("v2.api.haodanku.com", "v3.api.haodanku.com");
    }

    @Override
    protected String getSearchApiPath() {
        return "/unify_vip_item_query";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("min_id", request.getPageNo() == null ? 1 : request.getPageNo());
        params.put("back", request.getPageSize() == null ? 20 : request.getPageSize());
        params.put("order", convertSortType(request.getSortType()));
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                goodsList.add(CpsGoodsItem.builder()
                        .goodsId(firstText(item, "goodsId", "goods_id"))
                        .platformCode(getPlatformCode())
                        .vendorCode(getVendorCode())
                        .title(firstText(item, "goodsName", "goods_name"))
                        .mainPic(firstText(item, "goodsMainPicture", "goods_main_picture"))
                        .originalPrice(firstDecimal(item, "marketPrice", "market_price"))
                        .actualPrice(firstDecimal(item, "vipPrice", "vip_price"))
                        .commissionRate(firstDecimal(item, "commissionRate", "commission_rate"))
                        .commissionAmount(firstDecimal(item, "commission"))
                        .brandName(firstText(item, "brandName", "brand_name"))
                        .monthSales(item.path("productSales").asLong(0L))
                        .rawPayload(item.toString())
                        .build());
            }
        }
        return CpsGoodsSearchResult.builder()
                .list(goodsList)
                .total(response.path("total").asLong(-1L))
                .nextPageId(firstText(response, "min_id"))
                .pageNo(request.getPageNo() == null ? 1 : request.getPageNo())
                .pageSize(request.getPageSize() == null ? 20 : request.getPageSize())
                .build();
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "/unify_vip_item_convert";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goods_id", firstNonBlank(request.getGoodsId(), request.getGoodsSign()));
        params.put("channel", firstNonBlank(request.getChannelId(), request.getExternalId()));
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = hdkPayload(response);
        return CpsPromotionLinkResult.builder()
                .shortUrl(firstText(data, "url"))
                .longUrl(firstText(data, "longUrl", "long_url"))
                .tpwd(firstText(data, "onlyCommand", "only_command"))
                .mobileUrl(firstText(data, "deeplinkUrl", "deeplink_url"))
                .extraFields(selectedFields(data, "deeplinkUrl", "onlyCommand"))
                .rawPayload(response.toString())
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/vip_union_order_list";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("min_id", firstNonBlank(request.getPositionIndex(), String.valueOf(request.getPageNo())));
        params.put("back", request.getPageSize());
        params.put("start_date", toHdkUnixSeconds(request.getStartTime()));
        params.put("end_date", toHdkUnixSeconds(request.getEndTime()));
        params.put("date_type", request.getQueryType());
        params.put("state", request.getOrderStatus() == null ? 0 : request.getOrderStatus());
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        String nextPositionIndex = firstText(response, "min_id");
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            return orders;
        }
        for (JsonNode item : data) {
            Integer rawStatus = firstInt(item, "order_status");
            orders.add(CpsOrderDTO.builder()
                    .platformCode(getPlatformCode())
                    .vendorCode(getVendorCode())
                    .platformOrderId(firstText(item, "trade_id"))
                    .parentOrderId(firstText(item, "trade_parent_id"))
                    .itemId(firstText(item, "goods_id"))
                    .itemTitle(firstText(item, "item_title"))
                    .itemPic(firstText(item, "item_img"))
                    .finalPrice(firstDecimal(item, "pay_price"))
                    .commissionAmount(firstNonZeroDecimal(item, "actual_money", "predict_money"))
                    .platformStatus(mapVipOrderStatus(rawStatus))
                    .refundTag(Integer.valueOf(6).equals(rawStatus) ? 1 : 0)
                    .orderTime(firstText(item, "create_time"))
                    .payTime(firstText(item, "paid_time"))
                    .settleTime(firstText(item, "settled_at", "earning_time"))
                    .externalId(firstText(item, "channel_code"))
                    .nextPositionIndex(nextPositionIndex)
                    .extraFields(selectedFields(item, "shop_name", "order_status", "settled_status",
                            "earning_time", "settled_at", "channel_code", "updated_at"))
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
        return Map.of("keyword", "手机", "min_id", 1, "back", 10, "order", 0);
    }

    private int convertSortType(Integer sortType) {
        return switch (sortType == null ? 0 : sortType) {
            case 1 -> 6;
            case 2 -> 1;
            case 3 -> 2;
            default -> 0;
        };
    }

    private Integer mapVipOrderStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            case 5 -> 4;
            case 6 -> -1;
            default -> status;
        };
    }
}
