package com.qiji.cps.module.cps.service.selection;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionAiReviewReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeAiRecommendReqVO;
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
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionAiReviewDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import jakarta.validation.Valid;

import java.util.List;

public interface CpsSelectionThemeService {

    Long createTheme(@Valid CpsSelectionThemeSaveReqVO createReqVO);

    void updateTheme(@Valid CpsSelectionThemeSaveReqVO updateReqVO);

    void deleteTheme(Long id);

    void deleteThemeList(List<Long> ids);

    void publishTheme(Long id);

    void offlineTheme(Long id);

    CpsSelectionThemeDO getTheme(Long id);

    PageResult<CpsSelectionThemeDO> getThemePage(CpsSelectionThemePageReqVO pageReqVO);

    CpsSelectionThemeStatsRespVO getThemeStats(CpsSelectionThemePageReqVO pageReqVO);

    List<CpsSelectionThemeDO> listPublishedThemes(String keyword, String promotionEvent);

    List<CpsSelectionThemeItemDO> listItems(Long themeId);

    PageResult<CpsSelectionThemeItemDO> getItemPage(CpsSelectionThemeItemPageReqVO pageReqVO);

    List<CpsSelectionThemeItemDO> listEnabledItems(Long themeId);

    int importItems(@Valid CpsSelectionThemeItemImportReqVO reqVO);

    void updateItemSort(@Valid CpsSelectionThemeItemSortReqVO reqVO);

    void updateItemStatus(@Valid CpsSelectionThemeItemStatusReqVO reqVO);

    void deleteItem(Long id);

    CpsSelectionThemeOperationRespVO vendorPull(@Valid CpsSelectionThemeVendorPullReqVO reqVO);

    CpsSelectionThemeOperationRespVO syncVendorThemes(@Valid CpsSelectionThemeSyncReqVO reqVO);

    CpsSelectionThemeOperationRespVO syncDataokeThemes(@Valid CpsSelectionThemeSyncReqVO reqVO);

    CpsSelectionThemeOperationRespVO aiRecommend(@Valid CpsSelectionThemeAiRecommendReqVO reqVO);

    List<CpsSelectionThemeTemplateRespVO> listPromotionTemplates();

    Long createFromTemplate(@Valid CpsSelectionThemeTemplateCreateReqVO reqVO);

    CpsSelectionThemeOperationRespVO refreshAiSavedFilter(Long id);

    CpsSelectionThemeOperationRespVO refreshAiSavedFilters();

    List<CpsSelectionAiReviewDO> listAiReviews(String reviewContextId, Long ownerUserId);

    Long upsertAiReview(@Valid CpsSelectionAiReviewReqVO reqVO, Long reviewerId);
}
