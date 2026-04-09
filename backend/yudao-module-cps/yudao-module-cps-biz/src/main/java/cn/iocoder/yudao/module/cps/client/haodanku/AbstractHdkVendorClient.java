package cn.iocoder.yudao.module.cps.client.haodanku;

import cn.iocoder.yudao.module.cps.client.common.AbstractAggregatorVendorClient;
import cn.iocoder.yudao.module.cps.client.dto.CpsVendorConfig;
import cn.iocoder.yudao.module.cps.enums.CpsVendorCodeEnum;
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
 * <p>基础URL：http://v2.api.haodanku.com</p>
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

}
