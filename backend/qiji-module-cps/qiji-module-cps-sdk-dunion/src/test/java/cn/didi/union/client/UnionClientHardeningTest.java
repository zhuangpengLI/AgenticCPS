package cn.didi.union.client;

import cn.didi.union.client.impl.BasicClientImpl;
import cn.didi.union.models.DunionClientConfig;
import cn.didi.union.models.LinkResponse;
import cn.didi.union.models.Result;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class UnionClientHardeningTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldEncodeGetParametersAndUseConfiguredServer() throws Exception {
        server = server(exchange -> {
            byte[] body = exchange.getRequestURI().getRawQuery().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        DunionClientConfig config = config(1000);
        TreeMap<String, Object> params = new TreeMap<>();
        params.put("keyword", "a+b & 中");

        String response = new BasicClientImpl().doGet(config, config.getBaseUrl() + "/echo", 0, params);

        assertEquals("keyword=a%2Bb+%26+%E4%B8%AD", response);
    }

    @Test
    void shouldRejectNon200Response() throws Exception {
        server = server(exchange -> {
            byte[] body = "unavailable".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThrows(DunionClientException.class,
                () -> new BasicClientImpl().doGet(config(1000), url("/failed"), 0, new TreeMap<>()));
    }

    @Test
    void shouldTreatBusinessErrnoAsFailure() throws Exception {
        server = server(exchange -> {
            byte[] body = "{\"errno\":1001,\"errmsg\":\"invalid promotion\",\"traceid\":\"t-1\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        UnionClient client = DunionClientFactory.build(config(1000)).getUnionClient();

        Result<LinkResponse> result = client.generateH5Link(1, 2, "ops-test", 0);

        assertFalse(result.isSuccess());
        assertEquals(1001, result.getError().getCode());
        assertFalse(result.getError().getMessage().contains("secret-key"));
    }

    @Test
    void shouldApplyReadTimeout() throws Exception {
        server = server(exchange -> {
            try {
                Thread.sleep(300);
                exchange.sendResponseHeaders(200, 0);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });

        assertThrows(DunionClientException.class,
                () -> new BasicClientImpl().doGet(config(100), url("/slow"), 0, new TreeMap<>()));
    }

    @Test
    void shouldMaskAccessKeyInConfigText() throws Exception {
        server = server(exchange -> exchange.close());
        String text = config(1000).toString();
        assertFalse(text.contains("secret-key"));
        assertTrue(text.contains("accessKey=***"));
    }

    private DunionClientConfig config(int timeout) {
        return DunionClientConfig.builder()
                .appKey("test-app")
                .accessKey("secret-key")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .build();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", handler);
        httpServer.start();
        return httpServer;
    }
}
