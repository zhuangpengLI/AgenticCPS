package com.qiji.cps.module.cps.client.eleme;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderPageResult;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkRequest;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElemePlatformClientAdapter implements CpsPlatformClient {

    @Resource
    private CpsPlatformClientFactory factory;

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.ELEME.getCode();
    }

    @Override
    public boolean supportsGoodsSearch() {
        return false;
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        return CpsGoodsSearchResult.builder()
                .list(List.of())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        CpsApiVendorClient vendor = requireVendor();
        return vendor.generatePromotionLink(request, requireConfig());
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) {
        return requireVendor().queryOrders(request, requireConfig());
    }

    @Override
    public CpsOrderPageResult queryOrderPage(CpsOrderQueryRequest request) {
        return requireVendor().queryOrderPage(request, requireConfig());
    }

    @Override
    public boolean testConnection() {
        return requireVendor().testConnection(requireConfig());
    }

    private CpsApiVendorClient requireVendor() {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        if (vendor == null) {
            throw CpsVendorException.unavailable(getPlatformCode());
        }
        return vendor;
    }

    private CpsVendorConfig requireConfig() {
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (config == null) {
            throw CpsVendorException.unavailable(getPlatformCode());
        }
        return config;
    }
}
