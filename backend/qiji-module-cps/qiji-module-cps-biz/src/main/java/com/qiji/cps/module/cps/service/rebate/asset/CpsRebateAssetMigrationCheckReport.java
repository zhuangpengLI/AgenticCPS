package com.qiji.cps.module.cps.service.rebate.asset;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** 当前租户返利资产迁移预检的结构化结果。 */
@Value
@Builder
public class CpsRebateAssetMigrationCheckReport {
    String batchNo;
    Long tenantId;
    long duplicateAccountCount;
    long duplicateOrderCount;
    long duplicateRebateRecordCount;
    long duplicateLedgerIdempotencyCount;
    long duplicateFreezeIdempotencyCount;
    long accountLedgerMismatchCount;
    long freezeAccountMismatchCount;
    long missingOpeningBalanceCount;
    long orphanLedgerCount;
    long orphanActiveFreezeCount;
    boolean ready;
    String operatorId;
    LocalDateTime executedAt;
    String summary;
}
