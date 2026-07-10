package com.qiji.cps.module.cps.client.didi;

import cn.didi.union.client.DunionClientFactory;
import cn.didi.union.client.UnionClient;
import cn.didi.union.models.DunionClientConfig;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DidiUnionClientFactory {

    static final int DEFAULT_TIMEOUT_MS = 5000;
    static final int MIN_TIMEOUT_MS = 1000;
    static final int MAX_TIMEOUT_MS = 30000;

    public UnionClient create(CpsVendorConfig config) {
        int timeout = resolveTimeout(config);
        DunionClientConfig sdkConfig = DunionClientConfig.builder()
                .appKey(config.getAppKey())
                .accessKey(config.getAppSecret())
                .baseUrl(StringUtils.hasText(config.getApiBaseUrl()) ? config.getApiBaseUrl()
                        : DunionClientConfig.DEFAULT_BASE_URL)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .build();
        return DunionClientFactory.build(sdkConfig).getUnionClient();
    }

    public int resolveTimeout(CpsVendorConfig config) {
        String raw = config.getExtraConfig() == null ? null : config.getExtraConfig().get("timeoutMs");
        if (!StringUtils.hasText(raw)) return DEFAULT_TIMEOUT_MS;
        try {
            return Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, Integer.parseInt(raw)));
        } catch (NumberFormatException ignored) {
            return DEFAULT_TIMEOUT_MS;
        }
    }
}
