package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.*;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.PLATFORM_IS_DISABLE;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.PLATFORM_CAPABILITY_UNSUPPORTED;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.PLATFORM_NOT_EXISTS;

/**
 * CPS 商品搜索与转链 Service 实现类
 *
 * @author CPS System
 */
@Slf4j
@Service
public class CpsGoodsServiceImpl implements CpsGoodsService {

    private final CpsPlatformClientFactory platformClientFactory;

    private final CpsPlatformService platformService;

    private final CpsAdzoneService adzoneService;

    private final CpsGoodsAggregationExecutor goodsAggregationExecutor;

    public CpsGoodsServiceImpl(CpsPlatformClientFactory platformClientFactory,
                               CpsPlatformService platformService,
                               CpsAdzoneService adzoneService,
                               CpsGoodsAggregationExecutor goodsAggregationExecutor) {
        this.platformClientFactory = platformClientFactory;
        this.platformService = platformService;
        this.adzoneService = adzoneService;
        this.goodsAggregationExecutor = goodsAggregationExecutor;
    }

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
        if (!client.supportsGoodsSearch()) {
            throw exception(PLATFORM_CAPABILITY_UNSUPPORTED, platformCode);
        }
        return platformClientFactory.withVendorCode(vendorCode, () -> client.searchGoods(request));
    }

    @Override
    public List<CpsGoodsItem> searchGoodsAllPlatforms(CpsGoodsSearchRequest request) {
        List<CpsPlatformClient> enabledClients = platformClientFactory.getEnabledClients();
        List<CpsPlatformClient> searchableClients = enabledClients.stream()
                .filter(CpsPlatformClient::supportsGoodsSearch)
                .sorted(Comparator.comparing(CpsPlatformClient::getPlatformCode))
                .toList();
        if (searchableClients.isEmpty()) {
            return List.of();
        }

        List<Callable<CpsGoodsSearchResult>> searchTasks = searchableClients.stream()
                .<Callable<CpsGoodsSearchResult>>map(client -> {
                    CpsGoodsSearchRequest platformRequest = request.copyForPage(1, 10);
                    return () -> client.searchGoods(platformRequest);
                })
                .toList();
        List<CpsGoodsAggregationExecutor.TaskResult<CpsGoodsSearchResult>> taskResults =
                goodsAggregationExecutor.invokeAll(searchTasks);
        List<CpsGoodsItem> allItems = new ArrayList<>();

        for (int index = 0; index < searchableClients.size(); index++) {
            CpsPlatformClient client = searchableClients.get(index);
            CpsGoodsAggregationExecutor.TaskResult<CpsGoodsSearchResult> taskResult = taskResults.get(index);
            if (taskResult.timedOut()) {
                log.warn("[CpsGoodsService] 商品聚合批次预算耗尽，跳过平台 {} 的结果", client.getPlatformCode());
                continue;
            }
            if (taskResult.error() != null) {
                log.warn("[CpsGoodsService] 平台 {} 搜索失败，跳过: {}",
                        client.getPlatformCode(), taskResult.error().getMessage());
                continue;
            }
            CpsGoodsSearchResult result = taskResult.value();
            if (result != null && result.getList() != null) {
                allItems.addAll(result.getList());
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
        return generatePromotionLink(platformCode, goodsId, goodsSign, memberId, adzoneId, null, null);
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(String platformCode, String goodsId,
                                                         String goodsSign, Long memberId, String adzoneId,
                                                         String vendorCode) {
        return generatePromotionLink(platformCode, goodsId, goodsSign, memberId, adzoneId, vendorCode, null);
    }

    @Override
    public CpsPromotionLinkResult generatePromotionLink(String platformCode, String goodsId,
                                                         String goodsSign, Long memberId, String adzoneId,
                                                         String vendorCode, String originalContent) {
        return platformClientFactory.withVendorCode(vendorCode,
                () -> doGeneratePromotionLink(platformCode, goodsId, goodsSign, memberId, adzoneId,
                        originalContent));
    }

    private CpsPromotionLinkResult doGeneratePromotionLink(String platformCode, String goodsId,
                                                            String goodsSign, Long memberId, String adzoneId,
                                                            String originalContent) {
        // 校验平台
        CpsPlatformDO platform = validatePlatform(platformCode);
        CpsPlatformClient client = platformClientFactory.getRequiredClient(platformCode);

        // 确定使用的推广位
        PromotionAdzoneContext adzoneContext = resolvePromotionAdzone(platformCode, memberId, adzoneId, platform);

        // 构建转链请求
        CpsPromotionLinkRequest linkRequest = new CpsPromotionLinkRequest();
        linkRequest.setGoodsId(goodsId);
        linkRequest.setGoodsSign(goodsSign);
        linkRequest.setAdzoneId(adzoneContext.adzoneId());
        linkRequest.setOriginalContent(originalContent);
        // 将 memberId 作为外部用户标识，用于订单归因
        if (memberId != null) {
            String attributionId = String.valueOf(memberId);
            linkRequest.setExternalId(attributionId);
            applyAttributionParams(platformCode, adzoneContext.adzone(), linkRequest, attributionId);
        }

        return client.generatePromotionLink(linkRequest);
    }

    @Override
    public String resolvePromotionAdzoneId(String platformCode, Long memberId, String adzoneId) {
        CpsPlatformDO platform = validatePlatform(platformCode);
        return resolvePromotionAdzone(platformCode, memberId, adzoneId, platform).adzoneId();
    }

    private PromotionAdzoneContext resolvePromotionAdzone(String platformCode, Long memberId, String adzoneId,
                                                          CpsPlatformDO platform) {
        if (StringUtils.hasText(adzoneId)) {
            return new PromotionAdzoneContext(adzoneId, null);
        }
        if (memberId != null) {
            CpsAdzoneDO memberAdzone = adzoneService.getMemberAdzone(platformCode, memberId);
            if (memberAdzone != null && StringUtils.hasText(memberAdzone.getAdzoneId())) {
                return new PromotionAdzoneContext(memberAdzone.getAdzoneId(), memberAdzone);
            }
        }
        return new PromotionAdzoneContext(platform.getDefaultAdzoneId(), null);
    }

    private void applyAttributionParams(String platformCode, CpsAdzoneDO adzone,
                                        CpsPromotionLinkRequest linkRequest, String attributionId) {
        if (!"taobao".equalsIgnoreCase(platformCode)) {
            linkRequest.setChannelId(attributionId);
            return;
        }
        if (adzone == null) {
            return;
        }
        if ("member".equalsIgnoreCase(adzone.getRelationType()) && StringUtils.hasText(adzone.getExternalSpecialId())) {
            linkRequest.setSpecialId(adzone.getExternalSpecialId());
            linkRequest.setOrderScene(3);
            return;
        }
        if ("channel".equalsIgnoreCase(adzone.getRelationType()) && StringUtils.hasText(adzone.getExternalRelationId())) {
            linkRequest.setRelationId(adzone.getExternalRelationId());
            linkRequest.setChannelId(adzone.getExternalRelationId());
            linkRequest.setOrderScene(2);
        }
    }

    private record PromotionAdzoneContext(String adzoneId, CpsAdzoneDO adzone) {
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

}
