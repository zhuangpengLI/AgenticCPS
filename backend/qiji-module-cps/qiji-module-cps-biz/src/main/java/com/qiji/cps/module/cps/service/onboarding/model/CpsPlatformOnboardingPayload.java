package com.qiji.cps.module.cps.service.onboarding.model;

import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed business configuration held by a platform onboarding draft.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CpsPlatformOnboardingPayload {

    private CpsPlatformSaveReqVO platform;

    @ToString.Include
    private String primaryVendorCode;

    @ToString.Include
    private String runtimeDefaultAdzoneId;

    @Builder.Default
    private List<CpsOnboardingVendor> vendors = new ArrayList<>();

    @Builder.Default
    private List<CpsOnboardingAdzone> adzones = new ArrayList<>();

    @Builder.Default
    private List<CpsOnboardingRebateRule> rebateRules = new ArrayList<>();

    @ToString.Include(name = "platformCode")
    private String platformCodeForToString() {
        return platform == null ? null : platform.getPlatformCode();
    }

}
