package com.qiji.cps.module.cps.client.didi;

import cn.didi.union.client.UnionClient;
import cn.didi.union.enums.OrderType;
import cn.didi.union.models.*;
import com.google.gson.Gson;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DidiOfficialVendorClient implements CpsApiVendorClient {

    private static final Gson GSON = new Gson();
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DidiUnionClientFactory clientFactory;

    @Override public String getVendorCode() { return "official"; }
    @Override public String getPlatformCode() { return CpsPlatformCodeEnum.DIDI.getCode(); }
    @Override public String getVendorType() { return "official"; }

    @Override
    public Set<CpsVendorCapability> getCapabilities() {
        return EnumSet.of(
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST);
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        throw CpsVendorException.capabilityUnsupported(getVendorCode(), getPlatformCode(),
                CpsVendorCapability.GOODS_SEARCH);
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        long activityId = positiveLong(request.getGoodsId(), "activityId");
        long promotionId = positiveLong(firstText(request.getAdzoneId(), config.getDefaultAdzoneId()), "promotionId");
        String sourceId = StringUtils.hasText(request.getExternalId()) ? request.getExternalId()
                : "anonymous-" + UUID.randomUUID();
        UnionClient client = clientFactory.create(config);
        Result<LinkResponse> result = client.generateH5Link(activityId, promotionId, sourceId,
                clientFactory.resolveTimeout(config));
        LinkResponse response = requireSuccess(result, "generateH5Link");
        if (response.getData() == null || !StringUtils.hasText(response.getData().getLink())) {
            throw new IllegalStateException("DUnion generateH5Link returned no link");
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("dsi", response.getData().getDsi());
        extra.put("appId", response.getData().getAppId());
        extra.put("appSource", response.getData().getAppSource());
        extra.put("traceId", response.getTraceid());
        return CpsPromotionLinkResult.builder().longUrl(response.getData().getLink())
                .extraFields(extra).rawPayload(GSON.toJson(response)).build();
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        return queryOrderPage(request, config).getItems();
    }

    @Override
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request, CpsVendorConfig config) {
        long start = toEpochSecond(request.getStartTime(), Instant.now().minus(Duration.ofDays(1)));
        long end = toEpochSecond(request.getEndTime(), Instant.now());
        int page = parsePage(request);
        int size = clamp(request.getPageSize(), 1, 100, 50);
        Result<OrderResponse> result = clientFactory.create(config).queryOrderList(start, end, OrderType.All,
                page, size, clientFactory.resolveTimeout(config));
        OrderResponse response = requireSuccess(result, "queryOrderList");
        List<OrderDetail> sourceOrders = response.getData() == null || response.getData().getOrderList() == null
                ? List.of() : response.getData().getOrderList();
        long total = response.getData() == null ? 0L : response.getData().getTotal();
        boolean hasMore = (long) page * size < total;
        String nextPage = hasMore ? String.valueOf(page + 1) : null;
        List<CpsOrderDTO> orders = sourceOrders.stream().map(order -> mapOrder(order, nextPage)).toList();
        return CpsOrderPageResult.page(orders, hasMore ? page + 1 : null, hasMore);
    }

    @Override
    public boolean testConnection(CpsVendorConfig config) {
        try {
            long end = Instant.now().getEpochSecond();
            return clientFactory.create(config).queryOrderList(end - 3600, end, OrderType.All, 1, 1,
                    clientFactory.resolveTimeout(config)).isSuccess();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public UnionClient getClient(CpsVendorConfig config) { return clientFactory.create(config); }
    public int getTimeout(CpsVendorConfig config) { return clientFactory.resolveTimeout(config); }

    private CpsOrderDTO mapOrder(OrderDetail order, String nextPage) {
        boolean refunded = order.getOrderStatus() == 8 || order.getRefundPrice() > 0;
        int status = refunded || order.getIsRisk() != 0 || order.getStatus() == 6 ? -1
                : order.getStatus() == 7 ? 3 : 1;
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("cpaProfitCent", order.getCpaProfit());
        extra.put("cpsProfitCent", order.getCpsProfit());
        extra.put("cpaType", order.getCpaType());
        extra.put("isRisk", order.getIsRisk());
        extra.put("openUid", order.getOpenUid());
        extra.put("failReason", order.getFailReason());
        extra.put("refundPriceCent", order.getRefundPrice());
        return CpsOrderDTO.builder()
                .platformOrderId(order.getOrderId()).platformCode(getPlatformCode())
                .itemId(order.getProductId()).itemTitle(order.getTitle())
                .finalPrice(cents(order.getPayPrice()))
                .commissionAmount(cents(order.getCpaProfit() + order.getCpsProfit()))
                .quantity(1).platformStatus(status)
                .orderTime(formatEpoch(order.getPayTime())).payTime(formatEpoch(order.getPayTime()))
                .adzoneId(String.valueOf(order.getPromotionId())).externalId(order.getSourceId())
                .refundTag(refunded ? 1 : 0).nextPositionIndex(nextPage)
                .extraFields(extra).rawPayload(GSON.toJson(order)).build();
    }

    private <T extends BaseModel> T requireSuccess(Result<T> result, String operation) {
        if (result == null || !result.isSuccess() || result.getModel() == null) {
            String message = result != null && result.getError() != null ? result.getError().getMessage() : "empty result";
            throw new IllegalStateException("DUnion " + operation + " failed: " + message);
        }
        return result.getModel();
    }
    private long positiveLong(String value, String name) {
        try { long parsed = Long.parseLong(value); if (parsed > 0) return parsed; } catch (Exception ignored) { }
        throw new IllegalArgumentException(name + " must be a positive integer");
    }
    private String firstText(String first, String second) { return StringUtils.hasText(first) ? first : second; }
    private BigDecimal cents(long value) { return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY); }
    private String formatEpoch(long value) { return value <= 0 ? null : Instant.ofEpochSecond(value).atZone(ZONE_ID).format(DATE_TIME); }
    private long toEpochSecond(String value, Instant fallback) {
        return StringUtils.hasText(value) ? LocalDateTime.parse(value, DATE_TIME).atZone(ZONE_ID).toEpochSecond() : fallback.getEpochSecond();
    }
    private int parsePage(CpsOrderQueryRequest request) {
        if (StringUtils.hasText(request.getPositionIndex())) {
            try { return clamp(Integer.parseInt(request.getPositionIndex()), 1, 100, 1); } catch (NumberFormatException ignored) { }
        }
        return clamp(request.getPageNo(), 1, 100, 1);
    }
    private int clamp(Integer value, int min, int max, int fallback) { return value == null ? fallback : Math.max(min, Math.min(max, value)); }
}
