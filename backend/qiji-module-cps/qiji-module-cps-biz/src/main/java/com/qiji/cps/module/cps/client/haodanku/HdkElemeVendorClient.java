package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class HdkElemeVendorClient extends AbstractHdkVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.ELEME.getCode();
    }

    @Override
    public Set<CpsVendorCapability> getCapabilities() {
        return EnumSet.of(CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY, CpsVendorCapability.CONNECTION_TEST);
    }

    @Override
    protected String getSearchApiPath() {
        return "/elm_activity_ratesurl";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        return Map.of();
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode responseRoot, CpsGoodsSearchRequest request) {
        return buildEmptyResult(request);
    }

    @Override
    protected String getPromotionLinkApiPath() {
        return "/elm_activity_ratesurl";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("activity_id", request.getGoodsId());
        params.put("link", request.getItemLink());
        if (isValidSid(request.getExternalId())) {
            params.put("sid", request.getExternalId());
        }
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot) {
        JsonNode data = hdkPayload(responseRoot);
        Map<String, Object> extraFields = selectedFields(data, "mini_qrcode", "wx_appid", "wx_path",
                "tb_scheme_url", "alipay_mini_url");
        return CpsPromotionLinkResult.builder()
                .shortUrl(firstText(data, "h5_short_link", "h5ShortLink"))
                .longUrl(firstText(data, "h5_url", "h5Url"))
                .tpwd(firstText(data, "full_taobao_word", "fullTaobaoWord"))
                .mobileUrl(firstText(data, "ele_scheme_url", "eleSchemeUrl"))
                .extraFields(extraFields)
                .rawPayload(responseRoot.toString())
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/elm_order_list";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("min_id", firstNonBlank(request.getPositionIndex(),
                request.getPageNo() == null ? null : String.valueOf(request.getPageNo()), "1"));
        params.put("back", request.getPageSize() == null ? 50 : Math.min(request.getPageSize(), 100));
        params.put("start_date", toHdkUnixSeconds(request.getStartTime()));
        params.put("end_date", toHdkUnixSeconds(request.getEndTime()));
        params.put("date_type", request.getQueryType() == null ? 1 : request.getQueryType());
        params.put("state", request.getOrderStatus() == null ? 0 : request.getOrderStatus());
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = responseRoot.path("data");
        String nextPosition = firstText(responseRoot, "min_id", "minId");
        if (!data.isArray()) {
            return orders;
        }
        for (JsonNode item : data) {
            Integer rawStatus = firstInt(item, "order_status", "orderStatus");
            orders.add(CpsOrderDTO.builder()
                    .platformCode(getPlatformCode())
                    .vendorCode(getVendorCode())
                    .platformOrderId(firstText(item, "trade_id", "tradeId"))
                    .parentOrderId(firstText(item, "trade_parent_id", "tradeParentId"))
                    .itemId(firstText(item, "item_id", "itemId", "shop_id", "shopId"))
                    .itemTitle(firstText(item, "item_title", "itemTitle", "title"))
                    .itemPic(firstText(item, "item_img", "itemImg"))
                    .finalPrice(firstNonZeroDecimal(item, "pay_price", "payPrice"))
                    .commissionAmount(firstNonZeroDecimal(item, "actual_money", "predict_money"))
                    .platformStatus(mapHdkOrderStatus(rawStatus))
                    .refundTag(rawStatus != null && rawStatus == 3 ? 1 : 0)
                    .orderTime(firstText(item, "create_time", "createTime"))
                    .payTime(firstText(item, "paid_time", "paidTime"))
                    .settleTime(firstText(item, "earning_time", "hdk_settle_time", "earningTime"))
                    .externalId(firstText(item, "channel_code", "channelCode"))
                    .nextPositionIndex(nextPosition)
                    .extraFields(selectedFields(item, "settled_status", "order_item_status",
                            "order_item_status_name", "order_channel", "order_tags", "refund_time"))
                    .rawPayload(item.toString())
                    .build());
        }
        return orders;
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "/elm_activity_ratesurl";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        return Map.of();
    }

    private boolean isValidSid(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,15}");
    }
}
