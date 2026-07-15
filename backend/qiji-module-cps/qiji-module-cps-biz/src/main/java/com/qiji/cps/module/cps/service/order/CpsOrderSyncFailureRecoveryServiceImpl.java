package com.qiji.cps.module.cps.service.order;

import cn.hutool.core.util.StrUtil;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderSyncFailureDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderSyncFailureMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class CpsOrderSyncFailureRecoveryServiceImpl implements CpsOrderSyncFailureRecoveryService {

    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_RETRYING = "RETRYING";
    static final String STATUS_DEAD = "DEAD";
    private static final int DEFAULT_MAX_RETRY = 3;

    @Resource
    private CpsOrderSyncFailureMapper failureMapper;

    @Override
    public void recordFailure(CpsOrderSyncFailureRecordCommand command) {
        if (command == null || StrUtil.isBlank(command.idempotencyKey())) {
            return;
        }
        if (failureMapper.selectByIdempotencyKey(command.idempotencyKey()) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CpsOrderSyncFailureDO failure = CpsOrderSyncFailureDO.builder()
                .platformCode(truncate(command.platformCode(), 32))
                .vendorCode(truncate(command.vendorCode(), 64))
                .orderScene(command.orderScene())
                .queryType(truncate(command.queryType(), 16))
                .paginationMode(truncate(command.paginationMode(), 16))
                .pageNo(command.pageNo())
                .nextCursor(truncate(command.nextCursor(), 255))
                .syncBatchNo(truncate(command.syncBatchNo(), 255))
                .failureStage(truncate(command.failureStage(), 32))
                .requestSnapshot(truncate(mask(command.requestSnapshot()), 1000))
                .rawSummary(truncate(mask(command.rawSummary()), 2000))
                .failureReason(truncate(command.failureReason(), 1000))
                .status(STATUS_PENDING)
                .retryCount(0)
                .maxRetryCount(DEFAULT_MAX_RETRY)
                .nextRetryTime(now.plusSeconds(30))
                .idempotencyKey(truncate(command.idempotencyKey(), 128))
                .version(0)
                .build();
        failureMapper.insert(failure);
    }

    @Override
    public void replayFailure(Long id, Long operatorId, String auditNote) {
        CpsOrderSyncFailureDO failure = requireFailure(id);
        int nextRetryCount = value(failure.getRetryCount()) + 1;
        boolean exhausted = nextRetryCount >= maxRetry(failure);
        CpsOrderSyncFailureDO update = CpsOrderSyncFailureDO.builder()
                .id(id)
                .status(exhausted ? STATUS_DEAD : STATUS_RETRYING)
                .retryCount(nextRetryCount)
                .nextRetryTime(exhausted ? null : LocalDateTime.now())
                .lastReplayTime(LocalDateTime.now())
                .replayOperatorId(operatorId)
                .replayAuditNote(truncate(auditNote, 500))
                .version(failure.getVersion())
                .build();
        failureMapper.updateById(update);
    }

    @Override
    public void scheduleNextRetry(Long id, String failureReason) {
        CpsOrderSyncFailureDO failure = requireFailure(id);
        int nextRetryCount = value(failure.getRetryCount()) + 1;
        boolean exhausted = nextRetryCount >= maxRetry(failure);
        CpsOrderSyncFailureDO update = CpsOrderSyncFailureDO.builder()
                .id(id)
                .status(exhausted ? STATUS_DEAD : STATUS_RETRYING)
                .retryCount(nextRetryCount)
                .failureReason(truncate(failureReason, 1000))
                .nextRetryTime(exhausted ? null : LocalDateTime.now().plusSeconds(backoffSeconds(nextRetryCount)))
                .version(failure.getVersion())
                .build();
        failureMapper.updateById(update);
    }

    @Override
    public int compensateDueFailures(Integer limit) {
        List<CpsOrderSyncFailureDO> failures = failureMapper.selectDueRetryFailures(LocalDateTime.now(), limit);
        for (CpsOrderSyncFailureDO failure : failures) {
            scheduleNextRetry(failure.getId(), failure.getFailureReason());
        }
        return failures.size();
    }

    private CpsOrderSyncFailureDO requireFailure(Long id) {
        CpsOrderSyncFailureDO failure = failureMapper.selectById(id);
        if (failure == null) {
            throw new IllegalArgumentException("order sync failure not found: " + id);
        }
        return failure;
    }

    private int maxRetry(CpsOrderSyncFailureDO failure) {
        return failure.getMaxRetryCount() == null || failure.getMaxRetryCount() <= 0
                ? DEFAULT_MAX_RETRY : failure.getMaxRetryCount();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private long backoffSeconds(int retryCount) {
        return Math.min(3600L, 30L << Math.min(Math.max(retryCount - 1, 0), 7));
    }

    private String mask(String text) {
        if (text == null) {
            return null;
        }
        String masked = text.replaceAll("(?<!\\d)1[3-9]\\d{9}(?!\\d)", "***");
        masked = masked.replaceAll("(?i)(accessToken|token|secret|appSecret)(\\\"?\\s*[:=]\\s*\\\"?)[^,\\\"\\s}]+",
                "$1$2***");
        return masked;
    }

    private String truncate(String value, int maxLength) {
        return StrUtil.isBlank(value) ? value : StrUtil.subWithLength(value, 0, maxLength);
    }
}
