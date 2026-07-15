package com.qiji.cps.module.cps.service.goods;

import com.alibaba.ttl.TtlCallable;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 商品跨平台聚合的有界执行器。
 *
 * <p>所有任务先提交、再按提交顺序收集结果，既允许平台调用并发执行，
 * 又保证聚合结果与任务提交顺序一致。批次使用统一总预算，排队与执行时间均计入预算，避免多个慢平台
 * 将总耗时放大为 {@code 平台数 * 超时时间}。</p>
 */
@Component
public class CpsGoodsAggregationExecutor implements AutoCloseable {

    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

    private final ThreadPoolExecutor executor;
    private final long batchTimeoutNanos;

    public CpsGoodsAggregationExecutor(
            @Value("${qiji.cps.goods.aggregation.parallelism:4}") int parallelism,
            @Value("${qiji.cps.goods.aggregation.queue-capacity:16}") int queueCapacity,
            @Value("${qiji.cps.goods.aggregation.batch-timeout:${qiji.cps.goods.aggregation.platform-timeout:3s}}")
            Duration batchTimeout) {
        int boundedParallelism = Math.max(1, parallelism);
        int boundedQueueCapacity = Math.max(1, queueCapacity);
        Duration effectiveTimeout = batchTimeout == null || batchTimeout.isZero() || batchTimeout.isNegative()
                ? Duration.ofSeconds(3) : batchTimeout;
        this.batchTimeoutNanos = effectiveTimeout.toNanos();
        this.executor = new ThreadPoolExecutor(
                boundedParallelism,
                boundedParallelism,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(boundedQueueCapacity),
                aggregationThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    <T> List<TaskResult<T>> invokeAll(List<? extends Callable<T>> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        long deadlineNanos = System.nanoTime() + batchTimeoutNanos;
        List<SubmittedTask<T>> submittedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            try {
                Callable<TimedTaskOutcome<T>> timedTask = () -> {
                    try {
                        return TimedTaskOutcome.succeeded(task.call(), System.nanoTime());
                    } catch (Exception ex) {
                        return TimedTaskOutcome.failed(ex, System.nanoTime());
                    }
                };
                Future<TimedTaskOutcome<T>> future = executor.submit(TtlCallable.get(timedTask));
                submittedTasks.add(new SubmittedTask<>(future, null));
            } catch (RejectedExecutionException ex) {
                submittedTasks.add(new SubmittedTask<>(null, ex));
            }
        }

        List<TaskResult<T>> results = new ArrayList<>(tasks.size());
        for (int index = 0; index < submittedTasks.size(); index++) {
            SubmittedTask<T> submittedTask = submittedTasks.get(index);
            if (submittedTask.submissionFailure() != null) {
                results.add(TaskResult.failed(submittedTask.submissionFailure()));
                continue;
            }
            Future<TimedTaskOutcome<T>> future = submittedTask.future();
            try {
                TimedTaskOutcome<T> outcome;
                if (future.isDone()) {
                    outcome = future.get();
                } else {
                    long remainingNanos = deadlineNanos - System.nanoTime();
                    if (remainingNanos <= 0) {
                        future.cancel(true);
                        results.add(TaskResult.timeout());
                        continue;
                    }
                    outcome = future.get(remainingNanos, TimeUnit.NANOSECONDS);
                }
                if (!completedWithinDeadline(outcome.completedAtNanos(), deadlineNanos)) {
                    results.add(TaskResult.timeout());
                } else if (outcome.error() != null) {
                    results.add(TaskResult.failed(outcome.error()));
                } else {
                    results.add(TaskResult.succeeded(outcome.value()));
                }
            } catch (TimeoutException ex) {
                future.cancel(true);
                results.add(TaskResult.timeout());
            } catch (ExecutionException ex) {
                results.add(TaskResult.failed(ex.getCause() != null ? ex.getCause() : ex));
            } catch (InterruptedException ex) {
                cancelRemaining(submittedTasks, index);
                Thread.currentThread().interrupt();
                results.add(TaskResult.failed(ex));
                for (int remaining = index + 1; remaining < submittedTasks.size(); remaining++) {
                    results.add(TaskResult.failed(ex));
                }
                break;
            }
        }
        return results;
    }

    static boolean completedWithinDeadline(long completedAtNanos, long deadlineNanos) {
        return completedAtNanos <= deadlineNanos;
    }

    private void cancelRemaining(List<? extends SubmittedTask<?>> submittedTasks, int startIndex) {
        for (int index = startIndex; index < submittedTasks.size(); index++) {
            Future<?> future = submittedTasks.get(index).future();
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    private ThreadFactory aggregationThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "cps-goods-aggregation-" + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    @Override
    public void close() {
        executor.shutdownNow();
    }

    record TaskResult<T>(T value, Throwable error, boolean timedOut) {

        static <T> TaskResult<T> succeeded(T value) {
            return new TaskResult<>(value, null, false);
        }

        static <T> TaskResult<T> failed(Throwable error) {
            return new TaskResult<>(null, error, false);
        }

        static <T> TaskResult<T> timeout() {
            return new TaskResult<>(null, null, true);
        }
    }

    private record SubmittedTask<T>(Future<TimedTaskOutcome<T>> future, Throwable submissionFailure) {
    }

    private record TimedTaskOutcome<T>(T value, Throwable error, long completedAtNanos) {

        static <T> TimedTaskOutcome<T> succeeded(T value, long completedAtNanos) {
            return new TimedTaskOutcome<>(value, null, completedAtNanos);
        }

        static <T> TimedTaskOutcome<T> failed(Throwable error, long completedAtNanos) {
            return new TimedTaskOutcome<>(null, error, completedAtNanos);
        }
    }
}
