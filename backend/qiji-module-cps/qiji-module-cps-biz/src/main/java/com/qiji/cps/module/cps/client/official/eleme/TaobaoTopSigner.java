package com.qiji.cps.module.cps.client.official.eleme;

import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 淘宝开放平台 (TOP) 签名工具类
 *
 * <p>遵循淘宝开放平台 TOP 协议 MD5 签名规范：
 * <ol>
 *   <li>将所有请求参数（公共系统参数与业务参数，排除 sign 本身）按参数名 ASCII 升序排列；</li>
 *   <li>将排序后的参数名和参数值拼接：secret + key1 + val1 + key2 + val2 ... + secret；</li>
 *   <li>计算拼接后字符串的 MD5 摘要，并转换为 32 位大写十六进制字符串。</li>
 * </ol>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
public final class TaobaoTopSigner {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TaobaoTopSigner() {
    }

    /**
     * 计算 TOP MD5 签名
     *
     * @param params 请求参数键值对
     * @param secret 应用密钥 (AppSecret)
     * @return 32 位大写 MD5 签名字符串
     */
    public static String sign(Map<String, String> params, String secret) {
        if (params == null || secret == null) {
            throw new IllegalArgumentException("params and secret must not be null");
        }
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder query = new StringBuilder(secret);
        for (String key : keys) {
            if ("sign".equalsIgnoreCase(key)) {
                continue;
            }
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                query.append(key).append(value);
            }
        }
        query.append(secret);

        return md5Hex(query.toString()).toUpperCase(Locale.ROOT);
    }

    /**
     * 组装包含公共参数与签名的完整请求参数字典
     *
     * @param method    API 接口名称，例如 alibaba.alsc.union.eleme.promotion.officialactivity.get
     * @param appKey    应用 AppKey
     * @param appSecret 应用 AppSecret
     * @param bizParams 业务参数键值对
     * @return 包含系统公共参数和 sign 的完整参数 Map
     */
    public static Map<String, String> buildSignedParams(String method,
                                                        String appKey,
                                                        String appSecret,
                                                        Map<String, String> bizParams) {
        Map<String, String> fullParams = new LinkedHashMap<>();
        fullParams.put("method", method);
        fullParams.put("app_key", appKey);
        fullParams.put("timestamp", LocalDateTime.now().format(DATE_TIME_FORMATTER));
        fullParams.put("format", "json");
        fullParams.put("v", "2.0");
        fullParams.put("sign_method", "md5");

        if (bizParams != null) {
            for (Map.Entry<String, String> entry : bizParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    fullParams.put(entry.getKey(), entry.getValue());
                }
            }
        }

        String signature = sign(fullParams, appSecret);
        fullParams.put("sign", signature);
        return fullParams;
    }

    /**
     * 将参数 Map 转换为 application/x-www-form-urlencoded 格式字符串
     *
     * @param params 参数 Map
     * @return URL 编码后的请求体字符串
     */
    public static String buildFormUrlEncoded(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            } else {
                first = false;
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue() != null ? entry.getValue() : "", StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate MD5 digest", e);
        }
    }
}
