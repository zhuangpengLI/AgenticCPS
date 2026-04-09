package cn.iocoder.yudao.module.cps.client.common;

import cn.iocoder.yudao.module.cps.client.CpsApiVendorClient;
import cn.iocoder.yudao.module.cps.client.dto.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * API 供应商客户端抽象基类
 *
 * <p>提供所有供应商客户端的公共基础设施：
 * <ul>
 *   <li>ObjectMapper JSON 解析</li>
 *   <li>模板方法模式：统一执行流程 + 子类定制差异</li>
 *   <li>公共工具方法：空结果构建、安全数值解析</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Slf4j
public abstract class AbstractApiVendorClient implements CpsApiVendorClient {

    protected final ObjectMapper objectMapper;

    protected AbstractApiVendorClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ==================== 模板方法（商品搜索） ====================

    /** 子类实现：获取商品搜索 API 路径 */
    protected abstract String getSearchApiPath();

    /** 子类实现：构建商品搜索参数 */
    protected abstract Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config);

    /** 子类实现：解析商品搜索响应 */
    protected abstract CpsGoodsSearchResult parseSearchResponse(JsonNode responseRoot, CpsGoodsSearchRequest request);

    // ==================== 模板方法（推广转链） ====================

    /** 子类实现：获取推广转链 API 路径 */
    protected abstract String getPromotionLinkApiPath();

    /** 子类实现：构建推广转链参数 */
    protected abstract Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config);

    /** 子类实现：解析推广转链响应 */
    protected abstract CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode responseRoot);

    // ==================== 模板方法（订单查询） ====================

    /** 子类实现：获取订单查询 API 路径 */
    protected abstract String getOrderQueryApiPath();

    /** 子类实现：构建订单查询参数 */
    protected abstract Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config);

    /** 子类实现：解析订单查询响应 */
    protected abstract List<CpsOrderDTO> parseOrderQueryResponse(JsonNode responseRoot);

    // ==================== 模板方法（连接测试） ====================

    /** 子类实现：获取连接测试 API 路径 */
    protected abstract String getTestConnectionApiPath();

    /** 子类实现：构建连接测试参数 */
    protected abstract Map<String, Object> buildTestConnectionParams();

    // ==================== HTTP 执行（由子类实现） ====================

    /**
     * 执行 HTTP 请求并返回 JSON 响应
     *
     * @param path   API 路径
     * @param params 请求参数
     * @param config 供应商配置
     * @return JSON 响应根节点
     */
    protected abstract JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config);

    /**
     * 判断 API 响应是否成功
     *
     * @param root JSON 响应根节点
     * @return true-成功，false-失败
     */
    protected abstract boolean isSuccessResponse(JsonNode root);

    // ==================== 统一执行流程 ====================

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        try {
            String path = getSearchApiPath();
            Map<String, Object> params = buildSearchParams(request, config);
            JsonNode response = executeRequest(path, params, config);
            if (response == null || !isSuccessResponse(response)) {
                log.warn("[{}:{}] 搜索商品失败: {}", getVendorCode(), getPlatformCode(), response);
                return buildEmptyResult(request);
            }
            return parseSearchResponse(response, request);
        } catch (Exception e) {
            log.error("[{}:{}] 搜索商品异常: keyword={}", getVendorCode(), getPlatformCode(),
                    request.getKeyword(), e);
            return buildEmptyResult(request);
        }
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        try {
            String path = getPromotionLinkApiPath();
            Map<String, Object> params = buildPromotionLinkParams(request, config);
            JsonNode response = executeRequest(path, params, config);
            if (response == null || !isSuccessResponse(response)) {
                log.warn("[{}:{}] 转链失败: goodsId={}, response={}", getVendorCode(), getPlatformCode(),
                        request.getGoodsId(), response);
                return null;
            }
            return parsePromotionLinkResponse(response);
        } catch (Exception e) {
            log.error("[{}:{}] 转链异常: goodsId={}", getVendorCode(), getPlatformCode(),
                    request.getGoodsId(), e);
            return null;
        }
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request, CpsVendorConfig config) {
        try {
            String path = getOrderQueryApiPath();
            Map<String, Object> params = buildOrderQueryParams(request, config);
            JsonNode response = executeRequest(path, params, config);
            if (response == null || !isSuccessResponse(response)) {
                log.warn("[{}:{}] 查询订单失败: {}", getVendorCode(), getPlatformCode(), response);
                return Collections.emptyList();
            }
            return parseOrderQueryResponse(response);
        } catch (Exception e) {
            log.error("[{}:{}] 查询订单异常", getVendorCode(), getPlatformCode(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean testConnection(CpsVendorConfig config) {
        try {
            String path = getTestConnectionApiPath();
            Map<String, Object> params = buildTestConnectionParams();
            JsonNode response = executeRequest(path, params, config);
            return response != null && isSuccessResponse(response);
        } catch (Exception e) {
            log.warn("[{}:{}] 连接测试失败: {}", getVendorCode(), getPlatformCode(), e.getMessage());
            return false;
        }
    }

    // ==================== 公共工具方法 ====================

    /**
     * 构建空搜索结果
     */
    protected CpsGoodsSearchResult buildEmptyResult(CpsGoodsSearchRequest request) {
        return CpsGoodsSearchResult.builder()
                .list(Collections.emptyList())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    /**
     * 安全解析 JsonNode 字段为 BigDecimal
     */
    protected BigDecimal parseDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isNull() || field.isMissingNode()) {
            return null;
        }
        try {
            return new BigDecimal(field.asText());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全解析字符串为 BigDecimal
     */
    protected BigDecimal parseSafeDecimal(String str) {
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建带查询参数的 URL
     */
    protected String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        StringBuilder queryStr = new StringBuilder();
        params.forEach((k, v) -> {
            if (v != null) {
                if (queryStr.length() > 0) {
                    queryStr.append("&");
                }
                queryStr.append(k).append("=").append(v);
            }
        });
        return baseUrl + "?" + queryStr;
    }

}
