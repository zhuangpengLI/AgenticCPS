package com.qiji.cps.module.cps.client.dataoke;

import cn.hutool.crypto.digest.DigestUtil;
import com.qiji.cps.module.cps.client.common.AbstractAggregatorVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 大淘客聚合平台供应商客户端抽象基类
 *
 * <p>封装大淘客特有的 MD5 签名机制，所有通过大淘客对接的电商平台（淘宝/京东/拼多多）继承此类。</p>
 *
 * <p>签名规则：MD5(appKey={appKey}&timer={timestamp}&nonce={random6}&key={appSecret})，结果转大写。</p>
 *
 * @author CPS System
 */
@Slf4j
public abstract class AbstractDtkVendorClient extends AbstractAggregatorVendorClient {

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.DATAOKE.getCode();
    }

    @Override
    protected Map<String, String> computeSignContext(Map<String, Object> params, CpsVendorConfig config) {
        String timer = String.valueOf(System.currentTimeMillis());
        String nonce = String.valueOf(new Random().nextInt(900000) + 100000);
        String signSource = String.format("appKey=%s&timer=%s&nonce=%s&key=%s",
                config.getAppKey(), timer, nonce, config.getAppSecret());
        String sign = DigestUtil.md5Hex(signSource).toUpperCase();

        Map<String, String> context = new HashMap<>();
        context.put("timer", timer);
        context.put("nonce", nonce);
        context.put("sign", sign);
        return context;
    }

    @Override
    protected void injectSignParams(Map<String, Object> params, CpsVendorConfig config,
                                    Map<String, String> signContext) {
        params.put("appKey", config.getAppKey());
        params.put("timer", signContext.get("timer"));
        params.put("nonce", signContext.get("nonce"));
        params.put("signRan", signContext.get("sign"));
    }

    @Override
    protected JsonNode executeRequest(String path, Map<String, Object> params, CpsVendorConfig config) {
        return unwrapResponse(super.executeRequest(path, params, config));
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        JsonNode response = unwrapResponse(root);
        return response != null && "0".equals(response.path("code").asText());
    }

    /**
     * 大淘客新旧接口响应结构不完全一致：
     * <ul>
     *   <li>旧结构：{code, msg, data}</li>
     *   <li>新结构：{status, data: {code, msg, data}}</li>
     * </ul>
     * 统一拆到包含 code/msg/data 的业务响应层，避免各业务解析器重复判断。
     */
    protected JsonNode unwrapResponse(JsonNode root) {
        if (root != null && root.has("status") && root.path("data").has("code")) {
            return root.path("data");
        }
        return root;
    }

    protected String getExtraConfig(CpsVendorConfig config, String key) {
        return config != null && config.getExtraConfig() != null ? config.getExtraConfig().get(key) : null;
    }

    protected String firstNonBlankExtraConfig(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

}
