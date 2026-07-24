package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CpsPlatformOnboardingValidateReqVO {

    @NotNull
    @Valid
    private CpsPlatformOnboardingPayload payload;
}
