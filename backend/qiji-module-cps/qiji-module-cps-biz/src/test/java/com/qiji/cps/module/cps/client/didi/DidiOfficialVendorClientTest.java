package com.qiji.cps.module.cps.client.didi;

import cn.didi.union.client.UnionClient;
import cn.didi.union.enums.OrderType;
import cn.didi.union.models.LinkResponse;
import cn.didi.union.models.OrderResponse;
import cn.didi.union.models.Result;
import com.google.gson.Gson;
import com.qiji.cps.module.cps.client.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DidiOfficialVendorClientTest {

    @Mock private DidiUnionClientFactory clientFactory;
    @Mock private UnionClient unionClient;
    private DidiOfficialVendorClient client;

    @BeforeEach
    void setUp() {
        client = new DidiOfficialVendorClient(clientFactory);
        when(clientFactory.create(any())).thenReturn(unionClient);
        when(clientFactory.resolveTimeout(any())).thenReturn(5000);
    }

    @Test
    void shouldMapH5PromotionLink() {
        LinkResponse response = new Gson().fromJson("""
                {"errno":0,"traceid":"trace-1","data":{"app_id":"wx123","app_source":"gh_x","dsi":"dsi-1","link":"https://didi.example/link"}}
                """, LinkResponse.class);
        when(unionClient.generateH5Link(eq(1001L), eq(2002L), eq("88"), contains("source_id"), eq(5000)))
                .thenReturn(Result.Builder.<LinkResponse>builder().success(true).model(response).build());
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("1001"); request.setAdzoneId("2002"); request.setExternalId("88");

        CpsPromotionLinkResult result = client.generatePromotionLink(request, config());

        assertEquals("https://didi.example/link", result.getLongUrl());
        assertEquals("dsi-1", result.getExtraFields().get("dsi"));
        assertEquals("trace-1", result.getExtraFields().get("traceId"));
    }

    @Test
    void shouldAcceptLongActivityIdBeyondIntegerRange() {
        long activityId = 990715010527L;
        LinkResponse response = new Gson().fromJson("""
                {"errno":0,"traceid":"trace-long","data":{"link":"https://didi.example/long-link"}}
                """, LinkResponse.class);
        when(unionClient.generateH5Link(eq(activityId), eq(2002L), eq("88"), contains("source_id"), eq(5000)))
                .thenReturn(Result.Builder.<LinkResponse>builder().success(true).model(response).build());
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId(String.valueOf(activityId)); request.setAdzoneId("2002"); request.setExternalId("88");

        CpsPromotionLinkResult result = client.generatePromotionLink(request, config());

        assertEquals("https://didi.example/long-link", result.getLongUrl());
    }

    @Test
    void shouldMapOrderMoneyStatusAndAttribution() {
        OrderResponse response = new Gson().fromJson("""
                {"errno":0,"data":{"total":1,"order_list":[{"title":"快车券","order_id":"o-1","product_id":"p-1","pay_price":1234,"pay_time":1700000000,"cpa_profit":100,"cps_profit":23,"cpa_type":"new","status":7,"promotion_id":2002,"source_id":"88","is_risk":0,"order_status":2}]}}
                """, OrderResponse.class);
        when(unionClient.queryOrderList(anyLong(), anyLong(), eq(OrderType.All), eq(1), eq(50), eq(5000)))
                .thenReturn(Result.Builder.<OrderResponse>builder().success(true).model(response).build());
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setStartTime("2023-11-01 00:00:00"); request.setEndTime("2023-12-01 00:00:00");

        List<CpsOrderDTO> orders = client.queryOrders(request, config());

        assertEquals(1, orders.size());
        CpsOrderDTO order = orders.get(0);
        assertEquals(new BigDecimal("12.34"), order.getFinalPrice());
        assertEquals(new BigDecimal("1.23"), order.getCommissionAmount());
        assertEquals(3, order.getPlatformStatus());
        assertEquals("2002", order.getAdzoneId());
        assertEquals("88", order.getExternalId());
        assertEquals(100L, order.getExtraFields().get("cpaProfitCent"));
    }

    @Test
    void shouldInvalidateRiskOrderAndNotAdvancePendingStatus() {
        OrderResponse response = new Gson().fromJson("""
                {"errno":0,"data":{"total":2,"order_list":[
                  {"order_id":"risk","product_id":"p","pay_price":100,"pay_time":1700000000,"status":7,"is_risk":1,"order_status":2},
                  {"order_id":"pending","product_id":"p","pay_price":100,"pay_time":1700000000,"status":4,"is_risk":0,"order_status":2}
                ]}}
                """, OrderResponse.class);
        when(unionClient.queryOrderList(anyLong(), anyLong(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Result.Builder.<OrderResponse>builder().success(true).model(response).build());
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setStartTime("2023-11-01 00:00:00"); request.setEndTime("2023-12-01 00:00:00");

        List<CpsOrderDTO> orders = client.queryOrders(request, config());

        assertEquals(-1, orders.get(0).getPlatformStatus());
        assertEquals(1, orders.get(1).getPlatformStatus());
    }

    private CpsVendorConfig config() {
        return CpsVendorConfig.builder().vendorCode("official").platformCode("didi")
                .appKey("app").appSecret("secret").defaultAdzoneId("2002")
                .extraConfig(Map.of("timeoutMs", "5000")).build();
    }
}
