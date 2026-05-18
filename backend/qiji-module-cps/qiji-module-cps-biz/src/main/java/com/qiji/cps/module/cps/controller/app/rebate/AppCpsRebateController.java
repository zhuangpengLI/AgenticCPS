package com.qiji.cps.module.cps.controller.app.rebate;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.app.rebate.vo.AppCpsRebateAccountRespVO;
import com.qiji.cps.module.cps.controller.app.rebate.vo.AppCpsRebateRecordRespVO;
import com.qiji.cps.module.cps.controller.app.rebate.vo.AppCpsRebateTokenExchangePreviewReqVO;
import com.qiji.cps.module.cps.controller.app.rebate.vo.AppCpsRebateTokenExchangeSubmitReqVO;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import com.qiji.cps.module.cps.service.exchange.dto.CpsAitokenExchangePreviewRespDTO;
import com.qiji.cps.module.cps.service.rebate.CpsRebateRecordService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

/**
 * 用户 APP - 我的返利（记录 + 账户）
 *
 * @author CPS System
 */
@Tag(name = "用户 APP - 我的返利")
@RestController
@RequestMapping("/cps/rebate")
@Validated
public class AppCpsRebateController {

    @Resource
    private CpsRebateRecordService rebateRecordService;

    @Resource
    private CpsRebateSettleService rebateSettleService;

    @Resource
    private CpsRebateTokenExchangeService rebateTokenExchangeService;

    // ========== 返利账户 ==========

    @GetMapping("/account")
    @Operation(summary = "获取我的返利账户", description = "返回当前登录会员的返利账户余额信息")
    public CommonResult<AppCpsRebateAccountRespVO> getMyAccount() {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        CpsRebateAccountDO account = rebateSettleService.getOrInitAccount(memberId);
        return success(BeanUtils.toBean(account, AppCpsRebateAccountRespVO.class));
    }

    // ========== 返利记录 ==========

    @GetMapping("/record/page")
    @Operation(summary = "获取我的返利记录分页")
    @Parameter(name = "pageNo", description = "页码，从1开始", example = "1")
    @Parameter(name = "pageSize", description = "每页数量，默认10", example = "10")
    public CommonResult<PageResult<AppCpsRebateRecordRespVO>> getMyRebateRecordPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        PageResult<CpsRebateRecordDO> pageResult =
                rebateRecordService.getMemberRebateRecordPage(memberId, pageNo, pageSize);
        return success(BeanUtils.toBean(pageResult, AppCpsRebateRecordRespVO.class));
    }

    // ========== 返利兑换 AI Token ==========

    @PostMapping("/token-exchange/preview")
    @Operation(summary = "预估返利兑换 AI Token")
    public CommonResult<CpsAitokenExchangePreviewRespDTO> previewTokenExchange(
            @Valid @RequestBody AppCpsRebateTokenExchangePreviewReqVO reqVO) {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        return success(rebateTokenExchangeService.preview(memberId, reqVO.getAmount()));
    }

    @PostMapping("/token-exchange/submit")
    @Operation(summary = "提交返利兑换 AI Token")
    public CommonResult<CpsRebateTokenExchangeOrderDO> submitTokenExchange(
            @Valid @RequestBody AppCpsRebateTokenExchangeSubmitReqVO reqVO) {
        Long memberId = SecurityFrameworkUtils.getLoginUserId();
        return success(rebateTokenExchangeService.submit(memberId, reqVO.getAmount(), reqVO.getIdempotencyKey()));
    }

    @GetMapping("/token-exchange/{exchangeOrderNo}")
    @Operation(summary = "查询返利兑换 AI Token 订单")
    public CommonResult<CpsRebateTokenExchangeOrderDO> getTokenExchangeOrder(@PathVariable String exchangeOrderNo) {
        return success(rebateTokenExchangeService.getExchangeOrder(exchangeOrderNo));
    }

}
