package com.qiji.cps.module.cps.controller.admin.refund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CpsRefundReportDetailRespVO {
    private Long id;
    private Long importId;
    private String platformCode;
    private String platformOrderId;
    private String refundType;
    private BigDecimal refundAmount;
    private LocalDateTime refundTime;
    private Long orderId;
    private String matchStatus;
    private String differenceReason;
    private Long assetLedgerId;
}
