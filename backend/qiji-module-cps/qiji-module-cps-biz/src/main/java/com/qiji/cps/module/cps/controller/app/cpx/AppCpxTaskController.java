package com.qiji.cps.module.cps.controller.app.cpx;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.cpx.vo.CpxTaskRespVO;
import com.qiji.cps.module.cps.controller.app.CpsAppMemberContext;
import com.qiji.cps.module.cps.controller.app.cpx.vo.AppCpxTrackingLinkCreateReqVO;
import com.qiji.cps.module.cps.controller.app.cpx.vo.AppCpxTrackingLinkRespVO;
import com.qiji.cps.module.cps.dal.dataobject.cpx.CpxConversionDO;
import com.qiji.cps.module.cps.service.cpx.CpxTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - CPX 任务大厅")
@RestController
@RequestMapping("/cpx/task")
@Validated
public class AppCpxTaskController {

    @Resource
    private CpxTaskService taskService;

    @GetMapping("/list")
    @Operation(summary = "查询 CPX 任务大厅，默认 CPS 优先")
    public CommonResult<List<CpxTaskRespVO>> listTasks(@RequestParam(value = "keyword", required = false) String keyword,
                                                       @RequestParam(value = "promotionMethod", required = false) String promotionMethod,
                                                       @RequestParam(value = "limit", required = false) Integer limit) {
        return success(BeanUtils.toBean(taskService.listPublishedTasks(keyword, promotionMethod, limit), CpxTaskRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "查询 CPX 任务详情")
    public CommonResult<CpxTaskRespVO> getTask(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(taskService.getTask(id), CpxTaskRespVO.class));
    }

    @PostMapping("/tracking-link")
    @Operation(summary = "生成 CPX tracking link")
    public CommonResult<AppCpxTrackingLinkRespVO> generateTrackingLink(@Valid @RequestBody AppCpxTrackingLinkCreateReqVO createReqVO) {
        Long memberId = CpsAppMemberContext.requireMemberId();
        return success(BeanUtils.toBean(taskService.generateTrackingLink(createReqVO, memberId), AppCpxTrackingLinkRespVO.class));
    }

    @GetMapping("/my-conversions")
    @Operation(summary = "查询我的 CPX 转化")
    public CommonResult<List<CpxConversionDO>> getMyConversions(@RequestParam(value = "limit", required = false) Integer limit) {
        Long memberId = CpsAppMemberContext.requireMemberId();
        return success(taskService.listMemberConversions(memberId, limit));
    }
}
