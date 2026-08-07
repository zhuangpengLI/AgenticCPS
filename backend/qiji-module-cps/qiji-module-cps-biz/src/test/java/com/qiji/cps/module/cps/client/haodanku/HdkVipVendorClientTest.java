package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HdkVipVendorClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void searchShouldUseVipV3ContractAndMapGoods() throws Exception {
        HdkVipVendorClient client = new HdkVipVendorClient();
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("运动鞋");
        request.setPageNo(2);
        request.setPageSize(30);
        request.setSortType(1);

        Map<String, Object> params = client.buildSearchParams(request, CpsVendorConfig.builder().build());
        CpsGoodsSearchResult result = client.parseSearchResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"min_id":3,"data":[{
                  "goodsId":"VIP-1",
                  "goodsName":"唯品会运动鞋",
                  "goodsMainPicture":"https://img.example/vip.jpg",
                  "marketPrice":"599.00",
                  "vipPrice":"199.00",
                  "commissionRate":"12.5",
                  "commission":"24.88",
                  "brandName":"测试品牌",
                  "productSales":"321"
                }]}
                """), request);

        assertEquals("vip", client.getPlatformCode());
        assertTrue(client.isSuccessResponse(OBJECT_MAPPER.readTree("{\"code\":200}")));
        assertEquals("/unify_vip_item_query", client.getSearchApiPath());
        assertEquals("运动鞋", params.get("keyword"));
        assertEquals(2, params.get("min_id"));
        assertEquals(30, params.get("back"));
        assertEquals(6, params.get("order"));
        CpsGoodsItem goods = result.getList().get(0);
        assertEquals("VIP-1", goods.getGoodsId());
        assertEquals("vip", goods.getPlatformCode());
        assertEquals("唯品会运动鞋", goods.getTitle());
        assertEquals("https://img.example/vip.jpg", goods.getMainPic());
        assertEquals(new BigDecimal("599.00"), goods.getOriginalPrice());
        assertEquals(new BigDecimal("199.00"), goods.getActualPrice());
        assertEquals(new BigDecimal("12.5"), goods.getCommissionRate());
        assertEquals(new BigDecimal("24.88"), goods.getCommissionAmount());
        assertEquals("测试品牌", goods.getBrandName());
        assertEquals(321L, goods.getMonthSales());
        assertEquals(-1L, result.getTotal());
        assertEquals("3", result.getNextPageId());
    }

    @Test
    void promotionLinkShouldUseVipContractAndMapAllLinks() throws Exception {
        HdkVipVendorClient client = new HdkVipVendorClient();
        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("VIP-1");
        request.setChannelId("member_123");

        Map<String, Object> params = client.buildPromotionLinkParams(request, CpsVendorConfig.builder().build());
        CpsPromotionLinkResult result = client.parsePromotionLinkResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"data":{
                  "url":"https://s.example/vip",
                  "longUrl":"https://www.example/vip-long",
                  "onlyCommand":"￥唯口令￥",
                  "deeplinkUrl":"vipshop://goods/1"
                }}
                """));

        assertEquals("/unify_vip_item_convert", client.getPromotionLinkApiPath());
        assertEquals("VIP-1", params.get("goods_id"));
        assertEquals("member_123", params.get("channel"));
        assertEquals("https://s.example/vip", result.getShortUrl());
        assertEquals("https://www.example/vip-long", result.getLongUrl());
        assertEquals("￥唯口令￥", result.getTpwd());
        assertEquals("vipshop://goods/1", result.getMobileUrl());
    }

    @Test
    void orderQueryShouldUseOfficialEndpointAndPreserveAttribution() throws Exception {
        HdkVipVendorClient client = new HdkVipVendorClient();
        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setPositionIndex("8");
        request.setPageSize(100);
        request.setQueryType(4);
        request.setOrderStatus(5);
        request.setStartTime("2024-01-01 00:00:00");
        request.setEndTime("2024-01-01 01:00:00");

        Map<String, Object> params = client.buildOrderQueryParams(request, CpsVendorConfig.builder().build());
        List<CpsOrderDTO> orders = client.parseOrderQueryResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"min_id":"9","data":[{
                  "trade_id":"VIP-ORDER-1",
                  "trade_parent_id":"VIP-PARENT-1",
                  "order_status":5,
                  "shop_name":"唯品会品牌店",
                  "item_title":"唯品会测试商品",
                  "item_img":"https://img.example/order.jpg",
                  "goods_id":"VIP-1",
                  "create_time":"2026-08-01 10:00:00",
                  "paid_time":"2026-08-01 10:01:00",
                  "earning_time":"2026-09-01 10:00:00",
                  "settled_status":1,
                  "settled_at":"2026-09-02 10:00:00",
                  "pay_price":"199.00",
                  "predict_money":"25.00",
                  "actual_money":"24.88",
                  "channel_code":"member_123",
                  "updated_at":"2026-09-02 10:00:01"
                }]}
                """));

        assertEquals("/vip_union_order_list", client.getOrderQueryApiPath());
        assertEquals("8", params.get("min_id"));
        assertEquals(100, params.get("back"));
        assertEquals(4, params.get("date_type"));
        assertEquals(5, params.get("state"));
        assertEquals("1704038400", params.get("start_date"));
        assertEquals("1704042000", params.get("end_date"));
        CpsOrderDTO order = orders.get(0);
        assertEquals("vip", order.getPlatformCode());
        assertEquals("haodanku", order.getVendorCode());
        assertEquals("VIP-ORDER-1", order.getPlatformOrderId());
        assertEquals("VIP-PARENT-1", order.getParentOrderId());
        assertEquals("VIP-1", order.getItemId());
        assertEquals(new BigDecimal("199.00"), order.getFinalPrice());
        assertEquals(new BigDecimal("24.88"), order.getCommissionAmount());
        assertEquals(4, order.getPlatformStatus());
        assertEquals("member_123", order.getExternalId());
        assertEquals("9", order.getNextPositionIndex());
        assertEquals("唯品会品牌店", order.getExtraFields().get("shop_name"));
        assertEquals("1", order.getExtraFields().get("settled_status"));

    }

    @Test
    void orderStatusShouldMapAllOfficialVipStates() throws Exception {
        HdkVipVendorClient client = new HdkVipVendorClient();
        List<CpsOrderDTO> orders = client.parseOrderQueryResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"data":[
                  {"trade_id":"1","order_status":1},
                  {"trade_id":"2","order_status":2},
                  {"trade_id":"3","order_status":3},
                  {"trade_id":"4","order_status":4},
                  {"trade_id":"5","order_status":5},
                  {"trade_id":"6","order_status":6}
                ]}
                """));

        assertEquals(List.of(1, 1, 2, 2, 4, -1),
                orders.stream().map(CpsOrderDTO::getPlatformStatus).toList());
        assertEquals(1, orders.get(5).getRefundTag());
    }

    @Test
    void connectionTestShouldUseVipSearchAndDeclareAllImplementedCapabilities() {
        HdkVipVendorClient client = new HdkVipVendorClient();

        assertEquals("/unify_vip_item_query", client.getTestConnectionApiPath());
        assertEquals(Map.of("keyword", "手机", "min_id", 1, "back", 10, "order", 0),
                client.buildTestConnectionParams());
        assertTrue(client.getCapabilities().containsAll(List.of(
                CpsVendorCapability.GOODS_SEARCH,
                CpsVendorCapability.PROMOTION_LINK,
                CpsVendorCapability.ORDER_QUERY,
                CpsVendorCapability.CONNECTION_TEST)));
    }
}
