package com.qiji.cps.module.cps.controller.admin.refund;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.refund.vo.CpsRefundReportImportRespVO;
import com.qiji.cps.module.cps.controller.admin.refund.vo.CpsRefundReportDetailRespVO;
import com.qiji.cps.module.cps.service.refund.CpsRefundReportImportResult;
import com.qiji.cps.module.cps.service.refund.CpsRefundReportImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CPS 退款报表")
@RestController
@RequestMapping({"/cps/order/refund-reports", "/cps/orders/refund-reports"})
@Validated
public class CpsRefundReportController {
    @Resource private CpsRefundReportImportService importService;

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "导入维权退款报表")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<CpsRefundReportImportRespVO> importReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam("platformCode") String platformCode,
            @RequestParam(value = "vendorCode", required = false) String vendorCode,
            @RequestParam(value = "reportPeriod", required = false) String reportPeriod) throws IOException {
        CpsRefundReportImportResult result = importService.importReport(file, platformCode, vendorCode, reportPeriod);
        return success(BeanUtils.toBean(result.report(), CpsRefundReportImportRespVO.class));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询退款报表导入结果")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<CpsRefundReportImportRespVO> getReport(@PathVariable Long id) {
        return success(BeanUtils.toBean(importService.getReport(id), CpsRefundReportImportRespVO.class));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "查询退款报表导入明细")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<java.util.List<CpsRefundReportDetailRespVO>> getDetails(@PathVariable Long id) {
        return success(BeanUtils.toBean(importService.getDetails(id), CpsRefundReportDetailRespVO.class));
    }
}
