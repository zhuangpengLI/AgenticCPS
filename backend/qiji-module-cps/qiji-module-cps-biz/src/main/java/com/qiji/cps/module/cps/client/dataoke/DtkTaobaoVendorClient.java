package com.qiji.cps.module.cps.client.dataoke;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.client.selection.CpsTaobaoSelectionVendorClient;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
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
public class DtkTaobaoVendorClient extends AbstractDtkVendorClient implements CpsTaobaoSelectionVendorClient {

    private static final String PARSE_CONTENT_PATH = "/tb-service/parse-content";

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    // ==================== 商品搜索 ====================

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
        if (Boolean.TRUE.equals(request.getTmallOnly())) {
            params.put("tmall", 1);
        }
        if (Boolean.TRUE.equals(request.getBrandOnly())) {
            params.put("brand", 1);
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
        if (request.getChannelId() != null) {
            params.put("channelId", request.getChannelId());
        }
        if (request.getExternalId() != null) {
            params.put("externalId", request.getExternalId());
        }
        return params;
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
        if (hasText(config.getDefaultAdzoneId())) {
            params.put("pid", config.getDefaultAdzoneId());
        }

        JsonNode response = executeRequest(PARSE_CONTENT_PATH, params, config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsContentParseResult.unsupported("PARSE_FAILED", "大淘客万能解析失败，请检查口令或链接是否有效");
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

    // ==================== 订单查询 ====================

    @Override
    protected String getOrderQueryApiPath() {
        return "/tb-service/get-order-details";
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
        return "/goods/get-super-category";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        return new HashMap<>();
    }

    @Override
    public CpsGoodsSelectionMeta getSelectionMeta(CpsVendorConfig config) {
        JsonNode response = executeRequest("/goods/get-super-category", new LinkedHashMap<>(), config);
        if (response == null || !isSuccessResponse(response)) {
            return CpsGoodsSelectionMeta.builder().metaSource(getVendorCode()).build();
        }
        return CpsGoodsSelectionMeta.builder()
                .categories(parseCategoryOptions(response.path("data")))
                .metaSource(getVendorCode())
                .build();
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
                .commissionRate(parseDecimal(item, "commissionRate"))
                .monthSales(item.path("monthSales").asLong(0))
                .shopName(item.path("shopName").asText(null))
                .shopType(item.path("shopType").asInt(0))
                .itemLink(item.path("itemLink").asText(null))
                .brandName(item.path("brandName").asText(null))
                .vendorCode(getVendorCode())
                .source("大淘客")
                .activityTag(firstText(item, "activityType", "activityTag", "marketingTag"))
                .categoryName(firstText(item, "cidName", "categoryName", "subcidName"))
                .couponEndTime(firstText(item, "couponEndTime", "couponEndTimeStr"))
                .rankTag(firstText(item, "ranking", "rankTag"))
                .sellingPoint(firstText(item, "desc", "marketingMainPic", "dtitle"))
                .build();
    }

    private CpsOrderDTO parseTaobaoOrder(JsonNode item, String nextPositionIndex) {
        int tkStatus = item.path("tk_status").asInt(-1);
        return CpsOrderDTO.builder()
                .platformCode(getPlatformCode())
                .platformOrderId(item.path("trade_id").asText(null))
                .parentOrderId(item.path("trade_parent_id").asText(null))
                .itemId(item.path("item_id").asText(null))
                .itemTitle(item.path("item_title").asText(null))
                .itemPic(item.path("item_img").asText(null))
                .itemPrice(parseDecimal(item, "item_price"))
                .finalPrice(parseDecimal(item, "pay_price"))
                .commissionRate(parseDecimal(item, "total_commission_rate"))
                .commissionAmount(parseDecimal(item, "pub_share_fee"))
                .quantity(item.path("item_num").asInt(1))
                .platformStatus(tkStatus)
                .orderTime(item.path("tk_create_time").asText(null))
                .payTime(item.path("tk_paid_time").asText(null))
                .adzoneId(item.path("adzone_id").asText(null))
                .refundTag(item.path("refund_tag").asInt(0))
                .nextPositionIndex(nextPositionIndex)
                .build();
    }

    private String convertSortType(Integer sortType) {
        return switch (sortType) {
            case 1 -> "2";  // 销量降序
            case 2 -> "6";  // 价格升序
            case 3 -> "5";  // 价格降序
            case 4 -> "4";  // 佣金率降序
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
