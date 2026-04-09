package cn.iocoder.yudao.module.cps.client.pdd;

import cn.iocoder.yudao.module.cps.client.CpsApiVendorClient;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClient;
import cn.iocoder.yudao.module.cps.client.CpsPlatformClientFactory;
import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 拼多多联盟平台适配器（委托模式）
 *
 * <p>面向业务层的顶层适配器，通过 {@link CpsPlatformClientFactory} 获取当前激活的
 * {@link CpsApiVendorClient} 实现，委托执行具体的 API 调用。</p>
 *
 * <p>支持的供应商：大淘客、好单库、拼多多联盟官方API（通过后台配置切换）</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class PddPlatformClientAdapter implements CpsPlatformClient {

    @Resource
    private CpsPlatformClientFactory factory;

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.PDD.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[拼多多适配器] 未找到激活的供应商或配置");
            return buildEmptyResult(request);
        }
        return vendor.searchGoods(request, config);
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[拼多多适配器] 未找到激活的供应商或配置");
            return null;
        }
        return vendor.generatePromotionLink(request, config);
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[拼多多适配器] 未找到激活的供应商或配置");
            return Collections.emptyList();
        }
        return vendor.queryOrders(request, config);
    }

    @Override
    public boolean testConnection() {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[拼多多适配器] 未找到激活的供应商或配置");
            return false;
        }
        return vendor.testConnection(config);
    }

    private CpsGoodsSearchResult buildEmptyResult(CpsGoodsSearchRequest request) {
        return CpsGoodsSearchResult.builder()
                .list(Collections.emptyList())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

}
