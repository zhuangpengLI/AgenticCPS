package cn.iocoder.yudao.module.cps.client.dataoke;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.cps.client.common.AbstractAggregatorVendorClient;
import cn.iocoder.yudao.module.cps.client.dto.CpsVendorConfig;
import cn.iocoder.yudao.module.cps.enums.CpsVendorCodeEnum;
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
 * <p>签名规则：MD5(appKey={appKey}&timer={timestamp}&nonce={random6} + appSecret)</p>
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
        String nonce = String.format("%06d", new Random().nextInt(1000000));
        String urlParamStr = String.format("appKey=%s&timer=%s&nonce=%s", config.getAppKey(), timer, nonce);
        String sign = DigestUtil.md5Hex(urlParamStr + config.getAppSecret()).toLowerCase();

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
    protected boolean isSuccessResponse(JsonNode root) {
        return root != null && "0".equals(root.path("code").asText());
    }

}
