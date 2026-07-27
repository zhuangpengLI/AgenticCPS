package com.qiji.cps.module.cps.client.dataoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtkTaobaoVendorClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("testConnection - 使用大淘客超级分类正确路径")
    void testConnection_usesCategorySuperCategoryPath() throws Exception {
        TestDtkTaobaoVendorClient client = new TestDtkTaobaoVendorClient(
                OBJECT_MAPPER.readTree("{\"code\":0,\"data\":[]}"));

        boolean success = client.testConnection(CpsVendorConfig.builder().build());

        assertTrue(success);
        assertEquals("/category/get-super-category", client.requestedPath);
        assertEquals("v1.1.0", client.requestedParams.get("version"));
    }

    @Test
    @DisplayName("getSelectionMeta - 使用大淘客超级分类正确路径")
    void getSelectionMeta_usesCategorySuperCategoryPath() throws Exception {
        TestDtkTaobaoVendorClient client = new TestDtkTaobaoVendorClient(
                OBJECT_MAPPER.readTree("{\"code\":0,\"data\":[]}"));

        client.getSelectionMeta(CpsVendorConfig.builder().build());

        assertEquals("/category/get-super-category", client.requestedPath);
        assertEquals("v1.1.0", client.requestedParams.get("version"));
    }

    private static class TestDtkTaobaoVendorClient extends DtkTaobaoVendorClient {

        private final JsonNode response;
        private String requestedPath;
        private Map<String, Object> requestedParams;

        TestDtkTaobaoVendorClient(JsonNode response) {
            this.response = response;
        }

        @Override
        protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
            this.requestedPath = path;
            this.requestedParams = params;
            return response;
        }
    }
}
