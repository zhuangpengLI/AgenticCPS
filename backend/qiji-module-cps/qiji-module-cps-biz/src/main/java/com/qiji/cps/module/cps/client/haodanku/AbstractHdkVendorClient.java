package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.common.AbstractAggregatorVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 好单库聚合平台供应商客户端抽象基类
 *
 * <p>封装好单库特有的 apikey 鉴权机制，所有通过好单库对接的电商平台（淘宝/京东/拼多多）继承此类。</p>
 *
 * <p>鉴权方式：无签名，通过 apikey 参数传递认证信息</p>
 * <p>商品搜索基础URL：https://v2.api.haodanku.com</p>
 * <p>推广转链基础URL：https://v3.api.haodanku.com（POST方式）</p>
 *
 * @author CPS System
 */
@Slf4j
public abstract class AbstractHdkVendorClient extends AbstractAggregatorVendorClient {

    @Override
    public String getVendorCode() {
        return CpsVendorCodeEnum.HAODANKU.getCode();
    }

    @Override
    public CpsVendorConfigSchema getConfigSchema() {
        return new CpsVendorConfigSchema(List.of(
                CpsVendorConfigField.required("appKey", true),
                CpsVendorConfigField.required("apiBaseUrl", false),
                CpsVendorConfigField.optional("defaultAdzoneId", false),
                CpsVendorConfigField.optional("timeoutMs", false),
                CpsVendorConfigField.optional("rateLimitPerMinute", false),
                CpsVendorConfigField.optional("retryMaxAttempts", false)
        ));
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
    protected String resolveApiBaseUrl(CpsVendorConfig config) {
        String baseUrl = super.resolveApiBaseUrl(config);
        if (baseUrl != null && (baseUrl.startsWith("http://v2.api.haodanku.com")
                || baseUrl.startsWith("http://v3.api.haodanku.com"))) {
            return "https://" + baseUrl.substring("http://".length());
        }
        return baseUrl;
    }

    @Override
    protected boolean isSuccessResponse(JsonNode root) {
        // 好单库不同接口成功码不完全一致：淘宝商品/转链常见 code=1，京东/PDD/本地生活 v3 接口常见 code=200。
        int code = root == null ? -1 : root.path("code").asInt(-1);
        return code == 1 || code == 200;
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
        String baseUrl = resolveApiBaseUrl(config);
        if (baseUrl != null && baseUrl.contains("v2.api.haodanku.com")) {
            return baseUrl.replace("v2.api.haodanku.com", "v3.api.haodanku.com");
        }
        return baseUrl;
    }

    /**
     * 好单库部分接口把业务字段直接放在根节点，部分接口放在 data 节点。
     */
    protected JsonNode hdkPayload(JsonNode response) {
        JsonNode data = response == null ? null : response.path("data");
        return data != null && !data.isMissingNode() && !data.isNull() ? data : response;
    }

    protected String getExtraConfig(CpsVendorConfig config, String key) {
        return config != null && config.getExtraConfig() != null ? config.getExtraConfig().get(key) : null;
    }

    protected String firstNonBlank(String... values) {
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

    protected String firstText(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                String value = field.asText(null);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    protected BigDecimal firstDecimal(JsonNode node, String... fieldNames) {
        if (fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            BigDecimal value = parseDecimal(node, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected BigDecimal firstNonZeroDecimal(JsonNode node, String... fieldNames) {
        BigDecimal firstValue = null;
        if (fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            BigDecimal value = parseDecimal(node, fieldName);
            if (value == null) {
                continue;
            }
            if (firstValue == null) {
                firstValue = value;
            }
            if (BigDecimal.ZERO.compareTo(value) != 0) {
                return value;
            }
        }
        return firstValue;
    }

    protected Integer firstInt(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                return field.asInt();
            }
        }
        return null;
    }

    protected Map<String, Object> selectedFields(JsonNode node, String... fieldNames) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (node == null || fieldNames == null) {
            return fields;
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                fields.put(fieldName, field.asText());
            }
        }
        return fields;
    }

    protected Integer mapHdkOrderStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> -1;
            case 4 -> 4;
            default -> status;
        };
    }

    protected String toHdkUnixSeconds(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.matches("\\d+")) {
            return value;
        }
        LocalDateTime dateTime = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.valueOf(dateTime.atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond());
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
