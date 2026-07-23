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

    void deleteDraft(String platformCode, Long expectedVersion);

    CpsPlatformOnboardingPayload getRequiredPayload(String platformCode);

    DraftSnapshot getRequiredSnapshot(String platformCode, Long expectedVersion);

    void markValidating(Long draftId, Long expectedVersion);

    void markChecked(Long draftId, Long expectedVersion,
                     String status, String validatedFingerprint,
                     String checkSummary, LocalDateTime validatedAt);

    final class DraftSnapshot {

        private final Long id;
        private final Long version;
        private final String configFingerprint;
        private final CpsPlatformOnboardingPayload payload;

        public DraftSnapshot(Long id, Long version, String configFingerprint,
                             CpsPlatformOnboardingPayload payload) {
            this.id = id;
            this.version = version;
            this.configFingerprint = configFingerprint;
            this.payload = payload;
        }

        public Long id() {
            return id;
        }

        public Long version() {
            return version;
        }

        public String configFingerprint() {
            return configFingerprint;
        }

        public CpsPlatformOnboardingPayload payload() {
            return payload;
        }

        @Override
        public String toString() {
            return "DraftSnapshot(id=" + id + ", version=" + version
                    + ", configFingerprint=" + configFingerprint + ")";
        }
    }

}
