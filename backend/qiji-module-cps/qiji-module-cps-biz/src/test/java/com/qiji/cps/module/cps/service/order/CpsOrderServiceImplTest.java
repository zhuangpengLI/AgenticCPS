package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.client.dto.CpsOrderQueryRequest;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncLogDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncLogMapper;
import com.qiji.cps.module.cps.enums.CpsOrderStatusEnum;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderServiceImplTest {

    @InjectMocks
    private CpsOrderServiceImpl orderService;

    @Mock
    private CpsOrderMapper orderMapper;
    @Mock
    private CpsOrderSyncLogMapper syncLogMapper;
    @Mock
    private CpsPlatformClientFactory platformClientFactory;
    @Mock
    private CpsRebateSettleService rebateSettleService;
    @Mock
    private CpsPlatformClient platformClient;

    @Test
    @DisplayName("saveOrUpdateOrder - 已到账订单收到退款状态时触发返利扣回")
    void saveOrUpdateOrder_reverseRebateWhenRefundedAfterCredited() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(1L)
                .platformOrderId("TB-1")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus())
                .commissionAmount(new BigDecimal("12.00"))
                .rebateTime(LocalDateTime.now().minusDays(1))
                .build();
        when(orderMapper.selectByPlatformOrderId("TB-1")).thenReturn(existing);
        when(rebateSettleService.reverseRebate(1L)).thenReturn(true);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-1")
                .platformCode("taobao")
                .commissionAmount(new BigDecimal("12.00"))
                .refundTag(1)
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(rebateSettleService).reverseRebate(1L);
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(1L)
                        && CpsOrderStatusEnum.REFUNDED.getStatus().equals(order.getOrderStatus())
                        && order.getRefundTime() != null));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 已到账订单遇到较早平台状态时不回滚")
    void saveOrUpdateOrder_doesNotRollbackCreditedOrderStatus() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(2L)
                .platformOrderId("TB-2")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.REBATE_RECEIVED.getStatus())
                .commissionAmount(new BigDecimal("12.00"))
                .rebateTime(LocalDateTime.now().minusHours(2))
                .build();
        when(orderMapper.selectByPlatformOrderId("TB-2")).thenReturn(existing);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-2")
                .platformCode("taobao")
                .platformStatus(3)
                .commissionAmount(new BigDecimal("15.00"))
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(rebateSettleService, never()).reverseRebate(2L);
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(2L)
                        && CpsOrderStatusEnum.REBATE_RECEIVED.getStatus().equals(order.getOrderStatus())
                        && new BigDecimal("15.00").compareTo(order.getCommissionAmount()) == 0));
    }

    @Test
    @DisplayName("saveOrUpdateOrder - 重新同步应修正已有订单金额快照和会员归因")
    void saveOrUpdateOrder_refreshesExistingOrderSnapshotAndAttribution() {
        CpsOrderDO existing = CpsOrderDO.builder()
                .id(3L)
                .platformOrderId("TB-3")
                .platformCode("taobao")
                .orderStatus(CpsOrderStatusEnum.PAID.getStatus())
                .itemPrice(new BigDecimal("999.00"))
                .finalPrice(BigDecimal.ZERO)
                .commissionRate(new BigDecimal("4.5"))
                .commissionAmount(BigDecimal.ZERO)
                .estimateRebate(BigDecimal.ZERO)
                .build();
        when(orderMapper.selectByPlatformOrderId("TB-3")).thenReturn(existing);

        CpsOrderDTO dto = CpsOrderDTO.builder()
                .platformOrderId("TB-3")
                .platformCode("taobao")
                .parentOrderId("TB-PARENT-3")
                .itemId("ITEM-3")
                .itemTitle("旗舰婴儿推车")
                .itemPrice(new BigDecimal("999.00"))
                .finalPrice(new BigDecimal("399.00"))
                .commissionRate(new BigDecimal("4.5"))
                .commissionAmount(new BigDecimal("17.96"))
                .platformStatus(1)
                .adzoneId("mm_111_222_333")
                .externalId("1002")
                .build();

        int result = orderService.saveOrUpdateOrder(dto);

        assertEquals(2, result);
        verify(orderMapper).updateById(org.mockito.ArgumentMatchers.<CpsOrderDO>argThat(order ->
                order.getId().equals(3L)
                        && new BigDecimal("399.00").compareTo(order.getFinalPrice()) == 0
                        && new BigDecimal("17.96").compareTo(order.getCommissionAmount()) == 0
                        && new BigDecimal("14.37").compareTo(order.getEstimateRebate()) == 0
                        && Long.valueOf(1002L).equals(order.getMemberId())
                        && "1002".equals(order.getExternalInfo())
                        && "ITEM-3".equals(order.getItemId())));
    }

    @Test
    @DisplayName("manualSync - 状态同步应按更新时间查询平台订单")
    void manualSync_usesUpdateTimeQueryTypeForStatusSync() {
        when(platformClientFactory.getRequiredClient("taobao")).thenReturn(platformClient);
        when(platformClient.queryOrders(any(CpsOrderQueryRequest.class))).thenReturn(List.of(CpsOrderDTO.builder()
                .platformCode("taobao")
                .platformOrderId("TB-STATUS-1")
                .platformStatus(3)
                .commissionAmount(new BigDecimal("8.00"))
                .build()));
        when(orderMapper.selectByPlatformOrderId("TB-STATUS-1")).thenReturn(null);

        String result = orderService.manualSync("taobao", 6, 4);

        verify(platformClient).queryOrders(argThat(req ->
                Integer.valueOf(4).equals(req.getQueryType())
                        && Integer.valueOf(50).equals(req.getPageSize())
                        && req.getStartTime() != null
                        && req.getEndTime() != null));
        verify(syncLogMapper).insert(org.mockito.ArgumentMatchers.<CpsOrderSyncLogDO>argThat(log ->
                Integer.valueOf(4).equals(log.getQueryType())
                        && Integer.valueOf(1).equals(log.getSyncStatus())
                        && Integer.valueOf(1).equals(log.getTotalCount())));
        assertEquals("平台[taobao] 手动同步完成: 共1条，新增1，更新0，跳过0", result);
    }
}
