package com.qiji.cps.module.cps.client.didi;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DidiPlatformClientAdapter implements CpsPlatformClient {
    private final CpsPlatformClientFactory factory;
    @Override public String getPlatformCode() { return CpsPlatformCodeEnum.DIDI.getCode(); }
    @Override public boolean supportsGoodsSearch() { return false; }
    @Override public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        throw CpsVendorException.capabilityUnsupported(null, getPlatformCode(), CpsVendorCapability.GOODS_SEARCH);
    }
    @Override public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        return vendor().generatePromotionLink(request, config());
    }
    @Override public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request) {
        return vendor().queryOrderPage(request, config());
    }
    @Override public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) { return vendor().queryOrders(request, config()); }
    @Override public boolean testConnection() {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        return vendor != null && config != null && vendor.testConnection(config);
    }
    private CpsApiVendorClient vendor() {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        if (vendor == null) throw new IllegalStateException("Didi active vendor is not configured");
        return vendor;
    }
    private CpsVendorConfig config() {
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (config == null) throw new IllegalStateException("Didi active vendor config is missing");
        return config;
    }
}
