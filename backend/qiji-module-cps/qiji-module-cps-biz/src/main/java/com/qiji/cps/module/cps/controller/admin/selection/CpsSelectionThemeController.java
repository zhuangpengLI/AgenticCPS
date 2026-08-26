package com.qiji.cps.module.cps.controller.admin.selection;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeAiRecommendReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionAiReviewReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionAiReviewRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemImportReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemPageReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemSortReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeItemStatusReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeOperationRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemePageReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeStatsRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeSyncReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeTemplateCreateReqVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeTemplateRespVO;
import com.qiji.cps.module.cps.controller.admin.selection.vo.CpsSelectionThemeVendorPullReqVO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionThemeItemDO;
import com.qiji.cps.module.cps.dal.dataobject.selection.CpsSelectionAiReviewDO;
import com.qiji.cps.module.cps.service.selection.CpsSelectionThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - CPS选品库")
@RestController
@RequestMapping("/cps/selection-theme")
@Validated
public class CpsSelectionThemeController {

    @Resource
    private CpsSelectionThemeService selectionThemeService;

    @PostMapping("/create")
    @Operation(summary = "创建选品主题")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:create')")
    public CommonResult<Long> createTheme(@Valid @RequestBody CpsSelectionThemeSaveReqVO createReqVO) {
        return success(selectionThemeService.createTheme(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新选品主题")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<Boolean> updateTheme(@Valid @RequestBody CpsSelectionThemeSaveReqVO updateReqVO) {
        selectionThemeService.updateTheme(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除选品主题")
    @Parameter(name = "id", description = "主题ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:delete')")
    public CommonResult<Boolean> deleteTheme(@RequestParam("id") Long id) {
        selectionThemeService.deleteTheme(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除选品主题")
    @Parameter(name = "ids", description = "主题ID列表", required = true)
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:delete')")
    public CommonResult<Boolean> deleteThemeList(@RequestParam("ids") List<Long> ids) {
        selectionThemeService.deleteThemeList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得选品主题")
    @Parameter(name = "id", description = "主题ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<CpsSelectionThemeRespVO> getTheme(@RequestParam("id") Long id) {
        CpsSelectionThemeDO theme = selectionThemeService.getTheme(id);
        return success(BeanUtils.toBean(theme, CpsSelectionThemeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "选品主题分页")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<PageResult<CpsSelectionThemeRespVO>> getThemePage(
            @Valid CpsSelectionThemePageReqVO pageReqVO) {
        PageResult<CpsSelectionThemeDO> pageResult = selectionThemeService.getThemePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsSelectionThemeRespVO.class));
    }

    @GetMapping("/stats")
    @Operation(summary = "选品主题统计")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<CpsSelectionThemeStatsRespVO> getThemeStats(
            @Valid CpsSelectionThemePageReqVO pageReqVO) {
        return success(selectionThemeService.getThemeStats(pageReqVO));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布选品主题")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:publish')")
    public CommonResult<Boolean> publishTheme(@RequestParam("id") Long id) {
        selectionThemeService.publishTheme(id);
        return success(true);
    }

    @PutMapping("/offline")
    @Operation(summary = "下线选品主题")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:offline')")
    public CommonResult<Boolean> offlineTheme(@RequestParam("id") Long id) {
        selectionThemeService.offlineTheme(id);
        return success(true);
    }

    @PostMapping("/ai-recommend")
    @Operation(summary = "AI 推荐商品入库")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<CpsSelectionThemeOperationRespVO> aiRecommend(
            @Valid @RequestBody CpsSelectionThemeAiRecommendReqVO reqVO) {
        return success(selectionThemeService.aiRecommend(reqVO));
    }

    @PostMapping("/vendor-pull")
    @Operation(summary = "第三方平台拉取商品入库")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<CpsSelectionThemeOperationRespVO> vendorPull(
            @Valid @RequestBody CpsSelectionThemeVendorPullReqVO reqVO) {
        return success(selectionThemeService.vendorPull(reqVO));
    }

    @PostMapping("/dataoke-theme-sync")
    @Operation(summary = "同步大淘客主题和主题商品")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<CpsSelectionThemeOperationRespVO> syncDataokeThemes(
            @Valid @RequestBody CpsSelectionThemeSyncReqVO reqVO) {
        return success(selectionThemeService.syncDataokeThemes(reqVO));
    }

    @PostMapping("/vendor-theme-sync")
    @Operation(summary = "同步供应商选品主题和主题商品")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<CpsSelectionThemeOperationRespVO> syncVendorThemes(
            @Valid @RequestBody CpsSelectionThemeSyncReqVO reqVO) {
        return success(selectionThemeService.syncVendorThemes(reqVO));
    }

    @GetMapping("/items/list")
    @Operation(summary = "查询主题商品快照")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<List<CpsSelectionThemeItemRespVO>> listItems(@RequestParam("themeId") Long themeId) {
        List<CpsSelectionThemeItemDO> list = selectionThemeService.listItems(themeId);
        return success(BeanUtils.toBean(list, CpsSelectionThemeItemRespVO.class));
    }

    @GetMapping("/items/page")
    @Operation(summary = "分页查询主题商品快照")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<PageResult<CpsSelectionThemeItemRespVO>> getItemPage(
            @Valid CpsSelectionThemeItemPageReqVO pageReqVO) {
        PageResult<CpsSelectionThemeItemDO> pageResult = selectionThemeService.getItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsSelectionThemeItemRespVO.class));
    }

    @PostMapping("/items/import")
    @Operation(summary = "人工导入主题商品快照")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<Integer> importItems(@Valid @RequestBody CpsSelectionThemeItemImportReqVO reqVO) {
        return success(selectionThemeService.importItems(reqVO));
    }

    @PutMapping("/items/sort")
    @Operation(summary = "更新主题商品排序")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<Boolean> updateItemSort(@Valid @RequestBody CpsSelectionThemeItemSortReqVO reqVO) {
        selectionThemeService.updateItemSort(reqVO);
        return success(true);
    }

    @PutMapping("/items/status")
    @Operation(summary = "更新主题商品状态")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<Boolean> updateItemStatus(@Valid @RequestBody CpsSelectionThemeItemStatusReqVO reqVO) {
        selectionThemeService.updateItemStatus(reqVO);
        return success(true);
    }

    @DeleteMapping("/items/delete")
    @Operation(summary = "删除主题商品快照")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<Boolean> deleteItem(@RequestParam("id") Long id) {
        selectionThemeService.deleteItem(id);
        return success(true);
    }

    @GetMapping("/templates")
    @Operation(summary = "获得大促主题模板")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<List<CpsSelectionThemeTemplateRespVO>> listPromotionTemplates() {
        return success(selectionThemeService.listPromotionTemplates());
    }

    @PostMapping("/templates/create")
    @Operation(summary = "按大促模板创建主题草稿")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:create')")
    public CommonResult<Long> createFromTemplate(@Valid @RequestBody CpsSelectionThemeTemplateCreateReqVO reqVO) {
        return success(selectionThemeService.createFromTemplate(reqVO));
    }

    @PostMapping("/ai-saved-filters/refresh")
    @Operation(summary = "刷新 AI 保存条件")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<CpsSelectionThemeOperationRespVO> refreshAiSavedFilter(@RequestParam("id") Long id) {
        return success(selectionThemeService.refreshAiSavedFilter(id));
    }

    @GetMapping("/ai-reviews/list")
    @Operation(summary = "查询 AI 选品人工复核记录")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:query')")
    public CommonResult<List<CpsSelectionAiReviewRespVO>> listAiReviews(
            @RequestParam("reviewContextId") String reviewContextId) {
        List<CpsSelectionAiReviewDO> list = selectionThemeService.listAiReviews(reviewContextId, getLoginUserId());
        return success(BeanUtils.toBean(list, CpsSelectionAiReviewRespVO.class));
    }

    @PutMapping("/ai-reviews")
    @Operation(summary = "记录 AI 选品人工复核结果")
    @PreAuthorize("@ss.hasPermission('cps:selection-theme:update')")
    public CommonResult<Long> upsertAiReview(@Valid @RequestBody CpsSelectionAiReviewReqVO reqVO) {
        return success(selectionThemeService.upsertAiReview(reqVO, getLoginUserId()));
    }
}
