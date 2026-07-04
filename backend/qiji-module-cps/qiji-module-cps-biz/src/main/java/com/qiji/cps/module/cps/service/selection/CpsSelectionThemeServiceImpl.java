package com.qiji.cps.module.cps.service.selection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeAiRecommendReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemImportReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemSortReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemStatusReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeOperationRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemePageReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSyncReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeTemplateCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeTemplateRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeVendorPullReqVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import com.qiji.cps.module.cps.service.goods.CpsGoodsSquareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.SELECTION_THEME_CODE_DUPLICATE;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.SELECTION_THEME_ITEM_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.SELECTION_THEME_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.SELECTION_THEME_STATUS_INVALID;

@Service
@Validated
public class CpsSelectionThemeServiceImpl implements CpsSelectionThemeService {

    private static final int DEFAULT_PULL_COUNT = 20;
    private static final int MAX_PULL_COUNT = 100;
    private static final int DEFAULT_SYNC_THEME_PAGE_SIZE = 20;
    private static final int DEFAULT_SYNC_GOODS_PULL_COUNT = 20;
    private static final int DEFAULT_SYNC_MAX_PAGES = 1;
    private static final DateTimeFormatter TEMPLATE_CODE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private CpsSelectionThemeMapper themeMapper;

    @Resource
    private CpsSelectionThemeItemMapper itemMapper;

    @Resource
    private CpsGoodsSquareService goodsSquareService;

    @Resource
    private CpsSelectionAiRecommendService aiRecommendService;

