package com.qiji.cps.module.cps.service.onboarding.model;

import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed business configuration held by a platform onboarding draft.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformOnboardingPayload {

    private CpsPlatformSaveReqVO platform;

    private String primaryVendorCode;

    private String runtimeDefaultAdzoneId;

    @Builder.Default
    private List<CpsOnboardingVendor> vendors = new ArrayList<>();

    @Builder.Default
    private List<CpsOnboardingAdzone> adzones = new ArrayList<>();

    @Builder.Default
    private List<CpsOnboardingRebateRule> rebateRules = new ArrayList<>();

}
