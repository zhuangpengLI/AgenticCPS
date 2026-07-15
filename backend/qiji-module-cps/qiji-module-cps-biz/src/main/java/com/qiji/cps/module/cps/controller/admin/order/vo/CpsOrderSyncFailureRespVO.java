package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS 订单同步失败 Response VO")
@Data
public class CpsOrderSyncFailureRespVO {

    private Long id;
    private String platformCode;
    private String vendorCode;
    private Integer orderScene;
    private String queryType;
    private String paginationMode;
    private Integer pageNo;
    private String nextCursor;
    private String syncBatchNo;
    private String failureStage;
    private String requestSnapshot;
    private String rawSummary;
    private String failureReason;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime lastReplayTime;
    private Long replayOperatorId;
    private String replayAuditNote;
    private String idempotencyKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
