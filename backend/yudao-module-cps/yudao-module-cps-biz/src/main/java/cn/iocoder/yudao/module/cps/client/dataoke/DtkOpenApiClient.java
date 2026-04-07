package cn.iocoder.yudao.module.cps.client.dataoke;

import cn.iocoder.yudao.module.cps.dal.dataobject.platform.CpsPlatformDO;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 大淘客开放平台 HTTP 客户端（轻量封装，无需引入 SDK jar）
 *
 * <p>大淘客 API 签名规则：MD5(appKey=xxx&timer=xxx&nonce=xxx + appSecret)</p>
 *
 * @author CPS System
 */
@Slf4j
public class DtkOpenApiClient {

    private static final String BASE_URL = "https://openapi.dataoke.com/api";

    private final String appKey;
    private final String appSecret;
    private final ObjectMapper objectMapper;

    public DtkOpenApiClient(CpsPlatformDO platform) {
        this.appKey = platform.getAppKey();
        this.appSecret = platform.getAppSecret();
        this.objectMapper = new ObjectMapper();
    }

    public DtkOpenApiClient(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行 GET 请求到大淘客开放平台
     *
     * @param path      接口路径，如 /goods/get-dtk-search-goods
     * @param params    业务参数（不含签名公共参数）
     * @param typeRef   响应类型引用
     * @return 解析后的响应对象
     */
    public <T> T execute(String path, Map<String, Object> params, TypeReference<T> typeRef) {
        // 构建公共参数
        Map<String, Object> allParams = new LinkedHashMap<>(params);
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.format("%06d", new Random().nextInt(1000000));
        String urlParams = String.format("appKey=%s&timer=%s&nonce=%s", appKey, timer, nonce);
        String sign = DigestUtil.md5Hex(urlParams + appSecret).toLowerCase();

        allParams.put("appKey", appKey);
        allParams.put("timer", timer);
        allParams.put("nonce", nonce);
        allParams.put("signRan", sign);

        // 过滤掉值为 null 的参数
        allParams.entrySet().removeIf(e -> e.getValue() == null);

        // 将所有参数值转为字符串
        Map<String, Object> strParams = new LinkedHashMap<>();
        allParams.forEach((k, v) -> strParams.put(k, String.valueOf(v)));

        // 构建查询字符串
        StringBuilder queryStr = new StringBuilder();
        strParams.forEach((k, v) -> {
            if (v != null) {
                if (queryStr.length() > 0) queryStr.append("&");
                queryStr.append(k).append("=").append(v);
            }
        });
        String url = BASE_URL + path + "?" + queryStr;
        try {
            HttpResponse response = HttpRequest.get(url).timeout(5000).execute();
            String responseBody = response.body();
            log.debug("[DtkOpenApiClient] 请求: {} params={} 响应: {}", path, params, responseBody);
            return objectMapper.readValue(responseBody, typeRef);
        } catch (Exception e) {
            log.error("[DtkOpenApiClient] 请求异常: path={}, error={}", path, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 测试连接（调用一个轻量接口）
     */
    public boolean testConnection() {
        try {
            // 使用超级分类接口测试连通性（轻量无参）
            Map<String, Object> params = new HashMap<>();
            Map<String, Object> result = execute("/goods/get-super-category", params,
                    new TypeReference<Map<String, Object>>() {});
            return result != null;
        } catch (Exception e) {
            log.warn("[DtkOpenApiClient] 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

}
