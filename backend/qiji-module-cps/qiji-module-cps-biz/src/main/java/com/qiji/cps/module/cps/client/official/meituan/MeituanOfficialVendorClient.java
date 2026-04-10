package com.qiji.cps.module.cps.client.official.meituan;

import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 美团联盟官方 API 供应商客户端（骨架实现）
 *
 * <p>对接美团联盟开放平台官方 API。</p>
 * <p>认证方式：签名</p>
 * <p>基础URL：https://union.meituan.com/api</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class MeituanOfficialVendorClient extends AbstractOfficialVendorClient {

    @Override
    public String getPlatformCode() { return CpsPlatformCodeEnum.MEITUAN.getCode(); }

    @Override protected String getSearchApiPath() { return "/goods/search"; }
    @Override protected String getPromotionLinkApiPath() { return "/link/generate"; }
    @Override protected String getOrderQueryApiPath() { return "/order/list"; }
    @Override protected String getTestConnectionApiPath() { return "/goods/search"; }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        return params;
    }

    @Override protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        log.warn("[美团联盟官方] 商品搜索响应解析待实现"); return buildEmptyResult(request);
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        return Map.of("goodsId", request.getGoodsId());
    }

    @Override protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        log.warn("[美团联盟官方] 转链响应解析待实现"); return null;
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startTime", request.getStartTime());
        params.put("endTime", request.getEndTime());
        params.put("page", request.getPageNo());
        params.put("pageSize", request.getPageSize());
        return params;
    }

    @Override protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        log.warn("[美团联盟官方] 订单查询响应解析待实现"); return Collections.emptyList();
    }

    @Override protected Map<String, Object> buildTestConnectionParams() {
        return Map.of("keyword", "美食", "page", 1, "pageSize", 1);
    }

    @Override protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        log.warn("[美团联盟官方] HTTP请求执行待实现: path={}", path); return null;
    }

    @Override protected boolean isSuccessResponse(JsonNode root) {
        return root != null && root.path("code").asInt(-1) == 0;
    }

}
