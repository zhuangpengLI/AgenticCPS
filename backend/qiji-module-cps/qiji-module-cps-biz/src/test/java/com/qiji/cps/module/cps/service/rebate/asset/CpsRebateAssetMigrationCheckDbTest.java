package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckArchiveMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsRebateAssetMigrationCheckDbTest extends BaseDbUnitTest {

    @Resource private DataSource dataSource;
    @Resource private CpsRebateAssetMigrationCheckMapper checkMapper;
    @Resource private CpsRebateAssetMigrationCheckArchiveMapper archiveMapper;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void reportQueriesAndArchiveAreTenantIsolated() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO cps_rebate_account
                (member_id, total_rebate, available_balance, frozen_balance, debt_balance,
                 withdrawn_amount, status, version, tenant_id)
                VALUES (101, 10.00, 10.00, 0.00, 0.00, 0.00, 1, 0, 1)
                """);
        }
        CpsRebateAssetMigrationCheckService service =
                new CpsRebateAssetMigrationCheckServiceImpl(checkMapper, archiveMapper);

        TenantContextHolder.setTenantId(1L);
        CpsRebateAssetMigrationCheckReport blocked = service.runCheck("admin-1");
        assertFalse(blocked.isReady());
        assertEquals(1L, blocked.getAccountLedgerMismatchCount());
        assertEquals(1L, blocked.getMissingOpeningBalanceCount());
        assertEquals(1, service.getArchives().size());

        TenantContextHolder.setTenantId(2L);
        CpsRebateAssetMigrationCheckReport ready = service.runCheck("admin-2");
        assertTrue(ready.isReady());
        assertEquals(1, service.getArchives().size());
        assertEquals(2L, service.getArchives().getFirst().getTenantId());
    }

    @Test
    void nullAccountBalanceCannotHideLedgerMismatch() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO cps_rebate_account
                (id, member_id, total_rebate, available_balance, frozen_balance, debt_balance,
                 withdrawn_amount, status, version, tenant_id)
                VALUES (1001, 101, 0.00, NULL, 0.00, 0.00, 0.00, 1, 0, 1)
                """);
            statement.executeUpdate("""
                INSERT INTO cps_rebate_asset_ledger
                (member_id, source_system, business_type, business_id, idempotency_key,
                 available_change_cent, frozen_change_cent, debt_change_cent,
                 available_before_cent, available_after_cent, frozen_before_cent, frozen_after_cent,
                 debt_before_cent, debt_after_cent, operator_type, reason, tenant_id)
                VALUES (101, 'CPS_MIGRATION', 'OPENING_BALANCE', '1001', 'opening-balance:1001',
                        100, 0, 0, 0, 100, 0, 0, 0, 0, 'ADMIN', 'null balance mismatch', 1)
                """);
        }

        assertEquals(1L, checkMapper.countAccountLedgerNetMismatches(1L));
        assertEquals(0L, checkMapper.countAccountLedgerNetMismatches(2L));
    }

    @Test
    void orphanLedgerAndActiveFreezeRemainTenantIsolated() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO cps_rebate_account
                (member_id, total_rebate, available_balance, frozen_balance, debt_balance,
                 withdrawn_amount, status, version, tenant_id)
                VALUES (404, 0.00, 0.00, 0.00, 0.00, 0.00, 1, 0, 2),
                       (405, 0.00, 0.00, 0.00, 0.00, 0.00, 1, 0, 2)
                """);
            statement.executeUpdate("""
                INSERT INTO cps_rebate_asset_ledger
                (member_id, source_system, business_type, business_id, idempotency_key,
                 available_change_cent, frozen_change_cent, debt_change_cent,
                 available_before_cent, available_after_cent, frozen_before_cent, frozen_after_cent,
                 debt_before_cent, debt_after_cent, operator_type, reason, tenant_id)
                VALUES (404, 'CPS', 'ORDER_REBATE_FREEZE', 'order-404', 'ledger-orphan-404',
                        0, 100, 0, 0, 0, 0, 100, 0, 0, 'SYSTEM', 'orphan ledger', 1)
                """);
            statement.executeUpdate("""
                INSERT INTO cps_freeze_record
                (member_id, business_type, business_id, idempotency_key,
                 freeze_amount, amount_cent, status, tenant_id)
                VALUES (405, 'ORDER_REBATE', 'order-405', 'freeze-orphan-405',
                        1.00, 100, 'frozen', 1)
                """);
        }

        assertEquals(1L, checkMapper.countOrphanLedgerRecords(1L));
        assertEquals(1L, checkMapper.countOrphanActiveFreezeRecords(1L));
        assertEquals(0L, checkMapper.countOrphanLedgerRecords(2L));
        assertEquals(0L, checkMapper.countOrphanActiveFreezeRecords(2L));

        TenantContextHolder.setTenantId(1L);
        CpsRebateAssetMigrationCheckService service =
                new CpsRebateAssetMigrationCheckServiceImpl(checkMapper, archiveMapper);
        CpsRebateAssetMigrationCheckReport report = service.runCheck("admin-orphan-audit");
        assertFalse(report.isReady());
        assertEquals(1L, report.getOrphanLedgerCount());
        assertEquals(1L, report.getOrphanActiveFreezeCount());
        assertEquals(1L, service.getArchives().getFirst().getOrphanLedgerCount());
        assertEquals(1L, service.getArchives().getFirst().getOrphanActiveFreezeCount());
    }

    @Test
    void duplicateFreezeIdempotencyIsDetectedWithinTenantOnly() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                ALTER TABLE cps_freeze_record
                DROP CONSTRAINT IF EXISTS uk_freeze_tenant_business_idem
                """);
            try {
                statement.executeUpdate("""
                    INSERT INTO cps_freeze_record
                    (member_id, business_type, business_id, idempotency_key,
                     freeze_amount, amount_cent, status, tenant_id)
                    VALUES (501, 'ORDER_REBATE', 'order-a', 'same-freeze-key', 1.00, 100, 'frozen', 1),
                           (502, 'ORDER_REBATE', 'order-b', 'same-freeze-key', 2.00, 200, 'frozen', 1),
                           (503, 'ORDER_REBATE', 'order-c', 'same-freeze-key', 3.00, 300, 'frozen', 2)
                    """);

                assertEquals(1L, checkMapper.countDuplicateFreezeIdempotencyKeys(1L));
                assertEquals(0L, checkMapper.countDuplicateFreezeIdempotencyKeys(2L));
            } finally {
                statement.executeUpdate("DELETE FROM cps_freeze_record");
                statement.executeUpdate("""
                    ALTER TABLE cps_freeze_record
                    ADD CONSTRAINT uk_freeze_tenant_business_idem
                    UNIQUE (tenant_id, business_type, idempotency_key)
                    """);
            }
        }
    }
}
