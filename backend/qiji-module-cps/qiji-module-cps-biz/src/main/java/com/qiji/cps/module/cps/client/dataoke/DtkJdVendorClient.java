package com.qiji.cps.module.cps.client.dataoke;

import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 大淘客-京东供应商客户端
 *
 * <p>通过大淘客开放平台聚合 API 对接京东联盟，迁移自 JdPlatformClientAdapter 的业务逻辑。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class DtkJdVendorClient extends AbstractDtkVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.JD.getCode();
    }

    // ==================== 商品搜索 ====================

    @Override
    protected String getSearchApiPath() {
        return "/dels/jd/goods/search-goods";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("pageNo", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("version", "v1.0.0");
        if (request.getPriceLowerLimit() != null) {
            params.put("priceFrom", request.getPriceLowerLimit());
        }
        if (request.getPriceUpperLimit() != null) {
            params.put("priceTo", request.getPriceUpperLimit());
        }
        if (request.getHasCoupon() != null) {
            params.put("isCoupon", request.getHasCoupon());
        }
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        JsonNode list = data.path("list");
        List<CpsGoodsItem> goodsList = new ArrayList<>();
        if (list.isArray()) {
            for (JsonNode item : list) {
                goodsList.add(parseJdGoodsItem(item));
            }
        }
        long totalNum = data.path("totalNum").asLong(-1);
        return CpsGoodsSearchResult.builder()
                .list(goodsList)
                .total(totalNum)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    // ==================== 推广转链 ====================

    @Override
    protected String getPromotionLinkApiPath() {
        return "/dels/jd/kit/promotion-union-convert";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        String materialId = request.getItemLink() != null ? request.getItemLink()
                : "https://item.jd.com/" + request.getGoodsId() + ".html";
        params.put("materialId", materialId);
        params.put("unionId", config.getAuthToken()); // 京东联盟 unionId
        params.put("chainType", 2); // 短链
        params.put("version", "1.0.0");
        if (request.getChannelId() != null) {
            params.put("subUnionId", request.getChannelId());
        }
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("shortURL").asText(null))
                .longUrl(data.path("clickURL").asText(null))
                .build();
    }

    // ==================== 订单查询 ====================

    @Override
    protected String getOrderQueryApiPath() {
        return "/dels/jd/order/get-official-order-list";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("pageNo", request.getPageNo());
        params.put("key", config.getAuthToken()); // 京东推客授权key
        params.put("version", "1.0.0");
        if (request.getPositionIndex() != null) {
            params.put("type", request.getPositionIndex());
        }
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                orders.add(parseJdOrder(item));
            }
        }
        return orders;
    }

    // ==================== 连接测试 ====================

    @Override
    protected String getTestConnectionApiPath() {
        return "/dels/jd/goods/search-goods";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("version", "v1.0.0");
        params.put("cid1", 1L);
        params.put("pageNo", 1);
        params.put("pageSize", 1);
        params.put("keyword", "手机");
        return params;
    }

    // ==================== 私有方法 ====================

    private CpsGoodsItem parseJdGoodsItem(JsonNode item) {
        BigDecimal price = parseDecimal(item, "price");
        BigDecimal lowestPrice = parseDecimal(item, "lowestCouponPrice");
        return CpsGoodsItem.builder()
                .goodsId(item.path("skuId").asText(null))
                .platformCode(getPlatformCode())
                .title(item.path("skuName").asText(null))
                .mainPic(item.path("whiteImage").asText(null))
                .originalPrice(price)
                .actualPrice(lowestPrice != null ? lowestPrice : price)
                .commissionRate(parseDecimal(item, "commissionShare"))
                .monthSales(item.path("inOrderCount30Days").asLong(0))
                .shopName(item.path("shopName").asText(null))
                .shopType("g".equals(item.path("owner").asText()) ? 1 : 0)
                .itemLink("https://item.jd.com/" + item.path("skuId").asText() + ".html")
                .brandName(item.path("brandName").asText(null))
                .build();
    }

    private CpsOrderDTO parseJdOrder(JsonNode item) {
        return CpsOrderDTO.builder()
                .platformCode(getPlatformCode())
                .platformOrderId(item.path("orderId").asText(null))
                .itemId(item.path("skuId").asText(null))
                .itemTitle(item.path("skuName").asText(null))
                .finalPrice(parseDecimal(item, "actualPrice"))
                .commissionRate(parseDecimal(item, "commissionShare"))
                .commissionAmount(parseDecimal(item, "commission"))
                .platformStatus(item.path("validCode").asInt(-1))
                .orderTime(item.path("orderTime").asText(null))
                .payTime(item.path("payMonth").asText(null))
                .adzoneId(item.path("positionId").asText(null))
                .build();
    }

}
