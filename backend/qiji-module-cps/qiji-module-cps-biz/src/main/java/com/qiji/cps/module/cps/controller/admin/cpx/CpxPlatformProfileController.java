package com.qiji.cps.module.cps.controller.admin.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxPlatformProfileSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxPlatformProfileDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPX 平台对接中心")
@RestController
@RequestMapping("/cpx/platform-profile")
@Validated
public class CpxPlatformProfileController {

    @Resource
    private CpxTaskService taskService;

    @PostMapping("/create")
    @Operation(summary = "创建平台对接档案")
    @PreAuthorize("@ss.hasPermission('cpx:platform:create')")
    public CommonResult<Long> createPlatformProfile(@Valid @RequestBody CpxPlatformProfileSaveReqVO createReqVO) {
        return success(taskService.createPlatformProfile(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新平台对接档案")
    @PreAuthorize("@ss.hasPermission('cpx:platform:update')")
    public CommonResult<Boolean> updatePlatformProfile(@Valid @RequestBody CpxPlatformProfileSaveReqVO updateReqVO) {
        taskService.updatePlatformProfile(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取平台对接档案详情")
    @PreAuthorize("@ss.hasPermission('cpx:platform:query')")
    public CommonResult<CpxPlatformProfileDO> getPlatformProfile(@RequestParam("id") Long id) {
        return success(taskService.getPlatformProfile(id));
    }

    @GetMapping("/list")
    @Operation(summary = "查询平台对接档案")
    @PreAuthorize("@ss.hasPermission('cpx:platform:query')")
    public CommonResult<List<CpxPlatformProfileDO>> listPlatformProfiles() {
        return success(taskService.listPlatformProfiles());
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "查询可用平台对接档案")
    @PreAuthorize("@ss.hasPermission('cpx:platform:query')")
    public CommonResult<List<CpxPlatformProfileDO>> listEnabledPlatformProfiles() {
        return success(taskService.listEnabledPlatformProfiles());
    }
}
