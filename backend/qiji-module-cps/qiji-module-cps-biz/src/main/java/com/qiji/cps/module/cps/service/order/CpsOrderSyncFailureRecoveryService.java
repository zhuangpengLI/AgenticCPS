package com.qiji.cps.module.cps.service.order;

public interface CpsOrderSyncFailureRecoveryService {

    void recordFailure(CpsOrderSyncFailureRecordCommand command);

    void replayFailure(Long id, Long operatorId, String auditNote);

    void scheduleNextRetry(Long id, String failureReason);

    int compensateDueFailures(Integer limit);
}
