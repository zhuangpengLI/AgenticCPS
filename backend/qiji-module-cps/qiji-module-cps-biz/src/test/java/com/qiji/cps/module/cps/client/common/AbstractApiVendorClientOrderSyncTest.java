package com.qiji.cps.module.cps.client.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractApiVendorClientOrderSyncTest {

    @Test
    void unsuccessfulOrderResponseThrowsVendorExceptionInsteadOfReturningEmptySuccess() throws Exception {
        TestVendorClient client = new TestVendorClient(new ObjectMapper().readTree("{\"code\":500}"), false);

        assertThrows(CpsVendorException.class,
                () -> client.queryOrders(new CpsOrderQueryRequest(), CpsVendorConfig.builder().build()));
    }

    @Test
    void transportExceptionThrowsVendorExceptionInsteadOfReturningEmptySuccess() {
        TestVendorClient client = new TestVendorClient(null, true);

        assertThrows(CpsVendorException.class,
                () -> client.queryOrders(new CpsOrderQueryRequest(), CpsVendorConfig.builder().build()));
    }

    @Test
    void rootMinIdIsUsedAsOrderPageCursor() throws Exception {
        TestVendorClient client = new TestVendorClient(
                new ObjectMapper().readTree("{\"code\":0,\"min_id\":\"9\",\"data\":[]}"), false);

        CpsOrderPageResult result = client.queryOrderPage(
                new CpsOrderQueryRequest(), CpsVendorConfig.builder().build());

        assertEquals("9", result.getNextCursor());
        assertTrue(result.isHasMore());
    }

    private static final class TestVendorClient extends AbstractApiVendorClient {
        private final JsonNode response;
        private final boolean fail;

        private TestVendorClient(JsonNode response, boolean fail) {
            this.response = response;
            this.fail = fail;
        }

        @Override public String getVendorCode() { return "test-vendor"; }
        @Override public String getPlatformCode() { return "test-platform"; }
        @Override public String getVendorType() { return "aggregator"; }
        @Override protected String getSearchApiPath() { return "/search"; }
        @Override protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected CpsGoodsSearchResult parseSearchResponse(JsonNode responseRoot, CpsGoodsSearchRequest request) { return null; }
        @Override protected String getPromotionLinkApiPath() { return "/link"; }
        @Override protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot) { return null; }
        @Override protected String getOrderQueryApiPath() { return "/orders"; }
        @Override protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot) { return List.of(); }
        @Override protected String getTestConnectionApiPath() { return "/test"; }
        @Override protected Map<String, Object> buildTestConnectionParams() { return Map.of(); }
        @Override protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
            if (fail) {
                throw new IllegalStateException("transport down");
            }
            return response;
        }
        @Override protected boolean isSuccessResponse(JsonNode root) { return root.path("code").asInt() == 0; }
    }
}
