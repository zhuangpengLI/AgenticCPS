package com.qiji.cps.module.cps.service.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adzone business configuration carried by an onboarding draft.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOnboardingAdzone {

    private String platformCode;

    private String adzoneId;

    private String adzoneName;

    private String adzoneType;

    private String relationType;

    private Long relationId;

    private String externalRelationId;

    private String externalSpecialId;

    private Integer isDefault;

    private Integer status;

}
