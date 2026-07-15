package com.qiji.cps.module.cps.controller.app.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 我的返利欠款汇总 Response VO")
@Data
public class AppCpsRebateDebtSummaryRespVO {

    @Schema(description = "欠款记录数")
    private Long debtCount;

    @Schema(description = "原始欠款金额，单位：分")
    private Long originalDebtCent;

    @Schema(description = "已偿还金额，单位：分")
    private Long repaidDebtCent;

    @Schema(description = "已减免金额，单位：分")
    private Long waivedDebtCent;

    @Schema(description = "未偿还金额，单位：分")
    private Long outstandingDebtCent;
}
