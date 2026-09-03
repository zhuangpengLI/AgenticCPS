package com.qiji.cps.module.cps.controller.admin.rebate.vo;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CpsRebateAssetPolicySaveReqVO {

    private Boolean v2Enabled;
    private Boolean readOnly;
    @Min(1)
    private Long largeDebtThresholdCent;
    @Min(1)
    private Integer reminderIntervalDays;
    @Min(1)
    private Integer normalReminderDays;
    @Min(1)
    private Integer largeReminderDays;
    @Min(1)
    private Integer smsIntervalDays;
}
