package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.framework.common.exception.ServiceException;
import com.qiji.cps.module.cps.controller.admin.adzone.vo.CpsAdzoneSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPublishReqVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.rebate.vo.CpsRebateConfigSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.vendor.vo.CpsApiVendorSaveReqVO;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingStatusEnum;
import com.qiji.cps.module.cps.service.adzone.CpsAdzoneService;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import com.qiji.cps.module.cps.service.platform.CpsPlatformService;
import com.qiji.cps.module.cps.service.rebate.CpsRebateConfigService;
import com.qiji.cps.module.cps.service.vendor.CpsApiVendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_CONFIG_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_VERSION_CONFLICT;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_PUBLISH_CONFLICT;

@Service
@Validated
@RequiredArgsConstructor
public class CpsPlatformOnboardingServiceImpl implements CpsPlatformOnboardingService {

    private final CpsPlatformOnboardingDraftService draftService;
    private final CpsPlatformOnboardingValidator validator;
    private final CpsPlatformOnboardingFingerprint fingerprint;
    private final CpsPlatformService platformService;
    private final CpsApiVendorService vendorService;
    private final CpsAdzoneService adzoneService;
    private final CpsRebateConfigService rebateService;
    private final CpsPlatformOnboardingCacheInvalidator cacheInvalidator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsPlatformOnboardingDetailRespVO publish(CpsPlatformOnboardingPublishReqVO request) {
        if (request == null) {
            throw exception(ONBOARDING_PUBLISH_CONFLICT);
        }
        String platformCode =
                CpsPlatformOnboardingPayloadNormalizer.code(request.getPlatformCode());
        CpsPlatformOnboardingDraftService.DraftSnapshot draft =
                getPublishSnapshot(platformCode, request.getDraftVersion());

        String recalculatedFingerprint = fingerprint.calculate(draft.payload());
        requireMatchingFingerprints(draft, request, recalculatedFingerprint);
        if (CpsPlatformOnboardingStatusEnum.PUBLISHED.getCode().equals(draft.status())) {
            return draftService.getRuntimeDetail(platformCode);
        }
        if (!CpsPlatformOnboardingStatusEnum.READY.getCode().equals(draft.status())) {
            throw exception(ONBOARDING_PUBLISH_CONFLICT);
        }

        CpsPlatformOnboardingValidator.ValidationResult validation =
                validator.validateNormalized(draft.payload());
        if (!validation.response().isSuccess() || validation.normalizedPayload() == null) {
            throw exception(ONBOARDING_CONFIG_INVALID, "发布前配置校验失败");
        }
        CpsPlatformOnboardingPayload payload = validation.normalizedPayload();
        if (payload.getPlatform() == null
                || !Objects.equals(platformCode, payload.getPlatform().getPlatformCode())
                || !Objects.equals(recalculatedFingerprint, fingerprint.calculate(payload))) {
            throw exception(ONBOARDING_PUBLISH_CONFLICT);
        }

        Set<String> retainedVendorCodes = upsertVendors(platformCode, payload.getVendors());
        Set<String> retainedAdzoneIds = upsertAdzones(platformCode, payload.getAdzones());
        Set<String> retainedRebateScopes =
                upsertManagedRebates(platformCode, payload.getRebateRules());

        upsertPlatformLast(platformCode, payload,
                Boolean.TRUE.equals(request.getEnableAfterPublish()));

        vendorService.deleteVendorsNotIn(platformCode, retainedVendorCodes);
        adzoneService.deleteAdzonesNotIn(platformCode, retainedAdzoneIds);
        rebateService.deleteManagedRebateRulesNotIn(platformCode, retainedRebateScopes);

        draftService.markPublished(draft.id(), draft.version(),
                recalculatedFingerprint, LocalDateTime.now());
        cacheInvalidator.evictAfterCommit(platformCode);
        return draftService.getRuntimeDetail(platformCode);
    }

    private CpsPlatformOnboardingDraftService.DraftSnapshot getPublishSnapshot(
            String platformCode, Long draftVersion) {
        try {
            return draftService.getRequiredSnapshot(platformCode, draftVersion);
        } catch (ServiceException serviceException) {
            if (Objects.equals(serviceException.getCode(),
                    ONBOARDING_DRAFT_VERSION_CONFLICT.getCode())) {
                throw exception(ONBOARDING_PUBLISH_CONFLICT);
            }
            throw serviceException;
        }
    }

