package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.common.AbstractAggregatorVendorClient;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 好单库聚合平台供应商客户端抽象基类
 *
 * <p>封装好单库特有的 apikey 鉴权机制，所有通过好单库对接的电商平台（淘宝/京东/拼多多）继承此类。</p>
 *
 * <p>鉴权方式：无签名，通过 apikey 参数传递认证信息</p>
 * <p>商品搜索基础URL：http://v2.api.haodanku.com</p>
 * <p>推广转链基础URL：http://v3.api.haodanku.com（POST方式）</p>
 *
 * @author CPS System
 */
@Slf4j
public abstract class AbstractHdkVendorClient extends AbstractAggregatorVendorClient {

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.HAODANKU.getCode();
    }

    /**
     * 好单库无需签名，直接传递 apikey
     */
    @Override
    protected Map<String, String> computeSignContext(Map<String, Object> params, CpsVendorConfig config) {
        // 好单库无签名计算，返回空上下文
        return new HashMap<>();
    }

    /**
     * 好单库注入 apikey 参数
     */
    @Override
    protected void injectSignParams(Map<String, Object> params, CpsVendorConfig config,
                                    Map<String, String> signContext) {
        params.put("apikey", config.getAppKey());
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        // 好单库返回格式：{"code": 1, "msg": "success", "data": {...}}
        // code=1 表示成功
        return root != null && root.path("code").asInt(-1) == 1;
    }

    /**
     * 获取好单库转链API的基础URL
     *
     * <p>好单库转链API使用 v3 域名，与商品搜索的 v2 域名不同。
     * 此方法将配置中的 v2 URL 自动转换为 v3。</p>
     *
     * @param config 供应商配置
     * @return 转链API基础URL
     */
    protected String getPromotionLinkBaseUrl(CpsVendorConfig config) {
        String baseUrl = config.getApiBaseUrl();
        if (baseUrl != null && baseUrl.contains("v2.api.haodanku.com")) {
            return baseUrl.replace("v2.api.haodanku.com", "v3.api.haodanku.com");
        }
        return baseUrl;
    }

    /**
     * 重写转链流程：好单库转链API需要使用 POST 方式和 v3 域名
     *
     * <p>好单库的转链接口（/ratesurl）与商品搜索接口有两点关键差异：
     * <ul>
     *   <li>HTTP 方法：使用 POST（非 GET）</li>
     *   <li>域名：使用 v3.api.haodanku.com（非 v2）</li>
     * </ul>
     * </p>
     */
    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        try {
            String path = getPromotionLinkApiPath();
            Map<String, Object> params = buildPromotionLinkParams(request, config);
            String fullUrl = getPromotionLinkBaseUrl(config) + path;
            JsonNode response = executePostRequest(fullUrl, params, config);
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

}
