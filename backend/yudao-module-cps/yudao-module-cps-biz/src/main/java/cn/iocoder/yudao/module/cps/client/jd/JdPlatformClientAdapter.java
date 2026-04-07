package cn.iocoder.yudao.module.cps.client.jd;

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
 * 京东联盟平台适配器（基于大淘客开放平台聚合 API）
 *
 * @author CPS System
 */
@Slf4j
@Component
public class JdPlatformClientAdapter implements CpsPlatformClient {

    private static final String BASE_URL = "https://openapi.dataoke.com/api";

    private final ObjectMapper objectMapper;

    @Resource
    private CpsPlatformService platformService;

    public JdPlatformClientAdapter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.JD.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsPlatformDO platform = getPlatformConfig();
        if (platform == null) {
            return buildEmptyResult(request);
        }

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

        try {
            JsonNode response = doGet(platform, "/dels/jd/goods/search-goods", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[京东适配器] 搜索商品失败: {}", response);
                return buildEmptyResult(request);
            }
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
        } catch (Exception e) {
            log.error("[京东适配器] 搜索商品异常: keyword={}", request.getKeyword(), e);
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
        // 京东转链使用 materialId（商品详情页链接）或 skuId 拼成链接
        String materialId = request.getItemLink() != null ? request.getItemLink()
                : "https://item.jd.com/" + request.getGoodsId() + ".html";
        params.put("materialId", materialId);
        params.put("unionId", platform.getAuthToken()); // 京东联盟 unionId 存储在 authToken 字段
        params.put("chainType", 2); // 短链
        params.put("version", "1.0.0");
        if (request.getChannelId() != null) {
            params.put("subUnionId", request.getChannelId());
        }

        try {
            JsonNode response = doGet(platform, "/dels/jd/kit/promotion-union-convert", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[京东适配器] 转链失败: goodsId={}, response={}", request.getGoodsId(), response);
                return null;
            }
            JsonNode data = response.path("data");
            return CpsPromotionLinkResult.builder()
                    .shortUrl(data.path("shortURL").asText(null))
                    .longUrl(data.path("clickURL").asText(null))
                    .build();
        } catch (Exception e) {
            log.error("[京东适配器] 转链异常: goodsId={}", request.getGoodsId(), e);
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
        params.put("pageNo", request.getPageNo());
        params.put("key", platform.getAuthToken()); // 京东推客授权key
        params.put("version", "1.0.0");
        if (request.getPositionIndex() != null) {
            params.put("type", request.getPositionIndex());
        }

        List<CpsOrderDTO> orders = new ArrayList<>();
        try {
            JsonNode response = doGet(platform, "/dels/jd/order/get-official-order-list", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[京东适配器] 查询订单失败: {}", response);
                return orders;
            }
            JsonNode data = response.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    orders.add(parseJdOrder(item));
                }
            }
        } catch (Exception e) {
            log.error("[京东适配器] 查询订单异常", e);
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
            params.put("cid1", 1L);
            params.put("pageNo", 1);
            params.put("pageSize", 1);
            params.put("keyword", "手机");
            JsonNode response = doGet(platform, "/dels/jd/goods/search-goods", params);
            return response != null && isSuccess(response);
        } catch (Exception e) {
            log.warn("[京东适配器] 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private CpsPlatformDO getPlatformConfig() {
        CpsPlatformDO platform = platformService.getPlatformByCode(getPlatformCode());
        if (platform == null) {
            log.warn("[京东适配器] 未找到平台配置: platformCode={}", getPlatformCode());
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
            log.debug("[京东适配器] 请求: {} 响应: {}", path, body);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[京东适配器] HTTP请求异常: path={}", path, e);
            return null;
        }
    }

    private boolean isSuccess(JsonNode response) {
        return response != null && "0".equals(response.path("code").asText());
    }

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

    private BigDecimal parseDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isNull() || field.isMissingNode()) return null;
        try {
            return new BigDecimal(field.asText());
        } catch (Exception e) {
            return null;
        }
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
