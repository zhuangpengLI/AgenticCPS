package com.qiji.cps.module.cps.client.common;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聚合平台供应商客户端抽象基类
 *
 * <p>封装聚合平台（大淘客、好单库、喵有卷、实惠猪等）的通用逻辑：
 * <ul>
 *   <li>签名计算和注入（由子类定制策略）</li>
 *   <li>统一 HTTP GET 请求执行</li>
 *   <li>JSON 响应解析</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
public abstract class AbstractAggregatorVendorClient extends AbstractApiVendorClient {

    /** HTTP 请求超时时间（毫秒） */
    protected static final int HTTP_TIMEOUT = 5000;

    @Override
    public String getVendorType() {
        return "aggregator";
    }

    /**
     * 子类实现：计算签名
     *
     * @param params 请求参数
     * @param config 供应商配置
     * @return 签名上下文（包含签名值和过程参数，如 timer、nonce）
     */
    protected abstract Map<String, String> computeSignContext(Map<String, Object> params, CpsVendorConfig config);

    /**
     * 子类实现：将签名参数注入请求参数
     *
     * @param params      请求参数（会被修改）
     * @param config      供应商配置
     * @param signContext 签名上下文
     */
    protected abstract void injectSignParams(Map<String, Object> params, CpsVendorConfig config,
                                             Map<String, String> signContext);

    /**
     * Resolve the base URL used by GET requests. Vendor adapters may override this
     * to normalize legacy endpoints without mutating the persisted configuration.
     */
    protected String resolveApiBaseUrl(CpsVendorConfig config) {
        return config.getApiBaseUrl();
    }

    /**
     * Resolve the base URL for a concrete GET endpoint. Most vendors use one
     * host for every GET request; adapters with legacy endpoint splits can
     * override this hook without affecting their other operations.
     */
    protected String resolveApiBaseUrl(String path, CpsVendorConfig config) {
        return resolveApiBaseUrl(config);
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        // 1. 复制参数，避免修改原始参数
        Map<String, Object> allParams = new LinkedHashMap<>(params);

        // 2. 计算签名上下文
        Map<String, String> signContext = computeSignContext(allParams, config);

        // 3. 注入签名参数
        injectSignParams(allParams, config, signContext);
        allParams.entrySet().removeIf(entry -> entry.getValue() == null);

        // 4. 构建 URL
        String url = resolveApiBaseUrl(path, config) + path;
        String fullUrl = buildUrlWithParams(url, allParams);

        // 5. 发起 HTTP GET 请求
        try {
            HttpResponse response = HttpRequest.get(fullUrl).timeout(HTTP_TIMEOUT).execute();
            String body = response.body();
            log.debug("[{}:{}] 请求完成: path={}, status={}",
                    getVendorCode(), getPlatformCode(), path, response.getStatus());
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[{}:{}] HTTP请求异常: path={}, type={}",
                    getVendorCode(), getPlatformCode(), path, e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 执行 HTTP POST 请求（表单提交方式）
     *
     * <p>部分聚合平台的特定接口（如好单库转链API）要求 POST 方式提交参数。</p>
     *
     * @param fullUrl 完整请求URL（含基础URL和路径，不含参数）
     * @param params  请求参数（将注入签名参数后以表单方式提交）
     * @param config  供应商配置
     * @return JSON 响应根节点
     */
    protected JsonNode executePostRequest(String fullUrl, Map<String, Object> params, CpsVendorConfig config) {
        // 1. 复制参数，避免修改原始参数
        Map<String, Object> allParams = new LinkedHashMap<>(params);

        // 2. 计算签名上下文
        Map<String, String> signContext = computeSignContext(allParams, config);

        // 3. 注入签名参数
        injectSignParams(allParams, config, signContext);
        allParams.entrySet().removeIf(entry -> entry.getValue() == null);

        // 4. 发起 HTTP POST 请求（表单方式）
        try {
            HttpResponse response = HttpRequest.post(fullUrl)
                    .form(allParams)
                    .timeout(HTTP_TIMEOUT)
                    .execute();
            String body = response.body();
            log.debug("[{}:{}] POST请求完成: status={}",
                    getVendorCode(), getPlatformCode(), response.getStatus());
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[{}:{}] HTTP POST请求异常: type={}",
                    getVendorCode(), getPlatformCode(), e.getClass().getSimpleName());
            return null;
        }
    }

}
