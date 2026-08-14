package com.qiji.cps.module.cps.service.didi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.tenant.core.util.TenantUtils;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiOrderCallbackReqVO;
import com.qiji.cps.module.cps.controller.openapi.didi.vo.DidiRewardCallbackReqVO;
import com.qiji.cps.module.cps.dal.dataobject.didi.CpsDidiCallbackEventDO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.didi.CpsDidiCallbackEventMapper;
import com.qiji.cps.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DidiCallbackServiceImpl implements DidiCallbackService {
    private static final String PLATFORM = "didi";
    private static final String VENDOR = "official";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CpsApiVendorMapper vendorMapper;
    private final CpsDidiCallbackEventMapper eventMapper;
    private final CpsOrderService orderService;
    private final DidiCallbackSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleOrder(String appKey, String timestamp, String sign, String rawBody,
                               DidiOrderCallbackReqVO request) {
        CpsApiVendorDO vendor = resolveVendor(appKey);
        if (vendor == null || !signatureVerifier.verify(appKey, timestamp, sign, vendor.getAppSecret())) return false;
        return TenantUtils.execute(vendor.getTenantId(), () -> processOrder(appKey, rawBody, request));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleReward(String appKey, String timestamp, String sign, String rawBody,
                                DidiRewardCallbackReqVO request) {
        CpsApiVendorDO vendor = resolveVendor(appKey);
        if (vendor == null || !signatureVerifier.verify(appKey, timestamp, sign, vendor.getAppSecret())) return false;
        return TenantUtils.execute(vendor.getTenantId(), () -> processReward(appKey, rawBody, request));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processOrder(String appKey, String rawBody, DidiOrderCallbackReqVO request) {
        if (request == null || blank(request.getOrderId())) return false;
        String key = "ORDER:" + request.getOrderId() + ":" + value(request.getOrderStatus()) + ":"
                + value(request.getStatus()) + ":" + value(request.getRefundTime());
        CpsDidiCallbackEventDO existing = eventMapper.selectByIdempotencyKey(key);
        if (existing != null && "SUCCESS".equals(existing.getProcessStatus())) return true;
        if (existing == null && !insertEvent(buildOrderEvent(appKey, rawBody, request, key))) {
            existing = eventMapper.selectByIdempotencyKey(key);
            if (existing != null && "SUCCESS".equals(existing.getProcessStatus())) return true;
        }
        try {
            orderService.saveOrUpdateOrder(toOrder(request, rawBody));
            markSuccess(key);
            return true;
        } catch (RuntimeException ex) {
            markFailure(key, ex.getMessage());
            log.warn("Didi order callback processing failed: orderId={}", request.getOrderId(), ex);
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean processReward(String appKey, String rawBody, DidiRewardCallbackReqVO request) {
        if (request == null || blank(request.getTraceId())) return false;
        String key = "REWARD:" + request.getTraceId();
        if (!insertEvent(CpsDidiCallbackEventDO.builder()
                .eventType("REWARD").idempotencyKey(key).appKey(appKey).traceId(request.getTraceId())
                .activityId(request.getActivityId()).rewardSent(request.getRewardSent())
                .retryTimes(request.getRetryTimes()).sourceId(extractSourceId(request.getCallbackInfo()))
                .processStatus("SUCCESS").requestBody(rawBody).build())) return true;
        return true;
    }

    private CpsApiVendorDO resolveVendor(String appKey) {
        if (blank(appKey)) return null;
        List<CpsApiVendorDO> vendors = TenantUtils.executeIgnore(() -> vendorMapper.selectEnabledDidiByAppKey(appKey));
        if (vendors.size() != 1) {
            log.warn("Didi callback appKey resolved to {} active vendor records", vendors.size());
            return null;
        }
        return vendors.get(0);
    }

    private boolean insertEvent(CpsDidiCallbackEventDO event) {
        try { eventMapper.insert(event); return true; }
        catch (DuplicateKeyException ex) { return false; }
    }

    private CpsDidiCallbackEventDO buildOrderEvent(String appKey, String rawBody,
                                                    DidiOrderCallbackReqVO request, String key) {
        return CpsDidiCallbackEventDO.builder().eventType("ORDER").idempotencyKey(key).appKey(appKey)
                .platformOrderId(request.getOrderId()).activityId(request.getActivityId())
                .sourceId(request.getSourceId()).retryTimes(request.getRetryTimes())
                .processStatus("PROCESSING").requestBody(rawBody).build();
    }

    private CpsOrderDTO toOrder(DidiOrderCallbackReqVO request, String rawBody) {
        boolean refunded = value(request.getOrderStatus()) == 8 || value(request.getRefundPrice()) > 0;
        boolean invalid = value(request.getIsRisk()) != 0 || value(request.getStatus()) == 6
                || value(request.getOrderStatus()) < 0;
        int platformStatus = refunded || invalid ? -1 : 1;
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("cpaProfitCent", value(request.getCpaProfit()));
        extra.put("cpsProfitCent", value(request.getCpsProfit()));
        extra.put("cpaType", request.getCpaType());
        extra.put("isRisk", value(request.getIsRisk()));
        extra.put("openUid", request.getOpenUid());
        extra.put("failReason", request.getFailReason());
        extra.put("callback", request.getCallback());
        extra.put("activityId", request.getActivityId());
        return CpsOrderDTO.builder().platformOrderId(request.getOrderId()).platformCode(PLATFORM)
                .vendorCode(VENDOR).itemId(request.getProductId()).itemTitle(request.getTitle())
                .finalPrice(cents(value(request.getPayPrice())))
                .commissionAmount(cents(value(request.getCpaProfit()) + value(request.getCpsProfit())))
                .quantity(1).platformStatus(platformStatus).orderTime(formatEpoch(request.getPayTime()))
                .payTime(formatEpoch(request.getPayTime())).adzoneId(request.getPromotionId())
                .externalId(request.getSourceId()).refundTag(refunded ? 1 : 0)
                .extraFields(extra).rawPayload(rawBody).build();
    }

    private void markSuccess(String key) {
        CpsDidiCallbackEventDO event = eventMapper.selectByIdempotencyKey(key);
        if (event != null) eventMapper.updateById(CpsDidiCallbackEventDO.builder()
                .id(event.getId()).processStatus("SUCCESS").build());
    }

    private void markFailure(String key, String reason) {
        CpsDidiCallbackEventDO event = eventMapper.selectByIdempotencyKey(key);
        if (event != null) eventMapper.updateById(CpsDidiCallbackEventDO.builder().id(event.getId())
                .processStatus("FAILED").failureReason(limit(reason, 500)).build());
    }

    private String extractSourceId(String callbackInfo) {
        if (blank(callbackInfo)) return null;
        try {
            Map<?, ?> values = objectMapper.readValue(callbackInfo, Map.class);
            Object sourceId = values.get("source_id");
            return sourceId == null ? null : String.valueOf(sourceId);
        } catch (JsonProcessingException ex) { return null; }
    }

    private BigDecimal cents(long value) {
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
    }
    private String formatEpoch(Long value) {
        return value == null || value <= 0 ? null : Instant.ofEpochSecond(value).atZone(ZONE_ID).format(DATE_TIME);
    }
    private long value(Long value) { return value == null ? 0L : value; }
    private int value(Integer value) { return value == null ? 0 : value; }
    private String limit(String value, int length) { return value == null ? null : value.substring(0, Math.min(length, value.length())); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