    private void requireMatchingFingerprints(
            CpsPlatformOnboardingDraftService.DraftSnapshot draft,
            CpsPlatformOnboardingPublishReqVO request,
            String recalculatedFingerprint) {
        String expected = request.getConfigFingerprint();
        if (!Objects.equals(expected, draft.configFingerprint())
                || !Objects.equals(expected, draft.validatedFingerprint())
                || !Objects.equals(expected, recalculatedFingerprint)) {
            throw exception(ONBOARDING_PUBLISH_CONFLICT);
        }
    }

    private Set<String> upsertVendors(String platformCode,
                                      List<CpsOnboardingVendor> vendors) {
        Set<String> retained = new LinkedHashSet<>();
        for (CpsOnboardingVendor source : safeList(vendors)) {
            CpsApiVendorSaveReqVO request = new CpsApiVendorSaveReqVO();
            request.setVendorCode(source.getVendorCode());
            request.setVendorName(source.getVendorName());
            request.setVendorType(source.getVendorType());
            request.setPlatformCode(platformCode);
            request.setAppKey(source.getAppKey());
            request.setAppSecret(source.getAppSecret());
            request.setApiBaseUrl(source.getApiBaseUrl());
            request.setAuthToken(source.getAuthToken());
            request.setDefaultAdzoneId(source.getDefaultAdzoneId());
            request.setExtraConfig(source.getExtraConfig());
            request.setPriority(source.getPriority());
            request.setStatus(source.getStatus());
            request.setRemark(source.getRemark());
            vendorService.upsertVendorForOnboarding(request);
            retained.add(source.getVendorCode());
        }
        return retained;
    }

    private Set<String> upsertAdzones(String platformCode,
                                      List<CpsOnboardingAdzone> adzones) {
        Set<String> retained = new LinkedHashSet<>();
        for (CpsOnboardingAdzone source : safeList(adzones)) {
            CpsAdzoneSaveReqVO request = new CpsAdzoneSaveReqVO();
            request.setPlatformCode(platformCode);
            request.setAdzoneId(source.getAdzoneId());
            request.setAdzoneName(source.getAdzoneName());
            request.setAdzoneType(source.getAdzoneType());
            request.setRelationType(source.getRelationType());
            request.setRelationId(source.getRelationId());
            request.setExternalRelationId(source.getExternalRelationId());
            request.setExternalSpecialId(source.getExternalSpecialId());
            request.setIsDefault(source.getIsDefault());
            request.setStatus(source.getStatus());
            adzoneService.upsertAdzoneForOnboarding(request);
            retained.add(request.getAdzoneId());
        }
        return retained;
    }

    private Set<String> upsertManagedRebates(
            String platformCode, List<CpsOnboardingRebateRule> rules) {
        Set<String> retained = new LinkedHashSet<>();
        for (CpsOnboardingRebateRule source : safeList(rules)) {
            if (source.getMemberId() != null) {
                throw exception(ONBOARDING_CONFIG_INVALID,
                        "平台接入不允许发布个人返利规则");
            }
            CpsRebateConfigSaveReqVO request = new CpsRebateConfigSaveReqVO();
            request.setMemberId(null);
            request.setMemberLevelId(source.getMemberLevelId());
            request.setPlatformCode(platformCode);
            request.setRebateRate(source.getRebateRate());
            request.setMinRebateAmount(source.getMinRebateAmount());
            request.setMaxRebateAmount(source.getMaxRebateAmount());
            request.setStatus(source.getStatus());
            request.setPriority(source.getPriority());
            rebateService.upsertManagedRebateRuleForOnboarding(request);
            retained.add(CpsRebateConfigService.managedScopeKey(
                    request.getMemberLevelId(), request.getPriority()));
        }
        return retained;
    }

    private void upsertPlatformLast(String platformCode,
                                    CpsPlatformOnboardingPayload payload,
                                    boolean enableAfterPublish) {
        CpsPlatformSaveReqVO source = payload.getPlatform();
        CpsPlatformSaveReqVO request = new CpsPlatformSaveReqVO();
        request.setPlatformCode(platformCode);
        request.setPlatformName(source.getPlatformName());
        request.setPlatformLogo(source.getPlatformLogo());
        request.setDefaultAdzoneId(payload.getRuntimeDefaultAdzoneId());
        request.setPlatformServiceRate(source.getPlatformServiceRate());
        request.setSort(source.getSort());
        request.setStatus(enableAfterPublish ? 1 : 0);
        request.setExtraConfig(source.getExtraConfig());
        request.setRemark(source.getRemark());
        request.setActiveVendorCode(payload.getPrimaryVendorCode());
        List<String> supportedVendorCodes = safeList(payload.getVendors()).stream()
                .map(CpsOnboardingVendor::getVendorCode)
                .toList();
        platformService.upsertPlatformForOnboarding(request, supportedVendorCodes);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

}
