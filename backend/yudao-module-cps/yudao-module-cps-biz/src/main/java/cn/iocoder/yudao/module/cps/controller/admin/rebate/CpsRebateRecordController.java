package cn.iocoder.yudao.module.cps.controller.admin.rebate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.cps.controller.admin.rebate.vo.CpsRebateRecordPageReqVO;
import cn.iocoder.yudao.module.cps.controller.admin.rebate.vo.CpsRebateRecordRespVO;
import cn.iocoder.yudao.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import cn.iocoder.yudao.module.cps.service.rebate.CpsRebateRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - CPS 返利记录
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS返利记录")
@RestController
@RequestMapping("/admin-api/cps/rebate-record")
@Validated
public class CpsRebateRecordController {

    @Resource
    private CpsRebateRecordService rebateRecordService;

    @GetMapping("/page")
    @Operation(summary = "获取返利记录分页")
    @PreAuthorize("@ss.hasPermission('cps:rebate-record:query')")
    public CommonResult<PageResult<CpsRebateRecordRespVO>> getRebateRecordPage(@Valid CpsRebateRecordPageReqVO pageReqVO) {
        PageResult<CpsRebateRecordDO> pageResult = rebateRecordService.getRebateRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsRebateRecordRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取返利记录详情")
    @Parameter(name = "id", description = "返利记录ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('cps:rebate-record:query')")
    public CommonResult<CpsRebateRecordRespVO> getRebateRecord(@RequestParam("id") Long id) {
        CpsRebateRecordDO record = rebateRecordService.getRebateRecord(id);
        return success(BeanUtils.toBean(record, CpsRebateRecordRespVO.class));
    }

    @PostMapping("/reverse")
    @Operation(summary = "触发订单退款回扣", description = "扣回该订单已入账的返利金额")
    @Parameter(name = "orderId", description = "订单ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('cps:rebate-record:reverse')")
    public CommonResult<Boolean> reverseRebate(@RequestParam("orderId") Long orderId) {
        boolean result = rebateRecordService.reverseRebate(orderId);
        return success(result);
    }

}
