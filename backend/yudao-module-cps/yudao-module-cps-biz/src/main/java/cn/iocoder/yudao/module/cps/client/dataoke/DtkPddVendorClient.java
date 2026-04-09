package cn.iocoder.yudao.module.cps.client.dataoke;

import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 大淘客-拼多多供应商客户端
 *
 * <p>通过大淘客开放平台聚合 API 对接拼多多联盟，迁移自 PddPlatformClientAdapter 的业务逻辑。</p>
 *
 * <p>拼多多特殊处理：价格单位为分（需除以100转为元），佣金率需除以10。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class DtkPddVendorClient extends AbstractDtkVendorClient {

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.PDD.getCode();
    }

    // ==================== 商品搜索 ====================

    @Override
    protected String getSearchApiPath() {
        return "/dels/pdd/goods/search";
    }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("version", "v1.0.0");
        if (request.getHasCoupon() != null) {
            params.put("withCoupon", request.getHasCoupon());
        }
        if (request.getSortType() != null) {
            params.put("sortType", convertSortType(request.getSortType()));
        }
        // 使用配置中的默认推广位
        if (config.getDefaultAdzoneId() != null) {
            params.put("pid", config.getDefaultAdzoneId());
        }
        return params;
    }

    @Override
    protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        JsonNode data = response.path("data");
        JsonNode goodsList = data.path("goodsList");
        List<CpsGoodsItem> list = new ArrayList<>();
        if (goodsList.isArray()) {
            for (JsonNode item : goodsList) {
                list.add(parsePddGoodsItem(item));
            }
        }
        long total = data.path("totalCount").asLong(-1);
        return CpsGoodsSearchResult.builder()
                .list(list)
                .total(total)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .nextPageId(data.path("listId").asText(null))
                .build();
    }

    // ==================== 推广转链 ====================

    @Override
    protected String getPromotionLinkApiPath() {
        return "/dels/pdd/kit/goods-prom-generate";
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goodsSign", request.getGoodsSign() != null ? request.getGoodsSign() : request.getGoodsId());
        params.put("pid", request.getAdzoneId() != null ? request.getAdzoneId() : config.getDefaultAdzoneId());
        params.put("version", "1.0.0");
        if (request.getExternalId() != null) {
            params.put("customParameters", request.getExternalId());
        }
        return params;
    }

    @Override
    protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        JsonNode data = response.path("data");
        return CpsPromotionLinkResult.builder()
                .shortUrl(data.path("mobileShortUrl").asText(null))
                .longUrl(data.path("mobileUrl").asText(null))
                .mobileUrl(data.path("mobileUrl").asText(null))
                .build();
    }

    // ==================== 订单查询 ====================

    @Override
    protected String getOrderQueryApiPath() {
        return "/dels/pdd/order/get-order-list";
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("version", "v1.0.0");
        if (config.getDefaultAdzoneId() != null) {
            params.put("pid", config.getDefaultAdzoneId());
        }
        return params;
    }

    @Override
    protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        List<CpsOrderDTO> orders = new ArrayList<>();
        JsonNode data = response.path("data");
        JsonNode list = data.path("orderList");
        if (list.isArray()) {
            for (JsonNode item : list) {
                orders.add(parsePddOrder(item));
            }
        }
        return orders;
    }

    // ==================== 连接测试 ====================

    @Override
    protected String getTestConnectionApiPath() {
        return "/dels/pdd/goods/search";
    }

    @Override
    protected Map<String, Object> buildTestConnectionParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("version", "v1.0.0");
        params.put("keyword", "手机");
        params.put("page", 1);
        params.put("pageSize", 1);
        return params;
    }

    // ==================== 私有方法 ====================

    private CpsGoodsItem parsePddGoodsItem(JsonNode item) {
        String minNormalPriceStr = item.path("minNormalPrice").asText("0");
        String minGroupPriceStr = item.path("minGroupPrice").asText("0");
        BigDecimal normalPrice = parseSafeDecimal(minNormalPriceStr);
        BigDecimal groupPrice = parseSafeDecimal(minGroupPriceStr);
        // PDD价格单位为分
        BigDecimal normalPriceYuan = normalPrice != null ? normalPrice.divide(BigDecimal.valueOf(100)) : null;
        BigDecimal groupPriceYuan = groupPrice != null ? groupPrice.divide(BigDecimal.valueOf(100)) : null;

        String couponDiscountStr = item.path("couponDiscount").asText("0");
        BigDecimal couponAmount = parseSafeDecimal(couponDiscountStr);
        BigDecimal couponAmountYuan = couponAmount != null ? couponAmount.divide(BigDecimal.valueOf(100)) : null;

        BigDecimal commissionRate = parseSafeDecimal(item.path("promotionRate").asText("0"));
        BigDecimal commissionRatePct = commissionRate != null ? commissionRate.divide(BigDecimal.valueOf(10)) : null;

        return CpsGoodsItem.builder()
                .goodsId(item.path("goodsSign").asText(null))
                .goodsSign(item.path("goodsSign").asText(null))
                .platformCode(getPlatformCode())
                .title(item.path("goodsName").asText(null))
                .mainPic(item.path("goodsImageUrl").asText(null))
                .originalPrice(normalPriceYuan)
                .actualPrice(groupPriceYuan)
                .couponPrice(couponAmountYuan)
                .commissionRate(commissionRatePct)
                .shopName(item.path("mallName").asText(null))
                .brandName(item.path("brandName").asText(null))
                .build();
    }

    private CpsOrderDTO parsePddOrder(JsonNode item) {
        return CpsOrderDTO.builder()
                .platformCode(getPlatformCode())
                .platformOrderId(item.path("orderSn").asText(null))
                .itemId(item.path("goodsSign").asText(null))
                .itemTitle(item.path("goodsName").asText(null))
                .finalPrice(parseDecimal(item, "orderAmount"))
                .commissionRate(parseDecimal(item, "promotionRate"))
                .commissionAmount(parseDecimal(item, "promotionAmount"))
                .platformStatus(item.path("orderStatus").asInt(-1))
                .orderTime(item.path("orderCreateTime").asText(null))
                .build();
    }

    private Integer convertSortType(Integer sortType) {
        return switch (sortType) {
            case 1 -> 6;  // 销量降序
            case 2 -> 3;  // 价格升序
            case 3 -> 4;  // 价格降序
            case 4 -> 2;  // 佣金率降序
            default -> 0; // 综合排序
        };
    }

}
