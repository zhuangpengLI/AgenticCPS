package com.qiji.cps.module.cps.controller.app.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - 我的欠款偿还流水 Response VO")
@Data
public class AppCpsRebateDebtRepaymentRespVO {

    @Schema(description = "流水编号")
    private Long id;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "来源业务单号")
    private String businessId;

    @Schema(description = "来源订单编号")
    private Long orderId;

    @Schema(description = "欠款变化金额，负数表示偿还，单位：分")
    private Long debtChangeCent;

    @Schema(description = "变化前欠款余额，单位：分")
    private Long debtBeforeCent;

    @Schema(description = "变化后欠款余额，单位：分")
    private Long debtAfterCent;

    @Schema(description = "变更原因")
    private String reason;

    @Schema(description = "偿还时间")
    private LocalDateTime createTime;
}
