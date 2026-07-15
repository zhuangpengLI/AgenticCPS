package com.qiji.cps.module.cps.dal.dataobject.rebate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 迁移预检不可变归档。该模型不提供 setter，配套 Mapper 也不暴露更新或删除方法。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsRebateAssetMigrationCheckArchiveDO {
    private Long id;
    private String batchNo;
    private Long tenantId;
    private Long duplicateAccountCount;
    private Long duplicateOrderCount;
    private Long duplicateRebateRecordCount;
    private Long duplicateLedgerIdempotencyCount;
    private Long duplicateFreezeIdempotencyCount;
    private Long accountLedgerMismatchCount;
    private Long freezeAccountMismatchCount;
    private Long missingOpeningBalanceCount;
    private Long orphanLedgerCount;
    private Long orphanActiveFreezeCount;
    private boolean ready;
    private String operatorId;
    private LocalDateTime executedAt;
    private String summary;
}
