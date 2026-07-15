package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillRowDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsPlatformBillDiffMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsPlatformBillRowMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsPlatformBillReconciliationServiceImplTest {

    @InjectMocks
    private CpsPlatformBillReconciliationServiceImpl service;

    @Mock
    private CpsPlatformBillRowMapper billRowMapper;
    @Mock
    private CpsPlatformBillDiffMapper billDiffMapper;
    @Mock
    private CpsOrderMapper orderMapper;

    @Test
    void importAndReconcile_createsMissingOrderDiffForBillRowWithoutLocalOrder() {
        CpsPlatformBillImportRowCommand row = row("TB-404")
                .commissionAmount(new BigDecimal("12.30"))
                .build();
        when(billRowMapper.selectByIdempotencyKey("platform-bill:batch-1:taobao:TB-404")).thenReturn(null);
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-404")).thenReturn(null);
        when(billDiffMapper.selectByIdempotencyKey("platform-bill-diff:batch-1:taobao:TB-404:MISSING_ORDER"))
                .thenReturn(null);

        CpsPlatformBillReconciliationResult result = service.importAndReconcile(List.of(row));

        assertEquals(1, result.importedRows());
        assertEquals(1, result.createdDiffs());
        CpsPlatformBillDiffDO diff = captureInsertedDiff();
        assertEquals("MISSING_ORDER", diff.getDiffType());
        assertEquals("PENDING", diff.getDiffStatus());
        assertEquals("batch-1", diff.getBillBatchNo());
        assertEquals("TB-404", diff.getPlatformOrderId());
    }

    @Test
    void importAndReconcile_createsCommissionDiffWhenBillCommissionDiffersFromLocalOrder() {
        CpsPlatformBillImportRowCommand row = row("TB-COMMISSION")
                .commissionAmount(new BigDecimal("15.00"))
                .build();
        when(billRowMapper.selectByIdempotencyKey("platform-bill:batch-1:taobao:TB-COMMISSION")).thenReturn(null);
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-COMMISSION"))
                .thenReturn(order("TB-COMMISSION").commissionAmount(new BigDecimal("12.00")).build());
        when(billDiffMapper.selectByIdempotencyKey("platform-bill-diff:batch-1:taobao:TB-COMMISSION:COMMISSION_DIFF"))
                .thenReturn(null);

        service.importAndReconcile(List.of(row));

        CpsPlatformBillDiffDO diff = captureInsertedDiff();
        assertEquals("COMMISSION_DIFF", diff.getDiffType());
        assertEquals(new BigDecimal("12.00"), diff.getOrderCommissionAmount());
        assertEquals(new BigDecimal("15.00"), diff.getBillCommissionAmount());
    }

    @Test
    void importAndReconcile_createsRefundDiffWhenBillShowsRefundButLocalOrderIsNotReversed() {
        CpsPlatformBillImportRowCommand row = row("TB-REFUND")
                .billStatus("REFUNDED")
                .refundAmount(new BigDecimal("9.90"))
                .build();
        when(billRowMapper.selectByIdempotencyKey("platform-bill:batch-1:taobao:TB-REFUND")).thenReturn(null);
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-REFUND"))
                .thenReturn(order("TB-REFUND").orderStatus(CpsOrderStatusEnum.SETTLED.getStatus()).build());
        when(billDiffMapper.selectByIdempotencyKey("platform-bill-diff:batch-1:taobao:TB-REFUND:REFUND_DIFF"))
                .thenReturn(null);

        service.importAndReconcile(List.of(row));

        CpsPlatformBillDiffDO diff = captureInsertedDiff();
        assertEquals("REFUND_DIFF", diff.getDiffType());
        assertEquals(new BigDecimal("9.90"), diff.getBillRefundAmount());
    }

    @Test
    void importAndReconcile_createsSettlementTimeDiffWhenBillAndLocalSettleTimeDiffer() {
        LocalDateTime localSettleTime = LocalDateTime.of(2026, 7, 13, 10, 0);
        LocalDateTime billSettleTime = LocalDateTime.of(2026, 7, 14, 10, 0);
        CpsPlatformBillImportRowCommand row = row("TB-SETTLE")
                .settleTime(billSettleTime)
                .build();
        when(billRowMapper.selectByIdempotencyKey("platform-bill:batch-1:taobao:TB-SETTLE")).thenReturn(null);
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-SETTLE"))
                .thenReturn(order("TB-SETTLE").settleTime(localSettleTime).build());
        when(billDiffMapper.selectByIdempotencyKey("platform-bill-diff:batch-1:taobao:TB-SETTLE:SETTLEMENT_TIME_DIFF"))
                .thenReturn(null);

        service.importAndReconcile(List.of(row));

        CpsPlatformBillDiffDO diff = captureInsertedDiff();
        assertEquals("SETTLEMENT_TIME_DIFF", diff.getDiffType());
        assertEquals(localSettleTime, diff.getOrderSettleTime());
        assertEquals(billSettleTime, diff.getBillSettleTime());
    }

    @Test
    void importAndReconcile_createsUnattributedDiffWhenLocalOrderHasNoMember() {
        CpsPlatformBillImportRowCommand row = row("TB-NO-MEMBER").build();
        when(billRowMapper.selectByIdempotencyKey("platform-bill:batch-1:taobao:TB-NO-MEMBER")).thenReturn(null);
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-NO-MEMBER"))
                .thenReturn(order("TB-NO-MEMBER").memberId(null).build());
        when(billDiffMapper.selectByIdempotencyKey("platform-bill-diff:batch-1:taobao:TB-NO-MEMBER:UNATTRIBUTED_ORDER"))
                .thenReturn(null);

        service.importAndReconcile(List.of(row));

        CpsPlatformBillDiffDO diff = captureInsertedDiff();
        assertEquals("UNATTRIBUTED_ORDER", diff.getDiffType());
        assertEquals("local order has no trusted member attribution", diff.getDiffSummary());
    }

    @Test
    void importAndReconcile_isIdempotentForImportedRowsAndDiffs() {
        CpsPlatformBillImportRowCommand row = row("TB-IDEMPOTENT").build();
        when(billRowMapper.selectByIdempotencyKey("platform-bill:batch-1:taobao:TB-IDEMPOTENT"))
                .thenReturn(CpsPlatformBillRowDO.builder().id(9L).platformOrderId("TB-IDEMPOTENT").build());
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-IDEMPOTENT")).thenReturn(null);
        when(billDiffMapper.selectByIdempotencyKey("platform-bill-diff:batch-1:taobao:TB-IDEMPOTENT:MISSING_ORDER"))
                .thenReturn(CpsPlatformBillDiffDO.builder().id(10L).build());

        CpsPlatformBillReconciliationResult result = service.importAndReconcile(List.of(row));

        assertEquals(0, result.importedRows());
        assertEquals(0, result.createdDiffs());
        verify(billRowMapper, never()).insert(any(CpsPlatformBillRowDO.class));
        verify(billDiffMapper, never()).insert(any(CpsPlatformBillDiffDO.class));
    }

    @Test
    void handleDiff_recordsAuditedConclusion() {
        CpsPlatformBillDiffDO diff = CpsPlatformBillDiffDO.builder()
                .id(31L).diffStatus("PENDING").version(0).build();
        when(billDiffMapper.selectById(31L)).thenReturn(diff);

        service.handleDiff(31L, 1001L, "CONFIRMED_PLATFORM", "平台账单为准，等待补单");

        ArgumentCaptor<CpsPlatformBillDiffDO> captor = ArgumentCaptor.forClass(CpsPlatformBillDiffDO.class);
        verify(billDiffMapper).updateById(captor.capture());
        CpsPlatformBillDiffDO update = captor.getValue();
        assertEquals("HANDLED", update.getDiffStatus());
        assertEquals("CONFIRMED_PLATFORM", update.getHandleConclusion());
        assertEquals("平台账单为准，等待补单", update.getHandleAuditNote());
        assertEquals(1001L, update.getHandleOperatorId());
        assertNotNull(update.getHandleTime());
    }

    @Test
    void requestTargetedRepull_recordsAuditWithoutCallingExternalPlatform() {
        CpsPlatformBillDiffDO diff = CpsPlatformBillDiffDO.builder()
                .id(32L).diffStatus("PENDING").version(0).build();
        when(billDiffMapper.selectById(32L)).thenReturn(diff);

        service.requestTargetedRepull(32L, 1002L, "重拉平台订单");

        ArgumentCaptor<CpsPlatformBillDiffDO> captor = ArgumentCaptor.forClass(CpsPlatformBillDiffDO.class);
        verify(billDiffMapper).updateById(captor.capture());
        CpsPlatformBillDiffDO update = captor.getValue();
        assertEquals("REPULL_REQUESTED", update.getDiffStatus());
        assertEquals("TARGETED_REPULL", update.getHandleConclusion());
        assertEquals("重拉平台订单", update.getHandleAuditNote());
        assertEquals(1002L, update.getHandleOperatorId());
        assertNotNull(update.getHandleTime());
    }

    private CpsPlatformBillImportRowCommand.CpsPlatformBillImportRowCommandBuilder row(String platformOrderId) {
        return CpsPlatformBillImportRowCommand.builder()
                .platformCode("taobao")
                .vendorCode("dataoke")
                .billBatchNo("batch-1")
                .platformOrderId(platformOrderId)
                .billStatus("SETTLED")
                .commissionAmount(new BigDecimal("12.00"))
                .settleTime(LocalDateTime.of(2026, 7, 14, 10, 0))
                .rawSummary("{\"platformOrderId\":\"" + platformOrderId + "\"}");
    }

    private CpsOrderDO.CpsOrderDOBuilder order(String platformOrderId) {
        return CpsOrderDO.builder()
                .id(100L)
                .platformCode("taobao")
                .platformOrderId(platformOrderId)
                .memberId(200L)
                .orderStatus(CpsOrderStatusEnum.SETTLED.getStatus())
                .commissionAmount(new BigDecimal("12.00"))
                .settleTime(LocalDateTime.of(2026, 7, 14, 10, 0));
    }

    private CpsPlatformBillDiffDO captureInsertedDiff() {
        ArgumentCaptor<CpsPlatformBillDiffDO> captor = ArgumentCaptor.forClass(CpsPlatformBillDiffDO.class);
        verify(billDiffMapper).insert(captor.capture());
        return captor.getValue();
    }
}
