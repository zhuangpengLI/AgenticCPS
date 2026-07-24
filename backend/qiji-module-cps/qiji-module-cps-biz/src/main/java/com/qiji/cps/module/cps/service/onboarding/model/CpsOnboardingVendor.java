package com.qiji.cps.module.cps.service.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Vendor business configuration carried by an onboarding draft.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CpsOnboardingVendor {

    @ToString.Include
    private String vendorCode;

    @ToString.Include
    private String vendorName;

    @ToString.Include
    private String vendorType;

    @ToString.Include
    private String platformCode;

    private String appKey;

    private String appSecret;

    private String apiBaseUrl;

    private String authToken;

    @ToString.Include
    private Boolean appKeyConfigured;

    @ToString.Include
    private Boolean appSecretConfigured;

    @ToString.Include
    private Boolean authTokenConfigured;

    @ToString.Include
    private Boolean extraConfigConfigured;

    private String defaultAdzoneId;

    private String extraConfig;

    private Integer priority;

    @ToString.Include
    private Integer status;

    private String remark;

}
