package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetMigrationCheckArchiveDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 归档表只允许追加与按租户读取，不暴露 update/delete。 */
@Mapper
public interface CpsRebateAssetMigrationCheckArchiveMapper {

    @Insert("""
            INSERT INTO cps_rebate_asset_migration_check
            (batch_no, tenant_id, duplicate_account_count, duplicate_order_count,
             duplicate_rebate_record_count, duplicate_ledger_idempotency_count,
             duplicate_freeze_idempotency_count,
             account_ledger_mismatch_count, freeze_account_mismatch_count,
             missing_opening_balance_count, orphan_ledger_count, orphan_active_freeze_count,
             ready, operator_id, executed_at, summary)
            VALUES
            (#{batchNo}, #{tenantId}, #{duplicateAccountCount}, #{duplicateOrderCount},
             #{duplicateRebateRecordCount}, #{duplicateLedgerIdempotencyCount},
             #{duplicateFreezeIdempotencyCount},
             #{accountLedgerMismatchCount}, #{freezeAccountMismatchCount},
             #{missingOpeningBalanceCount}, #{orphanLedgerCount}, #{orphanActiveFreezeCount},
             #{ready}, #{operatorId}, #{executedAt}, #{summary})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CpsRebateAssetMigrationCheckArchiveDO archive);

    @Select("""
            SELECT id, batch_no, tenant_id, duplicate_account_count, duplicate_order_count,
                   duplicate_rebate_record_count, duplicate_ledger_idempotency_count,
                   duplicate_freeze_idempotency_count,
                   account_ledger_mismatch_count, freeze_account_mismatch_count,
                   missing_opening_balance_count, orphan_ledger_count, orphan_active_freeze_count,
                   ready, operator_id, executed_at, summary
            FROM cps_rebate_asset_migration_check
            WHERE tenant_id = #{tenantId}
            ORDER BY id DESC
            """)
    List<CpsRebateAssetMigrationCheckArchiveDO> selectByTenantId(@Param("tenantId") long tenantId);

    @Select("""
            SELECT id, batch_no, tenant_id, duplicate_account_count, duplicate_order_count,
                   duplicate_rebate_record_count, duplicate_ledger_idempotency_count,
                   duplicate_freeze_idempotency_count,
                   account_ledger_mismatch_count, freeze_account_mismatch_count,
                   missing_opening_balance_count, orphan_ledger_count, orphan_active_freeze_count,
                   ready, operator_id, executed_at, summary
            FROM cps_rebate_asset_migration_check
            WHERE tenant_id = #{tenantId}
            ORDER BY id DESC
            LIMIT 1
            """)
    CpsRebateAssetMigrationCheckArchiveDO selectLatestByTenantId(@Param("tenantId") long tenantId);
}
