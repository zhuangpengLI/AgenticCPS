package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.client.selection.CpsTaobaoSelectionVendorClient;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 好单库-淘宝供应商客户端
 *
 * <p>通过好单库 API 对接淘宝联盟。</p>
 * <p>API文档：https://www.haodanku.com/openapi</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class HdkTaobaoVendorClient extends AbstractHdkVendorClient implements CpsTaobaoSelectionVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    // ==================== 商品搜索 ====================

    @Override
    protected String getSearchApiPath() {
        return "/supersearch";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pagesize", request.getPageSize());
        if (request.getSortType() != null) {
            // 好单库排序：0-综合，1-券后价升，2-券后价降，3-销量降，4-佣金比例降
            params.put("sort", convertSortType(request.getSortType()));
        }
        if (request.getPriceLowerLimit() != null) {
            params.put("min_price", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("max_price", request.getPriceUpperLimit());
        }
        if (request.getHasCoupon() != null) {
            params.put("is_coupon", request.getHasCoupon());
        }
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                goodsList.add(parseGoodsItem(item));
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
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        if (!hasColumnSearchCondition(request)) {
            return super.searchGoods(request, config);
        }
        try {
            JsonNode response = executeRequest("/column", buildColumnSearchParams(request), config);
            if (response == null || !isSuccessResponse(response)) {
                log.warn("[{}:{}] column search failed: {}", getVendorCode(), getPlatformCode(), response);
                return buildEmptyResult(request);
            }
            return parseSearchResponse(response, request);
        } catch (Exception e) {
            log.error("[{}:{}] column search exception", getVendorCode(), getPlatformCode(), e);
            return buildEmptyResult(request);
        }
    }

    public Map<String, Object> buildColumnSearchParams(CpsGoodsSearchRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", convertChannelType(request.getChannelCode()));
        params.put("back", request.getPageSize());
        params.put("min_id", request.getPageNo());
        if (request.getSortType() != null) {
            params.put("sort", convertColumnSortType(request.getSortType()));
        }
        if (hasText(request.getCategoryId()) && !"0".equals(request.getCategoryId())) {
            params.put("cid", parseInteger(request.getCategoryId()));
        }
        if (request.getPriceLowerLimit() != null) {
            params.put("price_min", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("price_max", request.getPriceUpperLimit());
        }
        if (request.getCouponAmountMin() != null) {
            params.put("coupon_min", request.getCouponAmountMin());
        }
        if (request.getMinMonthSales() != null) {
            params.put("sale_min", request.getMinMonthSales());
        }
        if (Boolean.TRUE.equals(request.getTmallOnly())) {
            params.put("shoptype", "B");
        }
        return params;
    }

    // ==================== 推广转链 ====================

    @Override
    protected String getPromotionLinkApiPath() {
        return "/ratesurl";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("itemid", request.getGoodsId());
        if (request.getAdzoneId() != null) {
            params.put("pid", request.getAdzoneId());
        } else if (config.getDefaultAdzoneId() != null) {
            params.put("pid", config.getDefaultAdzoneId());
        }
        // 好单库转链API可选参数：淘宝授权账号昵称
        if (config.getAuthToken() != null) {
            params.put("tb_name", config.getAuthToken());
        }
        // 默认返回淘口令
        params.put("get_taoword", 1);
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        // 好单库 v3 转链API返回字段：coupon_click_url, item_url, taoword
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("coupon_click_url").asText(null))
                .longUrl(data.path("item_url").asText(null))
                .tpwd(data.path("taoword").asText(null))
                .build();
    }

    // ==================== 订单查询 ====================

    @Override
    protected String getOrderQueryApiPath() {
        return "/rest";
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        try {
            String fullUrl = getPromotionLinkBaseUrl(config) + getOrderQueryApiPath();
            JsonNode response = executePostRequest(fullUrl, buildOrderQueryParams(request, config), config);
            if (response == null || !isSuccessResponse(response)) {
                log.warn("[{}:{}] 查询订单失败: {}", getVendorCode(), getPlatformCode(), response);
                return Collections.emptyList();
            }
            return parseOrderQueryResponse(response);
        } catch (Exception e) {
            log.error("[{}:{}] 查询订单异常", getVendorCode(), getPlatformCode(), e);
            return Collections.emptyList();
        }
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("method", "tbk.order");
        params.put("v", firstNonBlank(getExtraConfig(config, "order_version"), "3.7.12"));
        params.put("app_id", firstNonBlank(getExtraConfig(config, "app_id"), getExtraConfig(config, "order_app_id")));
        params.put("sign", firstNonBlank(getExtraConfig(config, "sign"), getExtraConfig(config, "order_sign")));
        params.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("tb_name", firstNonBlank(config.getAuthToken(), getExtraConfig(config, "tb_name")));
        params.put("start_time", request.getStartTime());
        params.put("end_time", request.getEndTime());
        params.put("page_no", request.getPageNo());
        params.put("page_size", request.getPageSize());
        params.put("jump_type", firstNonBlank(getExtraConfig(config, "jump_type"), "1"));
        params.put("tk_status", request.getOrderStatus());
        params.put("order_scene", firstNonBlank(getExtraConfig(config, "order_scene"), "1"));
        params.put("query_type", request.getQueryType());
        params.put("member_type", getExtraConfig(config, "member_type"));
        params.put("position_index", request.getPositionIndex());
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                Integer tkStatus = firstInt(item, "tk_status");
                Integer rawStatus = tkStatus != null ? tkStatus : firstInt(item, "order_status");
                orders.add(CpsOrderDTO.builder()
                        .platformCode(getPlatformCode())
                        .platformOrderId(firstText(item, "trade_id", "tradeId", "order_id", "orderId"))
                        .parentOrderId(firstText(item, "trade_parent_id", "tradeParentId", "parent_order_id", "parentOrderId"))
                        .itemId(firstText(item, "item_id", "itemId", "goods_id", "goodsId"))
                        .itemTitle(firstText(item, "item_title", "itemTitle", "goods_name", "goodsName", "title"))
                        .itemPic(firstText(item, "item_img", "itemImg", "itempic", "item_pic"))
                        .itemPrice(firstDecimal(item, "item_price", "itemPrice", "auction_price", "auctionPrice"))
                        .finalPrice(firstNonZeroDecimal(item, "pay_price", "alipay_total_price", "payPrice", "alipayTotalPrice", "order_amount", "orderAmount"))
                        .commissionRate(firstNonZeroDecimal(item, "commission_rate", "pub_share_rate", "pubShareRate", "total_commission_rate", "totalCommissionRate"))
                        .commissionAmount(firstNonZeroDecimal(item, "commission", "predict_money", "actual_money", "pub_share_fee", "pub_share_pre_fee", "pubShareFee", "pubSharePreFee"))
                        .platformStatus(tkStatus != null ? mapTaobaoTkStatus(rawStatus) : mapHdkOrderStatus(rawStatus))
                        .orderTime(firstText(item, "create_time", "tk_create_time", "order_time", "orderTime"))
                        .payTime(firstText(item, "paid_time", "pay_time", "tk_paid_time", "tb_paid_time"))
                        .receiveTime(firstText(item, "receive_time", "confirm_receipt_time", "tk_deposit_time", "tb_deposit_time"))
                        .settleTime(firstText(item, "earning_time", "settle_time", "settled_at", "tk_earning_time"))
                        .adzoneId(firstText(item, "adzone_id", "adzoneId", "pid", "position_id"))
                        .externalId(firstText(item, "special_id", "specialId", "relation_id", "relationId",
                                "channel_code", "external_id", "externalId", "sid"))
                        .extraFields(selectedFields(item, "order_status", "tk_status", "settled_status", "special_id",
                                "relation_id", "channel_code", "sub_union_id", "refund_time", "fail_reason"))
                        .rawPayload(item.toString())
                        .build());
            }
        }
        return orders;
    }

    // ==================== 连接测试 ====================

    @Override
    protected String getTestConnectionApiPath() {
        return "/supersearch";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "手机");
        params.put("page", 1);
        params.put("pagesize", 1);
        return params;
    }

    @Override
    public CpsGoodsSelectionMeta getSelectionMeta(CpsVendorConfig config) {
        return CpsGoodsSelectionMeta.builder()
                .hotKeywords(fetchHotKeywords(config))
                .categories(fetchCategories(config))
                .activities(fetchColumns(config))
                .metaSource(getVendorCode())
                .build();
    }

    // ==================== 私有方法 ====================

    private CpsGoodsItem parseGoodsItem(JsonNode item) {
        return CpsGoodsItem.builder()
                .goodsId(item.path("itemid").asText(null))
                .platformCode(getPlatformCode())
                .title(item.path("itemtitle").asText(null))
                .mainPic(item.path("itempic").asText(null))
                .originalPrice(parseDecimal(item, "itemprice"))
                .actualPrice(parseDecimal(item, "itemendprice"))
                .couponPrice(parseDecimal(item, "couponmoney"))
                .commissionRate(parseDecimal(item, "tkrates"))
                .monthSales(item.path("itemsale").asLong(0))
                .shopName(item.path("shopname").asText(null))
                .shopType(item.path("shoptype").asInt(0))
                .itemLink(item.path("itemlink").asText(null))
                .vendorCode(getVendorCode())
                .source(firstText(item, "item_from", "source"))
                .activityTag(firstText(item, "activity_type", "activity_tag"))
                .categoryName(categoryName(item.path("fqcat").asText(null)))
                .couponEndTime(firstText(item, "couponendtime", "coupon_end_time"))
                .rankTag(firstText(item, "rank_tag", "son_category"))
                .sellingPoint(firstText(item, "itemdesc", "guide_article", "itemshorttitle"))
                .build();
    }

    private Integer convertSortType(Integer sortType) {
        return switch (sortType) {
            case 1 -> 3;  // 销量降序
            case 2 -> 1;  // 价格升序
            case 3 -> 2;  // 价格降序
            case 4 -> 4;  // 佣金率降序
            default -> 0; // 综合排序
        };
    }

    private boolean hasColumnSearchCondition(CpsGoodsSearchRequest request) {
        return hasText(request.getChannelCode())
                || (hasText(request.getCategoryId()) && !"0".equals(request.getCategoryId()))
                || request.getCouponAmountMin() != null
                || request.getMinMonthSales() != null
                || Boolean.TRUE.equals(request.getTmallOnly())
                || Boolean.TRUE.equals(request.getBrandOnly());
    }

    private Integer convertChannelType(String channelCode) {
        if (!hasText(channelCode)) {
            return 1;
        }
        return switch (channelCode) {
            case "brand" -> 8;
            case "presale" -> 7;
            case "flash" -> 5;
            case "tmall" -> 9;
            default -> 1;
        };
    }

    private Integer convertColumnSortType(Integer sortType) {
        return switch (sortType) {
            case 1 -> 4;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 5;
            default -> 0;
        };
    }

    private List<CpsGoodsSelectionOption> fetchHotKeywords(CpsVendorConfig config) {
        JsonNode response = executeRequest("/hot_key", new LinkedHashMap<>(), config);
        if (response == null || !isSuccessResponse(response)) {
            return Collections.emptyList();
        }
        JsonNode payload = hdkPayload(response);
        JsonNode list = payload.isArray() ? payload : firstArray(payload, "data", "hot_key", "list");
        return parseOptionList(list, "keyword", "keyword", "word", "name");
    }

    private List<CpsGoodsSelectionOption> fetchCategories(CpsVendorConfig config) {
        JsonNode response = executeRequest("/super_classify", new LinkedHashMap<>(), config);
        if (response == null || !isSuccessResponse(response)) {
            return Collections.emptyList();
        }
        JsonNode payload = hdkPayload(response);
        JsonNode list = payload.isArray() ? payload : firstArray(payload, "general_classify", "data", "list");
        return parseOptionList(list, "cid", "main_name", "name", "label");
    }

    private List<CpsGoodsSelectionOption> fetchColumns(CpsVendorConfig config) {
        JsonNode response = executeRequest("/column", Map.of("min_id", 1, "back", 1), config);
        if (response == null || !isSuccessResponse(response)) {
            return Collections.emptyList();
        }
        return List.of(
                option("hot", "今日热卖", "热", "实时热销选品"),
                option("brand", "品牌精选", "品", "品牌和天猫精选"),
                option("flash", "限时爆款", "抢", "限时高转化商品"),
                option("presale", "预售清单", null, "活动预售商品"));
    }

    private List<CpsGoodsSelectionOption> parseOptionList(JsonNode list, String valueField, String... labelFields) {
        if (list == null || !list.isArray()) {
            return Collections.emptyList();
        }
        List<CpsGoodsSelectionOption> options = new ArrayList<>();
        for (JsonNode item : list) {
            String value = firstText(item, valueField, "id", "cid");
            String label = firstText(item, labelFields);
            if (!hasText(label) && item.isTextual()) {
                label = item.asText();
                value = label;
            }
            if (hasText(value) && hasText(label)) {
                options.add(CpsGoodsSelectionOption.of(value, label));
            }
        }
        return options;
    }

    private CpsGoodsSelectionOption option(String value, String label, String tag, String description) {
        return CpsGoodsSelectionOption.builder()
                .value(value)
                .label(label)
                .tag(tag)
                .description(description)
                .build();
    }

    private JsonNode firstArray(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer mapTaobaoTkStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 12 -> 1;
            case 14 -> 2;
            case 3 -> 3;
            case 13 -> -1;
            default -> status;
        };
    }

    private String categoryName(String fqcat) {
        if (!hasText(fqcat)) {
            return null;
        }
        return switch (fqcat) {
            case "1" -> "女装";
            case "2" -> "男装";
            case "3" -> "内衣";
            case "4" -> "美妆";
            case "5" -> "配饰";
            case "6" -> "鞋品";
            case "7" -> "箱包";
            case "8" -> "儿童";
            case "9" -> "母婴";
            case "10" -> "居家";
            case "11" -> "美食";
            case "12" -> "数码家电";
            case "13" -> "车品";
            case "14" -> "文体";
            case "15" -> "宠物";
            default -> null;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
