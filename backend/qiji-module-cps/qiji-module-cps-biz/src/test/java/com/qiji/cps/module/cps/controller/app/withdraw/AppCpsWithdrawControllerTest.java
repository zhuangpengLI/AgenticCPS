package com.qiji.cps.module.cps.controller.app.withdraw;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.app.withdraw.vo.AppCpsWithdrawCreateReqVO;
import com.qiji.cps.module.cps.dal.dataobject.withdraw.CpsWithdrawDO;
import com.qiji.cps.module.cps.service.withdraw.CpsWithdrawService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsWithdrawControllerTest {

    @InjectMocks
    private AppCpsWithdrawController controller;
    @Mock
    private CpsWithdrawService withdrawService;

    @Test
    void createUsesLoginMemberAndRequestCannotCarryMemberId() {
        assertFalse(List.of(AppCpsWithdrawCreateReqVO.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getName().equals("memberId")));
        AppCpsWithdrawCreateReqVO request = new AppCpsWithdrawCreateReqVO();
        request.setAmountCent(1200L);
        request.setWithdrawType("alipay");
        request.setWithdrawAccount("member@example.com");
        request.setWithdrawAccountName("Member");
        request.setIdempotencyKey("withdraw-1");
        when(withdrawService.createWithdraw(1001L, request)).thenReturn(9L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);
            assertEquals(9L, controller.createWithdraw(request).getData());
        }

        verify(withdrawService).createWithdraw(1001L, request);
    }

    @Test
    void pageAndDetailAreAlwaysScopedToLoginMember() {
        CpsWithdrawDO withdraw = CpsWithdrawDO.builder().id(9L).memberId(1001L).amountCent(1200L).build();
        when(withdrawService.getMemberWithdrawPage(1001L, 2, 5))
                .thenReturn(new PageResult<>(List.of(withdraw), 1L));
        when(withdrawService.getMemberWithdraw(1001L, 9L)).thenReturn(withdraw);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);
            assertEquals(1L, controller.getMyWithdrawPage(2, 5).getData().getTotal());
            assertEquals(9L, controller.getMyWithdraw(9L).getData().getId());
        }

        verify(withdrawService).getMemberWithdrawPage(1001L, 2, 5);
        verify(withdrawService).getMemberWithdraw(1001L, 9L);
    }
}
