package com.qiji.cps.module.cps.controller.admin.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderAttributionLogRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderBindSpecialIdReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsFundsTraceReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffHandleReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsPlatformBillDiffRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailurePageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailureReplayReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncFailureRespVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointPageReqVO;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderSyncCheckpointRespVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderAttributionLogDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncFailureDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncCheckpointDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.service.order.CpsFundsTraceQuery;
import com.qiji.cps.module.cps.service.order.CpsFundsTraceResult;
import com.qiji.cps.module.cps.service.order.CpsFundsTraceService;
import com.qiji.cps.module.cps.service.order.CpsOrderManualBindCommand;
import com.qiji.cps.module.cps.service.order.CpsOrderObservabilityService;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import com.qiji.cps.module.cps.service.order.CpsOrderSyncFailureRecoveryService;
import com.qiji.cps.module.cps.service.order.CpsPlatformBillReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderControllerObservabilityTest {

    @InjectMocks
    private CpsOrderController controller;

    @Mock
    private CpsOrderService orderService;
    @Mock
    private CpsOrderObservabilityService observabilityService;
    @Mock
    private CpsOrderSyncFailureRecoveryService failureRecoveryService;
    @Mock
    private CpsPlatformBillReconciliationService billReconciliationService;
    @Mock
    private CpsFundsTraceService fundsTraceService;

    @Test
    void getAttributionLogPage_returnsResponseVoPage() {
        CpsOrderAttributionLogPageReqVO request = new CpsOrderAttributionLogPageReqVO();
        CpsOrderAttributionLogDO log = CpsOrderAttributionLogDO.builder()
                .id(11L).platformOrderId("TB-001").result("BOUND").build();
        when(observabilityService.getAttributionLogPage(request))
                .thenReturn(new PageResult<>(List.of(log), 1L));

        PageResult<CpsOrderAttributionLogRespVO> result = controller.getAttributionLogPage(request).getData();

        assertEquals(1L, result.getTotal());
        assertEquals(11L, result.getList().get(0).getId());
        assertEquals("TB-001", result.getList().get(0).getPlatformOrderId());
        assertEquals("BOUND", result.getList().get(0).getResult());
    }

    @Test
    void getSyncCheckpointPage_returnsResponseVoPage() {
        CpsOrderSyncCheckpointPageReqVO request = new CpsOrderSyncCheckpointPageReqVO();
        CpsOrderSyncCheckpointDO checkpoint = CpsOrderSyncCheckpointDO.builder()
                .id(21L).platformCode("taobao").vendorCode("dataoke")
                .lastSyncStatus("PARTIAL").failureSummary("page limit reached").build();
        when(observabilityService.getSyncCheckpointPage(request))
                .thenReturn(new PageResult<>(List.of(checkpoint), 1L));

        PageResult<CpsOrderSyncCheckpointRespVO> result = controller.getSyncCheckpointPage(request).getData();

        assertEquals(1L, result.getTotal());
        assertEquals(21L, result.getList().get(0).getId());
        assertEquals("dataoke", result.getList().get(0).getVendorCode());
        assertEquals("PARTIAL", result.getList().get(0).getLastSyncStatus());
        assertEquals("page limit reached", result.getList().get(0).getFailureSummary());
    }

    @Test
    void getSyncFailurePage_returnsResponseVoPage() {
        CpsOrderSyncFailurePageReqVO request = new CpsOrderSyncFailurePageReqVO();
        CpsOrderSyncFailureDO failure = CpsOrderSyncFailureDO.builder()
                .id(31L).platformCode("jd").vendorCode("jingdong")
                .status("PENDING").failureStage("PERSIST_PAGE").failureReason("db rejected").build();
        when(observabilityService.getSyncFailurePage(request))
                .thenReturn(new PageResult<>(List.of(failure), 1L));

        PageResult<CpsOrderSyncFailureRespVO> result = controller.getSyncFailurePage(request).getData();

        assertEquals(1L, result.getTotal());
        assertEquals(31L, result.getList().get(0).getId());
        assertEquals("PENDING", result.getList().get(0).getStatus());
        assertEquals("PERSIST_PAGE", result.getList().get(0).getFailureStage());
    }

    @Test
    void replaySyncFailure_delegatesToRecoveryService() {
        CpsOrderSyncFailureReplayReqVO request = new CpsOrderSyncFailureReplayReqVO();
        request.setId(31L);
        request.setOperatorId(1001L);
        request.setAuditNote("manual replay after vendor recovery");

        Boolean result = controller.replaySyncFailure(request).getData();

        assertEquals(Boolean.TRUE, result);
        verify(failureRecoveryService).replayFailure(31L, 1001L, "manual replay after vendor recovery");
    }

    @Test
    void getPlatformBillDiffPage_returnsResponseVoPage() {
        CpsPlatformBillDiffPageReqVO request = new CpsPlatformBillDiffPageReqVO();
        CpsPlatformBillDiffDO diff = CpsPlatformBillDiffDO.builder()
                .id(41L).platformCode("taobao").billBatchNo("batch-1")
                .platformOrderId("TB-404").diffType("MISSING_ORDER")
                .diffStatus("PENDING").build();
        when(billReconciliationService.getDiffPage(request))
                .thenReturn(new PageResult<>(List.of(diff), 1L));

        PageResult<CpsPlatformBillDiffRespVO> result = controller.getPlatformBillDiffPage(request).getData();

        assertEquals(1L, result.getTotal());
        assertEquals(41L, result.getList().get(0).getId());
        assertEquals("MISSING_ORDER", result.getList().get(0).getDiffType());
        assertEquals("PENDING", result.getList().get(0).getDiffStatus());
    }

    @Test
    void handlePlatformBillDiff_delegatesToReconciliationService() {
        CpsPlatformBillDiffHandleReqVO request = new CpsPlatformBillDiffHandleReqVO();
        request.setId(41L);
        request.setOperatorId(1001L);
        request.setConclusion("CONFIRMED_PLATFORM");
        request.setAuditNote("平台账单为准");

        Boolean result = controller.handlePlatformBillDiff(request).getData();

        assertEquals(Boolean.TRUE, result);
        verify(billReconciliationService).handleDiff(41L, 1001L, "CONFIRMED_PLATFORM", "平台账单为准");
    }

    @Test
    void requestPlatformBillDiffRepull_delegatesToReconciliationService() {
        CpsPlatformBillDiffHandleReqVO request = new CpsPlatformBillDiffHandleReqVO();
        request.setId(42L);
        request.setOperatorId(1002L);
        request.setAuditNote("重拉平台订单");

        Boolean result = controller.requestPlatformBillDiffRepull(request).getData();

        assertEquals(Boolean.TRUE, result);
        verify(billReconciliationService).requestTargetedRepull(42L, 1002L, "重拉平台订单");
    }

    @Test
    void bindSpecialIdToMember_usesLoginOperatorAndIdempotencyKey() {
        CpsOrderBindSpecialIdReqVO request = new CpsOrderBindSpecialIdReqVO();
        request.setOrderId(51L);
        request.setMemberId(1001L);
        request.setIdempotencyKey("manual-bind-controller");
        request.setAuditNote("平台截图与会员申诉单一致");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);

            Boolean result = controller.bindSpecialIdToMember(request).getData();

            assertEquals(Boolean.TRUE, result);
            ArgumentCaptor<CpsOrderManualBindCommand> captor =
                    ArgumentCaptor.forClass(CpsOrderManualBindCommand.class);
            verify(orderService).bindSpecialIdToMember(captor.capture());
            CpsOrderManualBindCommand command = captor.getValue();
            assertEquals(51L, command.orderId());
            assertEquals(1001L, command.memberId());
            assertEquals(9001L, command.operatorId());
            assertEquals("manual-bind-controller", command.idempotencyKey());
            assertEquals("平台截图与会员申诉单一致", command.auditNote());
        }
    }

    @Test
    void traceFunds_delegatesToTraceService() {
        CpsFundsTraceReqVO request = new CpsFundsTraceReqVO();
        request.setOrderId(91L);
        request.setBusinessId("biz-91");
        request.setIdempotencyKey("idem-91");
        when(fundsTraceService.traceFunds(new CpsFundsTraceQuery(91L, null, null, "biz-91", "idem-91")))
                .thenReturn(CpsFundsTraceResult.builder()
                        .order(CpsOrderDO.builder().id(91L).platformOrderId("TB-91").build())
                        .traceComplete(true)
                        .build());

        CpsFundsTraceResult result = controller.traceFunds(request).getData();

        assertEquals(91L, result.getOrder().getId());
        assertEquals("TB-91", result.getOrder().getPlatformOrderId());
        assertEquals(true, result.isTraceComplete());
        verify(fundsTraceService).traceFunds(new CpsFundsTraceQuery(91L, null, null, "biz-91", "idem-91"));
    }
}
