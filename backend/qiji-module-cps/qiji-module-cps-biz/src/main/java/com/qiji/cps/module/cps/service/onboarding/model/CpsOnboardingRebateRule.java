package com.qiji.cps.module.cps.service.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Rebate-rule business configuration carried by an onboarding draft.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOnboardingRebateRule {

    private Long memberId;

    private Long memberLevelId;

    private String platformCode;

    private BigDecimal rebateRate;

    private BigDecimal minRebateAmount;

    private BigDecimal maxRebateAmount;

    private BigDecimal freezeThresholdAmount;

    private Integer freezeDays;

    private Integer status;

    private Integer priority;

}
