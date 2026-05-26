package com.qiji.cps.module.cps.controller.admin.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxTaskListReqVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxTaskRespVO;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxTaskSaveReqVO;
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

@Tag(name = "管理后台 - CPX 任务中心")
@RestController
@RequestMapping("/cpx/task")
@Validated
public class CpxTaskController {

    @Resource
    private CpxTaskService taskService;

    @PostMapping("/create")
    @Operation(summary = "创建 CPX 任务")
    @PreAuthorize("@ss.hasPermission('cpx:task:create')")
    public CommonResult<Long> createTask(@Valid @RequestBody CpxTaskSaveReqVO createReqVO) {
        return success(taskService.createTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 CPX 任务")
    @PreAuthorize("@ss.hasPermission('cpx:task:update')")
    public CommonResult<Boolean> updateTask(@Valid @RequestBody CpxTaskSaveReqVO updateReqVO) {
        taskService.updateTask(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取 CPX 任务详情")
    @PreAuthorize("@ss.hasPermission('cpx:task:query')")
    public CommonResult<CpxTaskRespVO> getTask(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(taskService.getTask(id), CpxTaskRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取 CPX 任务列表，CPS 优先")
    @PreAuthorize("@ss.hasPermission('cpx:task:query')")
    public CommonResult<List<CpxTaskRespVO>> listTasks(@Valid CpxTaskListReqVO reqVO) {
        return success(BeanUtils.toBean(taskService.listAdminTasks(reqVO.getKeyword(), reqVO.getPromotionMethod(),
                reqVO.getLimit()), CpxTaskRespVO.class));
    }
}
