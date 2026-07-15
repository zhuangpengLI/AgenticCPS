package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CpsRebateAssetPolicySaveReqVO {

    @NotNull
    private Boolean v2Enabled;
    @NotNull
    private Boolean readOnly;
    @NotNull
    @Min(1)
    private Long largeDebtThresholdCent;
    @NotNull
    @Min(1)
    private Integer reminderIntervalDays;
    @NotNull
    @Min(1)
    private Integer normalReminderDays;
    @NotNull
    @Min(1)
    private Integer largeReminderDays;
    @NotNull
    @Min(1)
    private Integer smsIntervalDays;
}
