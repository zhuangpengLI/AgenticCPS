package com.qiji.cps.module.cps.controller.admin.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CPS 订单同步失败人工重放 Request VO")
@Data
public class CpsOrderSyncFailureReplayReqVO {

    @NotNull(message = "失败记录编号不能为空")
    private Long id;

    @Schema(description = "操作人编号；自动化调用无法获得登录上下文时由调用方显式传入")
    private Long operatorId;

    @Schema(description = "审计说明")
    private String auditNote;
}
