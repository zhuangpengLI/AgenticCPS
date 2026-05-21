package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsOrderDTO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
