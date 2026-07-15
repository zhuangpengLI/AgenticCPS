package com.qiji.cps.module.cps.controller.app.withdraw;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.app.withdraw.vo.AppCpsWithdrawCreateReqVO;
import com.qiji.cps.module.cps.controller.app.withdraw.vo.AppCpsWithdrawRespVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.service.withdraw.CpsWithdrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 APP - CPS 提现")
@RestController
@RequestMapping("/cps/withdraw")
@Validated
public class AppCpsWithdrawController {

    @Resource private CpsWithdrawService withdrawService;

    @PostMapping("/create")
    @Operation(summary = "申请提现")
    public CommonResult<Long> createWithdraw(@Valid @RequestBody AppCpsWithdrawCreateReqVO request) {
        return success(withdrawService.createWithdraw(SecurityFrameworkUtils.getLoginUserId(), request));
    }

    @GetMapping("/page")
    @Operation(summary = "获取我的提现记录")
    public CommonResult<PageResult<AppCpsWithdrawRespVO>> getMyWithdrawPage(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<CpsWithdrawDO> page = withdrawService.getMemberWithdrawPage(
                SecurityFrameworkUtils.getLoginUserId(), pageNo, pageSize);
        return success(BeanUtils.toBean(page, AppCpsWithdrawRespVO.class));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取我的提现详情")
    public CommonResult<AppCpsWithdrawRespVO> getMyWithdraw(@PathVariable Long id) {
        return success(BeanUtils.toBean(withdrawService.getMemberWithdraw(
                SecurityFrameworkUtils.getLoginUserId(), id), AppCpsWithdrawRespVO.class));
    }
}
