package com.qiji.cps.module.cps.service.order;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CpsPlatformBillImportRowCommand(
        String platformCode,
        String vendorCode,
        String billBatchNo,
        String platformOrderId,
        String parentOrderId,
        String billStatus,
        BigDecimal commissionAmount,
        BigDecimal refundAmount,
        LocalDateTime orderTime,
        LocalDateTime settleTime,
        LocalDateTime refundTime,
        String sourceFileName,
        String rawSummary
) {
}
