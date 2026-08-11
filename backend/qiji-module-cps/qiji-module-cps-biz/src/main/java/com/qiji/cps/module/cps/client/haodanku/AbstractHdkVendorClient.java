package com.qiji.cps.module.cps.client.haodanku;

import com.qiji.cps.module.cps.client.common.AbstractAggregatorVendorClient;
import com.qiji.cps.module.cps.client.CpsVendorConfigField;
import com.qiji.cps.module.cps.client.CpsVendorConfigSchema;
import com.qiji.cps.module.cps.client.CpsVendorException;
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
 * <p>普通接口通过 apikey 鉴权；v3 REST 增值接口还需要其文档声明的 app_id/sign 等参数。</p>
 * <p>好单库的 v2/v3 分配不是按业务类型统一划分，具体域名由各平台适配器按接口路径解析。</p>
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
                CpsVendorConfigField.optional("authToken", true),
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
        if (code == 1 || code == 200) {
            return true;
        }
        // 部分订单接口以 code=0 + 暂无数据表示请求成功但结果为空，不能误判为鉴权或接口失败。
        String message = firstNonBlank(root == null ? null : root.path("msg").asText(null),
                root == null ? null : root.path("message").asText(null));
        return code == 0 && message != null && message.contains("暂无数据");
    }

    /**
     * 获取好单库转链API的基础URL
     *
     * <p>不同平台转链接口可能属于 v2 或 v3，交由具体接口路径的版本路由决定。</p>
     *
     * @param config 供应商配置
     * @return 转链API基础URL
     */
    protected String getPromotionLinkBaseUrl(CpsVendorConfig config) {
        return resolveApiBaseUrl(getPromotionLinkApiPath(), config);
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
     * 重写转链流程：好单库当前接入的转链接口使用 POST 方式
     *
     * <p>接口域名由 {@link #getPromotionLinkBaseUrl(CpsVendorConfig)} 按具体路径选择。</p>
     */
    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request, CpsVendorConfig config) {
        try {
            String path = getPromotionLinkApiPath();
            Map<String, Object> params = buildPromotionLinkParams(request, config);
            String fullUrl = getPromotionLinkBaseUrl(config) + path;
            JsonNode response = executePostRequest(fullUrl, params, config);
            if (response == null || !isSuccessResponse(response)) {
                return rejectPromotionLink(request, response);
            }
            return parsePromotionLinkResponse(response);
        } catch (CpsVendorException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}:{}] 转链异常: goodsId={}", getVendorCode(), getPlatformCode(),
                    request.getGoodsId(), e);
            return null;
        }
    }

}
