package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDraftSaveReqVO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

public interface CpsPlatformOnboardingDraftService {

    CpsPlatformOnboardingDetailRespVO getDetail(String platformCode);

    CpsPlatformOnboardingDetailRespVO saveDraft(
            @Valid CpsPlatformOnboardingDraftSaveReqVO request);

    void deleteDraft(String platformCode);

    CpsPlatformOnboardingPayload getRequiredPayload(String platformCode);

    void markValidating(Long draftId, Long expectedVersion);

    void markChecked(Long draftId, Long expectedVersion,
                     String status, String validatedFingerprint,
                     String checkSummary, LocalDateTime validatedAt);

}
