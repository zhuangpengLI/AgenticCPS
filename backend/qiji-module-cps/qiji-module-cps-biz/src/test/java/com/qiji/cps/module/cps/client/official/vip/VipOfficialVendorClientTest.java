package com.qiji.cps.module.cps.client.official.vip;

import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.dto.*;
import com.vip.adp.api.open.service.*;
import com.vip.osp.sdk.exception.OspException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VipOfficialVendorClientTest {

    private UnionGoodsV2Service goodsService;
    private UnionUrlV2Service urlService;
    private UnionOrderV2Service orderService;
    private VipOfficialVendorClient client;

    private CpsVendorConfig baseConfig;

    @BeforeEach
    void setUp() {
        goodsService = mock(UnionGoodsV2Service.class);
        urlService = mock(UnionUrlV2Service.class);
        orderService = mock(UnionOrderV2Service.class);
        client = new VipOfficialVendorClient(goodsService, urlService, orderService);

        baseConfig = CpsVendorConfig.builder()
                .appKey("test_app_key")
                .appSecret("test_app_secret")
                .apiBaseUrl("https://vop.vipapis.com/")
                .defaultAdzoneId("default_vip_pid")
                .extraConfig(Map.of("timeoutMs", "8000"))
                .build();
    }

    @Test
    void capabilitiesAndMetadataShouldMatchOfficialVipPlatform() {
        assertEquals("vip", client.getPlatformCode());
        assertEquals("official", client.getVendorCode());
        assertTrue(client.getCapabilities().contains(CpsVendorCapability.GOODS_SEARCH));
        assertTrue(client.getCapabilities().contains(CpsVendorCapability.PROMOTION_LINK));
        assertTrue(client.getCapabilities().contains(CpsVendorCapability.ORDER_QUERY));
        assertTrue(client.getCapabilities().contains(CpsVendorCapability.CONNECTION_TEST));
        assertNotNull(client.getConfigSchema());
        assertNotNull(client.getGovernancePolicy());
    }

    @Test
    void searchGoods_withKeyword_shouldCallQueryAndMapFields() throws Exception {
        GoodsInfo info = new GoodsInfo();
        info.setGoodsId("VIP_G123");
        info.setGoodsName("唯品会正品女装连衣裙");
        info.setMarketPrice("499.00");
        info.setVipPrice("199.00");
        info.setCommissionRate("15.5");
        info.setCommission("30.85");
        info.setBrandName("品牌特卖");
        info.setGoodsMainPicture("https://img.vip.com/main.jpg");
        info.setProductSales("1200");
        info.setDestUrl("https://detail.vip.com/VIP_G123");

        GoodsInfoResponse mockResponse = new GoodsInfoResponse();
        mockResponse.setGoodsInfoList(List.of(info));
        mockResponse.setTotal(100);

        when(goodsService.query(any(QueryGoodsRequest.class))).thenReturn(mockResponse);

        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword("连衣裙");
        request.setPageNo(2);
        request.setPageSize(20);
        request.setSortType(2); // COMMISSION desc

        CpsGoodsSearchResult result = client.searchGoods(request, baseConfig);

        assertNotNull(result);
        assertEquals(100L, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(20, result.getPageSize());
        assertEquals(1, result.getList().size());

        CpsGoodsItem item = result.getList().get(0);
        assertEquals("VIP_G123", item.getGoodsId());
        assertEquals("vip", item.getPlatformCode());
        assertEquals("official", item.getVendorCode());
        assertEquals("唯品会正品女装连衣裙", item.getTitle());
        assertEquals(new BigDecimal("499.00"), item.getOriginalPrice());
        assertEquals(new BigDecimal("199.00"), item.getActualPrice());
        assertEquals(new BigDecimal("15.5"), item.getCommissionRate());
        assertEquals(new BigDecimal("30.85"), item.getCommissionAmount());
        assertEquals("品牌特卖", item.getBrandName());
        assertEquals(1200L, item.getMonthSales());
        assertEquals("https://detail.vip.com/VIP_G123", item.getItemLink());

        ArgumentCaptor<QueryGoodsRequest> captor = ArgumentCaptor.forClass(QueryGoodsRequest.class);
        verify(goodsService).query(captor.capture());
        assertEquals("连衣裙", captor.getValue().getKeyword());
        assertEquals(2, captor.getValue().getPage());
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals("COMMISSION", captor.getValue().getFieldName());
        assertEquals(1, captor.getValue().getOrder());
        assertEquals("default_vip_pid", captor.getValue().getChanTag());
    }

    @Test
    void searchGoods_withoutKeyword_shouldCallGoodsList() throws Exception {
        GoodsInfo info = new GoodsInfo();
        info.setGoodsId("VIP_HOT1");
        info.setGoodsName("爆款运动鞋");
        info.setVipPrice("299.00");
        info.setMarketPrice("599.00");

        GoodsInfoResponseV2 mockResponse = new GoodsInfoResponseV2();
        mockResponse.setGoodsInfoList(List.of(info));

        when(goodsService.goodsListV2(any(GoodsInfoRequestV2.class))).thenReturn(mockResponse);

        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setPageNo(1);
        request.setPageSize(10);

        CpsGoodsSearchResult result = client.searchGoods(request, baseConfig);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("VIP_HOT1", result.getList().get(0).getGoodsId());

        verify(goodsService).goodsListV2(any(GoodsInfoRequestV2.class));
        verify(goodsService, never()).query(any(QueryGoodsRequest.class));
    }

    @Test
    void generatePromotionLink_withGoodsId_shouldCallGenByGoodsId() throws Exception {
        UrlInfo urlInfo = new UrlInfo();
        urlInfo.setUrl("https://t.vip.com/s/short123");
        urlInfo.setLongUrl("https://detail.vip.com/long123");
        urlInfo.setDeeplinkUrl("vipshop://detail/VIP123");
        urlInfo.setOnlyCommand("￥唯品会口令￥");
        urlInfo.setUlUrl("https://ul.vip.com/123");

        UrlGenResponse mockResponse = new UrlGenResponse();
        mockResponse.setUrlInfoList(List.of(urlInfo));

        when(urlService.genByGoodsId(anyList(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any(UrlGenByGoodsIdRequest.class)))
                .thenReturn(mockResponse);

        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setGoodsId("VIP123");
        request.setAdzoneId("custom_pid_88");
        request.setRelationId("member_user_99");

        CpsPromotionLinkResult result = client.generatePromotionLink(request, baseConfig);

        assertNotNull(result);
        assertEquals("https://t.vip.com/s/short123", result.getShortUrl());
        assertEquals("https://detail.vip.com/long123", result.getLongUrl());
        assertEquals("vipshop://detail/VIP123", result.getMobileUrl());
        assertEquals("￥唯品会口令￥", result.getTpwd());
        assertEquals("https://ul.vip.com/123", result.getExtraFields().get("ulUrl"));

        verify(urlService).genByGoodsId(
                eq(List.of("VIP123")),
                eq("custom_pid_88"),
                anyString(),
                eq("member_user_99"),
                eq(true),
                eq(true),
                eq(true),
                any(UrlGenByGoodsIdRequest.class)
        );
    }

    @Test
    void generatePromotionLink_withItemLink_shouldCallGenByVIPUrl() throws Exception {
        UrlInfo urlInfo = new UrlInfo();
        urlInfo.setUrl("https://t.vip.com/s/short_url");
        urlInfo.setLongUrl("https://detail.vip.com/long_url");

        UrlGenResponse mockResponse = new UrlGenResponse();
        mockResponse.setUrlInfoList(List.of(urlInfo));

        when(urlService.genByVIPUrl(anyList(), anyString(), anyString(), nullable(String.class), any(UrlGenRequest.class)))
                .thenReturn(mockResponse);

        CpsPromotionLinkRequest request = new CpsPromotionLinkRequest();
        request.setItemLink("https://detail.vip.com/123456");

        CpsPromotionLinkResult result = client.generatePromotionLink(request, baseConfig);

        assertNotNull(result);
        assertEquals("https://t.vip.com/s/short_url", result.getShortUrl());
        verify(urlService).genByVIPUrl(
                eq(List.of("https://detail.vip.com/123456")),
                eq("default_vip_pid"),
                anyString(),
                isNull(),
                any(UrlGenRequest.class)
        );
    }

    @Test
    void queryOrders_shouldMapOrdersAndSubDetails() throws Exception {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderSn("VIP_ORD_001");
        orderInfo.setParentSn("VIP_PARENT_001");
        orderInfo.setOrderTime(1700000000000L); // 2023-11-15 06:13:20
        orderInfo.setSettledTime(1701000000000L);
        orderInfo.setStatus((short) 2); // 2: 已结算 -> 4
        orderInfo.setSettled((short) 1);
        orderInfo.setChannelTag("vip_pid_1");
        orderInfo.setStatParam("member_ext_01");
        orderInfo.setOrderSubStatusName("已结算");

        OrderDetailInfo detail1 = new OrderDetailInfo();
        detail1.setGoodsId("G_001");
        detail1.setGoodsName("唯品会T恤");
        detail1.setGoodsThumb("https://img.vip.com/thumb.jpg");
        detail1.setGoodsFinalPrice("88.00");
        detail1.setCommissionRate("10.0");
        detail1.setCommission("8.80");
        detail1.setGoodsCount(1);
        detail1.setBrandStoreName("品牌专卖");

        orderInfo.setDetailList(List.of(detail1));

        OrderResponse mockResponse = new OrderResponse();
        mockResponse.setOrderInfoList(List.of(orderInfo));

        when(orderService.orderList(any(OrderQueryModel.class))).thenReturn(mockResponse);

        CpsOrderQueryRequest request = new CpsOrderQueryRequest();
        request.setPageNo(1);
        request.setPageSize(20);
        request.setStartTime("2023-11-15 00:00:00");
        request.setEndTime("2023-11-16 00:00:00");
        request.setOrderStatus(4); // 请求已结算

        List<CpsOrderDTO> orders = client.queryOrders(request, baseConfig);

        assertNotNull(orders);
        assertEquals(1, orders.size());

        CpsOrderDTO order = orders.get(0);
        assertEquals("VIP_ORD_001", order.getPlatformOrderId());
        assertEquals("VIP_PARENT_001", order.getParentOrderId());
        assertEquals("vip", order.getPlatformCode());
        assertEquals("official", order.getVendorCode());
        assertEquals("G_001", order.getItemId());
        assertEquals("唯品会T恤", order.getItemTitle());
        assertEquals(new BigDecimal("88.00"), order.getFinalPrice());
        assertEquals(new BigDecimal("8.80"), order.getCommissionAmount());
        assertEquals(4, order.getPlatformStatus()); // 已结算
        assertEquals(0, order.getRefundTag());
        assertEquals("vip_pid_1", order.getAdzoneId());
        assertEquals("member_ext_01", order.getExternalId());
        assertEquals("品牌专卖", order.getExtraFields().get("shopName"));

        ArgumentCaptor<OrderQueryModel> captor = ArgumentCaptor.forClass(OrderQueryModel.class);
        verify(orderService).orderList(captor.capture());
        assertEquals((short) 2, captor.getValue().getStatus()); // 4 映射为唯品会状态 2
        assertEquals("default_vip_pid", captor.getValue().getChanTag());
    }

    @Test
    void testConnection_shouldReturnTrueWhenQuerySucceeds() throws Exception {
        GoodsInfoResponse mockResponse = new GoodsInfoResponse();
        mockResponse.setGoodsInfoList(Collections.emptyList());
        when(goodsService.query(any(QueryGoodsRequest.class))).thenReturn(mockResponse);

        boolean passed = client.testConnection(baseConfig);
        assertTrue(passed);
    }

    @Test
    void testConnection_shouldReturnFalseOnException() throws Exception {
        when(goodsService.query(any(QueryGoodsRequest.class)))
                .thenThrow(new OspException("401", "Invalid AppKey"));

        boolean passed = client.testConnection(baseConfig);
        assertFalse(passed);
    }
}
