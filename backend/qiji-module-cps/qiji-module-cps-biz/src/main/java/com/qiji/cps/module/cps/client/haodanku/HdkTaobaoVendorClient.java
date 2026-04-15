package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
public class HdkTaobaoVendorClient extends AbstractHdkVendorClient {

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
        return "/order_list";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start_time", request.getStartTime());
        params.put("end_time", request.getEndTime());
        params.put("page", request.getPageNo());
        params.put("pagesize", request.getPageSize());
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
                        .platformOrderId(item.path("trade_id").asText(null))
                        .itemId(item.path("item_id").asText(null))
                        .itemTitle(item.path("item_title").asText(null))
                        .finalPrice(parseDecimal(item, "pay_price"))
                        .commissionRate(parseDecimal(item, "commission_rate"))
                        .commissionAmount(parseDecimal(item, "commission"))
                        .platformStatus(item.path("order_status").asInt(-1))
                        .orderTime(item.path("create_time").asText(null))
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

}
