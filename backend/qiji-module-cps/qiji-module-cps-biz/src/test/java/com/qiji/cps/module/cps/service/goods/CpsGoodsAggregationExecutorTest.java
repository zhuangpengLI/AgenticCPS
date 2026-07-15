package com.qiji.cps.module.cps.service.goods;

import com.qiji.cps.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpsGoodsAggregationExecutorTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void completedWithinDeadline_rejectsResultCompletedAfterBatchTimeout() {
        assertTrue(CpsGoodsAggregationExecutor.completedWithinDeadline(100L, 100L));
        assertFalse(CpsGoodsAggregationExecutor.completedWithinDeadline(101L, 100L));
    }

    @Test
    void invokeAll_propagatesTenantContextToWorker() {
        TenantContextHolder.setTenantId(42L);

        try (CpsGoodsAggregationExecutor executor =
                     new CpsGoodsAggregationExecutor(1, 1, Duration.ofSeconds(1))) {
            List<CpsGoodsAggregationExecutor.TaskResult<Long>> results =
                    executor.invokeAll(List.of(TenantContextHolder::getRequiredTenantId));

            assertEquals(42L, results.getFirst().value());
        }
    }

    @Test
    void invokeAll_isolatesQueueRejectionWithinBatchBudget() {
        try (CpsGoodsAggregationExecutor executor =
                     new CpsGoodsAggregationExecutor(1, 1, Duration.ofMillis(100))) {
            List<CpsGoodsAggregationExecutor.TaskResult<String>> results = executor.invokeAll(List.of(
                    () -> {
                        Thread.sleep(1_000);
                        return "slow";
                    },
                    () -> "queued",
                    () -> "rejected"));

            assertEquals(3, results.size());
            assertTrue(results.get(0).timedOut());
            assertTrue(results.get(1).timedOut());
            assertInstanceOf(RejectedExecutionException.class, results.get(2).error());
        }
    }
}
