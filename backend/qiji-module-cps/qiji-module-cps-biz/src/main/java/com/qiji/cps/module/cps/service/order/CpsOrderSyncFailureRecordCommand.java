package com.qiji.cps.module.cps.service.order;

import lombok.Builder;

@Builder
public record CpsOrderSyncFailureRecordCommand(
        String platformCode,
        String vendorCode,
        Integer orderScene,
        String queryType,
        String paginationMode,
        Integer pageNo,
        String nextCursor,
        String syncBatchNo,
        String failureStage,
        String requestSnapshot,
        String rawSummary,
        String failureReason,
        String idempotencyKey
) {
}
