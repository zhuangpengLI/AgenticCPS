package com.qiji.cps.module.cps.controller.app.withdraw.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppCpsWithdrawCreateReqVO {

    @Min(value = 1, message = "提现金额必须大于0")
    @Max(value = Integer.MAX_VALUE, message = "提现金额超出单笔上限")
    @NotNull(message = "提现金额不能为空")
    private Long amountCent;

    @NotBlank(message = "提现类型不能为空")
    @Pattern(regexp = "alipay|wechat", message = "提现类型仅支持 alipay 或 wechat")
    private String withdrawType;

    @NotBlank(message = "提现账户不能为空")
    @Size(max = 128)
    private String withdrawAccount;

    @Size(max = 64)
    private String withdrawAccountName;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128)
    private String idempotencyKey;
}
