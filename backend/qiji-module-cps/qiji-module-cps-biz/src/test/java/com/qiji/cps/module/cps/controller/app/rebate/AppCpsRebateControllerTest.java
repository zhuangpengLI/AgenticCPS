package com.qiji.cps.module.cps.controller.app.rebate;

import com.qiji.cps.framework.common.pojo.PageParam;
import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtSummaryRespVO;
import com.qiji.cps.module.cps.dal.dataobject.exchange.CpsRebateTokenExchangeOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.service.exchange.CpsRebateTokenExchangeService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateRecordService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateSettleService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCpsRebateControllerTest {

    @InjectMocks
    private AppCpsRebateController controller;

    @Mock
    private CpsRebateAssetQueryService assetQueryService;

    @Mock
    private CpsRebateRecordService rebateRecordService;

    @Mock
    private CpsRebateSettleService rebateSettleService;

    @Mock
    private CpsRebateTokenExchangeService rebateTokenExchangeService;

    @Test
    void getMyAccountReturnsRealPendingRebateForLoginMember() {
        when(rebateSettleService.getOrInitAccount(1001L)).thenReturn(CpsRebateAccountDO.builder()
                .id(1L)
                .memberId(1001L)
                .availableBalance(new BigDecimal("30.00"))
                .frozenBalance(new BigDecimal("8.00"))
                .totalRebate(new BigDecimal("50.00"))
                .build());
        when(rebateRecordService.getMemberPendingRebate(1001L)).thenReturn(new BigDecimal("12.34"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.getMyAccount().getData();

            assertEquals(new BigDecimal("12.34"), response.getPendingRebate());
            assertEquals(new BigDecimal("30.00"), response.getWithdrawableBalance());
            assertEquals(new BigDecimal("30.00"), response.getExchangeableBalance());
        }

        verify(rebateSettleService).getOrInitAccount(1001L);
        verify(rebateRecordService).getMemberPendingRebate(1001L);
    }

    @Test
    void getMyDebtSummary_usesLoginMemberOnly() {
        when(assetQueryService.getDebtSummary(1001L)).thenReturn(CpsRebateDebtSummaryRespVO.builder()
                .memberId(1001L)
                .debtCount(2L)
                .originalDebtCent(5000L)
                .repaidDebtCent(1200L)
                .waivedDebtCent(300L)
                .outstandingDebtCent(3500L)
                .build());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.getMyDebtSummary().getData();

            assertEquals(2L, response.getDebtCount());
            assertEquals(3500L, response.getOutstandingDebtCent());
        }

        verify(assetQueryService).getDebtSummary(1001L);
    }

    @Test
    void getMyDebtRepaymentPage_usesLoginMemberAndRequestedPage() {
        CpsRebateAssetLedgerDO repayment = CpsRebateAssetLedgerDO.builder()
                .id(11L)
                .memberId(1001L)
                .businessType("ORDER_REBATE_RELEASE")
                .businessId("freeze-9")
                .orderId(9L)
                .debtChangeCent(-800L)
                .debtBeforeCent(2000L)
                .debtAfterCent(1200L)
                .reason("返利解冻自动偿债")
                .build();
        when(assetQueryService.getMemberDebtRepaymentPage(
                org.mockito.ArgumentMatchers.eq(1001L), argThat(page -> page.getPageNo() == 2
                        && page.getPageSize() == 5)))
                .thenReturn(new PageResult<>(List.of(repayment), 1L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            var response = controller.getMyDebtRepaymentPage(2, 5).getData();

            assertEquals(1L, response.getTotal());
            assertEquals(-800L, response.getList().get(0).getDebtChangeCent());
            assertEquals(1200L, response.getList().get(0).getDebtAfterCent());
        }

        verify(assetQueryService).getMemberDebtRepaymentPage(
                org.mockito.ArgumentMatchers.eq(1001L), argThat(page -> page.getPageNo() == 2
                        && page.getPageSize() == 5));
    }

    @Test
    void getTokenExchangeOrder_rejectsOrderOwnedByAnotherMember() {
        when(rebateTokenExchangeService.getExchangeOrder("CPSX-OTHER"))
                .thenReturn(CpsRebateTokenExchangeOrderDO.builder()
                        .exchangeOrderNo("CPSX-OTHER").memberId(2002L).build());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1001L);

            assertThrows(RuntimeException.class,
                    () -> controller.getTokenExchangeOrder("CPSX-OTHER"));
        }
    }
}
