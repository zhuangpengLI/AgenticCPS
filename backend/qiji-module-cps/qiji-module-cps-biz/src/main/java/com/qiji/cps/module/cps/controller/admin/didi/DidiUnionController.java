package com.qiji.cps.module.cps.controller.admin.didi;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.module.cps.controller.admin.didi.vo.DidiUnionMaterialGenerateReqVO;
import com.qiji.cps.module.cps.service.didi.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 滴滴联盟")
@RestController
@RequestMapping("/cps/didi-union")
@Validated
@RequiredArgsConstructor
public class DidiUnionController {
    private final DidiUnionMaterialService service;

    @PostMapping("/material/generate")
    @Operation(summary = "生成滴滴联盟推广素材")
    @PreAuthorize("@ss.hasPermission('cps:toolbox:link')")
    public CommonResult<DidiUnionMaterialResult> generateMaterial(@Valid @RequestBody DidiUnionMaterialGenerateReqVO request) {
        return success(service.generate(request.getMaterialType(), request.getActivityId(), request.getPromotionId()));
    }

    @GetMapping("/connection-test")
    @Operation(summary = "测试滴滴联盟连接")
    @PreAuthorize("@ss.hasPermission('cps:api-vendor:query')")
    public CommonResult<Boolean> testConnection() { return success(service.testConnection()); }

    @GetMapping("/order-attribution")
    @Operation(summary = "诊断滴滴联盟订单归因")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<DidiUnionOrderAttributionResult> queryOrderAttribution(
            @RequestParam("orderId") @NotBlank String orderId) {
        return success(service.queryOrderAttribution(orderId));
    }
}
