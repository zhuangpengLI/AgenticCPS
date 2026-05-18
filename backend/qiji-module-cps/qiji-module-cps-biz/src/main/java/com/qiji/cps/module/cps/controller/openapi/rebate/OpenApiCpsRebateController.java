package com.qiji.cps.module.cps.controller.openapi.rebate;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.openapi.rebate.vo.*;
import com.qiji.cps.module.cps.service.exchange.CpsOpenApiSignatureService;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "开放接口 - CPS返利资产")
@RestController
@RequestMapping("/openapi/cps/rebate")
@Validated
@PermitAll
public class OpenApiCpsRebateController {

    @Resource
    private CpsRebateTokenExchangeService exchangeService;

    @Resource
    private CpsOpenApiSignatureService signatureService;

    @GetMapping("/balance")
    @Operation(summary = "查询返利余额")
    public CommonResult<OpenApiCpsRebateBalanceRespVO> getBalance(@RequestParam("userId") Long userId,
                                                                  HttpServletRequest request) {
        signatureService.verify(request, null);
        return success(exchangeService.getBalance(userId));
    }

    @PostMapping("/freeze")
    @Operation(summary = "冻结返利")
    public CommonResult<OpenApiCpsRebateFreezeRespVO> freeze(@Valid @RequestBody OpenApiCpsRebateFreezeReqVO reqVO,
                                                             HttpServletRequest request) {
        signatureService.verify(request, reqVO);
        return success(exchangeService.freeze(reqVO));
    }

    @PostMapping("/unfreeze")
    @Operation(summary = "解冻返利")
    public CommonResult<Boolean> unfreeze(@Valid @RequestBody OpenApiCpsRebateUnfreezeReqVO reqVO,
                                          HttpServletRequest request) {
        signatureService.verify(request, reqVO);
        exchangeService.unfreeze(reqVO.getFreezeId(), reqVO.getReason());
        return success(true);
    }

    @PostMapping("/confirm-deduct")
    @Operation(summary = "确认扣减冻结返利")
    public CommonResult<Boolean> confirmDeduct(@Valid @RequestBody OpenApiCpsRebateConfirmDeductReqVO reqVO,
                                               HttpServletRequest request) {
        signatureService.verify(request, reqVO);
        exchangeService.confirmDeduct(reqVO.getFreezeId(), reqVO.getExchangeOrderId());
        return success(true);
    }
}
