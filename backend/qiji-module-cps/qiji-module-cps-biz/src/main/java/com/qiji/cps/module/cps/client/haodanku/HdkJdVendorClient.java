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
                orders.add(CpsOrderDTO.builder()
                        .platformCode(getPlatformCode())
                        .platformOrderId(item.path("order_id").asText(null))
                        .itemId(item.path("sku_id").asText(null))
                        .itemTitle(item.path("goods_name").asText(null))
                        .finalPrice(parseDecimal(item, "pay_price"))
                        .commissionRate(parseDecimal(item, "commission_rate"))
                        .commissionAmount(parseDecimal(item, "commission"))
                        .platformStatus(item.path("order_status").asInt(-1))
                        .orderTime(item.path("order_time").asText(null))
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
