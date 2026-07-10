package cn.didi.union.client.impl;

import cn.didi.union.auth.Auth;
import cn.didi.union.auth.Uuid;
import cn.didi.union.client.BasicClient;
import cn.didi.union.client.DunionClientException;
import cn.didi.union.models.DunionClientConfig;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BasicClientImpl implements BasicClient {
    private static final Logger LOG = LoggerFactory.getLogger(BasicClientImpl.class);
    private static final Gson GSON = new Gson();

    @Override
    public String doPost(DunionClientConfig config, String urlPath, int timeout, TreeMap<String, Object> params) {
        return execute(config, urlPath, timeout, params, true);
    }

    @Override
    public String doGet(DunionClientConfig config, String urlPath, int timeout, TreeMap<String, Object> params) {
        return execute(config, appendQuery(urlPath, params), timeout, params, false);
    }

    private String execute(DunionClientConfig config, String url, int timeout, TreeMap<String, Object> params, boolean post) {
        TreeMap<String, Object> signedHeaders = new TreeMap<>();
        signedHeaders.put("App-Key", config.getAppKey());
        signedHeaders.put("Timestamp", System.currentTimeMillis() / 1000);
        String requestId = Uuid.getUUID();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeout > 0 ? timeout : config.getReadTimeout()))
                .header("App-Key", config.getAppKey())
                .header("Timestamp", String.valueOf(signedHeaders.get("Timestamp")))
                .header("Sign", Auth.genSign(signedHeaders, params, config.getAccessKey()))
                .header("Didi-Header-Rid", requestId)
                .header("User-Agent", "dunion-java-sdk/1.3-agenticcps")
                .header("Accept", "application/json");
        if (post) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(params), StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new DunionClientException(response.statusCode(), "DUnion HTTP status " + response.statusCode()
                        + ", requestId=" + requestId);
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new DunionClientException("DUnion returned an empty response, requestId=" + requestId, null);
            }
            LOG.debug("DUnion request completed: path={}, requestId={}, status={}", URI.create(url).getPath(), requestId,
                    response.statusCode());
            return response.body();
        } catch (DunionClientException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DunionClientException("DUnion request interrupted, requestId=" + requestId, ex);
        } catch (Exception ex) {
            throw new DunionClientException("DUnion request failed, requestId=" + requestId + ": "
                    + ex.getClass().getSimpleName(), ex);
        }
    }

    private String appendQuery(String url, TreeMap<String, Object> params) {
        if (params.isEmpty()) return url;
        String query = params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
        return url + (url.contains("?") ? "&" : "?") + query;
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
