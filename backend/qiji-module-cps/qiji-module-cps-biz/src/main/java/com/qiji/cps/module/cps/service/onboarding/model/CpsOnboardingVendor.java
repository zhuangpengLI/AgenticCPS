package com.qiji.cps.module.cps.service.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vendor business configuration carried by an onboarding draft.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsOnboardingVendor {

    private String vendorCode;

    private String vendorName;

    private String vendorType;

    private String platformCode;

    private String appKey;

    private String appSecret;

    private String apiBaseUrl;

    private String authToken;

    private String defaultAdzoneId;

    private String extraConfig;

    private Integer priority;

    private Integer status;

    private String remark;

}
