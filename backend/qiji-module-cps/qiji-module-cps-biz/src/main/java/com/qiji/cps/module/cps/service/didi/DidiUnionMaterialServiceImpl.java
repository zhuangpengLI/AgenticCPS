package com.qiji.cps.module.cps.service.didi;

import cn.didi.union.client.UnionClient;
import cn.didi.union.models.*;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.didi.DidiOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

@Service
@RequiredArgsConstructor
public class DidiUnionMaterialServiceImpl implements DidiUnionMaterialService {
    private static final String VENDOR_CODE = "official";
    private static final String PLATFORM_CODE = "didi";
    private final CpsPlatformClientFactory platformFactory;
    private final DidiOfficialVendorClient vendorClient;

    @Override
    public DidiUnionMaterialResult generate(DidiUnionMaterialType type, long activityId, Long promotionIdValue) {
        CpsVendorConfig config = config();
        long promotionId = positiveLong(promotionIdValue != null ? String.valueOf(promotionIdValue)
                : config.getDefaultAdzoneId(), "promotionId");
        String sourceId = "ops-" + UUID.randomUUID();
        UnionClient client = vendorClient.getClient(config);
        int timeout = vendorClient.getTimeout(config);
        return switch (type) {
            case H5_LINK -> linkResult(type, activityId, promotionId, sourceId,
                    require(client.generateH5Link(activityId, promotionId, sourceId, timeout), "generateH5Link"));
            case MINI_LINK -> linkResult(type, activityId, promotionId, sourceId,
                    require(client.generateMiniLink(activityId, promotionId, sourceId, timeout), "generateMiniLink"));
            case H5_QR_CODE -> {
                LinkResponse link = require(client.generateH5Link(activityId, promotionId, sourceId, timeout), "generateH5Link");
                QrCodeResponse qr = require(client.generateH5Code(requireDsi(link), sourceId, timeout), "generateH5Code");
                yield new DidiUnionMaterialResult(type, activityId, promotionId, sourceId, link.getData().getLink(),
                        link.getData().getDsi(), link.getData().getAppId(), link.getData().getAppSource(),
                        qr.getData() == null ? null : qr.getData().getCodeLink(), null, null, qr.getTraceid());
            }
            case MINI_QR_CODE -> {
                LinkResponse link = require(client.generateMiniLink(activityId, promotionId, sourceId, timeout), "generateMiniLink");
                QrCodeResponse qr = require(client.generateMiniCode(requireDsi(link), sourceId, timeout), "generateMiniCode");
                yield new DidiUnionMaterialResult(type, activityId, promotionId, sourceId, link.getData().getLink(),
                        link.getData().getDsi(), link.getData().getAppId(), link.getData().getAppSource(),
                        qr.getData() == null ? null : qr.getData().getCodeLink(), null, null, qr.getTraceid());
            }
            case POSTER -> {
                LinkResponse link = require(client.generateMiniLink(activityId, promotionId, sourceId, timeout), "generateMiniLink");
                PosterResponse poster = require(client.generatePoster(requireDsi(link), sourceId, timeout), "generatePoster");
                yield new DidiUnionMaterialResult(type, activityId, promotionId, sourceId, link.getData().getLink(),
                        link.getData().getDsi(), link.getData().getAppId(), link.getData().getAppSource(), null,
                        poster.getData() == null ? null : poster.getData().getPosterLink(), null, poster.getTraceid());
            }
            case COUPON_CODE -> {
                ExchangePwdResponse pwd = require(client.generateCouponPwd(activityId, promotionId, sourceId, timeout), "generateCouponPwd");
                yield new DidiUnionMaterialResult(type, activityId, promotionId, sourceId, null, null, null, null,
                        null, null, pwd.getData() == null ? null : pwd.getData().getExchangePwd(), pwd.getTraceid());
            }
        };
    }

    @Override public boolean testConnection() { return vendorClient.testConnection(config()); }

    @Override
    public DidiUnionOrderAttributionResult queryOrderAttribution(String orderId) {
        if (!StringUtils.hasText(orderId)) throw exception(DIDI_UNION_CONFIG_INVALID, "orderId 不能为空");
        CpsVendorConfig config = config();
        OrderSelfQueryResponse response = require(vendorClient.getClient(config).selfQueryOrder(orderId,
                vendorClient.getTimeout(config)), "selfQueryOrder");
        EstimateQueryData data = response.getData();
        List<DidiUnionOrderAttributionResult.SuccessItem> success = data == null || data.getEstimateSuccessList() == null
                ? List.of() : data.getEstimateSuccessList().stream().map(item -> new DidiUnionOrderAttributionResult.SuccessItem(
                item.getEstimateTime(), item.getEstimateChannel(), item.getReceiveStatus(), item.getReceiveTime(), item.getSceneName())).toList();
        List<DidiUnionOrderAttributionResult.FailItem> fail = data == null || data.getEstimateFailList() == null
                ? List.of() : data.getEstimateFailList().stream().map(item -> new DidiUnionOrderAttributionResult.FailItem(
                item.getFailReason(), item.getSceneName())).toList();
        return new DidiUnionOrderAttributionResult(orderId, response.getTraceid(), success, fail);
    }

    private CpsVendorConfig config() {
        CpsVendorConfig config = platformFactory.getVendorConfig(VENDOR_CODE, PLATFORM_CODE);
        if (config == null) throw exception(VENDOR_NOT_EXISTS);
        if (!StringUtils.hasText(config.getAppKey()) || !StringUtils.hasText(config.getAppSecret())) {
            throw exception(DIDI_UNION_CONFIG_INVALID, "appKey/appSecret 不能为空");
        }
        return config;
    }
    private DidiUnionMaterialResult linkResult(DidiUnionMaterialType type, long activityId, long promotionId,
                                                String sourceId, LinkResponse response) {
        if (response.getData() == null) throw exception(DIDI_UNION_REQUEST_FAILED, "link data 为空");
        return new DidiUnionMaterialResult(type, activityId, promotionId, sourceId, response.getData().getLink(),
                response.getData().getDsi(), response.getData().getAppId(), response.getData().getAppSource(),
                null, null, null, response.getTraceid());
    }
    private String requireDsi(LinkResponse response) {
        if (response.getData() == null || !StringUtils.hasText(response.getData().getDsi())) {
            throw exception(DIDI_UNION_REQUEST_FAILED, "dsi 为空");
        }
        return response.getData().getDsi();
    }
    private <T> T require(Result<T> result, String operation) {
        if (result == null || !result.isSuccess() || result.getModel() == null) {
            String message = result != null && result.getError() != null ? result.getError().getMessage() : "empty result";
            throw exception(DIDI_UNION_REQUEST_FAILED, operation + ": " + message);
        }
        return result.getModel();
    }
    private long positiveLong(String value, String name) {
        try { long parsed = Long.parseLong(value); if (parsed > 0) return parsed; } catch (Exception ignored) { }
        throw exception(DIDI_UNION_CONFIG_INVALID, name + " 必须为正整数");
    }
}
