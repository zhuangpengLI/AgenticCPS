package com.qiji.cps.module.cps.client.dataoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtkJdVendorClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("testConnection uses the official Dataoke JD goods search path")
    void testConnection_usesOfficialJdGoodsSearchPath() throws Exception {
        TestDtkJdVendorClient client = new TestDtkJdVendorClient(
                OBJECT_MAPPER.readTree("{\"code\":0,\"data\":[]}"));

        boolean success = client.testConnection(CpsVendorConfig.builder().build());

        assertTrue(success);
        assertEquals("/dels/jd/goods/search", client.requestedPath);
        assertEquals("v1.0.0", client.requestedParams.get("version"));
        assertEquals(1, client.requestedParams.get("pageNo"));
        assertEquals(1, client.requestedParams.get("pageSize"));
    }

    private static class TestDtkJdVendorClient extends DtkJdVendorClient {

        private final JsonNode response;
        private String requestedPath;
        private Map<String, Object> requestedParams;

        TestDtkJdVendorClient(JsonNode response) {
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
