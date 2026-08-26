package com.qiji.cps.module.cps.service.selection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsThirdPartyActivityVendorClient;
import com.qiji.cps.module.cps.client.dataoke.DtkActivityVendorClient;
import com.qiji.cps.module.cps.client.dataoke.DtkSelectionLibraryClient;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivity;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyActivityRequest;
import com.qiji.cps.module.cps.client.dto.CpsThirdPartyPage;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.client.haodanku.activity.HaodankuActivityVendorClient;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareGoodsRespVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchReqVO;
import com.qiji.cps.module.cps.controller.admin.goods.vo.CpsGoodsSquareSearchRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeAiRecommendReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionAiReviewReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemImportReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemPageReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemSortReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemStatusReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeOperationRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemePageReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeStatsRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSyncReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeTemplateCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeTemplateRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeVendorPullReqVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionAiReviewDO;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionAiReviewMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeItemMapper;
import com.qiji.cps.module.cps.dal.mysql.selection.CpsSelectionThemeMapper;
import com.qiji.cps.module.cps.enums.CpsPlatformCodeEnum;
import com.qiji.cps.module.cps.enums.CpsVendorCodeEnum;
import com.qiji.cps.module.cps.service.goods.CpsGoodsSquareService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private static final int REFRESH_LEASE_MINUTES = 30;
    private static final DateTimeFormatter TEMPLATE_CODE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> RULE_FIELDS = Set.of(
            "keywords", "platforms", "vendorCode", "priceLowerLimit", "priceUpperLimit",
            "minCommissionRate", "minCommissionAmount", "minMonthSales", "couponAmountMin",
            "onlyCoupon", "categoryId", "channelCode", "activityTags", "sortType", "sortBy",
            "pullCount", "autoRefresh", "platformWeights", "vendorThemeSource", "externalThemeId",
            "externalThemeName", "themeListUrl", "themeListParams", "goodsListUrl", "goodsListParams",
            "prompt", "toolIntent", "mode");

    @Resource
    private CpsSelectionThemeMapper themeMapper;

    @Resource
    private CpsSelectionThemeItemMapper itemMapper;

    @Resource
    private CpsSelectionAiReviewMapper aiReviewMapper;

    @Resource
    private CpsGoodsSquareService goodsSquareService;

    @Resource
    private CpsSelectionAiRecommendService aiRecommendService;

    @Resource
    private DtkActivityVendorClient dtkActivityVendorClient;

    @Resource
    private DtkSelectionLibraryClient dtkSelectionLibraryClient;

    @Resource
    private HaodankuActivityVendorClient haodankuActivityVendorClient;

    @Resource
    private CpsPlatformClientFactory platformClientFactory;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTheme(CpsSelectionThemeSaveReqVO createReqVO) {
        validateThemeCodeUnique(createReqVO.getThemeCode(), null);
        validateTimeWindow(createReqVO.getStartTime(), createReqVO.getEndTime());
        CpsSelectionThemeDO theme = BeanUtils.toBean(createReqVO, CpsSelectionThemeDO.class);
        theme.setStatus(firstText(createReqVO.getStatus(), CpsSelectionConstants.ThemeStatus.DRAFT));
        theme.setGoodsSquareVisible(firstInt(createReqVO.getGoodsSquareVisible(), 1));
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
        updateObj.setGoodsSquareVisible(firstInt(updateReqVO.getGoodsSquareVisible(), existing.getGoodsSquareVisible(), 1));
        if (CpsSelectionConstants.ThemeStatus.PUBLISHED.equals(updateObj.getStatus())) {
            validateThemeHasEnabledItems(updateReqVO.getId());
        }
        themeMapper.updateById(updateObj);
    }

    @Override
    public void deleteTheme(Long id) {
        validateThemeExists(id);
        themeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteThemeList(List<Long> ids) {
        for (Long id : ids) {
            deleteTheme(id);
        }
    }

    @Override
    public void publishTheme(Long id) {
        CpsSelectionThemeDO theme = validateThemeExists(id);
        validateTimeWindow(theme.getStartTime(), theme.getEndTime());
        validateThemeHasEnabledItems(id);
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
    public CpsSelectionThemeStatsRespVO getThemeStats(CpsSelectionThemePageReqVO pageReqVO) {
        return CpsSelectionThemeStatsRespVO.builder()
                .total(themeMapper.countByStatus(pageReqVO, null))
                .draft(themeMapper.countByStatus(pageReqVO, CpsSelectionConstants.ThemeStatus.DRAFT))
                .published(themeMapper.countByStatus(pageReqVO, CpsSelectionConstants.ThemeStatus.PUBLISHED))
                .offline(themeMapper.countByStatus(pageReqVO, CpsSelectionConstants.ThemeStatus.OFFLINE))
                .build();
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
    public PageResult<CpsSelectionThemeItemDO> getItemPage(CpsSelectionThemeItemPageReqVO pageReqVO) {
        validateThemeExists(pageReqVO.getThemeId());
        return itemMapper.selectPage(pageReqVO);
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
                    .manualAdjusted(1)
                    .build());
        }
    }

    @Override
    public void updateItemStatus(CpsSelectionThemeItemStatusReqVO reqVO) {
        for (Long id : reqVO.getIds()) {
            validateItemExists(id);
            itemMapper.updateById(CpsSelectionThemeItemDO.builder().id(id).status(reqVO.getStatus())
                    .manualAdjusted(1).build());
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
            List<CpsGoodsSquareGoodsRespVO> pulled = pullThemeGoods(theme, rule);
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
        effectiveReqVO.setVendorCode(CpsVendorCodeEnum.DATAOKE.getCode());
        return syncVendorThemes(effectiveReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsSelectionThemeOperationRespVO syncVendorThemes(CpsSelectionThemeSyncReqVO reqVO) {
        CpsSelectionThemeSyncReqVO effectiveReqVO = reqVO == null ? new CpsSelectionThemeSyncReqVO() : reqVO;
        String vendorCode = firstText(effectiveReqVO.getVendorCode(), CpsVendorCodeEnum.DATAOKE.getCode());
        if (CpsVendorCodeEnum.DATAOKE.getCode().equals(vendorCode)) {
            return syncDataokeSelectionThemes(effectiveReqVO);
        }
        String platformCode = resolveThemeSyncPlatformCode(vendorCode);
        CpsThirdPartyActivityVendorClient activityClient = resolveThemeActivityClient(vendorCode);
        if (activityClient == null) {
            throw exception(SELECTION_THEME_STATUS_INVALID, vendorName(vendorCode) + "选品主题同步暂未接入");
        }
        CpsVendorConfig config = platformClientFactory.getVendorConfig(vendorCode, platformCode);
        if (config == null && CpsVendorCodeEnum.DATAOKE.getCode().equals(vendorCode)) {
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
            CpsThirdPartyPage<CpsThirdPartyActivity> page = activityClient.fetchActivities(
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
                CpsSelectionThemeDO theme = toVendorTheme(activity, vendorCode, goodsPullCount);
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
                    theme.setStatus(resolveSyncedThemeStatus(existing.getStatus()));
                    theme.setGoodsSquareVisible(firstInt(existing.getGoodsSquareVisible(), theme.getGoodsSquareVisible(), 1));
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
                .message(vendorName(vendorCode) + "主题同步完成，新建 " + insertedThemes + " 个，更新 " + updatedThemes
                        + " 个，跳过 " + skippedThemes + " 个，导入商品 " + importedGoods + " 个")
                .build();
    }

    private CpsSelectionThemeOperationRespVO syncDataokeSelectionThemes(CpsSelectionThemeSyncReqVO reqVO) {
        String vendorCode = CpsVendorCodeEnum.DATAOKE.getCode();
        String platformCode = CpsPlatformCodeEnum.TAOBAO.getCode();
        CpsVendorConfig config = platformClientFactory.getVendorConfig(vendorCode, platformCode);
        if (config == null) {
            throw exception(SELECTION_THEME_STATUS_INVALID, "大淘客供应商配置不存在");
        }
        int maxPages = clamp(reqVO.getMaxPages(), DEFAULT_SYNC_MAX_PAGES, 1, 20);
        int pageSize = clamp(reqVO.getPageSize(), DEFAULT_SYNC_THEME_PAGE_SIZE, 1, MAX_PULL_COUNT);
        int goodsPullCount = clamp(reqVO.getGoodsPullCount(), DEFAULT_SYNC_GOODS_PULL_COUNT, 1, MAX_PULL_COUNT);
        boolean syncGoods = !Boolean.FALSE.equals(reqVO.getSyncGoods());

        int pulledThemes = 0;
        int insertedThemes = 0;
        int updatedThemes = 0;
        int skippedThemes = 0;
        int importedGoods = 0;
        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            CpsThirdPartyPage<CpsThirdPartyActivity> page = dtkSelectionLibraryClient.fetchThemes(
                    reqVO,
                    CpsThirdPartyActivityRequest.builder()
                            .vendorCode(vendorCode)
                            .platformCode(platformCode)
                            .keyword(reqVO.getKeyword())
                            .pageNo(pageNo)
                            .pageSize(pageSize)
                            .build(),
                    config);
            if (page == null || page.getList() == null || page.getList().isEmpty()) {
                break;
            }
            for (CpsThirdPartyActivity activity : page.getList()) {
                CpsSelectionThemeDO theme = toVendorTheme(activity, vendorCode, goodsPullCount);
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
                    theme.setStatus(resolveSyncedThemeStatus(existing.getStatus()));
                    theme.setGoodsSquareVisible(firstInt(existing.getGoodsSquareVisible(), theme.getGoodsSquareVisible(), 1));
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
        String sourceName = firstText(reqVO.getThemeNamePrefix(), "大淘客选品库");
        String status = skippedThemes == 0
                ? CpsSelectionConstants.ImportTaskStatus.SUCCESS : CpsSelectionConstants.ImportTaskStatus.PARTIAL_SUCCESS;
        return CpsSelectionThemeOperationRespVO.builder()
                .status(status)
                .pulledCount(pulledThemes)
                .importedCount(importedGoods)
                .message(sourceName + "主题同步完成，新建 " + insertedThemes + " 个，更新 " + updatedThemes
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

    @Override
    public CpsSelectionThemeOperationRespVO refreshAiSavedFilter(Long id) {
        CpsSelectionThemeDO theme = validateThemeExists(id);
        if (!CpsSelectionConstants.ThemeType.AI_SAVED_FILTER.equals(theme.getThemeType())) {
            return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                    .status(CpsSelectionConstants.ImportTaskStatus.SKIPPED)
                    .message("仅支持刷新 AI 保存条件").build();
        }
        // cps_selection_theme_item.snapshot_time 使用 datetime(0)，刷新边界也统一到秒精度，
        // 避免同一秒写入的新快照因 JDBC 参数携带纳秒而被误判为旧快照。
        LocalDateTime startedAt = LocalDateTime.now().withNano(0);
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        int claimed = themeMapper.claimAiSavedFilterRefresh(id, batchNo, startedAt,
                startedAt.minusMinutes(REFRESH_LEASE_MINUTES));
        if (claimed == 0) {
            return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                    .status(CpsSelectionConstants.ImportTaskStatus.SKIPPED)
                    .message("刷新任务正在执行，请稍后重试").build();
        }
        try {
            CpsSelectionRule rule = parseRule(theme.getRuleJson());
            validateAiSavedFilterRule(rule);
            if (!hasStructuredRule(rule)) {
                String message = "保存条件仅包含自然语言提示词，需先补充结构化筛选规则后才能定时刷新";
                finishRefresh(id, batchNo, CpsSelectionConstants.ImportTaskStatus.SKIPPED, message);
                return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                        .status(CpsSelectionConstants.ImportTaskStatus.SKIPPED).message(message).build();
            }
            List<CpsGoodsSquareGoodsRespVO> pulled = pullThemeGoods(theme, rule);
            if (themeMapper.renewAiSavedFilterRefresh(id, batchNo, LocalDateTime.now()) == 0) {
                return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                        .status(CpsSelectionConstants.ImportTaskStatus.SKIPPED)
                        .message("刷新租约已失效，已放弃写入结果").build();
            }
            int imported = importRecommendedGoods(theme, pulled, rule, CpsSelectionConstants.SourceType.AUTO_REFRESH);
            if (!pulled.isEmpty()) {
                itemMapper.disableStaleAutoRefreshItems(id, startedAt);
            }
            String status = imported == pulled.size() ? CpsSelectionConstants.ImportTaskStatus.SUCCESS
                    : CpsSelectionConstants.ImportTaskStatus.PARTIAL_SUCCESS;
            String message = "结构化条件刷新完成";
            if (!finishRefresh(id, batchNo, status, message)) {
                return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                        .status(CpsSelectionConstants.ImportTaskStatus.SKIPPED)
                        .message("刷新租约已失效，结果未覆盖后续任务状态").build();
            }
            return CpsSelectionThemeOperationRespVO.builder().themeId(id).status(status)
                    .pulledCount(pulled.size()).importedCount(imported).message(message).build();
        } catch (Exception exception) {
            String message = trimToMax(exception.getMessage(), 500);
            String failureMessage = StringUtils.hasText(message) ? message : "刷新失败";
            if (!finishRefresh(id, batchNo, CpsSelectionConstants.ImportTaskStatus.FAILED, failureMessage)) {
                return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                        .status(CpsSelectionConstants.ImportTaskStatus.SKIPPED)
                        .message("刷新租约已失效，失败状态未覆盖后续任务").build();
            }
            return CpsSelectionThemeOperationRespVO.builder().themeId(id)
                    .status(CpsSelectionConstants.ImportTaskStatus.FAILED)
                    .message(failureMessage).build();
        }
    }

    @Override
    public CpsSelectionThemeOperationRespVO refreshAiSavedFilters() {
        int success = 0, skipped = 0, failed = 0, imported = 0;
        for (CpsSelectionThemeDO theme : themeMapper.selectAiSavedFilterList()) {
            try {
                if (!shouldAutoRefresh(theme.getRuleJson())) {
                    skipped++;
                    continue;
                }
                CpsSelectionThemeOperationRespVO result = refreshAiSavedFilter(theme.getId());
                imported += result.getImportedCount() == null ? 0 : result.getImportedCount();
                if (CpsSelectionConstants.ImportTaskStatus.SKIPPED.equals(result.getStatus())) skipped++;
                else if (CpsSelectionConstants.ImportTaskStatus.FAILED.equals(result.getStatus())) failed++;
                else success++;
            } catch (Exception exception) {
                failed++;
            }
        }
        String message = String.format("刷新完成：成功%d，跳过%d，失败%d，写入商品%d", success, skipped, failed, imported);
        return CpsSelectionThemeOperationRespVO.builder().status(failed > 0
                        ? CpsSelectionConstants.ImportTaskStatus.PARTIAL_SUCCESS
                        : CpsSelectionConstants.ImportTaskStatus.SUCCESS)
                .importedCount(imported).message(message).build();
    }

    @Override
    public List<CpsSelectionAiReviewDO> listAiReviews(String reviewContextId, Long ownerUserId) {
        if (!StringUtils.hasText(reviewContextId)) {
            return List.of();
        }
        return aiReviewMapper.selectListByContextId(reviewContextId.trim(), ownerUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsertAiReview(CpsSelectionAiReviewReqVO reqVO, Long reviewerId) {
        String vendorCode = normalizeOptionalIdentity(reqVO.getVendorCode());
        String goodsSign = normalizeOptionalIdentity(reqVO.getGoodsSign());
        CpsSelectionAiReviewDO existing = aiReviewMapper.selectOneByUnique(
                reqVO.getReviewContextId().trim(), reviewerId, reqVO.getPlatformCode().trim(), vendorCode,
                reqVO.getGoodsId().trim(), goodsSign);
        CpsSelectionAiReviewDO review = CpsSelectionAiReviewDO.builder()
                .reviewContextId(reqVO.getReviewContextId().trim())
                .ownerUserId(reviewerId)
                .platformCode(reqVO.getPlatformCode().trim())
                .vendorCode(vendorCode)
                .goodsId(reqVO.getGoodsId().trim())
                .goodsSign(goodsSign)
                .title(trimToMax(reqVO.getTitle(), 255))
                .mainPic(trimToMax(reqVO.getMainPic(), 1024))
                .reviewStatus(reqVO.getReviewStatus())
                .reviewerId(reviewerId)
                .reviewTime(LocalDateTime.now())
                .remark(trimToMax(reqVO.getRemark(), 500))
                .build();
        if (existing == null) {
            try {
                aiReviewMapper.insert(review);
            } catch (DuplicateKeyException duplicate) {
                existing = aiReviewMapper.selectOneByUniqueForUpdate(review.getReviewContextId(), reviewerId,
                        review.getPlatformCode(), review.getVendorCode(), review.getGoodsId(), review.getGoodsSign());
                if (existing == null) {
                    throw duplicate;
                }
                review.setId(existing.getId());
                aiReviewMapper.updateById(review);
            }
        } else {
            review.setId(existing.getId());
            aiReviewMapper.updateById(review);
        }
        return review.getId();
    }

    private boolean hasStructuredRule(CpsSelectionRule rule) {
        return rule != null && ((rule.getKeywords() != null && !rule.getKeywords().isEmpty())
                || StringUtils.hasText(rule.getCategoryId())
                || StringUtils.hasText(rule.getGoodsListUrl()));
    }

    private boolean finishRefresh(Long id, String batchNo, String status, String message) {
        return themeMapper.finishAiSavedFilterRefresh(id, batchNo, status,
                trimToMax(message, 500), LocalDateTime.now()) == 1;
    }

    private boolean shouldAutoRefresh(String ruleJson) {
        if (!StringUtils.hasText(ruleJson)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(ruleJson);
            if (root == null || !root.isObject()) {
                return true;
            }
            boolean hasUnknownField = false;
            var fields = root.fieldNames();
            while (fields.hasNext()) {
                if (!RULE_FIELDS.contains(fields.next())) {
                    hasUnknownField = true;
                    break;
                }
            }
            return hasUnknownField || root.path("autoRefresh").asBoolean(false);
        } catch (Exception ignored) {
            return true;
        }
    }

    private CpsSelectionThemeDO toVendorTheme(CpsThirdPartyActivity activity, String vendorCode, int goodsPullCount) {
        if (activity == null || !StringUtils.hasText(activity.getExternalActivityId())) {
            return null;
        }
        String themeCode = toVendorThemeCode(vendorCode, activity.getExternalActivityId());
        String themeName = firstText(activity.getActivityName(), activity.getSearchKeyword(), themeCode);
        String platformCode = firstText(activity.getPlatformCode(), CpsPlatformCodeEnum.TAOBAO.getCode());
        String displayName = vendorName(vendorCode);
        String tagText = firstText(activity.getTagText(), activity.getActivityType(), displayName);
        return CpsSelectionThemeDO.builder()
                .themeCode(themeCode)
                .themeName(trimToMax(themeName, 128))
                .themeType(resolveThemeType(vendorCode))
                .promotionEvent(trimToMax(firstText(activity.getActivityType(), tagText), 64))
                .platformCodes(platformCode)
                .vendorCode(vendorCode)
                .coverPic(activity.getMainPic())
                .description(trimToMax(activity.getShortDesc(), 512))
                .tags(trimToMax(displayName + "," + tagText, 255))
                .ruleJson(buildVendorRuleJson(activity, vendorCode, platformCode, goodsPullCount))
                .aiPrompt("围绕" + displayName + "主题“" + themeName + "”筛选高券、高佣、高转化商品，排序以佣金、券额和销量为主。")
                .status(CpsSelectionConstants.ThemeStatus.PUBLISHED)
                .goodsSquareVisible(1)
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .refreshStatus(CpsSelectionConstants.ImportTaskStatus.SUCCESS)
                .lastRefreshTime(LocalDateTime.now())
                .sort(0)
                .remark(trimToMax(activity.getExternalActivityId(), 500))
                .build();
    }

    private String resolveSyncedThemeStatus(String existingStatus) {
        if (!StringUtils.hasText(existingStatus) || CpsSelectionConstants.ThemeStatus.DRAFT.equals(existingStatus)) {
            return CpsSelectionConstants.ThemeStatus.PUBLISHED;
        }
        return existingStatus;
    }

    private int syncThemeGoods(CpsSelectionThemeDO theme, int goodsPullCount) {
        themeMapper.updateById(CpsSelectionThemeDO.builder()
                .id(theme.getId())
                .refreshStatus(CpsSelectionConstants.ImportTaskStatus.PROCESSING)
                .build());
        try {
            CpsSelectionRule rule = parseRule(theme.getRuleJson());
            rule.setPullCount(goodsPullCount);
            List<CpsGoodsSquareGoodsRespVO> pulled = pullThemeGoods(theme, rule);
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

    private String buildVendorRuleJson(CpsThirdPartyActivity activity, String vendorCode, String platformCode,
                                       int goodsPullCount) {
        CpsSelectionRule rule = new CpsSelectionRule();
        rule.setKeywords(List.of(firstText(activity.getSearchKeyword(), activity.getActivityName(), "今日精选")));
        rule.setPlatforms(List.of(platformCode));
        rule.setVendorCode(vendorCode);
        rule.setActivityTags(List.of(firstText(activity.getTagText(), activity.getActivityType(), vendorName(vendorCode))));
        rule.setOnlyCoupon(true);
        rule.setSortType(0);
        rule.setPullCount(goodsPullCount);
        enrichVendorRule(rule, activity);
        return toJson(rule);
    }

    @SuppressWarnings("unchecked")
    private void enrichVendorRule(CpsSelectionRule rule, CpsThirdPartyActivity activity) {
        if (activity == null || activity.getExtraFields() == null || activity.getExtraFields().isEmpty()) {
            return;
        }
        Map<String, Object> extra = activity.getExtraFields();
        rule.setVendorThemeSource(asString(extra.get("vendorThemeSource")));
        rule.setExternalThemeId(asString(extra.get("externalThemeId")));
        rule.setExternalThemeName(asString(extra.get("externalThemeName")));
        rule.setThemeListUrl(asString(extra.get("themeListUrl")));
        Object themeListParams = extra.get("themeListParams");
        if (themeListParams instanceof Map<?, ?> params) {
            rule.setThemeListParams((Map<String, Object>) params);
        }
        rule.setGoodsListUrl(asString(extra.get("goodsListUrl")));
        Object goodsListParams = extra.get("goodsListParams");
        if (goodsListParams instanceof Map<?, ?> params) {
            rule.setGoodsListParams((Map<String, Object>) params);
        }
    }

    private String toVendorThemeCode(String vendorCode, String externalActivityId) {
        String normalized = externalActivityId.replaceFirst("(?i)^(dtk|hdk):", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = Integer.toHexString(externalActivityId.hashCode()).toUpperCase(Locale.ROOT);
        }
        if (normalized.length() > 60) {
            normalized = normalized.substring(0, 60);
        }
        return vendorThemeCodePrefix(vendorCode) + "_" + normalized;
    }

    private CpsThirdPartyActivityVendorClient resolveThemeActivityClient(String vendorCode) {
        if (CpsVendorCodeEnum.DATAOKE.getCode().equals(vendorCode)) {
            return dtkActivityVendorClient;
        }
        if (CpsVendorCodeEnum.HAODANKU.getCode().equals(vendorCode)) {
            return haodankuActivityVendorClient;
        }
        return null;
    }

    private String resolveThemeSyncPlatformCode(String vendorCode) {
        if (CpsVendorCodeEnum.HAODANKU.getCode().equals(vendorCode)) {
            return "activity";
        }
        return CpsPlatformCodeEnum.TAOBAO.getCode();
    }

    private String resolveThemeType(String vendorCode) {
        return "VENDOR_COLUMN";
    }

    private String vendorThemeCodePrefix(String vendorCode) {
        if (CpsVendorCodeEnum.HAODANKU.getCode().equals(vendorCode)) {
            return "HDK";
        }
        if (CpsVendorCodeEnum.DATAOKE.getCode().equals(vendorCode)) {
            return "DTK";
        }
        String normalized = vendorCode.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return StringUtils.hasText(normalized) ? normalized : "VENDOR";
    }

    private String vendorName(String vendorCode) {
        CpsVendorCodeEnum vendor = CpsVendorCodeEnum.getByCode(vendorCode);
        return vendor == null ? vendorCode : vendor.getName();
    }

    private boolean isLastPage(CpsThirdPartyPage<?> page, int pageNo, int pageSize) {
        if (page.getTotal() != null && page.getTotal() >= 0) {
            return (long) pageNo * pageSize >= page.getTotal();
        }
        return page.getList() == null || page.getList().size() < pageSize;
    }

    private List<CpsGoodsSquareGoodsRespVO> pullThemeGoods(CpsSelectionThemeDO theme, CpsSelectionRule rule) {
        int pullCount = clamp(rule.getPullCount(), DEFAULT_PULL_COUNT, 1, MAX_PULL_COUNT);
        if (CpsVendorCodeEnum.DATAOKE.getCode().equals(firstText(rule.getVendorCode(), theme.getVendorCode()))
                && StringUtils.hasText(rule.getGoodsListUrl())) {
            CpsVendorConfig config = platformClientFactory.getVendorConfig(
                    CpsVendorCodeEnum.DATAOKE.getCode(), CpsPlatformCodeEnum.TAOBAO.getCode());
            if (config != null) {
                List<CpsGoodsSquareGoodsRespVO> apiGoods =
                        dtkSelectionLibraryClient.fetchThemeGoods(rule, pullCount, config);
                if (apiGoods != null && !apiGoods.isEmpty()) {
                    return apiGoods;
                }
            }
        }
        return searchCandidates(theme, rule);
    }

    private List<CpsGoodsSquareGoodsRespVO> searchCandidates(CpsSelectionThemeDO theme, CpsSelectionRule rule) {
        int pullCount = clamp(rule.getPullCount(), DEFAULT_PULL_COUNT, 1, MAX_PULL_COUNT);
        CpsGoodsSquareSearchReqVO searchReqVO = buildSearchReqVO(theme, rule);
        searchReqVO.setPageSize(pullCount);
        CpsGoodsSquareSearchRespVO response = goodsSquareService.searchGoods(searchReqVO);
        List<CpsGoodsSquareGoodsRespVO> exactMatched = distinctGoods(response, pullCount);
        if (!exactMatched.isEmpty()) {
            return exactMatched;
        }

        List<CpsGoodsSquareGoodsRespVO> fallbackMatched = new ArrayList<>();
        Set<String> existingKeys = new LinkedHashSet<>();
        for (String keyword : resolveFallbackKeywords(theme, rule, searchReqVO.getKeyword())) {
            CpsGoodsSquareSearchReqVO fallbackReqVO = buildRelaxedSearchReqVO(theme, rule, keyword, pullCount);
            CpsGoodsSquareSearchRespVO fallbackResponse = goodsSquareService.searchGoods(fallbackReqVO);
            appendDistinctGoods(fallbackMatched, existingKeys, fallbackResponse, pullCount);
            if (!fallbackMatched.isEmpty()) {
                break;
            }
        }
        return fallbackMatched;
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

    private CpsGoodsSquareSearchReqVO buildRelaxedSearchReqVO(CpsSelectionThemeDO theme, CpsSelectionRule rule,
                                                              String keyword, int pullCount) {
        CpsGoodsSquareSearchReqVO reqVO = buildSearchReqVO(theme, rule);
        reqVO.setKeyword(keyword);
        reqVO.setPageSize(pullCount);
        reqVO.setPriceLowerLimit(null);
        reqVO.setPriceUpperLimit(null);
        reqVO.setHasCoupon(null);
        reqVO.setCategoryId(null);
        reqVO.setChannelCode(null);
        reqVO.setMinCommissionRate(null);
        reqVO.setMinCommissionAmount(null);
        reqVO.setMinMonthSales(null);
        reqVO.setCouponAmountMin(null);
        reqVO.setActivityTag(null);
        return reqVO;
    }

    private List<CpsGoodsSquareGoodsRespVO> distinctGoods(CpsGoodsSquareSearchRespVO response, int limit) {
        List<CpsGoodsSquareGoodsRespVO> result = new ArrayList<>();
        appendDistinctGoods(result, new LinkedHashSet<>(), response, limit);
        return result;
    }

    private void appendDistinctGoods(List<CpsGoodsSquareGoodsRespVO> result, Set<String> existingKeys,
                                     CpsGoodsSquareSearchRespVO response, int limit) {
        if (response == null || response.getList() == null || response.getList().isEmpty()) {
            return;
        }
        for (CpsGoodsSquareGoodsRespVO goods : response.getList()) {
            if (goods == null) {
                continue;
            }
            if (existingKeys.add(goodsKey(goods))) {
                result.add(goods);
            }
            if (result.size() >= limit) {
                return;
            }
        }
    }

    private String goodsKey(CpsGoodsSquareGoodsRespVO goods) {
        return firstText(goods.getPlatformCode(), "") + "|"
                + firstText(goods.getVendorCode(), "") + "|"
                + firstText(goods.getGoodsId(), "") + "|"
                + firstText(goods.getGoodsSign(), "") + "|"
                + firstText(goods.getTitle(), "");
    }

    private List<String> resolveFallbackKeywords(CpsSelectionThemeDO theme, CpsSelectionRule rule, String exactKeyword) {
        List<String> keywords = new ArrayList<>();
        collectKeywordCandidates(keywords, exactKeyword);
        if (rule.getKeywords() != null) {
            rule.getKeywords().forEach(keyword -> collectKeywordCandidates(keywords, keyword));
        }
        collectKeywordCandidates(keywords, theme.getThemeName());
        collectKeywordCandidates(keywords, theme.getPromotionEvent());
        collectKeywordCandidates(keywords, theme.getTags());
        if (rule.getActivityTags() != null) {
            rule.getActivityTags().forEach(keyword -> collectKeywordCandidates(keywords, keyword));
        }
        addUniqueKeyword(keywords, "领券");
        addUniqueKeyword(keywords, "热卖");
        keywords.removeIf(keyword -> keyword.equals(exactKeyword));
        return keywords;
    }

    private void collectKeywordCandidates(List<String> keywords, String rawKeyword) {
        if (!StringUtils.hasText(rawKeyword)) {
            return;
        }
        for (String segment : rawKeyword.split("[,，、/|\\s]+")) {
            addExpandedKeyword(keywords, segment);
        }
    }

    private void addExpandedKeyword(List<String> keywords, String rawKeyword) {
        if (!StringUtils.hasText(rawKeyword)) {
            return;
        }
        String keyword = rawKeyword.trim();
        addUniqueKeyword(keywords, keyword);
        String normalized = keyword
                .replace("自营", "")
                .replace("优惠", "")
                .replace("福利", "")
                .replace("攻略", "")
                .replace("会场", "")
                .replace("专场", "")
                .replace("主题", "")
                .replace("活动", "")
                .replace("中心", "")
                .replace("首页", "")
                .replace("频道", "")
                .replace("入口", "")
                .replace("商品库", "")
                .replace("商品列表", "")
                .trim();
        addUniqueKeyword(keywords, normalized);
        if (keyword.contains("天猫国际") || keyword.contains("进口") || keyword.toLowerCase(Locale.ROOT).contains("global")) {
            addUniqueKeyword(keywords, "进口");
        }
        if (keyword.contains("领券") || keyword.contains("优惠券") || keyword.contains("满减")) {
            addUniqueKeyword(keywords, "领券");
        }
        if (keyword.contains("品牌")) {
            addUniqueKeyword(keywords, "品牌");
        }
        if (keyword.contains("热") || keyword.contains("爆")) {
            addUniqueKeyword(keywords, "热卖");
        }
        if (keyword.contains("天猫超市") || keyword.contains("超市")) {
            addUniqueKeyword(keywords, "天猫超市");
        }
        if (keyword.contains("淘金币")) {
            addUniqueKeyword(keywords, "淘金币");
        }
        if (keyword.contains("美妆")) {
            addUniqueKeyword(keywords, "美妆");
        }
        if (keyword.contains("家居")) {
            addUniqueKeyword(keywords, "家居");
        }
        if (keyword.contains("母婴")) {
            addUniqueKeyword(keywords, "母婴");
        }
        if (keyword.contains("零食") || keyword.contains("美食") || keyword.contains("食品")) {
            addUniqueKeyword(keywords, "零食");
        }
    }

    private void addUniqueKeyword(List<String> keywords, String keyword) {
        if (StringUtils.hasText(keyword) && !keywords.contains(keyword)) {
            keywords.add(keyword);
        }
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
                themeId, item.getPlatformCode(), normalizeOptionalIdentity(item.getVendorCode()), item.getGoodsId(),
                normalizeOptionalIdentity(item.getGoodsSign()));
        CpsSelectionThemeItemDO saveObj = BeanUtils.toBean(item, CpsSelectionThemeItemDO.class);
        saveObj.setThemeId(themeId);
        saveObj.setVendorCode(normalizeOptionalIdentity(item.getVendorCode()));
        saveObj.setGoodsSign(normalizeOptionalIdentity(item.getGoodsSign()));
        saveObj.setSourceType(firstText(sourceType, CpsSelectionConstants.SourceType.MANUAL));
        saveObj.setStatus(firstText(item.getStatus(), CpsSelectionConstants.ItemStatus.ENABLED));
        saveObj.setTopFlag(item.getTopFlag() == null ? 0 : item.getTopFlag());
        saveObj.setManualAdjusted(0);
        saveObj.setSnapshotTime(LocalDateTime.now());
        saveObj.setSort(item.getSort() == null ? 0 : item.getSort());
        if (exists == null) {
            itemMapper.insert(saveObj);
        } else {
            saveObj.setId(exists.getId());
            if (CpsSelectionConstants.SourceType.AUTO_REFRESH.equals(sourceType)) {
                saveObj.setSourceType(exists.getSourceType());
                saveObj.setManualAdjusted(exists.getManualAdjusted());
                boolean preserveManualState = Integer.valueOf(1).equals(exists.getManualAdjusted())
                        || !CpsSelectionConstants.SourceType.AUTO_REFRESH.equals(exists.getSourceType());
                if (preserveManualState) {
                    saveObj.setStatus(exists.getStatus());
                    saveObj.setTopFlag(exists.getTopFlag());
                    saveObj.setSort(exists.getSort());
                }
            }
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

    private void validateThemeHasEnabledItems(Long themeId) {
        if (itemMapper.selectEnabledListByThemeId(themeId).isEmpty()) {
            throw exception(SELECTION_THEME_STATUS_INVALID, "发布前至少需要一个启用商品");
        }
    }

    private CpsSelectionRule parseRule(String ruleJson) {
        if (!StringUtils.hasText(ruleJson)) {
            return new CpsSelectionRule();
        }
        try {
            JsonNode root = objectMapper.readTree(ruleJson);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("主题规则必须是 JSON 对象");
            }
            root.fieldNames().forEachRemaining(field -> {
                if (!RULE_FIELDS.contains(field)) {
                    throw new IllegalArgumentException("主题规则包含不支持的字段：" + field);
                }
            });
            CpsSelectionRule rule = objectMapper.treeToValue(root, CpsSelectionRule.class);
            validateRuleRanges(rule);
            return rule;
        } catch (Exception e) {
            String message = StringUtils.hasText(e.getMessage()) ? e.getMessage() : "主题规则 JSON 格式错误";
            throw exception(SELECTION_THEME_STATUS_INVALID, trimToMax(message, 500));
        }
    }

    private void validateRuleRanges(CpsSelectionRule rule) {
        if (rule.getPullCount() != null && (rule.getPullCount() < 1 || rule.getPullCount() > MAX_PULL_COUNT)) {
            throw new IllegalArgumentException("候选数量必须在 1 到 100 之间");
        }
        requireNonNegative(rule.getPriceLowerLimit(), "最低价格");
        requireNonNegative(rule.getPriceUpperLimit(), "最高价格");
        requireNonNegative(rule.getMinCommissionAmount(), "最低佣金金额");
        requireNonNegative(rule.getCouponAmountMin(), "最低优惠券金额");
        if (rule.getMinCommissionRate() != null
                && (rule.getMinCommissionRate().signum() < 0
                || rule.getMinCommissionRate().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("最低佣金率必须在 0 到 100 之间");
        }
        if (rule.getMinMonthSales() != null && rule.getMinMonthSales() < 0) {
            throw new IllegalArgumentException("最低月销量不能小于 0");
        }
        if (rule.getPriceLowerLimit() != null && rule.getPriceUpperLimit() != null
                && rule.getPriceLowerLimit().compareTo(rule.getPriceUpperLimit()) > 0) {
            throw new IllegalArgumentException("最低价格不能高于最高价格");
        }
        if (rule.getSortType() != null && (rule.getSortType() < 0 || rule.getSortType() > 4)) {
            throw new IllegalArgumentException("排序类型必须在 0 到 4 之间");
        }
        if (rule.getKeywords() != null) {
            if (rule.getKeywords().size() > 10) {
                throw new IllegalArgumentException("关键词数量不能超过 10 个");
            }
            for (String keyword : rule.getKeywords()) {
                if (!StringUtils.hasText(keyword) || keyword.trim().length() > 64) {
                    throw new IllegalArgumentException("关键词不能为空且长度不能超过 64 个字符");
                }
            }
        }
        if (rule.getPlatformWeights() != null && rule.getPlatformWeights().values().stream()
                .anyMatch(value -> value == null || value.signum() < 0)) {
            throw new IllegalArgumentException("平台权重不能小于 0");
        }
    }

    private void validateAiSavedFilterRule(CpsSelectionRule rule) {
        validateSafeVendorPath(rule.getThemeListUrl(), "主题列表 URL");
        validateSafeVendorPath(rule.getGoodsListUrl(), "商品列表 URL");
        if ((StringUtils.hasText(rule.getThemeListUrl()) || StringUtils.hasText(rule.getGoodsListUrl()))
                && !CpsVendorCodeEnum.DATAOKE.getCode().equals(firstText(rule.getVendorCode(),
                CpsVendorCodeEnum.DATAOKE.getCode()))) {
            throw new IllegalArgumentException("AI 保存条件只允许使用已配置的大淘客相对接口路径");
        }
    }

    private void validateSafeVendorPath(String path, String fieldName) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        if (!(path.startsWith("/api/") || path.startsWith("/open-api/"))
                || path.startsWith("//") || path.contains("://") || path.contains("\\")) {
            throw new IllegalArgumentException(fieldName + " 只允许已配置供应商的相对接口路径");
        }
    }

    private void requireNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于 0");
        }
    }

    private String normalizeOptionalIdentity(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
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

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private Integer firstInt(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
