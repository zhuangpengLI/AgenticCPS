package com.qiji.cps.module.cps.controller.admin.order;

import com.qiji.cps.framework.common.pojo.CommonResult;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderBindSpecialIdReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderClaimReviewReqVO;
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
import com.qiji.cps.module.cps.service.order.CpsOrderClaimResult;
import com.qiji.cps.module.cps.service.order.CpsOrderClaimReviewCommand;
import com.qiji.cps.module.cps.service.order.CpsOrderClaimService;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecoveryService;
import com.qiji.cps.module.cps.service.order.CpsPlatformBillReconciliationService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncBatchService;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncBatchDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncWindowDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private CpsOrderClaimService orderClaimService;

    @Resource
    private CpsOrderObservabilityService observabilityService;

    @Resource
    private CpsOrderSyncFailureRecoveryService failureRecoveryService;

    @Resource
    private CpsPlatformBillReconciliationService billReconciliationService;

    @Resource
    private CpsFundsTraceService fundsTraceService;

    @Resource
    private CpsOrderSyncBatchService syncBatchService;

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

    @GetMapping("/claim/page")
    @Operation(summary = "获取订单申领审核分页")
    @PreAuthorize("@ss.hasPermission('cps:order:attribution-bind')")
    public CommonResult<PageResult<CpsOrderAttributionLogRespVO>> getClaimPage(
            @Valid CpsOrderAttributionLogPageReqVO pageReqVO) {
        pageReqVO.setAction("CLAIM");
        if (pageReqVO.getReviewStatus() == null || pageReqVO.getReviewStatus().isBlank()) {
            pageReqVO.setReviewStatus("PENDING_REVIEW");
        }
        return success(BeanUtils.toBean(observabilityService.getAttributionLogPage(pageReqVO),
                CpsOrderAttributionLogRespVO.class));
    }

    @PostMapping("/claim/review")
    @Operation(summary = "审核订单申领")
    @PreAuthorize("@ss.hasPermission('cps:order:attribution-bind')")
    public CommonResult<CpsOrderClaimResult> reviewClaim(@Valid @RequestBody CpsOrderClaimReviewReqVO reqVO) {
        return success(orderClaimService.review(new CpsOrderClaimReviewCommand(reqVO.getClaimId(),
                Boolean.TRUE.equals(reqVO.getApproved()), getLoginUserId(), reqVO.getAuditNote())));
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
    @Operation(summary = "手动触发订单同步", description = "立即拉取指定平台订单；大淘客只接收开始时间并固定同步后续3小时")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    @Parameter(name = "platformCode", description = "平台编码（taobao/jd/pdd/douyin）", required = true)
    @Parameter(name = "vendorCode", description = "API 供应商编码；为空时使用平台默认供应商")
    @Parameter(name = "hours", description = "向前追溯小时数，默认2", example = "2")
    @Parameter(name = "queryType", description = "查询时间维度：1下单时间 2付款时间 3结算时间 4更新时间，默认1", example = "4")
    @Parameter(name = "orderStatus", description = "供应商原始订单状态；为空时同步全部状态")
    @Parameter(name = "startTime", description = "同步起始时间，格式 yyyy-MM-dd HH:mm:ss")
    @Parameter(name = "endTime", description = "同步结束时间；大淘客传入开始时间时由服务端固定为开始时间加3小时")
    public CommonResult<String> manualSync(@RequestParam("platformCode") String platformCode,
                                            @RequestParam(value = "vendorCode", required = false) String vendorCode,
                                            @RequestParam(value = "hours", defaultValue = "2") Integer hours,
                                            @RequestParam(value = "queryType", defaultValue = "1") Integer queryType,
                                            @RequestParam(value = "orderStatus", required = false) Integer orderStatus,
                                            @RequestParam(value = "startTime", required = false)
                                            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                            @RequestParam(value = "endTime", required = false)
                                            @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        if ("dataoke".equalsIgnoreCase(vendorCode) && startTime != null) {
            endTime = startTime.plusHours(3);
            hours = 3;
        }
        String result = orderService.manualSync(platformCode, vendorCode, hours, queryType, orderStatus,
                startTime, endTime);
        return success(result);
    }

    @PostMapping("/sync/batches")
    @Operation(summary = "创建订单同步补偿批次")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<CpsOrderSyncBatchDO> createSyncBatch(@RequestBody CpsOrderSyncBatchDO req) {
        return success(syncBatchService.create(req.getPlatformCode(), req.getVendorCode(), req.getBatchType(),
                req.getQueryType(), req.getStartTime(), req.getEndTime()));
    }

    @GetMapping("/sync/batches")
    @Operation(summary = "查询订单同步批次")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderSyncBatchDO>> getSyncBatchPage(
            @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String platformCode, @RequestParam(required = false) String status,
            @RequestParam(required = false) String batchType, @RequestParam(required = false) Integer queryType) {
        return success(syncBatchService.page(pageNo, pageSize, platformCode, status, batchType, queryType));
    }

    @GetMapping("/sync/batches/{id}/windows")
    @Operation(summary = "查询订单同步窗口")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<PageResult<CpsOrderSyncWindowDO>> getSyncBatchWindows(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "50") int pageSize) {
        return success(syncBatchService.windows(id, pageNo, pageSize));
    }

    @PostMapping("/sync/batches/{id}/{action}")
    @Operation(summary = "控制订单同步批次")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> controlSyncBatch(@PathVariable Long id, @PathVariable String action) {
        if (!List.of("pause", "resume", "cancel").contains(action)) throw new IllegalArgumentException("不支持的批次操作");
        syncBatchService.updateStatus(id, switch (action) { case "pause" -> "PAUSED"; case "resume" -> "RUNNING"; default -> "CANCELLED"; });
        return success(true);
    }

    @PostMapping("/sync/windows/{id}/replay")
    @Operation(summary = "重放订单同步窗口")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> replaySyncWindow(@PathVariable Long id) {
        syncBatchService.replayWindow(id); return success(true);
    }

    @DeleteMapping("/sync/batches/{id}")
    @Operation(summary = "删除订单同步补偿批次")
    @PreAuthorize("@ss.hasPermission('cps:order:sync')")
    public CommonResult<Boolean> deleteSyncBatch(@PathVariable Long id) {
        syncBatchService.delete(id);
        return success(true);
    }

    @GetMapping("/sync/metrics")
    @Operation(summary = "查询订单同步指标")
    @PreAuthorize("@ss.hasPermission('cps:order:query')")
    public CommonResult<Map<String, Object>> getSyncMetrics() {
        return success(syncBatchService.metrics());
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
