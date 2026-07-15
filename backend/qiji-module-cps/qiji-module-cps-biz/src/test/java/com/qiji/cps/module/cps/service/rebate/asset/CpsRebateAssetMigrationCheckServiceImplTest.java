package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckArchiveMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CpsRebateAssetMigrationCheckServiceImplTest {

    private CpsRebateAssetMigrationCheckMapper checkMapper;
    private CpsRebateAssetMigrationCheckArchiveMapper archiveMapper;
    private CpsRebateAssetMigrationCheckService service;

    @BeforeEach
    void setUp() {
        checkMapper = mock(CpsRebateAssetMigrationCheckMapper.class);
        archiveMapper = mock(CpsRebateAssetMigrationCheckArchiveMapper.class);
        service = new CpsRebateAssetMigrationCheckServiceImpl(checkMapper, archiveMapper);
        TenantContextHolder.setTenantId(7L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void cleanTenantIsReadyAndReportIsArchived() {
        CpsRebateAssetMigrationCheckReport report = service.runCheck("admin-1");

        assertTrue(report.isReady());
        assertEquals(7L, report.getTenantId());
        ArgumentCaptor<CpsRebateAssetMigrationCheckArchiveDO> archive =
                ArgumentCaptor.forClass(CpsRebateAssetMigrationCheckArchiveDO.class);
        verify(archiveMapper).insert(archive.capture());
        assertTrue(archive.getValue().isReady());
        assertEquals("admin-1", archive.getValue().getOperatorId());
    }

    @ParameterizedTest
    @EnumSource(Discrepancy.class)
    void everyDiscrepancyBlocksMigration(Discrepancy discrepancy) {
        discrepancy.stub(checkMapper, 7L);

        CpsRebateAssetMigrationCheckReport report = service.runCheck("admin-2");

        assertFalse(report.isReady());
        assertEquals(1L, discrepancy.value(report));
        ArgumentCaptor<CpsRebateAssetMigrationCheckArchiveDO> archive =
                ArgumentCaptor.forClass(CpsRebateAssetMigrationCheckArchiveDO.class);
        verify(archiveMapper).insert(archive.capture());
        assertFalse(archive.getValue().isReady());
    }

    @Test
    void archiveQueryIsRestrictedToCurrentTenant() {
        CpsRebateAssetMigrationCheckArchiveDO record =
                CpsRebateAssetMigrationCheckArchiveDO.builder().tenantId(7L).batchNo("batch-1").build();
        when(archiveMapper.selectByTenantId(7L)).thenReturn(List.of(record));

        assertEquals(List.of(record), service.getArchives());
        verify(archiveMapper).selectByTenantId(7L);
    }

    enum Discrepancy {
        DUPLICATE_ACCOUNT {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countDuplicateAccounts(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getDuplicateAccountCount(); }
        },
        DUPLICATE_ORDER {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countDuplicateOrders(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getDuplicateOrderCount(); }
        },
        DUPLICATE_REBATE {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countDuplicateRebateRecords(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getDuplicateRebateRecordCount(); }
        },
        DUPLICATE_LEDGER_IDEMPOTENCY {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countDuplicateLedgerIdempotencyKeys(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getDuplicateLedgerIdempotencyCount(); }
        },
        DUPLICATE_FREEZE_IDEMPOTENCY {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countDuplicateFreezeIdempotencyKeys(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getDuplicateFreezeIdempotencyCount(); }
        },
        ACCOUNT_LEDGER_MISMATCH {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countAccountLedgerNetMismatches(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getAccountLedgerMismatchCount(); }
        },
        FREEZE_ACCOUNT_MISMATCH {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countFreezeAccountMismatches(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getFreezeAccountMismatchCount(); }
        },
        MISSING_OPENING_BALANCE {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countMissingOpeningBalances(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getMissingOpeningBalanceCount(); }
        },
        ORPHAN_LEDGER {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countOrphanLedgerRecords(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getOrphanLedgerCount(); }
        },
        ORPHAN_ACTIVE_FREEZE {
            void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId) {
                when(mapper.countOrphanActiveFreezeRecords(tenantId)).thenReturn(1L);
            }
            long value(CpsRebateAssetMigrationCheckReport report) { return report.getOrphanActiveFreezeCount(); }
        };

        abstract void stub(CpsRebateAssetMigrationCheckMapper mapper, long tenantId);
        abstract long value(CpsRebateAssetMigrationCheckReport report);
    }
}
