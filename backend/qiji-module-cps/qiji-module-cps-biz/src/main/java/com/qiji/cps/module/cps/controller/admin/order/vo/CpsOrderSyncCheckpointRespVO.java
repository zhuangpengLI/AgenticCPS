package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CPS 订单同步检查点 Response VO")
@Data
public class CpsOrderSyncCheckpointRespVO {

    private Long id;
    private String platformCode;
    private String vendorCode;
    private Integer orderScene;
    private String queryType;
    private String paginationMode;
    private String nextCursor;
    private Integer nextPageNo;
    private LocalDateTime watermarkTime;
    private String lastSyncStatus;
    private Integer lastSuccessCount;
    private Integer lastFailureCount;
    private String failureSummary;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
