package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - CPS 资金追溯 Request VO")
@Data
public class CpsFundsTraceReqVO {

    @Schema(description = "订单ID", example = "10001")
    private Long orderId;

    @Schema(description = "平台编码", example = "taobao")
    private String platformCode;

    @Schema(description = "平台订单号", example = "TB_20260714001")
    private String platformOrderId;

    @Schema(description = "业务单号", example = "biz-10001")
    private String businessId;

    @Schema(description = "幂等键", example = "idem-10001")
    private String idempotencyKey;
}
