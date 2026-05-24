package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.PLATFORM_IS_DISABLE;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.PLATFORM_NOT_EXISTS;

/**
 * CPS 商品搜索与转链 Service 实现类
 *
 * @author CPS System
 */
@Slf4j
@Service
public class CpsGoodsServiceImpl implements CpsGoodsService {

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private CpsPlatformService platformService;

    @Override
    public CpsGoodsSearchResult searchGoods(String platformCode, CpsGoodsSearchRequest request) {
        return searchGoods(platformCode, request, null);
    }

    @Override
    public CpsGoodsSearchResult searchGoods(String platformCode, CpsGoodsSearchRequest request, String vendorCode) {
        // 校验平台
        validatePlatform(platformCode);
        // 获取适配器
        CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);
        return platformClientFactory.withVendorCode(vendorCode, () -> client.searchGoods(request));
    }

    @Override
    public List<CpsGoodsItem> searchGoodsAllPlatforms(CpsGoodsSearchRequest request) {
        List<CpsPlatformClient> enabledClients = platformClientFactory.getEnabledClients();
        List<CpsGoodsItem> allItems = new ArrayList<>();

        for (CpsPlatformClient client : enabledClients) {
            try {
                // 每个平台最多取前10条用于比价
                CpsGoodsSearchRequest platformReq = cloneRequestWithPageSize(request, 10);
                CpsGoodsSearchResult result = client.searchGoods(platformReq);
                if (result != null && result.getList() != null) {
                    allItems.addAll(result.getList());
                }
            } catch (Exception e) {
                log.warn("[CpsGoodsService] 平台 {} 搜索失败，跳过: {}", client.getPlatformCode(), e.getMessage());
            }
        }

        // 按券后价升序排序（null值排最后）
        allItems.sort(Comparator.comparing(
                item -> item.getActualPrice() != null ? item.getActualPrice() : BigDecimal.valueOf(Long.MAX_VALUE)
        ));

        return allItems;
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(String platformCode, String goodsId,
                                                         String goodsSign, Long memberId, String adzoneId) {
        return generatePromotionLink(platformCode, goodsId, goodsSign, memberId, adzoneId, null);
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(String platformCode, String goodsId,
                                                         String goodsSign, Long memberId, String adzoneId,
                                                         String vendorCode) {
        return platformClientFactory.withVendorCode(vendorCode,
                () -> doGeneratePromotionLink(platformCode, goodsId, goodsSign, memberId, adzoneId));
    }

    private CpsPromotionLinkResult doGeneratePromotionLink(String platformCode, String goodsId,
                                                           String goodsSign, Long memberId, String adzoneId) {
        // 校验平台
        validatePlatform(platformCode);
        CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);

        // 确定使用的推广位
        String finalAdzoneId = resolvePromotionAdzoneId(platformCode, memberId, adzoneId);

        // 构建转链请求
        CpsPromotionLinkRequest linkRequest = new CpsPromotionLinkRequest();
        linkRequest.setGoodsId(goodsId);
        linkRequest.setGoodsSign(goodsSign);
        linkRequest.setAdzoneId(finalAdzoneId);
        // 将 memberId 作为外部用户标识，用于订单归因
        if (memberId != null) {
            linkRequest.setExternalId(String.valueOf(memberId));
        }

        return client.generatePromotionLink(linkRequest);
    }

    @Override
    public String resolvePromotionAdzoneId(String platformCode, Long memberId, String adzoneId) {
        CpsPlatformDO platform = validatePlatform(platformCode);
        String finalAdzoneId = adzoneId;
        if (finalAdzoneId == null) {
            finalAdzoneId = platform.getDefaultAdzoneId();
        }
        return finalAdzoneId;
    }

    // ==================== 私有方法 ====================

    private CpsPlatformDO validatePlatform(String platformCode) {
        CpsPlatformDO platform = platformService.getPlatformByCode(platformCode);
        if (platform == null) {
            throw exception(PLATFORM_NOT_EXISTS);
        }
        if (platform.getStatus() != 1) {
            throw exception(PLATFORM_IS_DISABLE, platformCode);
        }
        return platform;
    }

    private CpsGoodsSearchRequest cloneRequestWithPageSize(CpsGoodsSearchRequest original, int pageSize) {
        CpsGoodsSearchRequest copy = new CpsGoodsSearchRequest();
        copy.setKeyword(original.getKeyword());
        copy.setPageNo(1);
        copy.setPageSize(pageSize);
        copy.setSortType(original.getSortType());
        copy.setPriceLowerLimit(original.getPriceLowerLimit());
        copy.setPriceUpperLimit(original.getPriceUpperLimit());
        copy.setHasCoupon(original.getHasCoupon());
        return copy;
    }

}
