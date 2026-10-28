package com.qiji.cps.module.cps.client.eleme;

import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorException;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 淘宝闪购平台适配器（委托模式 + 主备供应商容灾兜底）
 *
 * <p>面向业务层的顶层适配器，通过 {@link CpsPlatformClientFactory} 获取当前激活的
 * {@link CpsApiVendorClient} 实现，支持官方 OpenAPI 与好单库聚合供应商双向智能容灾。</p>
 *
 * @author CPS System
 */
@Slf4j
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
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        return vendor != null && vendor.getCapabilities().contains(CpsVendorCapability.GOODS_SEARCH);
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[淘宝闪购适配器] 未找到激活的供应商或配置");
            return buildEmptyResult(request);
        }

        if (!vendor.getCapabilities().contains(CpsVendorCapability.GOODS_SEARCH)) {
            return buildEmptyResult(request);
        }

        try {
            return vendor.searchGoods(request, config);
        } catch (Exception e) {
            log.warn("[淘宝闪购适配器] 主供应商 {} 商品搜索异常: {}", vendor.getVendorCode(), e.getMessage());
            return buildEmptyResult(request);
        }
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[淘宝闪购适配器] 未找到激活的供应商或配置");
            return null;
        }

        CpsVendorException primaryVendorEx = null;
        CpsPromotionLinkResult result = null;
        try {
            result = vendor.generatePromotionLink(request, config);
        } catch (CpsVendorException e) {
            primaryVendorEx = e;
            log.warn("[淘宝闪购适配器] 主供应商 {} 转链失败: {}", vendor.getVendorCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("[淘宝闪购适配器] 主供应商 {} 转链异常: {}", vendor.getVendorCode(), e.getMessage());
        }

        if (result != null && hasPromotionMaterial(result)) {
            return result;
        }

        // 若主供应商转链未返回有效内容，尝试使用备用供应商兜底
        String fallbackVendorCode = resolveFallbackVendorCode(vendor.getVendorCode());
        CpsApiVendorClient fallbackVendor = factory.getVendorClient(fallbackVendorCode, getPlatformCode());
        CpsVendorConfig fallbackConfig = factory.getVendorConfig(fallbackVendorCode, getPlatformCode());
        if (fallbackVendor != null && fallbackConfig != null) {
            log.info("[淘宝闪购适配器] 主供应商 {} 转链未返回有效信息，尝试备用供应商 {} 兜底",
                    vendor.getVendorCode(), fallbackVendorCode);
            try {
                CpsPromotionLinkResult fallbackResult = fallbackVendor.generatePromotionLink(request, fallbackConfig);
                if (fallbackResult != null && hasPromotionMaterial(fallbackResult)) {
                    return fallbackResult;
                }
            } catch (Exception e) {
                log.warn("[淘宝闪购适配器] 备用供应商 {} 转链异常: {}", fallbackVendorCode, e.getMessage());
            }
        }

        if (primaryVendorEx != null && request.isPropagateVendorError()) {
            throw primaryVendorEx;
        }
        return result;
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

    private String resolveFallbackVendorCode(String activeVendorCode) {
        if (CpsVendorCodeEnum.OFFICIAL.getCode().equals(activeVendorCode)) {
            return CpsVendorCodeEnum.HAODANKU.getCode();
        }
        if (CpsVendorCodeEnum.HAODANKU.getCode().equals(activeVendorCode)) {
            return CpsVendorCodeEnum.OFFICIAL.getCode();
        }
        return CpsVendorCodeEnum.HAODANKU.getCode();
    }

    private boolean hasPromotionMaterial(CpsPromotionLinkResult result) {
        if (result == null) {
            return false;
        }
        return (result.getShortUrl() != null && !result.getShortUrl().isBlank())
                || (result.getLongUrl() != null && !result.getLongUrl().isBlank())
                || (result.getTpwd() != null && !result.getTpwd().isBlank())
                || (result.getMobileUrl() != null && !result.getMobileUrl().isBlank());
    }

    private CpsGoodsSearchResult buildEmptyResult(CpsGoodsSearchRequest request) {
        return CpsGoodsSearchResult.builder()
                .list(List.of())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
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
