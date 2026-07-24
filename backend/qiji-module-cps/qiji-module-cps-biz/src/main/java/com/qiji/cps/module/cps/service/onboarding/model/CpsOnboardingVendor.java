package com.qiji.cps.module.cps.service.onboarding.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

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

    @JsonIgnore
    @Builder.Default
    private List<String> configuredFields = List.of();

    private String defaultAdzoneId;

    private String extraConfig;

    private Integer priority;

    @ToString.Include
    private Integer status;

    private String remark;

}
