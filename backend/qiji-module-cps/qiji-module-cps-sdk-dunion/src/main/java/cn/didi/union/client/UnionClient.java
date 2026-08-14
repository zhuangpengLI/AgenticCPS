package cn.didi.union.client;

import cn.didi.union.enums.OrderMockType;
import cn.didi.union.enums.OrderType;
import cn.didi.union.models.*;

public interface UnionClient extends BasicClient {
    Result<LinkResponse> generateH5Link(long activityId, long promotionId, String sourceId, int timeout);
    Result<LinkResponse> generateH5Link(long activityId, long promotionId, String sourceId, String callback, int timeout);
    Result<LinkResponse> generateMiniLink(long activityId, long promotionId, String sourceId, int timeout);
    Result<QrCodeResponse> generateH5Code(String dsi, String sourceId, int timeout);
    Result<QrCodeResponse> generateMiniCode(String dsi, String sourceId, int timeout);
    Result<PosterResponse> generatePoster(String dsi, String sourceId, int timeout);
    Result<QrCodeResponse> generateH5CodeDirectly(long activityId, long promotionId, String sourceId, int timeout);
    Result<QrCodeResponse> generateMiniCodeDirectly(long activityId, long promotionId, String sourceId, int timeout);
    Result<PosterResponse> generatePosterDirectly(long activityId, long promotionId, String sourceId, int timeout);
    Result<ExchangePwdResponse> generateCouponPwd(long activityId, long promotionId, String sourceId, int timeout);
    Result<OrderResponse> queryOrderList(long startTime, long endTime, OrderType type, int page, int size, int timeout);
    Result<OrderCallbackResponse> mockOrderCallback(String dsi, String sourceId, OrderMockType type, int timeout);
    Result<OrderSelfQueryResponse> selfQueryOrder(String orderId, int timeout);
}
