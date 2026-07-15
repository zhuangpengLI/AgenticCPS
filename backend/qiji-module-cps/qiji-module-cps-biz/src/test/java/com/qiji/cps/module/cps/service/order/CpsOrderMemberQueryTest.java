package com.qiji.cps.module.cps.service.order;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.controller.admin.order.vo.CpsOrderPageReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsOrderMemberQueryTest {

    @InjectMocks
    private CpsOrderServiceImpl orderService;

    @Mock
    private CpsOrderMapper orderMapper;

    @Test
    void getMemberOrderPageAlwaysUsesExplicitLoginMemberScope() {
        CpsOrderPageReqVO request = new CpsOrderPageReqVO();
        request.setPageNo(2);
        request.setPageSize(5);
        request.setPlatformCode("taobao");
        request.setOrderStatus("settled");
        request.setMemberId(2002L);
        when(orderMapper.selectPageByMemberId(request, 1001L))
                .thenReturn(new PageResult<>(List.of(CpsOrderDO.builder()
                        .id(9L)
                        .memberId(1001L)
                        .build()), 1L));

        PageResult<CpsOrderDO> page = orderService.getMemberOrderPage(request, 1001L);

        assertEquals(1L, page.getTotal());
        verify(orderMapper).selectPageByMemberId(same(request), org.mockito.ArgumentMatchers.eq(1001L));
    }

    @Test
    void getMemberOrderReturnsOnlyOwnedOrder() {
        when(orderMapper.selectById(9L)).thenReturn(CpsOrderDO.builder()
                .id(9L)
                .memberId(1001L)
                .platformCode("taobao")
                .build());

        CpsOrderDO order = orderService.getMemberOrder(1001L, 9L);

        assertEquals(9L, order.getId());
        assertEquals(1001L, order.getMemberId());
    }

    @Test
    void getMemberOrderRejectsAnotherMemberOrderAsMissing() {
        when(orderMapper.selectById(9L)).thenReturn(CpsOrderDO.builder()
                .id(9L)
                .memberId(2002L)
                .platformCode("taobao")
                .build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> orderService.getMemberOrder(1001L, 9L));

        assertEquals(ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }
}
