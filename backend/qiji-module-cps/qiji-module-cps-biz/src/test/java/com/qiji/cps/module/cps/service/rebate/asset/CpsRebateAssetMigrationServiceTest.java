package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;

@ExtendWith(MockitoExtension.class)
class CpsRebateAssetMigrationServiceTest {

    @Mock private CpsRebateAccountMapper accountMapper;
    @Mock private CpsRebateAssetLedgerMapper ledgerMapper;
    @Mock private CpsRebateAssetPolicyService policyService;
    private CpsRebateAssetMigrationService service;

    @BeforeEach
    void setUp() {
        service = new CpsRebateAssetMigrationService(accountMapper, ledgerMapper,
                new CpsMoneyConverter(), policyService);
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void backfillOpeningBalancesAppendsAuditableNetAssetWithoutChangingAccount() {
        when(policyService.getPolicy()).thenReturn(CpsRebateAssetPolicyDO.builder().v2Enabled(false).build());
        when(accountMapper.selectList(any())).thenReturn(List.of(CpsRebateAccountDO.builder()
                .id(7L).memberId(8L).availableBalance(new BigDecimal("12.34"))
                .frozenBalance(new BigDecimal("5.00")).debtBalance(new BigDecimal("2.00")).build()));
        when(ledgerMapper.selectOpeningBalanceByAccountId(7L)).thenReturn(null);

        assertEquals(1, service.backfillOpeningBalances("99"));

        ArgumentCaptor<CpsRebateAssetLedgerDO> captor = ArgumentCaptor.forClass(CpsRebateAssetLedgerDO.class);
        verify(ledgerMapper).insert(captor.capture());
        assertEquals(1234L, captor.getValue().getAvailableAfterCent());
        assertEquals(500L, captor.getValue().getFrozenAfterCent());
        assertEquals(200L, captor.getValue().getDebtAfterCent());
        assertEquals("opening-balance:7", captor.getValue().getIdempotencyKey());
        assertEquals(1L, captor.getValue().getTenantId());
    }

    @Test
    void backfillOpeningBalancesRejectsAfterV2Activation() {
        when(policyService.getPolicy()).thenReturn(CpsRebateAssetPolicyDO.builder().v2Enabled(true).build());

        assertThrows(IllegalStateException.class, () -> service.backfillOpeningBalances("99"));

        verifyNoInteractions(accountMapper, ledgerMapper);
    }

    @Test
    void backfillOpeningBalancesRejectsAfterMigrationWasMarkedReady() {
        when(policyService.getPolicy()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .v2Enabled(false).migrationReady(true).build());

        assertThrows(IllegalStateException.class, () -> service.backfillOpeningBalances("99"));

        verifyNoInteractions(accountMapper, ledgerMapper);
    }

    @Test
    void concurrentOpeningBalanceConflictReturnsIdempotentZero() {
        when(policyService.getPolicy()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .v2Enabled(false).migrationReady(false).build());
        when(accountMapper.selectList(any())).thenReturn(List.of(CpsRebateAccountDO.builder()
                .id(7L).memberId(8L).availableBalance(new BigDecimal("12.34"))
                .frozenBalance(BigDecimal.ZERO).debtBalance(BigDecimal.ZERO).build()));
        when(ledgerMapper.selectOpeningBalanceByAccountId(7L))
                .thenReturn(null, CpsRebateAssetLedgerDO.builder().id(99L).build());
        doThrow(new DuplicateKeyException("concurrent winner"))
                .when(ledgerMapper).insert(any(CpsRebateAssetLedgerDO.class));

        assertEquals(0, service.backfillOpeningBalances("99"));
    }
}
