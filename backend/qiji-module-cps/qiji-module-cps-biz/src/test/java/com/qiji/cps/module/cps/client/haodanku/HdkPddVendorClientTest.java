package com.qiji.cps.module.cps.client.haodanku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HdkPddVendorClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("拼多多搜索应映射好单库 v2 字段并保留元价格")
    void searchResponseShouldMapHdkV2FieldsAndYuanPrices() throws Exception {
        HdkPddVendorClient client = new HdkPddVendorClient();
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setPageNo(3);
        request.setPageSize(10);

        assertEquals("/pdd_goods_search", client.getSearchApiPath());
        assertEquals("/pdd_goods_search", client.getTestConnectionApiPath());
        assertEquals("https://v2.api.haodanku.com", client.resolveApiBaseUrl(CpsVendorConfig.builder()
                .apiBaseUrl("http://v2.api.haodanku.com")
                .build()));
        CpsVendorConfig v3Config = CpsVendorConfig.builder()
                .apiBaseUrl("https://v3.api.haodanku.com")
                .build();
        assertEquals("https://v2.api.haodanku.com",
                client.resolveApiBaseUrl(client.getSearchApiPath(), v3Config));
        assertEquals("https://v3.api.haodanku.com",
                client.resolveApiBaseUrl(client.getOrderQueryApiPath(), v3Config));

        CpsGoodsSearchResult result = client.parseSearchResponse(OBJECT_MAPPER.readTree("""
                {
                  "code": 200,
                  "min_id": 4,
                  "data": [{
                    "goods_sign": "pdd-sign-1",
                    "goodsname": "拼多多测试商品",
                    "itemprice": "29.90",
                    "itemendprice": "19.80",
                    "itempic": "https://img.example/pdd.jpg",
                    "itemsale": "1234",
                    "shopname": "拼多多测试店铺",
                    "promotion_rate": "12.50",
                    "commission": "2.48",
                    "couponmoney": "10.10"
                  }]
                }
                """), request);

        CpsGoodsItem goods = result.getList().get(0);
        assertEquals("pdd-sign-1", goods.getGoodsId());
        assertEquals("pdd-sign-1", goods.getGoodsSign());
        assertEquals("拼多多测试商品", goods.getTitle());
        assertEquals("https://img.example/pdd.jpg", goods.getMainPic());
        assertEquals(new BigDecimal("29.90"), goods.getOriginalPrice());
        assertEquals(new BigDecimal("19.80"), goods.getActualPrice());
        assertEquals(new BigDecimal("10.10"), goods.getCouponPrice());
        assertEquals(new BigDecimal("12.50"), goods.getCommissionRate());
        assertEquals(new BigDecimal("2.48"), goods.getCommissionAmount());
        assertEquals(1234L, goods.getMonthSales());
        assertEquals("拼多多测试店铺", goods.getShopName());
        assertEquals("haodanku", goods.getVendorCode());
        assertEquals(1L, result.getTotal());
        assertEquals("4", result.getNextPageId());
        assertEquals(3, result.getPageNo());
        assertEquals(10, result.getPageSize());
    }

    @Test
    @DisplayName("拼多多搜索有显式总数时应优先使用并为缺失分页参数兜底")
    void searchResponseShouldPreferTotalAndFallbackRequestPagination() throws Exception {
        HdkPddVendorClient client = new HdkPddVendorClient();
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setPageNo(null);
        request.setPageSize(null);

        Map<String, Object> params = client.buildSearchParams(request, CpsVendorConfig.builder().build());

        CpsGoodsSearchResult result = client.parseSearchResponse(OBJECT_MAPPER.readTree("""
                {"code":200,"total":88,"data":[]}
                """), request);

        assertEquals(1, params.get("min_id"));
        assertEquals(20, params.get("limit"));
        assertEquals(88L, result.getTotal());
        assertEquals(1, result.getPageNo());
        assertEquals(20, result.getPageSize());
    }
}
