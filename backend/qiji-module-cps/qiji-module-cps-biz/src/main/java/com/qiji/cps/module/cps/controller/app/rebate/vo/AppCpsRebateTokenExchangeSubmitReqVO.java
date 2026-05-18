package com.qiji.cps.module.cps.controller.app.rebate.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppCpsRebateTokenExchangeSubmitReqVO {

    @NotNull(message = "兑换金额不能为空")
    @DecimalMin(value = "0.01", message = "兑换金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
