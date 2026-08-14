package cn.didi.union.client.impl;

import cn.didi.union.client.DunionClientException;
import cn.didi.union.client.UnionClient;
import cn.didi.union.enums.LinkType;
import cn.didi.union.enums.OrderMockType;
import cn.didi.union.enums.OrderType;
import cn.didi.union.errors.BizError;
import cn.didi.union.errors.ParamError;
import cn.didi.union.errors.SystemError;
import cn.didi.union.models.*;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.TreeMap;
import java.util.function.Supplier;

public class UnionClientImpl extends BasicClientImpl implements UnionClient {
    private static final Gson GSON = new Gson();
    private final DunionClientConfig config;

    public UnionClientImpl(DunionClientConfig config) { this.config = config; }

    @Override public Result<LinkResponse> generateH5Link(long activityId, long promotionId, String sourceId, int timeout) {
        return generateH5Link(activityId, promotionId, sourceId, null, timeout);
    }
    @Override public Result<LinkResponse> generateH5Link(long activityId, long promotionId, String sourceId, String callback, int timeout) {
        return generateLink(activityId, promotionId, sourceId, callback, LinkType.H5, timeout);
    }
    @Override public Result<LinkResponse> generateMiniLink(long activityId, long promotionId, String sourceId, int timeout) {
        return generateLink(activityId, promotionId, sourceId, null, LinkType.Mini, timeout);
    }
    private Result<LinkResponse> generateLink(long activityId, long promotionId, String sourceId, String callback,
                                              LinkType type, int timeout) {
        if (activityId <= 0 || promotionId <= 0 || blank(sourceId)) return param("activityId, promotionId and sourceId are required");
        TreeMap<String, Object> params = params("activity_id", activityId, "promotion_id", promotionId,
                "source_id", sourceId, "link_type", type.getValue());
        if (!blank(callback)) params.put("dunion_callback", callback);
        return call(() -> doPost(config, path("/link/generate"), timeout, params), LinkResponse.class);
    }
    @Override public Result<QrCodeResponse> generateH5Code(String dsi, String sourceId, int timeout) {
        return generateCode(dsi, sourceId, LinkType.H5, timeout);
    }
    @Override public Result<QrCodeResponse> generateMiniCode(String dsi, String sourceId, int timeout) {
        return generateCode(dsi, sourceId, LinkType.Mini, timeout);
    }
    private Result<QrCodeResponse> generateCode(String dsi, String sourceId, LinkType type, int timeout) {
        if (blank(dsi) || blank(sourceId)) return param("dsi and sourceId are required");
        TreeMap<String, Object> params = params("dsi", dsi, "source_id", sourceId, "type", type.getValue());
        return call(() -> doGet(config, path("/code/generate"), timeout, params), QrCodeResponse.class);
    }
    @Override public Result<PosterResponse> generatePoster(String dsi, String sourceId, int timeout) {
        if (blank(dsi) || blank(sourceId)) return param("dsi and sourceId are required");
        TreeMap<String, Object> params = params("dsi", dsi, "source_id", sourceId);
        return call(() -> doGet(config, path("/poster/generate"), timeout, params), PosterResponse.class);
    }
    @Override public Result<ExchangePwdResponse> generateCouponPwd(long activityId, long promotionId, String sourceId, int timeout) {
        if (activityId <= 0 || promotionId <= 0 || blank(sourceId)) return param("activityId, promotionId and sourceId are required");
        TreeMap<String, Object> params = params("activity_id", activityId, "promotion_id", promotionId,
                "source_id", sourceId, "pwd_type", "coupon");
        return call(() -> doGet(config, path("/exchange/pwd/generate"), timeout, params), ExchangePwdResponse.class);
    }
    @Override public Result<QrCodeResponse> generateH5CodeDirectly(long activityId, long promotionId, String sourceId, int timeout) {
        Result<LinkResponse> link = generateH5Link(activityId, promotionId, sourceId, timeout);
        return link.isSuccess() && link.getModel().getData() != null
                ? generateH5Code(link.getModel().getData().getDsi(), sourceId, timeout) : propagate(link);
    }
    @Override public Result<QrCodeResponse> generateMiniCodeDirectly(long activityId, long promotionId, String sourceId, int timeout) {
        Result<LinkResponse> link = generateMiniLink(activityId, promotionId, sourceId, timeout);
        return link.isSuccess() && link.getModel().getData() != null
                ? generateMiniCode(link.getModel().getData().getDsi(), sourceId, timeout) : propagate(link);
    }
    @Override public Result<PosterResponse> generatePosterDirectly(long activityId, long promotionId, String sourceId, int timeout) {
        Result<LinkResponse> link = generateMiniLink(activityId, promotionId, sourceId, timeout);
        return link.isSuccess() && link.getModel().getData() != null
                ? generatePoster(link.getModel().getData().getDsi(), sourceId, timeout) : propagate(link);
    }
    @Override public Result<OrderResponse> queryOrderList(long startTime, long endTime, OrderType type, int page, int size, int timeout) {
        if (startTime <= 0 || endTime < startTime || page <= 0 || page > 100 || size <= 0 || size > 100) return param("invalid order query parameters");
        TreeMap<String, Object> params = params("pay_start_time", startTime, "pay_end_time", endTime, "page", page, "size", size);
        if (type != null && type != OrderType.All) params.put("type", type.getValue());
        return call(() -> doGet(config, path("/order/list"), timeout, params), OrderResponse.class);
    }
    @Override public Result<OrderCallbackResponse> mockOrderCallback(String dsi, String sourceId, OrderMockType type, int timeout) {
        if (blank(dsi) || blank(sourceId) || type == null) return param("dsi, sourceId and type are required");
        TreeMap<String, Object> params = params("dsi", dsi, "source_id", sourceId, "type", type.getValue());
        return call(() -> doGet(config, path("/orderMock/callback"), timeout, params), OrderCallbackResponse.class);
    }
    @Override public Result<OrderSelfQueryResponse> selfQueryOrder(String orderId, int timeout) {
        if (blank(orderId)) return param("orderId is required");
        TreeMap<String, Object> params = params("order_id", orderId);
        return call(() -> doGet(config, path("/order/selfQuery"), timeout, params), OrderSelfQueryResponse.class);
    }

    private <T extends BaseModel> Result<T> call(Supplier<String> request, Class<T> type) {
        try {
            T model = GSON.fromJson(request.get(), type);
            if (model == null) return failure(new SystemError("DUnion response is empty"));
            if (model.getErrno() != 0) return failure(new BizError((int) model.getErrno(), safe(model.getErrmsg()) + trace(model)));
            return Result.Builder.<T>builder().success(true).model(model).build();
        } catch (DunionClientException | JsonParseException ex) {
            return failure(new SystemError(ex.getMessage()));
        }
    }
    private String path(String suffix) { return config.getBaseUrl() + suffix; }
    private String trace(BaseModel model) { return blank(model.getTraceid()) ? "" : ", traceId=" + model.getTraceid(); }
    private String safe(String value) { return blank(value) ? "DUnion business error" : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private <T> Result<T> param(String message) { return failure(new ParamError(message)); }
    private <T> Result<T> failure(cn.didi.union.errors.ErrorBase error) { return Result.Builder.<T>builder().success(false).error(error).build(); }
    private <T> Result<T> propagate(Result<?> source) { return Result.Builder.<T>builder().success(false).error(source.getError()).build(); }
    private TreeMap<String, Object> params(Object... values) {
        TreeMap<String, Object> result = new TreeMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
        return result;
    }
}
