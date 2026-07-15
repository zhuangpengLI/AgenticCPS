package com.qiji.cps.module.cps.service.rebate;

import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateRecordDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateRecordMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsMoneyConverter;
import com.qiji.cps.module.member.api.user.MemberUserApi;
import com.qiji.cps.module.member.api.user.dto.MemberUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CpsRebateSettleServiceImplTest {

    @Mock private CpsOrderMapper orderMapper;
    @Mock private CpsRebateRecordMapper rebateRecordMapper;
    @Mock private CpsRebateAccountMapper rebateAccountMapper;
    @Mock private CpsRebateConfigService rebateConfigService;
    @Mock private CpsRebateAssetService assetService;
    @Mock private CpsMoneyConverter moneyConverter;
    @Mock private MemberUserApi memberUserApi;
    @Mock private CpsRebateSettleExecutor settleExecutor;

    private CpsRebateSettleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CpsRebateSettleServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "rebateRecordMapper", rebateRecordMapper);
        ReflectionTestUtils.setField(service, "rebateAccountMapper", rebateAccountMapper);
        ReflectionTestUtils.setField(service, "rebateConfigService", rebateConfigService);
        ReflectionTestUtils.setField(service, "rebateAssetService", assetService);
        ReflectionTestUtils.setField(service, "moneyConverter", moneyConverter);
        ReflectionTestUtils.setField(service, "memberUserApi", memberUserApi);
        ReflectionTestUtils.setField(service, "settleExecutor", settleExecutor);
    }

    @Test
    void receivedOrderWithoutPlatformSettlementCannotCreateRebate() {
        CpsOrderDO order = order(CpsOrderStatusEnum.RECEIVED.getStatus(), null);
        when(orderMapper.selectForUpdateById(11L)).thenReturn(order);

        assertFalse(service.settleOrder(order));

        verifyNoInteractions(rebateRecordMapper, assetService);
    }

    @Test
    void settledOrderCreatesPendingRecordThenFrozenAssetInsteadOfAvailableBalance() {
        CpsOrderDO order = order(CpsOrderStatusEnum.SETTLED.getStatus(), LocalDateTime.now());
        when(orderMapper.selectForUpdateById(11L)).thenReturn(order);
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(9L);
        member.setLevelId(3L);
        when(memberUserApi.getUser(9L)).thenReturn(member);
        when(rebateConfigService.matchRebateConfig(9L, 3L, "taobao"))
                .thenReturn(CpsRebateConfigDO.builder().id(7L).rebateRate(new BigDecimal("80")).build());
        when(moneyConverter.yuanToCent(new BigDecimal("8.00"))).thenReturn(800L);
        when(assetService.createOrderRebateFreeze(11L, "order-rebate:11"))
                .thenReturn(com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO.builder()
                        .unfreezeTime(LocalDateTime.now().plusDays(15)).build());
        when(orderMapper.updateRebateFreezeByStatusVersion(any(CpsOrderDO.class), eq(0))).thenReturn(1);

        assertTrue(service.settleOrder(order));

        verify(rebateRecordMapper).insert(org.mockito.ArgumentMatchers.<CpsRebateRecordDO>argThat(record ->
                "pending".equals(record.getRebateStatus())
                        && Long.valueOf(7L).equals(record.getRebateConfigId())
                        && Long.valueOf(3L).equals(record.getMemberLevelIdSnapshot())));
        verify(assetService).createOrderRebateFreeze(11L, "order-rebate:11");
        verify(rebateAccountMapper, never()).updateById(org.mockito.ArgumentMatchers.<com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO>any());
        verify(orderMapper).updateRebateFreezeByStatusVersion(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(update ->
                CpsOrderStatusEnum.SETTLED.getStatus().equals(update.getOrderStatus())
                        && update.getRebateTime() == null), eq(0));
    }

    @Test
    void memberServiceFailureKeepsOrderPendingWithoutFallbackRule() {
        CpsOrderDO order = order(CpsOrderStatusEnum.SETTLED.getStatus(), LocalDateTime.now());
        when(orderMapper.selectForUpdateById(11L)).thenReturn(order);
        when(memberUserApi.getUser(9L)).thenThrow(new IllegalStateException("member unavailable"));

        assertThrows(IllegalStateException.class, () -> service.settleOrder(order));

        verifyNoInteractions(rebateConfigService, assetService);
        verify(rebateRecordMapper, never()).insert(org.mockito.ArgumentMatchers.<CpsRebateRecordDO>any());
    }

    @Test
    void staleSettledSnapshotCannotOverwriteRefundedOrder() {
        CpsOrderDO stale = order(CpsOrderStatusEnum.SETTLED.getStatus(), LocalDateTime.now());
        CpsOrderDO refunded = order(CpsOrderStatusEnum.REFUNDED.getStatus(), LocalDateTime.now());
        refunded.setStatusVersion(1);
        when(orderMapper.selectForUpdateById(11L)).thenReturn(refunded);

        assertFalse(service.settleOrder(stale));

        verifyNoInteractions(memberUserApi, rebateConfigService, assetService);
        verify(rebateRecordMapper, never()).insert(any(CpsRebateRecordDO.class));
        verify(orderMapper, never()).updateRebateFreezeByStatusVersion(any(CpsOrderDO.class), anyInt());
    }

    @Test
    void settlementVersionConflictFailsWholeOrder() {
        CpsOrderDO order = order(CpsOrderStatusEnum.SETTLED.getStatus(), LocalDateTime.now());
        when(orderMapper.selectForUpdateById(11L)).thenReturn(order);
        MemberUserRespDTO member = new MemberUserRespDTO();
        member.setId(9L);
        member.setLevelId(3L);
        when(memberUserApi.getUser(9L)).thenReturn(member);
        when(rebateConfigService.matchRebateConfig(9L, 3L, "taobao"))
                .thenReturn(CpsRebateConfigDO.builder().id(7L).rebateRate(new BigDecimal("80")).build());
        when(moneyConverter.yuanToCent(new BigDecimal("8.00"))).thenReturn(800L);
        when(assetService.createOrderRebateFreeze(11L, "order-rebate:11"))
                .thenReturn(com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeRecordDO.builder()
                        .unfreezeTime(LocalDateTime.now().plusDays(15)).build());
        when(orderMapper.updateRebateFreezeByStatusVersion(any(CpsOrderDO.class), eq(0))).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.settleOrder(order));
    }

    @Test
    void skippedAndFailedOrdersAreMarkedForFairRetry() {
        CpsOrderDO skipped = order(CpsOrderStatusEnum.SETTLED.getStatus(), LocalDateTime.now());
        CpsOrderDO failed = order(CpsOrderStatusEnum.SETTLED.getStatus(), LocalDateTime.now());
        failed.setId(12L);
        when(orderMapper.selectPendingSettleOrders(anyList(), eq(2))).thenReturn(List.of(skipped, failed));
        when(settleExecutor.settleOne(skipped)).thenReturn(false);
        when(settleExecutor.settleOne(failed)).thenThrow(new IllegalStateException("member unavailable"));

        assertArrayEquals(new int[]{0, 1, 1}, service.batchSettle(2));

        verify(orderMapper).markSettleRetry(eq(11L), contains("待处理"), any(LocalDateTime.class));
        verify(orderMapper).markSettleRetry(eq(12L), contains("member unavailable"), any(LocalDateTime.class));
    }

    @Test
    void refundDelegatesToUnifiedAssetService() {
        when(rebateRecordMapper.selectByOrderIdAndType(eq(11L), anyString()))
                .thenReturn(CpsRebateRecordDO.builder().id(21L).orderId(11L).memberId(9L)
                        .rebateStatus("received").build());

        assertTrue(service.reverseRebate(11L));

        verify(assetService).reverseOrderRebate(11L, "order-refund:11");
        verify(rebateAccountMapper, never()).updateById(org.mockito.ArgumentMatchers.<com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO>any());
    }

    private static CpsOrderDO order(String status, LocalDateTime settleTime) {
        return CpsOrderDO.builder().id(11L).memberId(9L).platformCode("taobao")
                .platformOrderId("TB-11").orderStatus(status)
                .commissionAmount(new BigDecimal("10.00")).finalPrice(new BigDecimal("100.00"))
                .confirmReceiptTime(LocalDateTime.now().minusDays(1)).settleTime(settleTime)
                .statusVersion(0).build();
    }
}
