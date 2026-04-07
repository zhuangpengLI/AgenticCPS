package cn.iocoder.yudao.module.cps.client.taobao;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClient;
import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 淘宝联盟平台适配器（基于大淘客开放平台 API）
 *
 * <p>大淘客 API 文档：https://www.dataoke.com/pmc/</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class TaobaoPlatformClientAdapter implements CpsPlatformClient {

    private static final String BASE_URL = "https://openapi.dataoke.com/api";

    private final ObjectMapper objectMapper;

    @Resource
    private CpsPlatformService platformService;

    public TaobaoPlatformClientAdapter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsPlatformDO platform = getPlatformConfig();
        if (platform == null) {
            return buildEmptyResult(request);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyWords", request.getKeyword());
        params.put("pageId", String.valueOf(request.getPageNo()));
        params.put("pageSize", request.getPageSize());
        if (request.getSortType() != null) {
            // 大淘客排序：0-综合，2-销量，5-价格降，6-价格升，4-佣金率
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
        params.put("version", "v2.1.2");

        try {
            JsonNode response = doGet(platform, "/goods/get-dtk-search-goods", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[淘宝适配器] 搜索商品失败: {}", response);
                return buildEmptyResult(request);
            }

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
        } catch (Exception e) {
            log.error("[淘宝适配器] 搜索商品异常: keyword={}", request.getKeyword(), e);
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
        params.put("goodsId", request.getGoodsId());
        params.put("version", "v1.3.0");
        if (request.getAdzoneId() != null) {
            params.put("pid", request.getAdzoneId());
        }
        if (request.getChannelId() != null) {
            params.put("channelId", request.getChannelId());
        }
        if (request.getExternalId() != null) {
            params.put("externalId", request.getExternalId());
        }

        try {
            JsonNode response = doGet(platform, "/tb-service/get-privilege-link", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[淘宝适配器] 转链失败: goodsId={}, response={}", request.getGoodsId(), response);
                return null;
            }
            JsonNode data = response.path("data");
            return CpsPromotionLinkResult.builder()
                    .shortUrl(data.path("shortUrl").asText(null))
                    .longUrl(data.path("itemUrl").asText(null))
                    .tpwd(data.path("tpwd").asText(null))
                    .couponInfo(data.path("couponInfo").asText(null))
                    .commissionRate(parseDecimal(data, "maxCommissionRate"))
                    .actualPrice(parseDecimal(data, "actualPrice"))
                    .build();
        } catch (Exception e) {
            log.error("[淘宝适配器] 转链异常: goodsId={}", request.getGoodsId(), e);
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
        params.put("queryType", request.getQueryType());
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("pageSize", request.getPageSize());
        params.put("version", "1.0.0");
        if (request.getPositionIndex() != null) {
            params.put("positionIndex", request.getPositionIndex());
        }

        List<CpsOrderDTO> orders = new ArrayList<>();
        try {
            JsonNode response = doGet(platform, "/tb-service/get-order-details", params);
            if (response == null || !isSuccess(response)) {
                log.warn("[淘宝适配器] 查询订单失败: {}", response);
                return orders;
            }
            JsonNode data = response.path("data");
            JsonNode results = data.path("results");
            JsonNode items = results.path("publisher_order_dto");
            String nextPositionIndex = data.path("positionIndex").asText(null);

            if (items.isArray()) {
                for (JsonNode item : items) {
                    CpsOrderDTO order = parseTaobaoOrder(item, nextPositionIndex);
                    orders.add(order);
                }
            }
        } catch (Exception e) {
            log.error("[淘宝适配器] 查询订单异常", e);
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
            JsonNode response = doGet(platform, "/goods/get-super-category", new HashMap<>());
            return response != null && isSuccess(response);
        } catch (Exception e) {
            log.warn("[淘宝适配器] 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private CpsPlatformDO getPlatformConfig() {
        CpsPlatformDO platform = platformService.getPlatformByCode(getPlatformCode());
        if (platform == null) {
            log.warn("[淘宝适配器] 未找到平台配置: platformCode={}", getPlatformCode());
        }
        return platform;
    }

    /**
     * 执行大淘客 GET 请求（带签名）
     */
    private JsonNode doGet(CpsPlatformDO platform, String path, Map<String, Object> params) {
        String appKey = platform.getAppKey();
        String appSecret = platform.getAppSecret();

        // 构建签名
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.format("%06d", new Random().nextInt(1000000));
        String urlParamStr = String.format("appKey=%s&timer=%s&nonce=%s", appKey, timer, nonce);
        String sign = DigestUtil.md5Hex(urlParamStr + appSecret).toLowerCase();

        // 合并参数
        Map<String, Object> allParams = new LinkedHashMap<>(params);
        allParams.put("appKey", appKey);
        allParams.put("timer", timer);
        allParams.put("nonce", nonce);
        allParams.put("signRan", sign);

        // 构建 URL
        StringBuilder queryStr = new StringBuilder();
        allParams.forEach((k, v) -> {
            if (v != null) {
                if (queryStr.length() > 0) {
                    queryStr.append("&");
                }
                queryStr.append(k).append("=").append(v);
            }
        });

        String url = BASE_URL + path + "?" + queryStr;
        try {
            HttpResponse response = HttpRequest.get(url).timeout(5000).execute();
            String body = response.body();
            log.debug("[淘宝适配器] 请求: {} 响应: {}", path, body);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[淘宝适配器] HTTP请求异常: path={}", path, e);
            return null;
        }
    }

    private boolean isSuccess(JsonNode response) {
        return response != null && "0".equals(response.path("code").asText());
    }

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
                .build();
    }

    private CpsOrderDTO parseTaobaoOrder(JsonNode item, String nextPositionIndex) {
        // 淘宝订单状态映射：12-付款,13-关闭,14-确认收货,3-结算成功
        int tkStatus = item.path("tk_status").asInt(-1);
        return CpsOrderDTO.builder()
                .platformCode(getPlatformCode())
                .platformOrderId(item.path("trade_id").asText(null))
                .parentOrderId(item.path("trade_parent_id").asText(null))
                .itemId(item.path("item_id").asText(null))
                .itemTitle(item.path("item_title").asText(null))
                .itemPic(item.path("item_img").asText(null))
                .itemPrice(parseDecimalFromStr(item, "item_price"))
                .finalPrice(parseDecimalFromStr(item, "pay_price"))
                .commissionRate(parseDecimalFromStr(item, "total_commission_rate"))
                .commissionAmount(parseDecimalFromStr(item, "pub_share_fee"))
                .quantity(item.path("item_num").asInt(1))
                .platformStatus(tkStatus)
                .orderTime(item.path("tk_create_time").asText(null))
                .payTime(item.path("tk_paid_time").asText(null))
                .adzoneId(item.path("adzone_id").asText(null))
                .refundTag(item.path("refund_tag").asInt(0))
                .nextPositionIndex(nextPositionIndex)
                .build();
    }

    private BigDecimal parseDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isNull() || field.isMissingNode()) {
            return null;
        }
        try {
            return new BigDecimal(field.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseDecimalFromStr(JsonNode node, String fieldName) {
        return parseDecimal(node, fieldName);
    }

    private String convertSortType(Integer sortType) {
        return switch (sortType) {
            case 1 -> "2"; // 销量降序
            case 2 -> "6"; // 价格升序
            case 3 -> "5"; // 价格降序
            case 4 -> "4"; // 佣金率降序
            default -> "0"; // 综合排序
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
