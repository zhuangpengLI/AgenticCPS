package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsGoodsItem;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSelectionMeta;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSelectionOption;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchRequest;
import com.qiji.cps.module.cps.client.dto.CpsGoodsSearchResult;
import com.qiji.cps.module.cps.client.dto.CpsPromotionLinkResult;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.dataoke.DtkSelectionLibraryClient;
import com.qiji.cps.module.cps.client.selection.CpsSearchAssistVendorClient;
import com.qiji.cps.module.cps.client.selection.CpsTaobaoSelectionVendorClient;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareLinkRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaItemRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareMetaRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.dal.dataobject.transfer.CpsTransferRecordDO;
import com.qiji.cps.module.cps.dal.mysql.transfer.CpsTransferRecordMapper;
import com.qiji.cps.module.cps.service.selection.CpsSelectionRule;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class CpsGoodsSquareServiceImpl implements CpsGoodsSquareService {

    private static final String DEFAULT_KEYWORD = "今日精选";
    private static final String PLATFORM_TAOBAO = "taobao";
    private static final String DEFAULT_VENDOR_CODE = "dataoke";
    private static final String SEARCH_MODE_IMAGE = "dataoke_image";
    private static final String LINK_STATUS_SUCCESS = "SUCCESS";
    private static final String LINK_STATUS_FAILED = "FAILED";

    @Resource
    private CpsGoodsService goodsService;

    @Resource
    private CpsTransferRecordMapper transferRecordMapper;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    @Resource
    private DtkSelectionLibraryClient dtkSelectionLibraryClient;

    @Override
    public CpsGoodsSquareMetaRespVO getMeta(String platformCode, String vendorCode) {
        String effectivePlatformCode = StringUtils.hasText(platformCode) ? platformCode : PLATFORM_TAOBAO;
        String effectiveVendorCode = resolveVendorCode(effectivePlatformCode, vendorCode);
        CpsGoodsSelectionMeta meta = null;
        boolean usingVendorMeta = false;
        if (PLATFORM_TAOBAO.equals(effectivePlatformCode)) {
            try {
                CpsApiVendorClient vendorClient = platformClientFactory.getVendorClient(effectiveVendorCode, effectivePlatformCode);
                if (vendorClient instanceof CpsTaobaoSelectionVendorClient selectionVendorClient) {
                    CpsVendorConfig config = platformClientFactory.getVendorConfig(effectiveVendorCode, effectivePlatformCode);
                    meta = selectionVendorClient.getSelectionMeta(config);
                    usingVendorMeta = hasVendorMeta(meta);
                }
            } catch (Exception ignored) {
                meta = null;
            }
        }
        CpsGoodsSelectionMeta mergedMeta = mergeWithDefaultMeta(meta);
        return CpsGoodsSquareMetaRespVO.builder()
                .platformCode(effectivePlatformCode)
                .vendorCode(effectiveVendorCode)
                .metaSource(usingVendorMeta ? firstText(mergedMeta.getMetaSource(), effectiveVendorCode) : "default")
                .usingVendorMeta(usingVendorMeta)
                .taobaoSelectionSupported(PLATFORM_TAOBAO.equals(effectivePlatformCode))
                .capabilityDesc(PLATFORM_TAOBAO.equals(effectivePlatformCode)
                        ? "淘宝选品库支持热词、类目、活动入口和淘系高级筛选"
                        : "当前平台使用通用商品搜索与转链能力")
                .activities(toMetaItemRespList(mergedMeta.getActivities()))
                .hotKeywords(toMetaItemRespList(mergedMeta.getHotKeywords()))
                .categories(toMetaItemRespList(mergedMeta.getCategories()))
                .sortOptions(toMetaItemRespList(mergedMeta.getSortOptions()))
                .filterOptions(toMetaItemRespList(mergedMeta.getFilterOptions()))
                .build();
    }

    @Override
    public CpsGoodsSquareSearchRespVO searchGoods(CpsGoodsSquareSearchReqVO reqVO) {
        CpsGoodsSearchRequest request = buildSearchRequest(reqVO);
        if (StringUtils.hasText(reqVO.getPlatformCode())) {
            CpsGoodsSearchResult result = goodsService.searchGoods(reqVO.getPlatformCode(), request, reqVO.getVendorCode());
            List<CpsGoodsItem> list = result == null || result.getList() == null
                    ? Collections.emptyList() : result.getList();
            return CpsGoodsSquareSearchRespVO.builder()
                    .list(toGoodsRespList(list, reqVO.getVendorCode()))
                    .total(result != null && result.getTotal() != null ? result.getTotal() : (long) list.size())
                    .nextPageId(result != null ? result.getNextPageId() : null)
                    .pageNo(result != null && result.getPageNo() != null ? result.getPageNo() : request.getPageNo())
                    .pageSize(result != null && result.getPageSize() != null ? result.getPageSize() : request.getPageSize())
                    .build();
        }

        List<CpsGoodsItem> list = goodsService.searchGoodsAllPlatforms(request);
        if (list == null) {
            list = Collections.emptyList();
        }
        return CpsGoodsSquareSearchRespVO.builder()
                .list(toGoodsRespList(list, reqVO.getVendorCode()))
                .total((long) list.size())
                .pageNo(request.getPageNo())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    public List<CpsGoodsSquareMetaItemRespVO> getHotKeywords(String platformCode, String vendorCode, Integer type) {
        List<CpsGoodsSelectionOption> options = callSearchAssist(platformCode, vendorCode,
                (client, config) -> client.getHotKeywords(type == null ? 1 : type, config));
        if (!isEmpty(options)) {
            return toMetaItemRespList(options);
        }
        return toMetaItemRespList(buildDefaultMeta().getHotKeywords());
    }

    @Override
    public List<CpsGoodsSquareMetaItemRespVO> suggestKeywords(String platformCode, String vendorCode, String keyword, Integer type) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        List<CpsGoodsSelectionOption> options = callSearchAssist(platformCode, vendorCode,
                (client, config) -> client.suggestKeywords(keyword, type == null ? 1 : type, config));
        return toMetaItemRespList(options);
    }

    @Override
    public CpsGoodsSquareSearchRespVO getVendorGoods(String sourceCode, String platformCode, String vendorCode, Integer pageSize) {
        String effectivePlatformCode = StringUtils.hasText(platformCode) ? platformCode : PLATFORM_TAOBAO;
        String effectiveVendorCode = StringUtils.hasText(vendorCode) ? vendorCode : DEFAULT_VENDOR_CODE;
        int pullCount = pageSize == null ? 20 : Math.max(1, Math.min(pageSize, 100));
        if (!PLATFORM_TAOBAO.equals(effectivePlatformCode) || !DEFAULT_VENDOR_CODE.equals(effectiveVendorCode)) {
            return CpsGoodsSquareSearchRespVO.builder()
                    .list(Collections.emptyList())
                    .total(0L)
                    .pageNo(1)
                    .pageSize(pullCount)
                    .build();
        }
        CpsVendorConfig config = platformClientFactory.getVendorConfig(effectiveVendorCode, effectivePlatformCode);
        CpsSelectionRule rule = buildVendorGoodsRule(sourceCode, effectiveVendorCode, effectivePlatformCode);
        List<CpsGoodsSquareGoodsRespVO> list = dtkSelectionLibraryClient.fetchThemeGoods(rule, pullCount, config);
        if (list == null) {
            list = Collections.emptyList();
        }
        return CpsGoodsSquareSearchRespVO.builder()
                .list(list)
                .total((long) list.size())
                .pageNo(1)
                .pageSize(pullCount)
                .build();
    }

    @Override
    public CpsGoodsSquareSearchRespVO searchByImage(CpsGoodsSquareSearchReqVO reqVO) {
        reqVO.setPlatformCode(PLATFORM_TAOBAO);
        reqVO.setVendorCode(StringUtils.hasText(reqVO.getVendorCode()) ? reqVO.getVendorCode() : DEFAULT_VENDOR_CODE);
        reqVO.setSearchMode(SEARCH_MODE_IMAGE);
        reqVO.setImageBase64(normalizeImageBase64(reqVO.getImageBase64()));
        return searchGoods(reqVO);
    }

    @Override
    public CpsGoodsSquareLinkRespVO generateLink(CpsGoodsSquareLinkReqVO reqVO) {
        String adzoneId = goodsService.resolvePromotionAdzoneId(
                reqVO.getPlatformCode(), reqVO.getMemberId(), reqVO.getAdzoneId());
        CpsPromotionLinkResult linkResult = goodsService.generatePromotionLink(
                reqVO.getPlatformCode(), reqVO.getGoodsId(), reqVO.getGoodsSign(), reqVO.getMemberId(),
                adzoneId, reqVO.getVendorCode());
        if (linkResult == null) {
            return CpsGoodsSquareLinkRespVO.builder()
                    .linkStatus(LINK_STATUS_FAILED)
                    .linkMessage("平台未返回有效推广链接")
                    .adzoneId(adzoneId)
                    .build();
        }

        String promotionUrl = firstText(linkResult.getShortUrl(), linkResult.getLongUrl(), linkResult.getMobileUrl());
        CpsTransferRecordDO record = CpsTransferRecordDO.builder()
                .memberId(reqVO.getMemberId())
                .platformCode(reqVO.getPlatformCode())
                .originalContent(reqVO.getOriginalContent())
                .itemId(reqVO.getGoodsId())
                .itemTitle(reqVO.getTitle())
                .promotionUrl(promotionUrl)
                .taoCommand(linkResult.getTpwd())
                .adzoneId(adzoneId)
                .status(1)
                .build();
        transferRecordMapper.insert(record);

        return CpsGoodsSquareLinkRespVO.builder()
                .linkStatus(LINK_STATUS_SUCCESS)
                .linkMessage("转链成功")
                .transferRecordId(record.getId())
                .adzoneId(adzoneId)
                .shortUrl(linkResult.getShortUrl())
                .longUrl(linkResult.getLongUrl())
                .tpwd(linkResult.getTpwd())
                .mobileUrl(linkResult.getMobileUrl())
                .actualPrice(linkResult.getActualPrice())
                .commissionRate(linkResult.getCommissionRate())
                .commissionAmount(linkResult.getCommissionAmount())
                .couponInfo(linkResult.getCouponInfo())
                .promotionContent(buildPromotionContent(reqVO, linkResult))
                .build();
    }

    private CpsGoodsSearchRequest buildSearchRequest(CpsGoodsSquareSearchReqVO reqVO) {
        CpsGoodsSearchRequest request = new CpsGoodsSearchRequest();
        request.setKeyword(StringUtils.hasText(reqVO.getKeyword()) ? reqVO.getKeyword() : DEFAULT_KEYWORD);
        request.setSearchMode(reqVO.getSearchMode());
        request.setSearchField(reqVO.getSearchField());
        request.setImageBase64(normalizeImageBase64(reqVO.getImageBase64()));
        request.setPageNo(reqVO.getPageNo() == null ? 1 : reqVO.getPageNo());
        request.setPageSize(reqVO.getPageSize() == null ? 20 : reqVO.getPageSize());
        request.setPriceLowerLimit(reqVO.getPriceLowerLimit());
        request.setPriceUpperLimit(reqVO.getPriceUpperLimit());
        request.setSortType(reqVO.getSortType() == null ? 0 : reqVO.getSortType());
        request.setHasCoupon(reqVO.getHasCoupon());
        if (PLATFORM_TAOBAO.equals(reqVO.getPlatformCode())) {
            request.setChannelCode(reqVO.getChannelCode());
            request.setCategoryId(reqVO.getCategoryId());
            request.setMinCommissionRate(reqVO.getMinCommissionRate());
            request.setMinCommissionAmount(reqVO.getMinCommissionAmount());
            request.setMinMonthSales(reqVO.getMinMonthSales());
            request.setCouponAmountMin(reqVO.getCouponAmountMin());
            request.setCouponPriceUpperLimit(reqVO.getCouponPriceUpperLimit());
            request.setHotRankMin(reqVO.getHotRankMin());
            request.setCouponExpireDays(reqVO.getCouponExpireDays());
            request.setTmallOnly(reqVO.getTmallOnly());
            request.setBrandOnly(reqVO.getBrandOnly());
            request.setHaitaoOnly(reqVO.getHaitaoOnly());
            request.setGoldSellerOnly(reqVO.getGoldSellerOnly());
            request.setTchaoshiOnly(reqVO.getTchaoshiOnly());
            request.setJuhuasuanOnly(reqVO.getJuhuasuanOnly());
            request.setTaoqianggouOnly(reqVO.getTaoqianggouOnly());
            request.setInspectedGoodsOnly(reqVO.getInspectedGoodsOnly());
            request.setFreeshipRemoteDistrict(reqVO.getFreeshipRemoteDistrict());
            request.setShopType(reqVO.getShopType());
            request.setGoodsPerformance(reqVO.getGoodsPerformance());
            request.setCommercialOnly(reqVO.getCommercialOnly());
            request.setPreSaleOnly(reqVO.getPreSaleOnly());
            request.setActivityTag(reqVO.getActivityTag());
        }
        return request;
    }

    private List<CpsGoodsSquareGoodsRespVO> toGoodsRespList(List<CpsGoodsItem> list, String vendorCode) {
        List<CpsGoodsSquareGoodsRespVO> result = BeanUtils.toBean(list, CpsGoodsSquareGoodsRespVO.class);
        result.forEach(item -> {
            if (!StringUtils.hasText(item.getVendorCode())) {
                item.setVendorCode(vendorCode);
            }
        });
        return result;
    }

    private CpsSelectionRule buildVendorGoodsRule(String sourceCode, String vendorCode, String platformCode) {
        String normalizedSourceCode = StringUtils.hasText(sourceCode) ? sourceCode.trim().toUpperCase() : "DAILY_EXPLOSIVE";
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setPlatforms(List.of(platformCode));
        rule.setVendorCode(vendorCode);
        rule.setVendorThemeSource(normalizedSourceCode);
        rule.setExternalThemeName(vendorGoodsTitle(normalizedSourceCode));
        rule.setGoodsListParams(new LinkedHashMap<>(vendorGoodsParams(normalizedSourceCode)));
        rule.setGoodsListUrl(vendorGoodsUrl(normalizedSourceCode));
        return rule;
    }

    private String vendorGoodsUrl(String sourceCode) {
        return switch (sourceCode) {
            case "FRIENDS_CIRCLE" -> "/api/goods/friends-circle-list";
            case "RANKING", "HOT_SALE_RANK", "INDUSTRY_TREND_RANK" -> "/api/goods/get-ranking-list";
            case "TIP_OFF" -> "/api/dels/spider/list-tip-off";
            case "NINE_NINE" -> "/api/goods/nine/op-goods-list";
            case "EXPLOSIVE_RADAR" -> "/open-api/goods/radar";
            case "FEATURE_GOODS" -> "/open-api/goods/get-feature-goods";
            default -> "/api/goods/explosive-goods-list";
        };
    }

    private Map<String, Object> vendorGoodsParams(String sourceCode) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "v1.0.0");
        switch (sourceCode) {
            case "FRIENDS_CIRCLE" -> params.put("sort", 0);
            case "RANKING", "HOT_SALE_RANK" -> params.put("rankType", 1);
            case "INDUSTRY_TREND_RANK" -> params.put("rankType", 2);
            case "TIP_OFF" -> params.put("platform", 0);
            case "NINE_NINE" -> params.put("nineCid", -1);
            case "FEATURE_GOODS" -> params.put("pallet_type", 1);
            default -> params.put("PriceCid", 1);
        }
        return params;
    }

    private String vendorGoodsTitle(String sourceCode) {
        return switch (sourceCode) {
            case "FRIENDS_CIRCLE" -> "朋友圈文案";
            case "RANKING", "HOT_SALE_RANK" -> "商品热销榜";
            case "INDUSTRY_TREND_RANK" -> "行业趋势榜";
            case "TIP_OFF" -> "线报中心";
            case "NINE_NINE" -> "9.9包邮";
            case "EXPLOSIVE_RADAR" -> "爆品雷达";
            case "FEATURE_GOODS" -> "品牌优选";
            default -> "爆品清单";
        };
    }

    private String resolveVendorCode(String platformCode, String vendorCode) {
        if (StringUtils.hasText(vendorCode)) {
            return vendorCode;
        }
        String activeVendorCode = platformClientFactory.resolveActiveVendorCode(platformCode);
        return StringUtils.hasText(activeVendorCode) ? activeVendorCode : DEFAULT_VENDOR_CODE;
    }

    private boolean hasVendorMeta(CpsGoodsSelectionMeta meta) {
        return meta != null
                && ((!isEmpty(meta.getActivities()))
                || (!isEmpty(meta.getHotKeywords()))
                || (!isEmpty(meta.getCategories())));
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private List<CpsGoodsSelectionOption> callSearchAssist(
            String platformCode,
            String vendorCode,
            java.util.function.BiFunction<CpsSearchAssistVendorClient, CpsVendorConfig, List<CpsGoodsSelectionOption>> callback) {
        String effectivePlatformCode = StringUtils.hasText(platformCode) ? platformCode : PLATFORM_TAOBAO;
        String effectiveVendorCode = resolveVendorCode(effectivePlatformCode, vendorCode);
        try {
            CpsApiVendorClient vendorClient = platformClientFactory.getVendorClient(effectiveVendorCode, effectivePlatformCode);
            if (vendorClient instanceof CpsSearchAssistVendorClient searchAssistVendorClient) {
                CpsVendorConfig config = platformClientFactory.getVendorConfig(effectiveVendorCode, effectivePlatformCode);
                List<CpsGoodsSelectionOption> result = callback.apply(searchAssistVendorClient, config);
                return result == null ? Collections.emptyList() : result;
            }
        } catch (Exception ignored) {
            // Fallback below keeps the page usable when the vendor API is unavailable.
        }
        return Collections.emptyList();
    }

    private CpsGoodsSelectionMeta mergeWithDefaultMeta(CpsGoodsSelectionMeta vendorMeta) {
        CpsGoodsSelectionMeta defaultMeta = buildDefaultMeta();
        if (vendorMeta == null) {
            return defaultMeta;
        }
        return CpsGoodsSelectionMeta.builder()
                .activities(isEmpty(vendorMeta.getActivities()) ? defaultMeta.getActivities() : vendorMeta.getActivities())
                .hotKeywords(isEmpty(vendorMeta.getHotKeywords()) ? defaultMeta.getHotKeywords() : vendorMeta.getHotKeywords())
                .categories(isEmpty(vendorMeta.getCategories()) ? defaultMeta.getCategories() : vendorMeta.getCategories())
                .sortOptions(isEmpty(vendorMeta.getSortOptions()) ? defaultMeta.getSortOptions() : vendorMeta.getSortOptions())
                .filterOptions(isEmpty(vendorMeta.getFilterOptions()) ? defaultMeta.getFilterOptions() : vendorMeta.getFilterOptions())
                .metaSource(firstText(vendorMeta.getMetaSource(), defaultMeta.getMetaSource()))
                .build();
    }

    private CpsGoodsSelectionMeta buildDefaultMeta() {
        return CpsGoodsSelectionMeta.builder()
                .activities(List.of(
                        option("hot", "聚好券", "热", null, "高热度优惠券商品"),
                        option("brand", "品牌优选", "新", null, "品牌与天猫优选商品"),
                        option("flash", "限时补贴", "热", null, "大额券与限时活动商品"),
                        option("presale", "预告清单", null, null, "活动预告商品")))
                .hotKeywords(List.of(
                        CpsGoodsSelectionOption.of("洗衣液", "洗衣液"),
                        CpsGoodsSelectionOption.of("卫生巾", "卫生巾"),
                        CpsGoodsSelectionOption.of("蚊香液", "蚊香液"),
                        CpsGoodsSelectionOption.of("防晒霜", "防晒霜"),
                        CpsGoodsSelectionOption.of("牛奶", "牛奶"),
                        CpsGoodsSelectionOption.of("淘礼金洗品", "淘礼金洗品")))
                .categories(List.of(
                        CpsGoodsSelectionOption.of("0", "全部"),
                        CpsGoodsSelectionOption.of("10", "居家日用"),
                        CpsGoodsSelectionOption.of("11", "美食"),
                        CpsGoodsSelectionOption.of("9", "母婴"),
                        CpsGoodsSelectionOption.of("4", "美妆"),
                        CpsGoodsSelectionOption.of("1", "女装"),
                        CpsGoodsSelectionOption.of("12", "数码家电"),
                        CpsGoodsSelectionOption.of("6", "鞋品"),
                        CpsGoodsSelectionOption.of("2", "男装"),
                        CpsGoodsSelectionOption.of("7", "箱包")))
                .sortOptions(List.of(
                        CpsGoodsSelectionOption.of("0", "综合"),
                        CpsGoodsSelectionOption.of("1", "月销量"),
                        CpsGoodsSelectionOption.of("2", "券后价升序"),
                        CpsGoodsSelectionOption.of("3", "券后价降序"),
                        CpsGoodsSelectionOption.of("4", "佣金比例")))
                .filterOptions(List.of(
                        CpsGoodsSelectionOption.of("hasCoupon", "只看有券"),
                        CpsGoodsSelectionOption.of("tmallOnly", "天猫"),
                        CpsGoodsSelectionOption.of("brandOnly", "品牌库"),
                        CpsGoodsSelectionOption.of("minCommissionRate", "佣金优选")))
                .metaSource("default")
                .build();
    }

    private CpsGoodsSelectionOption option(String value, String label, String tag, String imageUrl, String description) {
        return CpsGoodsSelectionOption.builder()
                .value(value)
                .label(label)
                .tag(tag)
                .imageUrl(imageUrl)
                .description(description)
                .build();
    }

    private List<CpsGoodsSquareMetaItemRespVO> toMetaItemRespList(List<CpsGoodsSelectionOption> list) {
        return BeanUtils.toBean(list == null ? Collections.emptyList() : list, CpsGoodsSquareMetaItemRespVO.class);
    }

    private String buildPromotionContent(CpsGoodsSquareLinkReqVO reqVO, CpsPromotionLinkResult linkResult) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(reqVO.getTitle())) {
            builder.append(reqVO.getTitle()).append('\n');
        }
        if (StringUtils.hasText(linkResult.getCouponInfo())) {
            builder.append(linkResult.getCouponInfo()).append('\n');
        }
        String url = firstText(linkResult.getShortUrl(), linkResult.getMobileUrl(), linkResult.getLongUrl());
        if (StringUtils.hasText(url)) {
            builder.append(url).append('\n');
        }
        if (StringUtils.hasText(linkResult.getTpwd())) {
            builder.append(linkResult.getTpwd());
        }
        return builder.toString().trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeImageBase64(String imageBase64) {
        if (!StringUtils.hasText(imageBase64)) {
            return null;
        }
        int commaIndex = imageBase64.indexOf(',');
        return commaIndex >= 0 ? imageBase64.substring(commaIndex + 1) : imageBase64;
    }

}
