package com.qiji.cps.module.cps.dal.mysql.rebate;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.qiji.cps.framework.mybatis.core.util.MyBatisUtils;
import com.qiji.cps.framework.tenant.config.TenantProperties;
import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import com.qiji.cps.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.qiji.cps.framework.test.core.ut.BaseDbUnitTest;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateAssetLedgerDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(CpsRebateAssetLedgerConcurrencyTest.TenantTestConfiguration.class)
class CpsRebateAssetLedgerConcurrencyTest extends BaseDbUnitTest {

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

    @Resource
    private CpsRebateAssetLedgerMapper ledgerMapper;

    @Test
    void twentyConcurrentRequestsAppendExactlyOneLedgerForSameIdempotencyKey() throws Exception {
        int concurrency = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int index = 0; index < concurrency; index++) {
            tasks.add(() -> {
                TenantContextHolder.setTenantId(1L);
                try {
                    ready.countDown();
                    start.await();
                    ledgerMapper.insert(newLedger());
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                } finally {
                    TenantContextHolder.clear();
                }
            });
        }
        List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();
        ready.await();
        start.countDown();
        int success = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) success++;
        }
        executor.shutdownNow();

        TenantContextHolder.setTenantId(1L);
        try {
            assertEquals(1, success);
            assertEquals(1L, ledgerMapper.selectCount(null));
        } finally {
            TenantContextHolder.clear();
        }
    }

    private CpsRebateAssetLedgerDO newLedger() {
        return CpsRebateAssetLedgerDO.builder()
                .memberId(99L).sourceSystem("CPS").businessType("ORDER_REBATE")
                .businessId("order-99").idempotencyKey("same-idempotency-key")
                .availableChangeCent(0L).frozenChangeCent(100L).debtChangeCent(0L)
                .availableBeforeCent(0L).availableAfterCent(0L)
                .frozenBeforeCent(0L).frozenAfterCent(100L)
                .debtBeforeCent(0L).debtAfterCent(0L)
                .operatorType("SYSTEM").operatorId("job").reason("并发幂等验证")
                .build();
    }
}
