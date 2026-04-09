package cn.iocoder.yudao.module.cps.client.haodanku;

import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
        return "/pdd/item_search";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pagesize", request.getPageSize());
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
        return "/pdd/get_item_link";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goods_sign", request.getGoodsSign() != null ? request.getGoodsSign() : request.getGoodsId());
        params.put("pid", request.getAdzoneId() != null ? request.getAdzoneId() : config.getDefaultAdzoneId());
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("short_url").asText(null))
                .longUrl(data.path("mobile_url").asText(null))
                .mobileUrl(data.path("mobile_url").asText(null))
                .build();
    }

    @Override
    protected String getOrderQueryApiPath() {
        return "/pdd/order_list";
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
                        .platformOrderId(item.path("order_sn").asText(null))
                        .itemId(item.path("goods_sign").asText(null))
                        .itemTitle(item.path("goods_name").asText(null))
                        .finalPrice(parseDecimal(item, "order_amount"))
                        .commissionRate(parseDecimal(item, "promotion_rate"))
                        .commissionAmount(parseDecimal(item, "promotion_amount"))
                        .platformStatus(item.path("order_status").asInt(-1))
                        .orderTime(item.path("order_create_time").asText(null))
                        .build());
            }
        }
        return orders;
    }

    @Override
    protected String getTestConnectionApiPath() {
        return "/pdd/item_search";
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
