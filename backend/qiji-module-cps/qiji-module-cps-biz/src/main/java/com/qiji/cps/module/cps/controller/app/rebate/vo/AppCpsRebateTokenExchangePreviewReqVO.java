package com.qiji.cps.module.cps.controller.app.rebate.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppCpsRebateTokenExchangePreviewReqVO {

    @NotNull(message = "兑换金额不能为空")
    @DecimalMin(value = "0.01", message = "兑换金额必须大于0")
    private BigDecimal amount;
}
