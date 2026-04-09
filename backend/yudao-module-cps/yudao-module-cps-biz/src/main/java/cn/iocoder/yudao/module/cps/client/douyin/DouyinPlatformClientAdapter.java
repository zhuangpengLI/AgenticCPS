package cn.iocoder.yudao.module.cps.client.douyin;

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
 * 抖音联盟平台适配器（委托模式）
 *
 * <p>面向业务层的顶层适配器，通过 {@link CpsPlatformClientFactory} 获取当前激活的
 * {@link CpsApiVendorClient} 实现，委托执行具体的 API 调用。</p>
 *
 * <p>当前暂无可用供应商实现，调用时会返回空结果/桩响应。
 * 待接入抖音联盟官方API后，配置 active_vendor_code 即可启用。</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class DouyinPlatformClientAdapter implements CpsPlatformClient {

    @Resource
    private CpsPlatformClientFactory factory;

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.DOUYIN.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[抖音适配器] 商品搜索功能暂未实现，等待接入抖音联盟 API");
            return buildEmptyResult(request);
        }
        return vendor.searchGoods(request, config);
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[抖音适配器] 转链功能暂未实现，等待接入抖音联盟 API");
            return null;
        }
        return vendor.generatePromotionLink(request, config);
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[抖音适配器] 订单查询功能暂未实现，等待接入抖音联盟 API");
            return Collections.emptyList();
        }
        return vendor.queryOrders(request, config);
    }

    @Override
    public boolean testConnection() {
        CpsApiVendorClient vendor = factory.getActiveVendorClient(getPlatformCode());
        CpsVendorConfig config = factory.getActiveVendorConfig(getPlatformCode());
        if (vendor == null || config == null) {
            log.warn("[抖音适配器] 连接测试功能暂未实现");
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
