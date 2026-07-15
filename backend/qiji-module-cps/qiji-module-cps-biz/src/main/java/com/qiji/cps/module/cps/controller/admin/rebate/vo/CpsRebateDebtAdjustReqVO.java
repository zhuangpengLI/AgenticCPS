package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CpsRebateDebtAdjustReqVO {
    @NotNull
    private Long memberId;
    @NotBlank
    @Pattern(regexp = "WAIVE|INCREASE")
    private String action;
    @NotNull
    @Min(1)
    private Long amountCent;
    @NotBlank
    private String reason;
    @NotBlank
    private String idempotencyKey;
}
