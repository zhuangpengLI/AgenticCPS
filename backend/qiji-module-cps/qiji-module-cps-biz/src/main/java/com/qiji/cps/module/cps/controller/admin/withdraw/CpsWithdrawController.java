package com.qiji.cps.module.cps.controller.admin.withdraw;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.withdraw.vo.CpsWithdrawPageReqVO;
import com.qiji.cps.module.cps.controller.admin.withdraw.vo.CpsWithdrawRespVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.service.withdraw.CpsWithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS提现管理 Controller
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS提现管理")
@RestController
@RequestMapping("/cps/withdraw")
@Validated
public class CpsWithdrawController {

    @Resource
    private CpsWithdrawService withdrawService;

    @GetMapping("/page")
    @Operation(summary = "提现申请分页查询")
    @PreAuthorize("@ss.hasPermission('cps:withdraw:query')")
    public CommonResult<PageResult<CpsWithdrawRespVO>> getWithdrawPage(
            @Valid CpsWithdrawPageReqVO reqVO) {
        PageResult<CpsWithdrawDO> page = withdrawService.getWithdrawPage(reqVO);
        return success(BeanUtils.toBean(page, CpsWithdrawRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取提现申请详情")
    @Parameter(name = "id", description = "提现ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:withdraw:query')")
    public CommonResult<CpsWithdrawRespVO> getWithdraw(@RequestParam("id") Long id) {
        CpsWithdrawDO withdraw = withdrawService.getWithdraw(id);
        return success(BeanUtils.toBean(withdraw, CpsWithdrawRespVO.class));
    }

    @PutMapping("/approve")
    @Operation(summary = "审核通过提现申请")
    @Parameter(name = "id", description = "提现ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:withdraw:audit')")
    public CommonResult<Boolean> approveWithdraw(
            @RequestParam("id") Long id,
            @RequestParam(value = "reviewNote", required = false) String reviewNote) {
        withdrawService.approveWithdraw(id, reviewNote);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回提现申请")
    @Parameter(name = "id", description = "提现ID", required = true)
    @PreAuthorize("@ss.hasPermission('cps:withdraw:audit')")
    public CommonResult<Boolean> rejectWithdraw(
            @RequestParam("id") Long id,
            @RequestParam("reviewNote") String reviewNote) {
        withdrawService.rejectWithdraw(id, reviewNote);
        return success(true);
    }

}
