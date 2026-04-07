package cn.iocoder.yudao.module.cps.client.douyin;

import cn.iocoder.yudao.module.cps.client.CpsPlatformClient;
import cn.iocoder.yudao.module.cps.client.dto.*;
import cn.iocoder.yudao.module.cps.enums.CpsPlatformCodeEnum;
import cn.iocoder.yudao.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 抖音联盟平台适配器（桩实现，待接入抖音官方联盟 API）
 *
 * <p>抖音联盟 API 尚未通过大淘客聚合，需直接对接抖音电商联盟开放平台：
 * https://union.jinritemai.com/</p>
 *
 * @author CPS System
 */
@Slf4j
@Component
public class DouyinPlatformClientAdapter implements CpsPlatformClient {

    @Resource
    private CpsPlatformService platformService;

    @Override
    public String getPlatformCode() {
        return CpsPlatformCodeEnum.DOUYIN.getCode();
    }

    @Override
    public CpsGoodsSearchResult searchGoods(CpsGoodsSearchRequest request) {
        log.warn("[抖音适配器] 商品搜索功能暂未实现，等待接入抖音联盟 API");
        return CpsGoodsSearchResult.builder()
                .list(Collections.emptyList())
                .total(0L)
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(CpsPromotionLinkRequest request) {
        log.warn("[抖音适配器] 转链功能暂未实现，等待接入抖音联盟 API");
        return null;
    }

    @Override
    public List<CpsOrderDTO> queryOrders(CpsOrderQueryRequest request) {
        log.warn("[抖音适配器] 订单查询功能暂未实现，等待接入抖音联盟 API");
        return Collections.emptyList();
    }

    @Override
    public boolean testConnection() {
        log.warn("[抖音适配器] 连接测试功能暂未实现");
        return false;
    }

}
