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

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        // 1. 复制参数，避免修改原始参数
        Map<String, Object> allParams = new LinkedHashMap<>(params);

        // 2. 计算签名上下文
        Map<String, String> signContext = computeSignContext(allParams, config);

        // 3. 注入签名参数
        injectSignParams(allParams, config, signContext);

        // 4. 构建 URL
        String url = config.getApiBaseUrl() + path;
        String fullUrl = buildUrlWithParams(url, allParams);

        // 5. 发起 HTTP GET 请求
        try {
            HttpResponse response = HttpRequest.get(fullUrl).timeout(HTTP_TIMEOUT).execute();
            String body = response.body();
            log.debug("[{}:{}] 请求: {} 响应: {}", getVendorCode(), getPlatformCode(), path, body);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[{}:{}] HTTP请求异常: path={}", getVendorCode(), getPlatformCode(), path, e);
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

        // 4. 发起 HTTP POST 请求（表单方式）
        try {
            HttpResponse response = HttpRequest.post(fullUrl)
                    .form(allParams)
                    .timeout(HTTP_TIMEOUT)
                    .execute();
            String body = response.body();
            log.debug("[{}:{}] POST请求: {} 响应: {}", getVendorCode(), getPlatformCode(), fullUrl, body);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("[{}:{}] HTTP POST请求异常: url={}", getVendorCode(), getPlatformCode(), fullUrl, e);
            return null;
        }
    }

}
