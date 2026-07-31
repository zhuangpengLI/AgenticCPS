package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HdkElemeVendorClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("淘宝闪购订单请求应使用好单库 elm_order_list 契约")
    void orderQueryShouldUseOfficialEndpointAndParams() {
        HdkElemeVendorClient client = new HdkElemeVendorClient();
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setPositionIndex("3");
        request.setPageSize(50);
        request.setQueryType(4);
        request.setOrderStatus(2);
        request.setStartTime("2024-01-01 00:00:00");
        request.setEndTime("2024-01-01 01:00:00");

        Map<String, Object> params = client.buildOrderQueryParams(request, CpsVendorConfig.builder().build());

        assertEquals("/elm_order_list", client.getOrderQueryApiPath());
        assertEquals("3", params.get("min_id"));
        assertEquals(50, params.get("back"));
        assertEquals(4, params.get("date_type"));
        assertEquals(2, params.get("state"));
        assertEquals("1704038400", params.get("start_date"));
        assertEquals("1704042000", params.get("end_date"));
    }

    @Test
    @DisplayName("淘宝闪购订单响应应保留渠道、结算和退款字段")
    void orderResponseShouldMapAttributionAndSettlementFields() throws Exception {
        HdkElemeVendorClient client = new HdkElemeVendorClient();

        List<CpsOrderDTO> orders = client.parseOrderQueryResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"min_id":"4","data":[{
                  "trade_id":"ELM-ORDER-1",
                  "trade_parent_id":"ELM-PARENT-1",
                  "item_title":"淘宝闪购测试订单",
                  "item_img":"https://img.example/elm.jpg",
                  "pay_price":"48.80",
                  "predict_money":"3.20",
                  "actual_money":"2.90",
                  "order_status":3,
                  "settled_status":2,
                  "create_time":"2026-07-30 12:00:00",
                  "paid_time":"2026-07-30 12:01:00",
                  "earning_time":"2026-08-30 12:00:00",
                  "channel_code":"Abc_123456789",
                  "order_item_status":"REFUNDED",
                  "order_item_status_name":"已退款",
                  "order_channel":"ELEME",
                  "order_tags":"activity"
                }]}
                """));

        CpsOrderDTO order = orders.get(0);
        assertEquals("eleme", order.getPlatformCode());
        assertEquals("ELM-ORDER-1", order.getPlatformOrderId());
        assertEquals("ELM-PARENT-1", order.getParentOrderId());
        assertEquals(new BigDecimal("48.80"), order.getFinalPrice());
        assertEquals(new BigDecimal("2.90"), order.getCommissionAmount());
        assertEquals(-1, order.getPlatformStatus());
        assertEquals(1, order.getRefundTag());
        assertEquals("Abc_123456789", order.getExternalId());
        assertEquals("2", order.getExtraFields().get("settled_status"));
        assertEquals("REFUNDED", order.getExtraFields().get("order_item_status"));
        assertEquals("4", order.getNextPositionIndex());
    }
}
