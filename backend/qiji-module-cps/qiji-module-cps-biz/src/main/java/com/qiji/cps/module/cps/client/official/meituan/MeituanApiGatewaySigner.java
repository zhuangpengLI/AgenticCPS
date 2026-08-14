package com.qiji.cps.module.cps.client.official.meituan;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 美团 API Gateway 签名实现，规则与官方 sign-java 示例保持一致。
 *
 * <p>签名串为：HTTP_METHOD + "\\n" + Content-MD5 + "\\n" +
 * 已签名请求头 + resource。请求体通过 Content-MD5 参与签名，resource
 * 只包含 path 和按字典序拼接的 query 参数。</p>
 */
public final class MeituanApiGatewaySigner {

    private MeituanApiGatewaySigner() {
    }

    public static String sign(String secret, String method, String path,
                               Map<String, String> headers,
                               Map<String, String> queryParams,
                               List<String> signatureHeaders) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(
                    buildStringToSign(method, path, headers, queryParams, signatureHeaders)
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("美团 API Gateway 签名失败", e);
        }
    }

    static String buildStringToSign(String method, String path,
                                     Map<String, String> headers,
                                     Map<String, String> queryParams,
                                     List<String> signatureHeaders) {
        StringBuilder result = new StringBuilder(method.toUpperCase()).append('\n');
        result.append(headers == null ? "" : value(headers, "Content-MD5")).append('\n');

        if (headers != null && signatureHeaders != null) {
            List<String> names = new ArrayList<>(signatureHeaders);
            names.removeIf(name -> name == null || name.isBlank()
                    || "S-Ca-Signature".equalsIgnoreCase(name)
                    || "Content-MD5".equalsIgnoreCase(name)
                    || "S-Ca-Signature-Headers".equalsIgnoreCase(name));
            names.sort(String.CASE_INSENSITIVE_ORDER);
            for (String name : names) {
                result.append(name).append(':').append(valueIgnoreCase(headers, name)).append('\n');
            }
        }

        result.append(path == null ? "" : path);
        if (queryParams != null && queryParams.entrySet().stream()
                .anyMatch(entry -> entry.getKey() != null && !entry.getKey().isBlank())) {
            result.append('?');
            boolean first = true;
            Map<String, String> sortedQuery = new TreeMap<>();
            queryParams.forEach((key, value) -> {
                if (key != null && !key.isBlank()) {
                    sortedQuery.put(key, value);
                }
            });
            for (Map.Entry<String, String> entry : sortedQuery.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                if (!first) {
                    result.append('&');
                }
                result.append(entry.getKey());
                if (entry.getValue() != null && !entry.getValue().isBlank()) {
                    result.append('=').append(entry.getValue());
                }
                first = false;
            }
        }
        return result.toString();
    }

    static String signatureHeaderValue(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>(headers);
        names.removeIf(name -> name == null || name.isBlank());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(",", names);
    }

    private static String value(Map<String, String> headers, String name) {
        return valueIgnoreCase(headers, name);
    }

    private static String valueIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue() == null ? "" : entry.getValue();
            }
        }
        return "";
    }
}
