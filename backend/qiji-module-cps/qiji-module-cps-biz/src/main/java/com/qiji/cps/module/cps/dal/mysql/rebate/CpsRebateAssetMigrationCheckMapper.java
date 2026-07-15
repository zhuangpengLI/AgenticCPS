package com.qiji.cps.module.cps.dal.mysql.rebate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 迁移预检只读查询；所有 SQL 显式限定 tenant_id。 */
@Mapper
public interface CpsRebateAssetMigrationCheckMapper {

    @Select("""
            SELECT COUNT(*) FROM (
              SELECT member_id FROM cps_rebate_account
              WHERE tenant_id = #{tenantId} AND deleted = 0
              GROUP BY member_id HAVING COUNT(*) > 1
            ) duplicate_accounts
            """)
    long countDuplicateAccounts(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*) FROM (
              SELECT platform_code, platform_order_id FROM cps_order
              WHERE tenant_id = #{tenantId} AND deleted = 0
              GROUP BY platform_code, platform_order_id HAVING COUNT(*) > 1
            ) duplicate_orders
            """)
    long countDuplicateOrders(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*) FROM (
              SELECT order_id, rebate_type FROM cps_rebate_record
              WHERE tenant_id = #{tenantId} AND deleted = 0
              GROUP BY order_id, rebate_type HAVING COUNT(*) > 1
            ) duplicate_rebates
            """)
    long countDuplicateRebateRecords(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*) FROM (
              SELECT business_type, idempotency_key FROM cps_rebate_asset_ledger
              WHERE tenant_id = #{tenantId} AND deleted = 0
              GROUP BY business_type, idempotency_key HAVING COUNT(*) > 1
            ) duplicate_ledgers
            """)
    long countDuplicateLedgerIdempotencyKeys(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*) FROM (
              SELECT business_type, idempotency_key FROM cps_freeze_record
              WHERE tenant_id = #{tenantId} AND deleted = 0
                AND business_type IS NOT NULL AND idempotency_key IS NOT NULL
              GROUP BY business_type, idempotency_key HAVING COUNT(*) > 1
            ) duplicate_freezes
            """)
    long countDuplicateFreezeIdempotencyKeys(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM cps_rebate_account account
            LEFT JOIN (
              SELECT member_id,
                     SUM(available_change_cent + frozen_change_cent - debt_change_cent) net_cent
              FROM cps_rebate_asset_ledger
              WHERE tenant_id = #{tenantId} AND deleted = 0
              GROUP BY member_id
            ) ledger ON ledger.member_id = account.member_id
            WHERE account.tenant_id = #{tenantId} AND account.deleted = 0
              AND ROUND((COALESCE(account.available_balance, 0)
                         + COALESCE(account.frozen_balance, 0)
                         - COALESCE(account.debt_balance, 0)) * 100)
                  <> COALESCE(ledger.net_cent, 0)
            """)
    long countAccountLedgerNetMismatches(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM cps_rebate_account account
            LEFT JOIN (
              SELECT member_id, SUM(COALESCE(amount_cent, ROUND(freeze_amount * 100))) frozen_cent
              FROM cps_freeze_record
              WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = 'frozen'
              GROUP BY member_id
            ) freeze_summary ON freeze_summary.member_id = account.member_id
            WHERE account.tenant_id = #{tenantId} AND account.deleted = 0
              AND ROUND(COALESCE(account.frozen_balance, 0) * 100)
                  <> COALESCE(freeze_summary.frozen_cent, 0)
            """)
    long countFreezeAccountMismatches(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM cps_rebate_account account
            WHERE account.tenant_id = #{tenantId} AND account.deleted = 0
              AND NOT EXISTS (
                SELECT 1 FROM cps_rebate_asset_ledger ledger
                WHERE ledger.tenant_id = account.tenant_id AND ledger.deleted = 0
                  AND ledger.business_type = 'OPENING_BALANCE'
                  AND ledger.business_id = CAST(account.id AS CHAR)
              )
            """)
    long countMissingOpeningBalances(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM cps_rebate_asset_ledger ledger
            LEFT JOIN cps_rebate_account account
              ON account.tenant_id = ledger.tenant_id
             AND account.member_id = ledger.member_id
             AND account.deleted = 0
            WHERE ledger.tenant_id = #{tenantId} AND ledger.deleted = 0
              AND account.id IS NULL
            """)
    long countOrphanLedgerRecords(@Param("tenantId") long tenantId);

    @Select("""
            SELECT COUNT(*)
            FROM cps_freeze_record freeze_record
            LEFT JOIN cps_rebate_account account
              ON account.tenant_id = freeze_record.tenant_id
             AND account.member_id = freeze_record.member_id
             AND account.deleted = 0
            WHERE freeze_record.tenant_id = #{tenantId}
              AND freeze_record.deleted = 0
              AND freeze_record.status = 'frozen'
              AND account.id IS NULL
            """)
    long countOrphanActiveFreezeRecords(@Param("tenantId") long tenantId);
}
