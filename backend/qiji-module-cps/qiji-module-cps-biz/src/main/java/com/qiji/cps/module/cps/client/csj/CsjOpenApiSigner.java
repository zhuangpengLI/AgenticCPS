package com.qiji.cps.module.cps.client.csj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * 穿山甲电商联盟（CSJ Pangolin E-commerce）OpenAPI 签名计算与报文包装器
 *
 * <p>遵循穿山甲官方电商联盟（如《订单查询接口》28733）安全规范：
 * <ul>
 *   <li>公共参数：app_id, timestamp, version, req_id, sign_type, data</li>
 *   <li>业务参数载荷：data 字段为紧凑 JSON 字符串</li>
 *   <li>签名计算（sign）：参与参数按 ASCII 字典升序拼接为 key1=val1&key2=val2...，末尾直接拼装 secure_key，计算 32 位小写 MD5</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
public final class CsjOpenApiSigner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CsjOpenApiSigner() {
    }

    /**
     * 计算穿山甲 OpenAPI MD5 签名
     *
     * @param params    所有入参键值对（内部会自动过滤 null、空值及 "sign" 字段本身）
     * @param secureKey 穿山甲媒体平台分配的安全密钥（secure_key）
     * @return 32 位小写 MD5 签名字符串
     */
    public static String generateSign(Map<String, Object> params, String secureKey) {
        if (params == null || !StringUtils.hasText(secureKey)) {
            throw new IllegalArgumentException("Params and secureKey must not be empty for CSJ sign calculation");
        }

        // 1. 过滤并按 ASCII 字典序排序
        List<String> sortedKeys = new ArrayList<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getKey() == null || "sign".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null) {
                continue;
            }
            sortedKeys.add(entry.getKey());
        }
        Collections.sort(sortedKeys);

        // 2. 拼接 key=value 键值串
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : sortedKeys) {
            Object val = params.get(key);
            String strVal = (val instanceof String) ? (String) val : compactJson(val);
            if (!first) {
                sb.append('&');
            }
            sb.append(key).append('=').append(strVal);
            first = false;
        }

        // 3. 末尾直接拼接 secure_key
        sb.append(secureKey);

        // 4. 计算 MD5
        return md5Hex(sb.toString());
    }

    /**
     * 构建标准请求报文 Envelope，包含公共参数与计算好的 MD5 sign
     *
     * @param appId     穿山甲媒体应用 ID (app_id)
     * @param secureKey 穿山甲安全密钥 (secure_key)
     * @param dataObj   业务参数对象（或已经序列化的 JSON 字符串）
     * @return 包含公共参数与 sign 的完整请求 Body Map
     */
    public static Map<String, Object> buildEnvelope(String appId, String secureKey, Object dataObj) {
        return buildEnvelope(appId, secureKey, dataObj, UUID.randomUUID().toString(), Instant.now().getEpochSecond());
    }

    /**
     * 构建标准请求报文 Envelope（可指定 reqId 与 timestamp，供测试与重放诊断使用）
     */
    public static Map<String, Object> buildEnvelope(String appId, String secureKey, Object dataObj,
                                                    String reqId, long timestamp) {
        String dataStr = (dataObj instanceof String s) ? s : compactJson(dataObj);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("app_id", appId != null ? appId.trim() : "");
        envelope.put("timestamp", timestamp);
        envelope.put("version", "1");
        envelope.put("req_id", reqId);
        envelope.put("sign_type", "MD5");
        envelope.put("data", dataStr);

        String sign = generateSign(envelope, secureKey != null ? secureKey.trim() : "");
        envelope.put("sign", sign);
        return envelope;
    }

    /**
     * 将对象紧凑序列化为 JSON 字符串（无额外空白字符）
     */
    public static String compactJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize CSJ data object to compact JSON", e);
        }
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
