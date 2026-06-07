package com.qiji.cps.module.cps.controller.admin.activity;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityCenterRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityPageReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivityRespVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySaveReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySyncReqVO;
import com.qiji.cps.module.cps.controller.admin.activity.vo.CpsRebateActivitySyncRespVO;
import com.qiji.cps.module.cps.dal.dataobject.activity.CpsRebateActivityDO;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivityService;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivitySyncRequest;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivitySyncResult;
import com.qiji.cps.module.cps.service.activity.CpsRebateActivitySyncServiceImpl;
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

@Tag(name = "管理后台 - CPS返利活动广场")
@RestController
@RequestMapping("/cps/rebate-activity")
@Validated
public class CpsRebateActivityController {

    @Resource
    private CpsRebateActivityService activityService;

    @Resource
    private CpsRebateActivitySyncServiceImpl activitySyncService;

    @PostMapping("/create")
    @Operation(summary = "创建返利活动")
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:create')")
    public CommonResult<Long> createActivity(@Valid @RequestBody CpsRebateActivitySaveReqVO createReqVO) {
        return success(activityService.createActivity(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新返利活动")
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:update')")
    public CommonResult<Boolean> updateActivity(@Valid @RequestBody CpsRebateActivitySaveReqVO updateReqVO) {
        activityService.updateActivity(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除返利活动")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:delete')")
    public CommonResult<Boolean> deleteActivity(@RequestParam("id") Long id) {
        activityService.deleteActivity(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得返利活动")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:query')")
    public CommonResult<CpsRebateActivityRespVO> getActivity(@RequestParam("id") Long id) {
        CpsRebateActivityDO activity = activityService.getActivity(id);
        return success(BeanUtils.toBean(activity, CpsRebateActivityRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得返利活动分页")
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:query')")
    public CommonResult<PageResult<CpsRebateActivityRespVO>> getActivityPage(
            @Valid CpsRebateActivityPageReqVO pageReqVO) {
        PageResult<CpsRebateActivityDO> pageResult = activityService.getActivityPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsRebateActivityRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获得当前启用返利活动列表")
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:query')")
    public CommonResult<List<CpsRebateActivityRespVO>> getEnabledActivityList() {
        List<CpsRebateActivityDO> list = activityService.getEnabledActivityList();
        return success(BeanUtils.toBean(list, CpsRebateActivityRespVO.class));
    }

    @GetMapping("/center")
    @Operation(summary = "获得活动中心聚合卡片")
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:query')")
    public CommonResult<CpsRebateActivityCenterRespVO> getActivityCenter(
            @Valid CpsRebateActivityCenterReqVO reqVO) {
        return success(activityService.getActivityCenter(reqVO));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步第三方返利活动")
    @PreAuthorize("@ss.hasPermission('cps:rebate-activity:update')")
    public CommonResult<CpsRebateActivitySyncRespVO> syncActivities(
            @Valid @RequestBody CpsRebateActivitySyncReqVO reqVO) {
        CpsRebateActivitySyncRequest request = BeanUtils.toBean(reqVO, CpsRebateActivitySyncRequest.class);
        CpsRebateActivitySyncResult result = activitySyncService.syncThirdPartyActivities(request);
        return success(BeanUtils.toBean(result, CpsRebateActivitySyncRespVO.class));
    }

}
