package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPublishReqVO;
import jakarta.validation.Valid;

public interface CpsPlatformOnboardingService {

    CpsPlatformOnboardingDetailRespVO publish(
            @Valid CpsPlatformOnboardingPublishReqVO request);

}
