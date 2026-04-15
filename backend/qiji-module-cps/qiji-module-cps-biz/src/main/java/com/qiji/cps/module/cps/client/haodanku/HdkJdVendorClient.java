package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
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
        // 好单库京东搜索接口：/jd_goods_search (GET, v2)，返回 code=200
        // 已验证：http://v2.api.haodanku.com/jd_goods_search 可正常返回京东商品数据
        return "/jd_goods_search";
    }

    /**
     * 好单库京东搜索接口返回 code=200（而非通用的 code=1）
     */
    @Override
    protected boolean isSuccessResponse(com.fasterxml.jackson.databind.JsonNode root) {
        return root != null && root.path("code").asInt(-1) == 200;
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pagesize", request.getPageSize());
        // /jd_goods_search 支持价格筛选
        if (request.getPriceLowerLimit() != null) {
            params.put("price_from", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("price_to", request.getPriceUpperLimit());
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
        // TODO 待验证：好单库京东转链接口路径
        return "/jd/ratesurl";
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
        return "/jd_goods_search";
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
