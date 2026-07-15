package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS 平台账单差异 Response VO")
@Data
public class CpsPlatformBillDiffRespVO {

    private Long id;
    private Long billRowId;
    private Long orderId;
    private String platformCode;
    private String vendorCode;
    private String billBatchNo;
    private String platformOrderId;
    private String diffType;
    private String diffStatus;
    private String diffSummary;
    private BigDecimal orderCommissionAmount;
    private BigDecimal billCommissionAmount;
    private BigDecimal billRefundAmount;
    private String orderStatus;
    private String billStatus;
    private LocalDateTime orderSettleTime;
    private LocalDateTime billSettleTime;
    private String handleConclusion;
    private String handleAuditNote;
    private Long handleOperatorId;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
