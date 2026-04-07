package cn.iocoder.yudao.module.cps.client.pdd;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClient;
import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 拼多多联盟平台适配器（基于大淘客开放平台聚合 API）
 *
 * @author CPS System
 */
@Slf4j
@Component
public class PddPlatformClientAdapter implements CpsPlatformClient {

    private static final String BASE_URL = "https://openapi.dataoke.com/api";

    private final ObjectMapper objectMapper;

    @Resource
    private CpsPlatformService platformService;

    public PddPlatformClientAdapter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.PDD.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsPlatformDO platform = getPlatformConfig();
        if (platform == null) {
            return buildEmptyResult(request);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("version", "v1.0.0");
        if (request.getHasCoupon() != null) {
            params.put("withCoupon", request.getHasCoupon());
        }
        // 拼多多排序：0-综合，2-佣金率，3-价格升，4-价格降，6-销量降
        if (request.getSortType() != null) {
            params.put("sortType", convertSortType(request.getSortType()));
        }
        // 使用平台默认推广位
        if (platform.getDefaultAdzoneId() != null) {
            params.put("pid", platform.getDefaultAdzoneId());
        }

        try {
            JsonNode response = doGet(platform, "/dels/pdd/goods/search", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[拼多多适配器] 搜索商品失败: {}", response);
                return buildEmptyResult(request);
            }
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
        } catch (Exception e) {
            log.error("[拼多多适配器] 搜索商品异常: keyword={}", request.getKeyword(), e);
            return buildEmptyResult(request);
        }
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        CpsPlatformDO platform = getPlatformConfig();
        if (platform == null) {
            return null;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goodsSign", request.getGoodsSign() != null ? request.getGoodsSign() : request.getGoodsId());
        params.put("pid", request.getAdzoneId() != null ? request.getAdzoneId() : platform.getDefaultAdzoneId());
        params.put("version", "1.0.0");
        if (request.getExternalId() != null) {
            params.put("customParameters", request.getExternalId());
        }

        try {
            JsonNode response = doGet(platform, "/dels/pdd/kit/goods-prom-generate", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[拼多多适配器] 转链失败: goodsId={}, response={}", request.getGoodsId(), response);
                return null;
            }
            JsonNode data = response.path("data");
            return CpsPromotionLinkResult.builder()
                    .shortUrl(data.path("mobileShortUrl").asText(null))
                    .longUrl(data.path("mobileUrl").asText(null))
                    .mobileUrl(data.path("mobileUrl").asText(null))
                    .build();
        } catch (Exception e) {
            log.error("[拼多多适配器] 转链异常: goodsId={}", request.getGoodsId(), e);
            return null;
        }
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) {
        CpsPlatformDO platform = getPlatformConfig();
        if (platform == null) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        params.put("version", "v1.0.0");
        if (platform.getDefaultAdzoneId() != null) {
            params.put("pid", platform.getDefaultAdzoneId());
        }

        List<CpsOrderDTO> orders = new ArrayList<>();
        try {
            JsonNode response = doGet(platform, "/dels/pdd/order/get-order-list", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[拼多多适配器] 查询订单失败: {}", response);
                return orders;
            }
            JsonNode data = response.path("data");
            JsonNode list = data.path("orderList");
            if (list.isArray()) {
                for (JsonNode item : list) {
                    orders.add(parsePddOrder(item));
                }
            }
        } catch (Exception e) {
            log.error("[拼多多适配器] 查询订单异常", e);
        }
        return orders;
    }

    @Override
    public boolean testConnection() {
        CpsPlatformDO platform = getPlatformConfig();
        if (platform == null) {
            return false;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("version", "v1.0.0");
            params.put("keyword", "手机");
            params.put("page", 1);
            params.put("pageSize", 1);
            JsonNode response = doGet(platform, "/dels/pdd/goods/search", params);
            return response != null && isSuccess(response);
        } catch (Exception e) {
            log.warn("[拼多多适配器] 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private CpsPlatformDO getPlatformConfig() {
        CpsPlatformDO platform = platformService.getPlatformByCode(getPlatformCode());
        if (platform == null) {
            log.warn("[拼多多适配器] 未找到平台配置: platformCode={}", getPlatformCode());
        }
        return platform;
    }

    private JsonNode doGet(CpsPlatformDO platform, String path, Map<String, Object> params) {
        String appKey = platform.getAppKey();
        String appSecret = platform.getAppSecret();
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.format("%06d", new Random().nextInt(1000000));
        String urlParamStr = String.format("appKey=%s&timer=%s&nonce=%s", appKey, timer, nonce);
        String sign = DigestUtil.md5Hex(urlParamStr + appSecret).toLowerCase();

        Map<String, Object> allParams = new LinkedHashMap<>(params);
        allParams.put("appKey", appKey);
        allParams.put("timer", timer);
        allParams.put("nonce", nonce);
        allParams.put("signRan", sign);

        StringBuilder queryStr = new StringBuilder();
        allParams.forEach((k, v) -> {
            if (v != null) {
                if (queryStr.length() > 0) queryStr.append("&");
                queryStr.append(k).append("=").append(v);
            }
        });

        String url = BASE_URL + path + "?" + queryStr;
        try {
            HttpResponse response = HttpRequest.get(url).timeout(5000).execute();
            String body = response.body();
            log.debug("[拼多多适配器] 请求: {} 响应: {}", path, body);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[拼多多适配器] HTTP请求异常: path={}", path, e);
            return null;
        }
    }

    private boolean isSuccess(JsonNode response) {
        return response != null && "0".equals(response.path("code").asText());
    }

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

    private BigDecimal parseSafeDecimal(String str) {
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isNull() || field.isMissingNode()) return null;
        try {
            return new BigDecimal(field.asText());
        } catch (Exception e) {
            return null;
        }
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

    private CpsGoodsSearchResult buildEmptyResult(CpsGoodsSearchRequest request) {
        return CpsGoodsSearchResult.builder()
                .list(Collections.emptyList())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

}
