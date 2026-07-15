package com.qiji.cps.module.cps.controller.admin.rebate;

import com.qiji.cps.framework.security.core.util.SecurityFrameworkUtils;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateDebtAdjustReqVO;
import com.qiji.cps.module.cps.service.rebate.asset.CpsAssetOperatorContext;
import com.qiji.cps.module.cps.service.rebate.asset.CpsDebtAdjustAction;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetQueryService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetPolicyService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationCheckReport;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationCheckService;
import com.qiji.cps.module.cps.service.rebate.asset.CpsRebateAssetMigrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateAssetControllerTest {

    @InjectMocks
    private CpsRebateAssetController controller;

    @Mock
    private CpsRebateAssetQueryService queryService;
    @Mock
    private CpsRebateAssetService assetService;
    @Mock
    private CpsRebateAssetPolicyService policyService;
    @Mock
    private CpsRebateAssetMigrationCheckService migrationCheckService;
    @Mock
    private CpsRebateAssetMigrationService migrationService;

    @Test
    void runMigrationCheckUsesLoggedInAdminAndReturnsArchivedReport() {
        CpsRebateAssetMigrationCheckReport report = CpsRebateAssetMigrationCheckReport.builder()
                .batchNo("batch-1").tenantId(1L).ready(true).build();
        when(migrationCheckService.runCheck("9001")).thenReturn(report);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            assertEquals(report, controller.runMigrationCheck().getData());
        }
        verify(migrationCheckService).runCheck("9001");
    }

    @Test
    void adjustDebt_delegatesAuditedAdminContext() {
        CpsRebateDebtAdjustReqVO request = new CpsRebateDebtAdjustReqVO();
        request.setMemberId(1001L);
        request.setAction("WAIVE");
        request.setAmountCent(1250L);
        request.setReason("客服核实退款责任由平台承担");
        request.setIdempotencyKey("debt-adjust-001");

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);

            assertEquals(Boolean.TRUE, controller.adjustDebt(request).getData());
        }

        ArgumentCaptor<CpsAssetOperatorContext> contextCaptor =
                ArgumentCaptor.forClass(CpsAssetOperatorContext.class);
        verify(assetService).manualAdjustDebt(
                org.mockito.ArgumentMatchers.eq(1001L),
                org.mockito.ArgumentMatchers.eq(CpsDebtAdjustAction.WAIVE),
                org.mockito.ArgumentMatchers.eq(1250L),
                org.mockito.ArgumentMatchers.eq("ADMIN_DEBT_ADJUST:1001:debt-adjust-001"),
                contextCaptor.capture());
        CpsAssetOperatorContext context = contextCaptor.getValue();
        assertEquals("ADMIN", context.operatorType());
        assertEquals("9001", context.operatorId());
        assertEquals("debt-adjust-001", context.idempotencyKey());
        assertEquals("客服核实退款责任由平台承担", context.reason());
    }
}
