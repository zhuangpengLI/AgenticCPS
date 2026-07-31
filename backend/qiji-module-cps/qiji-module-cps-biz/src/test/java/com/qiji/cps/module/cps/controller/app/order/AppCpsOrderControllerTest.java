package com.qiji.cps.module.cps.controller.app.order;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.app.order.vo.AppCpsOrderPageReqVO;
import com.qiji.cps.module.cps.controller.app.order.vo.AppCpsOrderClaimReqVO;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.service.order.CpsOrderService;
import com.qiji.cps.module.cps.service.order.CpsOrderClaimCommand;
import com.qiji.cps.module.cps.service.order.CpsOrderClaimResult;
import com.qiji.cps.module.cps.service.order.CpsOrderClaimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsOrderControllerTest {

    @InjectMocks
    private AppCpsOrderController controller;

    @Mock
    private CpsOrderService orderService;
    @Mock
    private CpsOrderClaimService orderClaimService;

    @Test
    void pageUsesLoginMemberAndRequestCannotCarryMemberId() {
        assertFalse(List.of(AppCpsOrderPageReqVO.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getName().equals("memberId")));
        AppCpsOrderPageReqVO request = new AppCpsOrderPageReqVO();
        request.setPageNo(2);
        request.setPageSize(5);
        request.setPlatformCode("taobao");
        request.setOrderStatus("settled");
        when(orderService.getMemberOrderPage(argThat(req ->
                        req.getPageNo().equals(2)
                                && req.getPageSize().equals(5)
                                && "taobao".equals(req.getPlatformCode())
                                && "settled".equals(req.getOrderStatus())
                                && req.getMemberId() == null),
                org.mockito.ArgumentMatchers.eq(1001L)))
                .thenReturn(new PageResult<>(List.of(CpsOrderDO.builder()
                        .id(9L)
                        .memberId(1001L)
                        .platformCode("taobao")
                        .platformOrderId("TB-9")
                        .itemTitle("order item")
                        .estimateRebate(new BigDecimal("1.23"))
                        .build()), 1L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var page = controller.getMyOrderPage(request).getData();

            assertEquals(1L, page.getTotal());
            assertEquals(9L, page.getList().get(0).getId());
        }

        verify(orderService).getMemberOrderPage(argThat(req ->
                req.getMemberId() == null
                        && "taobao".equals(req.getPlatformCode())
                        && "settled".equals(req.getOrderStatus())), org.mockito.ArgumentMatchers.eq(1001L));
    }

    @Test
    void detailIsAlwaysScopedToLoginMember() {
        when(orderService.getMemberOrder(1001L, 9L)).thenReturn(CpsOrderDO.builder()
                .id(9L)
                .memberId(1001L)
                .platformCode("taobao")
                .platformOrderId("TB-9")
                .build());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            assertEquals(9L, controller.getMyOrder(9L).getData().getId());
        }

        verify(orderService).getMemberOrder(1001L, 9L);
    }

    @Test
    void claimUsesLoginMemberAndRequestCannotChooseMemberIdentity() {
        assertFalse(List.of(AppCpsOrderClaimReqVO.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getName().equals("memberId")));
        AppCpsOrderClaimReqVO request = new AppCpsOrderClaimReqVO();
        request.setPlatformCode("eleme");
        request.setPlatformOrderId("ELM-CLAIM-1");
        request.setIdempotencyKey("app-claim-1");
        when(orderClaimService.claim(new CpsOrderClaimCommand(1001L, "eleme", "ELM-CLAIM-1",
                null, null, null, "app-claim-1")))
                .thenReturn(new CpsOrderClaimResult(1L, 2L, "eleme", "ELM-CLAIM-1",
                        "PENDING_REVIEW", "等待审核"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            assertEquals("PENDING_REVIEW", controller.claimOrder(request).getData().status());
        }

        verify(orderClaimService).claim(new CpsOrderClaimCommand(1001L, "eleme", "ELM-CLAIM-1",
                null, null, null, "app-claim-1"));
    }
}
