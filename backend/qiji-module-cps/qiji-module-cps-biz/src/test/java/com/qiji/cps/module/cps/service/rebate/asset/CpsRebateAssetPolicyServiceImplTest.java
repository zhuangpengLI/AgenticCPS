package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.freeze.CpsFreezeConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.mysql.freeze.CpsFreezeConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckArchiveMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetPolicyMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpsRebateAssetPolicyServiceImplTest {

    @Mock
    private CpsRebateAssetPolicyMapper policyMapper;
    @Mock
    private CpsFreezeConfigMapper freezeConfigMapper;
    @Mock
    private CpsRebateAssetMigrationCheckService migrationCheckService;
    @Mock
    private CpsRebateAssetMigrationCheckArchiveMapper archiveMapper;

    private CpsRebateAssetPolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CpsRebateAssetPolicyServiceImpl(policyMapper, freezeConfigMapper,
                migrationCheckService, archiveMapper);
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getPolicy_returnsSafeDefaultsWhenTenantHasNoConfiguration() {
        CpsRebateAssetPolicyDO policy = service.getPolicy();

        assertFalse(policy.getV2Enabled());
        assertFalse(policy.getReadOnly());
        assertEquals(10_000L, policy.getLargeDebtThresholdCent());
        assertEquals(7, policy.getReminderIntervalDays());
        assertEquals(30, policy.getNormalReminderDays());
        assertEquals(180, policy.getLargeReminderDays());
        assertEquals(30, policy.getSmsIntervalDays());
    }

    @Test
    void initializePolicy_createsDefaultsAndIsSafeToRetry() {
        when(policyMapper.selectCurrentTenant()).thenReturn(null);
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(java.util.List.of());

        CpsRebateAssetPolicyDO policy = service.initializePolicy();

        assertFalse(policy.getV2Enabled());
        verify(policyMapper).insert(policy);
        verify(freezeConfigMapper).insert(org.mockito.ArgumentMatchers.argThat((CpsFreezeConfigDO config) ->
                Long.valueOf(1000L).equals(config.getMinAmountCent())
                        && Integer.valueOf(7).equals(config.getUnfreezeDays())));
    }

    @Test
    void confirmMigrationReady_bindsLatestReadyArchiveAndApprovalRef() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 9, 3, 10, 0);
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).v2Enabled(false).migrationReady(false).build());
        when(archiveMapper.selectLatestByTenantId(7L)).thenReturn(readyArchive("ready-batch", executedAt));

        CpsRebateAssetPolicyDO confirmed = service.confirmMigrationReady("  CHG-20260903-001  ");

        assertTrue(confirmed.getMigrationReady());
        assertEquals("ready-batch", confirmed.getLatestReadyCheckBatchNo());
        assertEquals(executedAt, confirmed.getReadyCheckTime());
        assertEquals("CHG-20260903-001", confirmed.getMigrationApprovalRef());
        verify(policyMapper).updateById(confirmed);
    }

    @Test
    void confirmMigrationReady_rejectsWhenLatestArchiveIsBlocked() {
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).v2Enabled(false).migrationReady(false).build());
        when(archiveMapper.selectLatestByTenantId(7L)).thenReturn(
                CpsRebateAssetMigrationCheckArchiveDO.builder().ready(false).build());

        assertThrows(IllegalStateException.class, () -> service.confirmMigrationReady("CHG-1"));
        verify(policyMapper, never()).updateById(any(CpsRebateAssetPolicyDO.class));
    }

    @Test
    void savePolicy_appliesDefaultsWhenOptionalFieldsAreOmitted() {
        when(policyMapper.selectCurrentTenant()).thenReturn(null);
        when(freezeConfigMapper.selectEnabledRules()).thenReturn(java.util.List.of());

        CpsRebateAssetPolicyDO policy = CpsRebateAssetPolicyDO.builder().build();
        service.savePolicy(policy);

        assertFalse(policy.getV2Enabled());
        assertFalse(policy.getReadOnly());
        assertEquals(10_000L, policy.getLargeDebtThresholdCent());
        assertEquals(7, policy.getReminderIntervalDays());
        verify(policyMapper).insert(policy);
    }

    @Test
    void assertWritable_rejectsAllMoneyMutationWhenTenantIsReadOnly() {
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .readOnly(true).v2Enabled(true).build());

        assertThrows(IllegalStateException.class, service::assertWritable);
    }

    @Test
    void assertWritable_rejectsMoneyMutationBeforeTenantEnablesV2() {
        when(policyMapper.selectCurrentTenant()).thenReturn(null);

        assertThrows(IllegalStateException.class, service::assertWritable);
    }

    @Test
    void savePolicy_updatesExistingTenantRowWithoutChangingItsIdentity() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 13, 16, 0);
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).migrationReady(true).latestReadyCheckBatchNo("release-b-batch")
                .readyCheckTime(approvedAt).build());
        when(archiveMapper.selectLatestByTenantId(7L)).thenReturn(readyArchive("release-b-batch", approvedAt));
        LocalDateTime recheckedAt = approvedAt.plusMinutes(5);
        when(migrationCheckService.runCheck("SYSTEM:ASSET_POLICY_ENABLE"))
                .thenReturn(CpsRebateAssetMigrationCheckReport.builder()
                        .tenantId(7L).batchNo("enable-batch").executedAt(recheckedAt).ready(true).build());
        CpsRebateAssetPolicyDO requested = CpsRebateAssetPolicyDO.builder()
                .v2Enabled(true).readOnly(false).largeDebtThresholdCent(20_000L)
                .reminderIntervalDays(5).normalReminderDays(20).largeReminderDays(120).smsIntervalDays(15).build();

        service.savePolicy(requested);

        requested.setId(9L);
        requested.setMigrationReady(true);
        requested.setLatestReadyCheckBatchNo("enable-batch");
        requested.setReadyCheckTime(recheckedAt);
        verify(policyMapper).updateById(requested);
        assertTrue(requested.getV2Enabled());
    }

    @Test
    void savePolicy_rejectsDisablingV2AfterTenantHasEnabledIt() {
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).v2Enabled(true).migrationReady(true).build());

        CpsRebateAssetPolicyDO requested = validPolicy(false);

        assertThrows(IllegalStateException.class, () -> service.savePolicy(requested));
    }

    @Test
    void savePolicy_rejectsV2ActivationUntilReleaseBAndOpeningLedgerAreVerified() {
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).v2Enabled(false).migrationReady(false).build());

        assertThrows(IllegalStateException.class, () -> service.savePolicy(validPolicy(true)));
    }

    @Test
    void savePolicy_rejectsV2ActivationWhenApprovalDoesNotPointToLatestReadyArchive() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 13, 16, 0);
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).v2Enabled(false).migrationReady(true)
                .latestReadyCheckBatchNo("stale-batch").readyCheckTime(approvedAt).build());
        when(archiveMapper.selectLatestByTenantId(7L))
                .thenReturn(readyArchive("newer-batch", approvedAt.plusMinutes(1)));

        assertThrows(IllegalStateException.class, () -> service.savePolicy(validPolicy(true)));

        verify(migrationCheckService, never()).runCheck(any());
        verify(policyMapper, never()).updateById(any(CpsRebateAssetPolicyDO.class));
    }

    @Test
    void savePolicy_rechecksInsideActivationAndRejectsCurrentDrift() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 13, 16, 0);
        when(policyMapper.selectCurrentTenant()).thenReturn(CpsRebateAssetPolicyDO.builder()
                .id(9L).v2Enabled(false).migrationReady(true)
                .latestReadyCheckBatchNo("release-b-batch").readyCheckTime(approvedAt).build());
        when(archiveMapper.selectLatestByTenantId(7L)).thenReturn(readyArchive("release-b-batch", approvedAt));
        when(migrationCheckService.runCheck("SYSTEM:ASSET_POLICY_ENABLE"))
                .thenReturn(CpsRebateAssetMigrationCheckReport.builder()
                        .tenantId(7L).batchNo("blocked-batch").executedAt(approvedAt.plusMinutes(5))
                        .ready(false).build());

        assertThrows(IllegalStateException.class, () -> service.savePolicy(validPolicy(true)));

        verify(policyMapper, never()).updateById(any(CpsRebateAssetPolicyDO.class));
    }

    private CpsRebateAssetPolicyDO validPolicy(boolean enabled) {
        return CpsRebateAssetPolicyDO.builder().v2Enabled(enabled).readOnly(false)
                .largeDebtThresholdCent(10_000L).reminderIntervalDays(7)
                .normalReminderDays(30).largeReminderDays(180).smsIntervalDays(30).build();
    }


    private CpsRebateAssetMigrationCheckArchiveDO readyArchive(String batchNo, LocalDateTime executedAt) {
        return CpsRebateAssetMigrationCheckArchiveDO.builder()
                .tenantId(7L).batchNo(batchNo).executedAt(executedAt).ready(true).build();
    }
}
