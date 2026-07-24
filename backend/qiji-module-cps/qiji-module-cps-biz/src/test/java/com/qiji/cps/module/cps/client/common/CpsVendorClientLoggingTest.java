package com.qiji.cps.module.cps.client.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CpsVendorClientLoggingTest {

    private static final String RESPONSE_SECRET = "sentinel-response-body";
    private static final String EXCEPTION_SECRET = "sentinel-exception-token";
    private static final String SIGNED_URL_SECRET = "sentinel-signed-url";
    private static final String APP_KEY_SECRET = "sentinel-app-key";

    @Test
    void vendorClientLogs_shouldNeverExposeResponsesExceptionsSignedUrlsOrCredentials() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level originalLevel = root.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);
        HttpServer server = null;
        try {
            JsonNode rejected = new ObjectMapper().readTree(
                    "{\"code\":500,\"token\":\"" + RESPONSE_SECRET + "\"}");
            TestApiVendorClient apiClient = new TestApiVendorClient(rejected, null);
            apiClient.searchGoods(new CpsGoodsSearchRequest(), config("http://example.invalid"));

            TestApiVendorClient failingClient = new TestApiVendorClient(
                    null, new IllegalStateException(EXCEPTION_SECRET));
            failingClient.testConnection(config("http://example.invalid"));

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/test", exchange -> {
                byte[] body = ("{\"code\":0,\"token\":\"" + RESPONSE_SECRET + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            TestAggregatorVendorClient aggregator = new TestAggregatorVendorClient();
            aggregator.executeGet(config("http://127.0.0.1:" + server.getAddress().getPort()));
            aggregator.executePost("http://127.0.0.1:1/test?token=" + SIGNED_URL_SECRET,
                    config("http://127.0.0.1:1"));

            String captured = captured(appender.list);
            assertFalse(captured.contains(RESPONSE_SECRET), captured);
            assertFalse(captured.contains(EXCEPTION_SECRET), captured);
            assertFalse(captured.contains(SIGNED_URL_SECRET), captured);
            assertFalse(captured.contains(APP_KEY_SECRET), captured);
        } finally {
            if (server != null) {
                server.stop(0);
            }
            root.detachAppender(appender);
            root.setLevel(originalLevel);
            appender.stop();
        }
    }

    private static CpsVendorConfig config(String baseUrl) {
        return CpsVendorConfig.builder()
                .apiBaseUrl(baseUrl)
                .appKey(APP_KEY_SECRET)
                .appSecret("sentinel-app-secret")
                .authToken("sentinel-auth-token")
                .build();
    }

    private static String captured(List<ILoggingEvent> events) {
        StringBuilder captured = new StringBuilder();
        for (ILoggingEvent event : events) {
            captured.append(event.getFormattedMessage()).append('\n');
            for (IThrowableProxy throwable = event.getThrowableProxy();
                 throwable != null; throwable = throwable.getCause()) {
                captured.append(throwable.getMessage()).append('\n');
            }
        }
        return captured.toString();
    }

    private static class TestApiVendorClient extends AbstractApiVendorClient {

        private final JsonNode response;
        private final RuntimeException failure;

        TestApiVendorClient(JsonNode response, RuntimeException failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override public String getVendorCode() { return "test-vendor"; }
        @Override public String getPlatformCode() { return "test-platform"; }
        @Override public String getVendorType() { return "aggregator"; }
        @Override protected String getSearchApiPath() { return "/search"; }
        @Override protected Map<String, Object> buildSearchParams(
                CpsGoodsSearchRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected CpsGoodsSearchResult parseSearchResponse(
                JsonNode responseRoot, CpsGoodsSearchRequest request) { return null; }
        @Override protected String getPromotionLinkApiPath() { return "/link"; }
        @Override protected Map<String, Object> buildPromotionLinkParams(
                CpsPromotionLinkRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot) { return null; }
        @Override protected String getOrderQueryApiPath() { return "/orders"; }
        @Override protected Map<String, Object> buildOrderQueryParams(
                CpsOrderQueryRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot) { return List.of(); }
        @Override protected String getTestConnectionApiPath() { return "/test"; }
        @Override protected Map<String, Object> buildTestConnectionParams() { return Map.of(); }
        @Override protected JsonNode executeRequest(
                String path, Map<String, Object> params, CpsVendorConfig config) {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
        @Override protected boolean isSuccessResponse(JsonNode root) {
            return root.path("code").asInt() == 0;
        }
    }

    private static final class TestAggregatorVendorClient extends AbstractAggregatorVendorClient {

        JsonNode executeGet(CpsVendorConfig config) {
            return executeRequest("/test", Map.of("appKey", APP_KEY_SECRET), config);
        }

        JsonNode executePost(String url, CpsVendorConfig config) {
            return executePostRequest(url, Map.of("appKey", APP_KEY_SECRET), config);
        }

        @Override protected Map<String, String> computeSignContext(
                Map<String, Object> params, CpsVendorConfig config) {
            return Map.of("sign", SIGNED_URL_SECRET);
        }
        @Override protected void injectSignParams(
                Map<String, Object> params, CpsVendorConfig config, Map<String, String> signContext) {
            params.putAll(signContext);
        }
        @Override public String getVendorCode() { return "test-aggregator"; }
        @Override public String getPlatformCode() { return "test-platform"; }
        @Override protected String getSearchApiPath() { return "/search"; }
        @Override protected Map<String, Object> buildSearchParams(
                CpsGoodsSearchRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected CpsGoodsSearchResult parseSearchResponse(
                JsonNode responseRoot, CpsGoodsSearchRequest request) { return null; }
        @Override protected String getPromotionLinkApiPath() { return "/link"; }
        @Override protected Map<String, Object> buildPromotionLinkParams(
                CpsPromotionLinkRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot) { return null; }
        @Override protected String getOrderQueryApiPath() { return "/orders"; }
        @Override protected Map<String, Object> buildOrderQueryParams(
                CpsOrderQueryRequest request, CpsVendorConfig config) { return Map.of(); }
        @Override protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot) { return List.of(); }
        @Override protected String getTestConnectionApiPath() { return "/test"; }
        @Override protected Map<String, Object> buildTestConnectionParams() { return Map.of(); }
        @Override protected boolean isSuccessResponse(JsonNode root) {
            return root.path("code").asInt() == 0;
        }
    }

}