    @Resource
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTheme(CpsSelectionThemeSaveReqVO createReqVO) {
        validateThemeCodeUnique(createReqVO.getThemeCode(), null);
        validateTimeWindow(createReqVO.getStartTime(), createReqVO.getEndTime());
        CpsSelectionThemeDO theme = BeanUtils.toBean(createReqVO, CpsSelectionThemeDO.class);
        theme.setStatus(firstText(createReqVO.getStatus(), CpsSelectionConstants.ThemeStatus.DRAFT));
        theme.setRefreshStatus(firstText(theme.getRefreshStatus(), CpsSelectionConstants.ImportTaskStatus.SUCCESS));
        theme.setSort(theme.getSort() == null ? 0 : theme.getSort());
        themeMapper.insert(theme);
        return theme.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTheme(CpsSelectionThemeSaveReqVO updateReqVO) {
        CpsSelectionThemeDO existing = validateThemeExists(updateReqVO.getId());
        validateThemeCodeUnique(updateReqVO.getThemeCode(), updateReqVO.getId());
        validateTimeWindow(updateReqVO.getStartTime(), updateReqVO.getEndTime());
        CpsSelectionThemeDO updateObj = BeanUtils.toBean(updateReqVO, CpsSelectionThemeDO.class);
        updateObj.setStatus(firstText(updateReqVO.getStatus(), existing.getStatus(), CpsSelectionConstants.ThemeStatus.DRAFT));
        themeMapper.updateById(updateObj);
    }

    @Override
    public void deleteTheme(Long id) {
        validateThemeExists(id);
        themeMapper.deleteById(id);
    }

    @Override
    public void publishTheme(Long id) {
        CpsSelectionThemeDO theme = validateThemeExists(id);
        validateTimeWindow(theme.getStartTime(), theme.getEndTime());
        if (itemMapper.selectEnabledListByThemeId(id).isEmpty()) {
            throw exception(SELECTION_THEME_STATUS_INVALID, "发布前至少需要一个启用商品");
        }
        themeMapper.updateById(CpsSelectionThemeDO.builder()
                .id(id)
                .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                .build());
    }

    @Override
    public void offlineTheme(Long id) {
        validateThemeExists(id);
        themeMapper.updateById(CpsSelectionThemeDO.builder()
                .id(id)
                .status(CpsSelectionConstants.ThemeStatus.OFFLINE)
                .build());
    }

    @Override
    public CpsSelectionThemeDO getTheme(Long id) {
        return themeMapper.selectById(id);
    }

    @Override
    public PageResult<CpsSelectionThemeDO> getThemePage(CpsSelectionThemePageReqVO pageReqVO) {
        return themeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CpsSelectionThemeDO> listPublishedThemes(String keyword, String promotionEvent) {
        return themeMapper.selectPublishedList(keyword, promotionEvent);
    }

    @Override
    public List<CpsSelectionThemeItemDO> listItems(Long themeId) {
        validateThemeExists(themeId);
        return itemMapper.selectListByThemeId(themeId);
    }

    @Override
    public List<CpsSelectionThemeItemDO> listEnabledItems(Long themeId) {
        validateThemeExists(themeId);
        return itemMapper.selectEnabledListByThemeId(themeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importItems(CpsSelectionThemeItemImportReqVO reqVO) {
        validateThemeExists(reqVO.getThemeId());
        int count = 0;
        for (CpsSelectionThemeItemImportReqVO.ImportItem item : reqVO.getItems()) {
            upsertItem(reqVO.getThemeId(), item, firstText(reqVO.getSourceType(), CpsSelectionConstants.SourceType.MANUAL));
            count++;
        }
        return count;
    }

    @Override
    public void updateItemSort(CpsSelectionThemeItemSortReqVO reqVO) {
        for (CpsSelectionThemeItemSortReqVO.SortItem item : reqVO.getItems()) {
            CpsSelectionThemeItemDO exists = validateItemExists(item.getId());
            if (!reqVO.getThemeId().equals(exists.getThemeId())) {
                throw exception(SELECTION_THEME_ITEM_NOT_EXISTS);
            }
            itemMapper.updateById(CpsSelectionThemeItemDO.builder()
                    .id(item.getId())
                    .sort(item.getSort())
                    .topFlag(item.getTopFlag())
                    .build());
        }
    }

    @Override
    public void updateItemStatus(CpsSelectionThemeItemStatusReqVO reqVO) {
        for (Long id : reqVO.getIds()) {
            validateItemExists(id);
            itemMapper.updateById(CpsSelectionThemeItemDO.builder().id(id).status(reqVO.getStatus()).build());
        }
    }

    @Override
    public void deleteItem(Long id) {
        validateItemExists(id);
        itemMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsSelectionThemeOperationRespVO vendorPull(CpsSelectionThemeVendorPullReqVO reqVO) {
        CpsSelectionThemeDO theme = validateThemeExists(reqVO.getThemeId());
        themeMapper.updateById(CpsSelectionThemeDO.builder()
                .id(theme.getId())
                .refreshStatus(CpsSelectionConstants.ImportTaskStatus.PROCESSING)
                .build());
        try {
            CpsSelectionRule rule = parseRule(firstText(reqVO.getRuleJson(), theme.getRuleJson()));
            List<CpsGoodsSquareGoodsRespVO> pulled = searchCandidates(theme, rule);
            int imported = importRecommendedGoods(theme, pulled, rule, CpsSelectionConstants.SourceType.VENDOR_PULL);
            String status = imported == pulled.size()
                    ? CpsSelectionConstants.ImportTaskStatus.SUCCESS : CpsSelectionConstants.ImportTaskStatus.PARTIAL_SUCCESS;
            themeMapper.updateById(CpsSelectionThemeDO.builder()
                    .id(theme.getId())
                    .refreshStatus(status)
                    .lastRefreshTime(LocalDateTime.now())
                    .build());
            return CpsSelectionThemeOperationRespVO.builder()
                    .themeId(theme.getId())
                    .status(status)
                    .pulledCount(pulled.size())
                    .importedCount(imported)
                    .message("第三方拉取完成")
                    .build();
        } catch (Exception e) {
            themeMapper.updateById(CpsSelectionThemeDO.builder()
                    .id(theme.getId())
                    .refreshStatus(CpsSelectionConstants.ImportTaskStatus.FAILED)
                    .lastRefreshTime(LocalDateTime.now())
                    .build());
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsSelectionThemeOperationRespVO syncDataokeThemes(CpsSelectionThemeSyncReqVO reqVO) {
        CpsSelectionThemeSyncReqVO effectiveReqVO = reqVO == null ? new CpsSelectionThemeSyncReqVO() : reqVO;
        String vendorCode = CpsVendorCodeEnum.DATAOKE.getCode();
        String platformCode = CpsPlatformCodeEnum.TAOBAO.getCode();
        CpsVendorConfig config = platformClientFactory.getVendorConfig(vendorCode, platformCode);
        if (config == null) {
            throw exception(SELECTION_THEME_STATUS_INVALID, "大淘客供应商配置不存在");
        }
        int maxPages = clamp(effectiveReqVO.getMaxPages(), DEFAULT_SYNC_MAX_PAGES, 1, 20);
        int pageSize = clamp(effectiveReqVO.getPageSize(), DEFAULT_SYNC_THEME_PAGE_SIZE, 1, MAX_PULL_COUNT);
        int goodsPullCount = clamp(effectiveReqVO.getGoodsPullCount(), DEFAULT_SYNC_GOODS_PULL_COUNT, 1, MAX_PULL_COUNT);
        boolean syncGoods = !Boolean.FALSE.equals(effectiveReqVO.getSyncGoods());

        int pulledThemes = 0;
        int insertedThemes = 0;
        int updatedThemes = 0;
        int skippedThemes = 0;
        int importedGoods = 0;
        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            CpsThirdPartyPage<CpsThirdPartyActivity> page = dtkActivityVendorClient.fetchActivities(
                    CpsThirdPartyActivityRequest.builder()
                            .vendorCode(vendorCode)
                            .platformCode(platformCode)
                            .keyword(effectiveReqVO.getKeyword())
                            .pageNo(pageNo)
                            .pageSize(pageSize)
                            .build(),
                    config);
            if (page == null || page.getList() == null || page.getList().isEmpty()) {
                break;
            }
            for (CpsThirdPartyActivity activity : page.getList()) {
                CpsSelectionThemeDO theme = toDataokeTheme(activity, goodsPullCount);
                if (theme == null) {
                    skippedThemes++;
                    continue;
                }
                pulledThemes++;
                CpsSelectionThemeDO existing = themeMapper.selectByThemeCode(theme.getThemeCode());
                if (existing == null) {
                    themeMapper.insert(theme);
                    insertedThemes++;
                } else {
                    theme.setId(existing.getId());
                    theme.setStatus(firstText(existing.getStatus(), CpsSelectionConstants.ThemeStatus.DRAFT));
                    themeMapper.updateById(theme);
                    updatedThemes++;
                }
                if (syncGoods && theme.getId() != null) {
                    importedGoods += syncThemeGoods(theme, goodsPullCount);
                }
            }
            if (isLastPage(page, pageNo, pageSize)) {
                break;
            }
        }
        String status = skippedThemes == 0
                ? CpsSelectionConstants.ImportTaskStatus.SUCCESS : CpsSelectionConstants.ImportTaskStatus.PARTIAL_SUCCESS;
        return CpsSelectionThemeOperationRespVO.builder()
                .status(status)
                .pulledCount(pulledThemes)
                .importedCount(importedGoods)
                .message("大淘客主题同步完成，新建 " + insertedThemes + " 个，更新 " + updatedThemes
                        + " 个，跳过 " + skippedThemes + " 个，导入商品 " + importedGoods + " 个")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsSelectionThemeOperationRespVO aiRecommend(CpsSelectionThemeAiRecommendReqVO reqVO) {
        CpsSelectionThemeDO theme = validateThemeExists(reqVO.getThemeId());
        CpsSelectionRule rule = parseRule(firstText(reqVO.getRuleJson(), theme.getRuleJson()));
        List<CpsGoodsSquareGoodsRespVO> pulled = searchCandidates(theme, rule);
        int imported = importRecommendedGoods(theme, pulled, rule, CpsSelectionConstants.SourceType.AI_RECOMMEND);
        if (StringUtils.hasText(reqVO.getObjective())) {
            themeMapper.updateById(CpsSelectionThemeDO.builder()
                    .id(theme.getId())
                    .aiPrompt(reqVO.getObjective())
                    .aiSummary("已根据运营目标生成关键词、筛选条件和商品推荐理由，排序由规则评分决定。")
                    .refreshStatus(CpsSelectionConstants.ImportTaskStatus.SUCCESS)
                    .lastRefreshTime(LocalDateTime.now())
                    .build());
        }
        return CpsSelectionThemeOperationRespVO.builder()
                .themeId(theme.getId())
                .status(CpsSelectionConstants.ImportTaskStatus.SUCCESS)
                .pulledCount(pulled.size())
                .importedCount(imported)
                .message("AI 推荐已按规则评分入库")
                .build();
    }

    @Override
    public List<CpsSelectionThemeTemplateRespVO> listPromotionTemplates() {
        return promotionTemplates();
    }

    @Override
    public Long createFromTemplate(CpsSelectionThemeTemplateCreateReqVO reqVO) {
        CpsSelectionThemeTemplateRespVO template = promotionTemplates().stream()
                .filter(item -> item.getTemplateCode().equals(reqVO.getTemplateCode()))
                .findFirst()
                .orElseThrow(() -> exception(SELECTION_THEME_STATUS_INVALID, "模板不存在"));
        String themeCode = firstText(reqVO.getThemeCode(),
                template.getTemplateCode() + "_" + LocalDateTime.now().format(TEMPLATE_CODE_SUFFIX));
        CpsSelectionThemeSaveReqVO saveReqVO = new CpsSelectionThemeSaveReqVO();
        saveReqVO.setThemeCode(themeCode);
        saveReqVO.setThemeName(template.getThemeName());
        saveReqVO.setThemeType("PROMOTION");
        saveReqVO.setPromotionEvent(template.getPromotionEvent());
        saveReqVO.setDescription(template.getDescription());
        saveReqVO.setTags(template.getTags());
        saveReqVO.setRuleJson(template.getRuleJson());
        saveReqVO.setAiPrompt(template.getAiPrompt());
        saveReqVO.setStatus(CpsSelectionConstants.ThemeStatus.DRAFT);
        saveReqVO.setSort(0);
        return createTheme(saveReqVO);
    }

    private CpsSelectionThemeDO toDataokeTheme(CpsThirdPartyActivity activity, int goodsPullCount) {
        if (activity == null || !StringUtils.hasText(activity.getExternalActivityId())) {
            return null;
        }
        String themeCode = toDataokeThemeCode(activity.getExternalActivityId());
        String themeName = firstText(activity.getActivityName(), activity.getSearchKeyword(), themeCode);
        String platformCode = firstText(activity.getPlatformCode(), CpsPlatformCodeEnum.TAOBAO.getCode());
        String tagText = firstText(activity.getTagText(), activity.getActivityType(), "大淘客");
        return CpsSelectionThemeDO.builder()
                .themeCode(themeCode)
                .themeName(trimToMax(themeName, 128))
                .themeType("PROMOTION")
                .promotionEvent(trimToMax(firstText(activity.getActivityType(), tagText), 64))
                .platformCodes(platformCode)
                .vendorCode(CpsVendorCodeEnum.DATAOKE.getCode())
                .coverPic(activity.getMainPic())
                .description(trimToMax(activity.getShortDesc(), 512))
                .tags(trimToMax("大淘客," + tagText, 255))
                .ruleJson(buildDataokeRuleJson(activity, platformCode, goodsPullCount))
                .aiPrompt("围绕大淘客主题“" + themeName + "”筛选高券、高佣、高转化商品，排序以佣金、券额和销量为主。")
                .status(CpsSelectionConstants.ThemeStatus.DRAFT)
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .refreshStatus(CpsSelectionConstants.ImportTaskStatus.SUCCESS)
                .lastRefreshTime(LocalDateTime.now())
                .sort(0)
                .remark(trimToMax(activity.getExternalActivityId(), 500))
                .build();
    }

    private int syncThemeGoods(CpsSelectionThemeDO theme, int goodsPullCount) {
        themeMapper.updateById(CpsSelectionThemeDO.builder()
                .id(theme.getId())
                .refreshStatus(CpsSelectionConstants.ImportTaskStatus.PROCESSING)
                .build());
        try {
            CpsSelectionRule rule = parseRule(theme.getRuleJson());
            rule.setPullCount(goodsPullCount);
            List<CpsGoodsSquareGoodsRespVO> pulled = searchCandidates(theme, rule);
            int imported = importRecommendedGoods(theme, pulled, rule, CpsSelectionConstants.SourceType.VENDOR_PULL);
            String status = imported == pulled.size()
                    ? CpsSelectionConstants.ImportTaskStatus.SUCCESS : CpsSelectionConstants.ImportTaskStatus.PARTIAL_SUCCESS;
            themeMapper.updateById(CpsSelectionThemeDO.builder()
                    .id(theme.getId())
                    .refreshStatus(status)
                    .lastRefreshTime(LocalDateTime.now())
                    .build());
            return imported;
        } catch (Exception e) {
            themeMapper.updateById(CpsSelectionThemeDO.builder()
                    .id(theme.getId())
                    .refreshStatus(CpsSelectionConstants.ImportTaskStatus.FAILED)
                    .lastRefreshTime(LocalDateTime.now())
                    .build());
            throw e;
        }
    }

    private String buildDataokeRuleJson(CpsThirdPartyActivity activity, String platformCode, int goodsPullCount) {
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setKeywords(List.of(firstText(activity.getSearchKeyword(), activity.getActivityName(), "今日精选")));
        rule.setPlatforms(List.of(platformCode));
        rule.setVendorCode(CpsVendorCodeEnum.DATAOKE.getCode());
        rule.setActivityTags(List.of(firstText(activity.getTagText(), activity.getActivityType(), "大淘客")));
        rule.setOnlyCoupon(true);
        rule.setSortType(0);
        rule.setPullCount(goodsPullCount);
        return toJson(rule);
    }

    private String toDataokeThemeCode(String externalActivityId) {
        String normalized = externalActivityId.replaceFirst("(?i)^dtk:", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = Integer.toHexString(externalActivityId.hashCode()).toUpperCase(Locale.ROOT);
        }
        if (normalized.length() > 60) {
            normalized = normalized.substring(0, 60);
        }
        return "DTK_" + normalized;
    }

    private boolean isLastPage(CpsThirdPartyPage<?> page, int pageNo, int pageSize) {
        if (page.getTotal() != null && page.getTotal() >= 0) {
            return (long) pageNo * pageSize >= page.getTotal();
        }
        return page.getList() == null || page.getList().size() < pageSize;
    }

    private List<CpsGoodsSquareGoodsRespVO> searchCandidates(CpsSelectionThemeDO theme, CpsSelectionRule rule) {
        CpsGoodsSquareSearchReqVO searchReqVO = buildSearchReqVO(theme, rule);
        CpsGoodsSquareSearchRespVO response = goodsSquareService.searchGoods(searchReqVO);
        return response == null || response.getList() == null ? List.of() : response.getList();
    }

    private CpsGoodsSquareSearchReqVO buildSearchReqVO(CpsSelectionThemeDO theme, CpsSelectionRule rule) {
        CpsGoodsSquareSearchReqVO reqVO = new CpsGoodsSquareSearchReqVO();
        reqVO.setKeyword(resolveKeyword(theme, rule));
        reqVO.setPlatformCode(resolveFirst(rule.getPlatforms(), theme.getPlatformCodes()));
        reqVO.setVendorCode(firstText(rule.getVendorCode(), theme.getVendorCode()));
        reqVO.setPageNo(1);
        reqVO.setPageSize(Math.min(MAX_PULL_COUNT,
                rule.getPullCount() == null || rule.getPullCount() <= 0 ? DEFAULT_PULL_COUNT : rule.getPullCount()));
        reqVO.setPriceLowerLimit(rule.getPriceLowerLimit());
        reqVO.setPriceUpperLimit(rule.getPriceUpperLimit());
        reqVO.setSortType(rule.getSortType());
        reqVO.setHasCoupon(Boolean.TRUE.equals(rule.getOnlyCoupon()) ? 1 : null);
        reqVO.setCategoryId(rule.getCategoryId());
        reqVO.setChannelCode(rule.getChannelCode());
        reqVO.setMinCommissionRate(rule.getMinCommissionRate());
        reqVO.setMinCommissionAmount(rule.getMinCommissionAmount());
        reqVO.setMinMonthSales(rule.getMinMonthSales());
        reqVO.setCouponAmountMin(rule.getCouponAmountMin());
        reqVO.setActivityTag(rule.getActivityTags() == null || rule.getActivityTags().isEmpty()
                ? null : rule.getActivityTags().get(0));
        return reqVO;
    }

    private int importRecommendedGoods(CpsSelectionThemeDO theme, List<CpsGoodsSquareGoodsRespVO> goodsList,
                                       CpsSelectionRule rule, String sourceType) {
        List<CpsSelectionAiRecommendService.RecommendedGoods> recommended =
                aiRecommendService == null ? null : aiRecommendService.recommend(theme, goodsList, rule.getPullCount());
        if (recommended == null || (recommended.isEmpty() && !goodsList.isEmpty())) {
            recommended = goodsList.stream()
                    .map(goods -> new CpsSelectionAiRecommendService.RecommendedGoods(goods, null, null))
                    .toList();
        }
        int imported = 0;
        int sort = 0;
        for (CpsSelectionAiRecommendService.RecommendedGoods recommendedGoods : recommended) {
            CpsSelectionThemeItemImportReqVO.ImportItem item = toImportItem(recommendedGoods.getGoods());
            item.setRecommendScore(recommendedGoods.getRecommendScore());
            item.setRecommendReason(recommendedGoods.getRecommendReason());
            item.setSort(sort++);
            upsertItem(theme.getId(), item, sourceType);
            imported++;
        }
        return imported;
    }

    private CpsSelectionThemeItemImportReqVO.ImportItem toImportItem(CpsGoodsSquareGoodsRespVO goods) {
        CpsSelectionThemeItemImportReqVO.ImportItem item = BeanUtils.toBean(goods, CpsSelectionThemeItemImportReqVO.ImportItem.class);
        item.setItemLink(goods.getItemLink());
        item.setRawData(toJson(goods));
        return item;
    }

    private void upsertItem(Long themeId, CpsSelectionThemeItemImportReqVO.ImportItem item, String sourceType) {
        CpsSelectionThemeItemDO exists = itemMapper.selectOneByUnique(
                themeId, item.getPlatformCode(), firstText(item.getVendorCode(), ""), item.getGoodsId(),
                firstText(item.getGoodsSign(), ""));
        CpsSelectionThemeItemDO saveObj = BeanUtils.toBean(item, CpsSelectionThemeItemDO.class);
        saveObj.setThemeId(themeId);
        saveObj.setVendorCode(firstText(item.getVendorCode(), ""));
        saveObj.setGoodsSign(firstText(item.getGoodsSign(), ""));
        saveObj.setSourceType(firstText(sourceType, CpsSelectionConstants.SourceType.MANUAL));
        saveObj.setStatus(firstText(item.getStatus(), CpsSelectionConstants.ItemStatus.ENABLED));
        saveObj.setTopFlag(item.getTopFlag() == null ? 0 : item.getTopFlag());
        saveObj.setSnapshotTime(LocalDateTime.now());
        saveObj.setSort(item.getSort() == null ? 0 : item.getSort());
        if (exists == null) {
            itemMapper.insert(saveObj);
        } else {
            saveObj.setId(exists.getId());
            itemMapper.updateById(saveObj);
        }
    }

    private CpsSelectionThemeDO validateThemeExists(Long id) {
        CpsSelectionThemeDO theme = themeMapper.selectById(id);
        if (theme == null) {
            throw exception(SELECTION_THEME_NOT_EXISTS);
        }
        return theme;
    }

    private CpsSelectionThemeItemDO validateItemExists(Long id) {
        CpsSelectionThemeItemDO item = itemMapper.selectById(id);
        if (item == null) {
            throw exception(SELECTION_THEME_ITEM_NOT_EXISTS);
        }
        return item;
    }

    private void validateThemeCodeUnique(String themeCode, Long selfId) {
        CpsSelectionThemeDO exists = themeMapper.selectByThemeCode(themeCode);
        if (exists != null && (selfId == null || !selfId.equals(exists.getId()))) {
            throw exception(SELECTION_THEME_CODE_DUPLICATE, themeCode);
        }
    }

    private void validateTimeWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw exception(SELECTION_THEME_STATUS_INVALID, "上线时间不能晚于下线时间");
        }
    }

    private CpsSelectionRule parseRule(String ruleJson) {
        if (!StringUtils.hasText(ruleJson)) {
            return new CpsSelectionRule();
        }
        try {
            return objectMapper.readValue(ruleJson, CpsSelectionRule.class);
        } catch (Exception e) {
            throw exception(SELECTION_THEME_STATUS_INVALID, "主题规则 JSON 格式错误");
        }
    }

    private String resolveKeyword(CpsSelectionThemeDO theme, CpsSelectionRule rule) {
        if (rule.getKeywords() != null && !rule.getKeywords().isEmpty()) {
            return String.join(" ", rule.getKeywords());
        }
        return firstText(theme.getThemeName(), "今日精选");
    }

    private String resolveFirst(List<String> values, String csvFallback) {
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        if (StringUtils.hasText(csvFallback)) {
            return csvFallback.split(",")[0].trim();
        }
        return null;
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int clamp(Integer value, int defaultValue, int min, int max) {
        int resolved = value == null || value <= 0 ? defaultValue : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private String trimToMax(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private List<CpsSelectionThemeTemplateRespVO> promotionTemplates() {
        List<CpsSelectionThemeTemplateRespVO> templates = new ArrayList<>();
        templates.add(template("618_PRE_BUY", "618抢先购", "618", "抢先购高佣高券爆品",
                "618,抢先购,爆品", "洗衣液", "hot"));
        templates.add(template("618_PRESALE", "618预售", "618", "预售期定金膨胀与大额券商品",
                "618,预售", "防晒霜", "presale"));
        templates.add(template("DOUBLE11_HOT", "双11爆品", "双11", "双11高转化爆品池",
                "双11,爆品", "纸巾", "hot"));
        templates.add(template("DOUBLE12_CLEARANCE", "双12清仓", "双12", "双12清仓与补贴商品",
                "双12,清仓", "零食", "flash"));
        templates.add(template("NEW_YEAR", "年货节", "年货节", "年货礼盒与家庭囤货",
                "年货节,礼盒", "坚果礼盒", "brand"));
        templates.add(template("SCHOOL_SEASON", "开学季", "开学季", "开学文具和宿舍用品",
                "开学季,学生", "书包", "hot"));
        templates.add(template("MID_AUTUMN_GIFT", "中秋礼赠", "中秋", "中秋礼赠和团圆食品",
                "中秋,礼赠", "月饼礼盒", "brand"));
        return templates;
    }

    private CpsSelectionThemeTemplateRespVO template(String code, String name, String event, String desc,
                                                     String tags, String keyword, String channel) {
        String rule = "{\"keywords\":[\"" + keyword + "\"],\"platforms\":[\"taobao\"],\"vendorCode\":\"dataoke\","
                + "\"channelCode\":\"" + channel + "\",\"onlyCoupon\":true,\"minCommissionRate\":10,\"pullCount\":30}";
        return CpsSelectionThemeTemplateRespVO.builder()
                .templateCode(code)
                .themeName(name)
                .promotionEvent(event)
                .description(desc)
                .tags(tags)
                .ruleJson(rule)
                .aiPrompt("围绕" + name + "生成关键词、筛选条件和商品推荐理由，排序以佣金、券额、销量和活动标签为主。")
                .build();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
