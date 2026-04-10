package com.qiji.cps.module.cps.client.official.pdd;

import com.qiji.cps.module.cps.client.common.AbstractOfficialVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 拼多多联盟官方 API 供应商客户端（骨架实现）
 *
 * <p>对接多多客开放平台官方 API。</p>
 * <p>认证方式：MD5签名</p>
 * <p>基础URL：https://gw-api.pinduoduo.com/api/router</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class PddOfficialVendorClient extends AbstractOfficialVendorClient {

    @Override
    public String getPlatformCode() { return CpsPlatformCodeEnum.PDD.getCode(); }

    @Override protected String getSearchApiPath() { return "/pdd.ddk.goods.search"; }
    @Override protected String getPromotionLinkApiPath() { return "/pdd.ddk.goods.promotion.url.generate"; }
    @Override protected String getOrderQueryApiPath() { return "/pdd.ddk.order.list.increment.get"; }
    @Override protected String getTestConnectionApiPath() { return "/pdd.ddk.goods.search"; }

    @Override
    protected Map<String, Object> buildSearchParams(CpsGoodsSearchRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", request.getKeyword());
        params.put("page", request.getPageNo());
        params.put("page_size", request.getPageSize());
        if (config.getDefaultAdzoneId() != null) { params.put("pid", config.getDefaultAdzoneId()); }
        return params;
    }

    @Override protected CpsGoodsSearchResult parseSearchResponse(JsonNode response, CpsGoodsSearchRequest request) {
        log.warn("[拼多多联盟官方] 商品搜索响应解析待实现"); return buildEmptyResult(request);
    }

    @Override
    protected Map<String, Object> buildPromotionLinkParams(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("goods_sign_list", List.of(request.getGoodsSign() != null ? request.getGoodsSign() : request.getGoodsId()));
        params.put("p_id", request.getAdzoneId() != null ? request.getAdzoneId() : config.getDefaultAdzoneId());
        return params;
    }

    @Override protected CpsPromotionLinkResult parsePromotionLinkResponse(JsonNode response) {
        log.warn("[拼多多联盟官方] 转链响应解析待实现"); return null;
    }

    @Override
    protected Map<String, Object> buildOrderQueryParams(CpsOrderQueryRequest request, CpsVendorConfig config) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start_update_time", request.getStartTime());
        params.put("end_update_time", request.getEndTime());
        params.put("page", request.getPageNo());
        params.put("page_size", request.getPageSize());
        return params;
    }

    @Override protected List<CpsOrderDTO> parseOrderQueryResponse(JsonNode response) {
        log.warn("[拼多多联盟官方] 订单查询响应解析待实现"); return Collections.emptyList();
    }

    @Override protected Map<String, Object> buildTestConnectionParams() {
        return Map.of("keyword", "手机", "page", 1, "page_size", 1);
    }

    @Override protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        log.warn("[拼多多联盟官方] HTTP请求执行待实现: path={}", path); return null;
    }

    @Override protected boolean isSuccessResponse(JsonNode root) {
        return root != null && root.path("error_response").isMissingNode();
    }

}
