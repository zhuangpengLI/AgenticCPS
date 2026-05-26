package com.qiji.cps.module.cps.controller.admin.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxDashboardRespVO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPX 看板")
@RestController
@RequestMapping("/cpx/dashboard")
@Validated
public class CpxDashboardController {

    @Resource
    private CpxTaskService taskService;

    @GetMapping("/summary")
    @Operation(summary = "获取 CPX 任务漏斗和结算汇总")
    @PreAuthorize("@ss.hasPermission('cpx:dashboard:query')")
    public CommonResult<CpxDashboardRespVO> getDashboardSummary() {
        return success(taskService.getDashboardSummary());
    }
}
