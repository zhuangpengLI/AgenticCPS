package com.qiji.cps.module.cps.controller.admin.order;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderBindSpecialIdReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsFundsTraceReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffHandleReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailurePageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailureReplayReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailureRespVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.service.order.CpsFundsTraceQuery;
import com.qiji.cps.module.cps.service.order.CpsFundsTraceResult;
import com.qiji.cps.module.cps.service.order.CpsFundsTraceService;
import com.qiji.cps.module.cps.service.order.CpsOrderObservabilityService;
import com.qiji.cps.module.cps.service.order.CpsOrderManualBindCommand;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecoveryService;
import com.qiji.cps.module.cps.service.order.CpsPlatformBillReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.qiji.cps.framework.common.pojo.CommonResult.success;
import static com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 管理后台 - CPS 订单管理
 *
 * @author CPS System
 */
@Tag(name = "管理后台 - CPS 订单管理")
@RestController
@RequestMapping("/cps/order")
@Validated
public class CpsOrderController {

    @Resource
    private CpsOrderService orderService;

    @Resource
    private CpsOrderObservabilityService observabilityService;

    @Resource
    private CpsOrderSyncFailureRecoveryService failureRecoveryService;

    @Resource
    private CpsPlatformBillReconciliationService billReconciliationService;

    @Resource
    private CpsFundsTraceService fundsTraceService;

    @GetMapping("/page")
    @Operation(summary = "获取订单分页")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderRespVO>> getOrderPage(@Valid CpsOrderPageReqVO pageReqVO) {
        PageResult<CpsOrderDO> pageResult = orderService.getOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CpsOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取订单详情")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    @Parameter(name = "id", description = "订单ID", required = true, example = "1")
    public CommonResult<CpsOrderRespVO> getOrder(@RequestParam("id") Long id) {
        CpsOrderDO order = orderService.getOrder(id);
        return success(BeanUtils.toBean(order, CpsOrderRespVO.class));
    }

    @GetMapping("/attribution-log/page")
    @Operation(summary = "获取订单归因日志分页")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderAttributionLogRespVO>> getAttributionLogPage(
            @Valid CpsOrderAttributionLogPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(observabilityService.getAttributionLogPage(pageReqVO),
                CpsOrderAttributionLogRespVO.class));
    }

    @GetMapping("/sync-checkpoint/page")
    @Operation(summary = "获取订单同步检查点分页")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderSyncCheckpointRespVO>> getSyncCheckpointPage(
            @Valid CpsOrderSyncCheckpointPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(observabilityService.getSyncCheckpointPage(pageReqVO),
                CpsOrderSyncCheckpointRespVO.class));
    }

    @GetMapping("/sync-failure/page")
    @Operation(summary = "获取订单同步失败恢复队列分页")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderSyncFailureRespVO>> getSyncFailurePage(
            @Valid CpsOrderSyncFailurePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(observabilityService.getSyncFailurePage(pageReqVO),
                CpsOrderSyncFailureRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除订单")
    @PreAuthorize("@ss.hasPermission('cps:order:delete')")
    @Parameter(name = "id", description = "订单ID", required = true, example = "1")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        orderService.deleteOrder(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除订单")
    @PreAuthorize("@ss.hasPermission('cps:order:delete')")
    @Parameter(name = "ids", description = "订单ID列表", required = true)
    public CommonResult<Boolean> deleteOrderList(@RequestParam("ids") List<Long> ids) {
        orderService.deleteOrderList(ids);
        return success(true);
    }

    @PostMapping("/sync")
    @Operation(summary = "手动触发订单同步", description = "立即拉取指定平台最近N小时订单，默认同步最近2小时")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    @Parameter(name = "platformCode", description = "平台编码（taobao/jd/pdd/douyin）", required = true)
    @Parameter(name = "hours", description = "向前追溯小时数，默认2", example = "2")
    @Parameter(name = "queryType", description = "查询时间维度：1下单时间 2付款时间 3结算时间 4更新时间，默认1", example = "4")
    public CommonResult<String> manualSync(@RequestParam("platformCode") String platformCode,
                                            @RequestParam(value = "hours", defaultValue = "2") Integer hours,
                                            @RequestParam(value = "queryType", defaultValue = "1") Integer queryType) {
        String result = orderService.manualSync(platformCode, hours, queryType);
        return success(result);
    }

    @PostMapping("/sync-failure/replay")
    @Operation(summary = "人工重放订单同步失败记录")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> replaySyncFailure(@Valid @RequestBody CpsOrderSyncFailureReplayReqVO reqVO) {
        failureRecoveryService.replayFailure(reqVO.getId(), reqVO.getOperatorId(), reqVO.getAuditNote());
        return success(true);
    }

    @GetMapping("/platform-bill-diff/page")
    @Operation(summary = "获取平台账单对账差异分页")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsPlatformBillDiffRespVO>> getPlatformBillDiffPage(
            @Valid CpsPlatformBillDiffPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(billReconciliationService.getDiffPage(pageReqVO),
                CpsPlatformBillDiffRespVO.class));
    }

    @PostMapping("/platform-bill-diff/handle")
    @Operation(summary = "处理平台账单对账差异")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> handlePlatformBillDiff(@Valid @RequestBody CpsPlatformBillDiffHandleReqVO reqVO) {
        billReconciliationService.handleDiff(reqVO.getId(), reqVO.getOperatorId(),
                reqVO.getConclusion(), reqVO.getAuditNote());
        return success(true);
    }

    @PostMapping("/platform-bill-diff/repull")
    @Operation(summary = "请求重拉平台账单差异订单")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> requestPlatformBillDiffRepull(@Valid @RequestBody CpsPlatformBillDiffHandleReqVO reqVO) {
        billReconciliationService.requestTargetedRepull(reqVO.getId(), reqVO.getOperatorId(), reqVO.getAuditNote());
        return success(true);
    }

    @PostMapping("/bind-special-id-member")
    @Operation(summary = "手动绑定 special_id 到会员")
    @PreAuthorize("@ss.hasPermission('cps:order:attribution-bind')")
    public CommonResult<Boolean> bindSpecialIdToMember(@Valid @RequestBody CpsOrderBindSpecialIdReqVO reqVO) {
        orderService.bindSpecialIdToMember(new CpsOrderManualBindCommand(
                reqVO.getOrderId(), reqVO.getMemberId(), getLoginUserId(),
                reqVO.getIdempotencyKey(), reqVO.getAuditNote()));
        return success(true);
    }

    @GetMapping("/funds-trace")
    @Operation(summary = "查询订单资金追溯链路")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<CpsFundsTraceResult> traceFunds(@Valid CpsFundsTraceReqVO reqVO) {
        return success(fundsTraceService.traceFunds(new CpsFundsTraceQuery(
                reqVO.getOrderId(), reqVO.getPlatformCode(), reqVO.getPlatformOrderId(),
                reqVO.getBusinessId(), reqVO.getIdempotencyKey())));
    }

}
