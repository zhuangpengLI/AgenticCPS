package cn.iocoder.yudao.module.cps.client.haodanku;

import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
        return "/jd/item_search";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pagesize", request.getPageSize());
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
                BigDecimal price = parseDecimal(item, "price");
                BigDecimal finalPrice = parseDecimal(item, "final_price");
                goodsList.add(CpsGoodsItem.builder()
                        .goodsId(item.path("sku_id").asText(null))
                        .platformCode(getPlatformCode())
                        .title(item.path("goods_name").asText(null))
                        .mainPic(item.path("pic_url").asText(null))
                        .originalPrice(price)
                        .actualPrice(finalPrice != null ? finalPrice : price)
                        .commissionRate(parseDecimal(item, "commission_rate"))
                        .shopName(item.path("shop_name").asText(null))
                        .itemLink(item.path("item_link").asText(null))
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
        return "/jd/get_item_link";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sku_id", request.getGoodsId());
        if (config.getAuthToken() != null) {
            params.put("union_id", config.getAuthToken());
        }
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("short_url").asText(null))
                .longUrl(data.path("click_url").asText(null))
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/jd/order_list";
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
        return "/jd/item_search";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "手机");
        params.put("page", 1);
        params.put("pagesize", 1);
        return params;
    }

}
