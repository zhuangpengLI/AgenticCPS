package com.qiji.cps.module.cps.service.rebate.asset;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckArchiveMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetMigrationCheckMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CpsRebateAssetMigrationCheckServiceImpl implements CpsRebateAssetMigrationCheckService {

    private final CpsRebateAssetMigrationCheckMapper checkMapper;
    private final CpsRebateAssetMigrationCheckArchiveMapper archiveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRebateAssetMigrationCheckReport runCheck(String operatorId) {
        long tenantId = TenantContextHolder.getRequiredTenantId();
        long duplicateAccounts = checkMapper.countDuplicateAccounts(tenantId);
        long duplicateOrders = checkMapper.countDuplicateOrders(tenantId);
        long duplicateRebates = checkMapper.countDuplicateRebateRecords(tenantId);
        long duplicateLedgers = checkMapper.countDuplicateLedgerIdempotencyKeys(tenantId);
        long duplicateFreezes = checkMapper.countDuplicateFreezeIdempotencyKeys(tenantId);
        long accountLedgerMismatches = checkMapper.countAccountLedgerNetMismatches(tenantId);
        long freezeAccountMismatches = checkMapper.countFreezeAccountMismatches(tenantId);
        long missingOpeningBalances = checkMapper.countMissingOpeningBalances(tenantId);
        long orphanLedgers = checkMapper.countOrphanLedgerRecords(tenantId);
        long orphanActiveFreezes = checkMapper.countOrphanActiveFreezeRecords(tenantId);
        boolean ready = duplicateAccounts + duplicateOrders + duplicateRebates + duplicateLedgers + duplicateFreezes
                + accountLedgerMismatches + freezeAccountMismatches + missingOpeningBalances
                + orphanLedgers + orphanActiveFreezes == 0;
        LocalDateTime executedAt = LocalDateTime.now();
        String batchNo = UUID.randomUUID().toString();
        String summary = ready ? "READY" : "BLOCKED: migration discrepancies must be reviewed";
        CpsRebateAssetMigrationCheckReport report = CpsRebateAssetMigrationCheckReport.builder()
                .batchNo(batchNo).tenantId(tenantId)
                .duplicateAccountCount(duplicateAccounts).duplicateOrderCount(duplicateOrders)
                .duplicateRebateRecordCount(duplicateRebates)
                .duplicateLedgerIdempotencyCount(duplicateLedgers)
                .duplicateFreezeIdempotencyCount(duplicateFreezes)
                .accountLedgerMismatchCount(accountLedgerMismatches)
                .freezeAccountMismatchCount(freezeAccountMismatches)
                .missingOpeningBalanceCount(missingOpeningBalances)
                .orphanLedgerCount(orphanLedgers)
                .orphanActiveFreezeCount(orphanActiveFreezes)
                .ready(ready).operatorId(operatorId).executedAt(executedAt).summary(summary)
                .build();
        archiveMapper.insert(toArchive(report));
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CpsRebateAssetMigrationCheckArchiveDO> getArchives() {
        return archiveMapper.selectByTenantId(TenantContextHolder.getRequiredTenantId());
    }

    private CpsRebateAssetMigrationCheckArchiveDO toArchive(CpsRebateAssetMigrationCheckReport report) {
        return CpsRebateAssetMigrationCheckArchiveDO.builder()
                .batchNo(report.getBatchNo()).tenantId(report.getTenantId())
                .duplicateAccountCount(report.getDuplicateAccountCount())
                .duplicateOrderCount(report.getDuplicateOrderCount())
                .duplicateRebateRecordCount(report.getDuplicateRebateRecordCount())
                .duplicateLedgerIdempotencyCount(report.getDuplicateLedgerIdempotencyCount())
                .duplicateFreezeIdempotencyCount(report.getDuplicateFreezeIdempotencyCount())
                .accountLedgerMismatchCount(report.getAccountLedgerMismatchCount())
                .freezeAccountMismatchCount(report.getFreezeAccountMismatchCount())
                .missingOpeningBalanceCount(report.getMissingOpeningBalanceCount())
                .orphanLedgerCount(report.getOrphanLedgerCount())
                .orphanActiveFreezeCount(report.getOrphanActiveFreezeCount())
                .ready(report.isReady()).operatorId(report.getOperatorId())
                .executedAt(report.getExecutedAt()).summary(report.getSummary())
                .build();
    }
}
