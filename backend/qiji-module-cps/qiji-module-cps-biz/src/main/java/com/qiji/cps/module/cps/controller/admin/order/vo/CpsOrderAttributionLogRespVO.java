package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS 订单归因日志 Response VO")
@Data
public class CpsOrderAttributionLogRespVO {

    private Long id;
    private Long orderId;
    private String platformCode;
    private String platformOrderId;
    private Long candidateMemberId;
    private Long attributedMemberId;
    private String attributionSource;
    private String bindingType;
    private String bindingId;
    private String action;
    private String result;
    private String rejectReason;
    private String operatorType;
    private String operatorId;
    private String idempotencyKey;
    private String reviewStatus;
    private String reviewAuditNote;
    private Long reviewOperatorId;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
