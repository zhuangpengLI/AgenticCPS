package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CpsPlatformOnboardingVendorTestReqVO {

    @NotBlank
    private String platformCode;

    @NotNull
    private Long draftVersion;

    @NotBlank
    private String vendorCode;
}
