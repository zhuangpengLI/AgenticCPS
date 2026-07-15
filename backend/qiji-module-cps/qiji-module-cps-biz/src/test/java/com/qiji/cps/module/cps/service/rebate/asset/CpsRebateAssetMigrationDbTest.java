package com.qiji.cps.module.cps.service.rebate.asset;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAccountDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetPolicyDO;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAccountMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetLedgerMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateAssetPolicyMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.qiji.cps.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(CpsRebateAssetMigrationDbTest.TenantTestConfiguration.class)
class CpsRebateAssetMigrationDbTest extends BaseDbUnitTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantTestConfiguration {
        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor, tenantInterceptor, 0);
            return tenantInterceptor;
        }
    }

    @Resource private CpsRebateAccountMapper accountMapper;
    @Resource private CpsRebateAssetLedgerMapper ledgerMapper;
    @Resource private CpsRebateAssetPolicyMapper policyMapper;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void openingBalanceBackfillIsIdempotentAndTenantIsolated() {
        insertAccount(1L, 101L, "12.34", "5.00", "2.00");
        insertAccount(2L, 202L, "8.00", "0.00", "0.00");
        CpsRebateAssetPolicyService policyService = mock(CpsRebateAssetPolicyService.class);
        when(policyService.getPolicy()).thenReturn(CpsRebateAssetPolicyDO.builder().v2Enabled(false).build());
        CpsRebateAssetMigrationService service = new CpsRebateAssetMigrationService(
                accountMapper, ledgerMapper, new CpsMoneyConverter(), policyService);

        TenantContextHolder.setTenantId(1L);
        assertEquals(1, service.backfillOpeningBalances("admin-1"));
        assertEquals(0, service.backfillOpeningBalances("admin-1"));
        assertEquals(1L, countLedgers(1L));
        assertEquals(1L, ledgerMapper.selectList(new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eq(CpsRebateAssetLedgerDO::getTenantId, 1L)).getFirst().getTenantId());

        TenantContextHolder.setTenantId(2L);
        assertEquals(1, service.backfillOpeningBalances("admin-2"));
        assertEquals(1L, countLedgers(2L));
        assertEquals(2L, ledgerMapper.selectList(new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eq(CpsRebateAssetLedgerDO::getTenantId, 2L)).getFirst().getTenantId());
    }

    @Test
    void migrationApprovalBatchAndTimePersistPerTenant() {
        LocalDateTime readyAt = LocalDateTime.of(2026, 7, 13, 17, 0);
        TenantContextHolder.setTenantId(1L);
        CpsRebateAssetPolicyDO policy = CpsRebateAssetPolicyDO.builder()
                .v2Enabled(false).migrationReady(true).latestReadyCheckBatchNo("ready-batch-1")
                .readyCheckTime(readyAt).readOnly(false).largeDebtThresholdCent(10_000L)
                .reminderIntervalDays(7).normalReminderDays(30).largeReminderDays(180)
                .smsIntervalDays(30).build();
        policy.setTenantId(1L);
        policyMapper.insert(policy);

        CpsRebateAssetPolicyDO tenantOne = policyMapper.selectCurrentTenant();
        assertEquals("ready-batch-1", tenantOne.getLatestReadyCheckBatchNo());
        assertEquals(readyAt, tenantOne.getReadyCheckTime());

        TenantContextHolder.setTenantId(2L);
        assertEquals(null, policyMapper.selectCurrentTenant());
    }

    private void insertAccount(long tenantId, long memberId, String available, String frozen, String debt) {
        TenantContextHolder.setTenantId(tenantId);
        CpsRebateAccountDO account = CpsRebateAccountDO.builder()
                .memberId(memberId).totalRebate(BigDecimal.ZERO)
                .availableBalance(new BigDecimal(available)).frozenBalance(new BigDecimal(frozen))
                .debtBalance(new BigDecimal(debt)).withdrawnAmount(BigDecimal.ZERO)
                .status(1).version(0).build();
        account.setTenantId(tenantId);
        accountMapper.insert(account);
    }

    private long countLedgers(long tenantId) {
        return ledgerMapper.selectCount(new LambdaQueryWrapperX<CpsRebateAssetLedgerDO>()
                .eq(CpsRebateAssetLedgerDO::getTenantId, tenantId));
    }
}
