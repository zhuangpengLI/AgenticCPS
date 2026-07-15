package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderStatusEventDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsPlatformBillDiffDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateDebtDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeRecordMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderStatusEventMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsPlatformBillDiffMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateDebtMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsFundsTraceServiceImplTest {

    @InjectMocks
    private CpsFundsTraceServiceImpl traceService;

    @Mock
    private CpsOrderMapper orderMapper;
    @Mock
    private CpsOrderStatusEventMapper statusEventMapper;
    @Mock
    private CpsRebateRecordMapper rebateRecordMapper;
    @Mock
    private CpsFreezeRecordMapper freezeRecordMapper;
    @Mock
    private CpsRebateDebtMapper debtMapper;
    @Mock
    private CpsRebateAssetLedgerMapper assetLedgerMapper;
    @Mock
    private CpsPlatformBillDiffMapper billDiffMapper;

    @Test
    void traceFunds_linksOrderStatusRebateFreezeDebtLedgerAndBillDiff() {
        CpsOrderDO order = CpsOrderDO.builder()
                .id(11L).platformCode("taobao").platformOrderId("TB-TRACE-1").memberId(1001L).build();
        when(orderMapper.selectById(11L)).thenReturn(order);
        when(statusEventMapper.selectListByTrace(11L, "TB-TRACE-1"))
                .thenReturn(List.of(CpsOrderStatusEventDO.builder().id(21L).orderId(11L).build()));
        when(rebateRecordMapper.selectListByTrace(11L, "TB-TRACE-1", "idem-1"))
                .thenReturn(List.of(CpsRebateRecordDO.builder().id(31L).orderId(11L).idempotencyKey("idem-1").build()));
        when(freezeRecordMapper.selectListByTrace(11L, "TB-TRACE-1", "biz-1", "idem-1"))
                .thenReturn(List.of(CpsFreezeRecordDO.builder().id(41L).orderId(11L).businessId("biz-1").build()));
        when(debtMapper.selectListByTrace(11L, "TB-TRACE-1", "biz-1", "idem-1"))
                .thenReturn(List.of(CpsRebateDebtDO.builder().id(51L).orderId(11L).sourceBusinessId("biz-1").build()));
        when(assetLedgerMapper.selectListByTrace(11L, "TB-TRACE-1", "biz-1", "idem-1"))
                .thenReturn(List.of(CpsRebateAssetLedgerDO.builder()
                        .id(61L).orderId(11L).businessId("biz-1").idempotencyKey("idem-1")
                        .operatorType("SYSTEM").operatorId("order-settle").build()));
        when(billDiffMapper.selectListByTrace(11L, "TB-TRACE-1", "idem-1"))
                .thenReturn(List.of(CpsPlatformBillDiffDO.builder().id(71L).orderId(11L).build()));

        CpsFundsTraceResult result = traceService.traceFunds(
                new CpsFundsTraceQuery(11L, null, null, "biz-1", "idem-1"));

        assertTrue(result.isTraceComplete());
        assertEquals(11L, result.getOrder().getId());
        assertEquals(1, result.getStatusEvents().size());
        assertEquals(1, result.getRebateRecords().size());
        assertEquals(1, result.getFreezeRecords().size());
        assertEquals(1, result.getDebtRecords().size());
        assertEquals(1, result.getAssetLedgers().size());
        assertEquals(1, result.getBillDiffs().size());
        assertTrue(result.getTraceWarnings().isEmpty());
    }

    @Test
    void traceFunds_warnsWhenAssetLedgerHasNoSourceOrOperator() {
        CpsOrderDO order = CpsOrderDO.builder()
                .id(12L).platformCode("taobao").platformOrderId("TB-TRACE-2").memberId(1002L).build();
        when(orderMapper.selectByPlatformOrderId("taobao", "TB-TRACE-2")).thenReturn(order);
        when(statusEventMapper.selectListByTrace(12L, "TB-TRACE-2")).thenReturn(List.of());
        when(rebateRecordMapper.selectListByTrace(12L, "TB-TRACE-2", null)).thenReturn(List.of());
        when(freezeRecordMapper.selectListByTrace(12L, "TB-TRACE-2", null, null)).thenReturn(List.of());
        when(debtMapper.selectListByTrace(12L, "TB-TRACE-2", null, null)).thenReturn(List.of());
        when(assetLedgerMapper.selectListByTrace(12L, "TB-TRACE-2", null, null))
                .thenReturn(List.of(CpsRebateAssetLedgerDO.builder().id(62L).build()));
        when(billDiffMapper.selectListByTrace(12L, "TB-TRACE-2", null)).thenReturn(List.of());

        CpsFundsTraceResult result = traceService.traceFunds(
                new CpsFundsTraceQuery(null, "taobao", "TB-TRACE-2", null, null));

        assertFalse(result.isTraceComplete());
        assertEquals(2, result.getTraceWarnings().size());
        assertTrue(result.getTraceWarnings().get(0).contains("资产流水缺少订单、业务单号和幂等键"));
        assertTrue(result.getTraceWarnings().get(1).contains("资产流水缺少操作主体"));
    }
}
