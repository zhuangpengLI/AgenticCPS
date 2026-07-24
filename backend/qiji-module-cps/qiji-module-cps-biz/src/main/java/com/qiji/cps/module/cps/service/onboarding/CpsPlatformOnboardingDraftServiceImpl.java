package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.framework.common.util.object.BeanUtils;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsOnboardingPlatformRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsOnboardingVendorRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDetailRespVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingDraftSaveReqVO;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingPayloadRespVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.adzone.CpsAdzoneDO;
import com.qiji.cps.module.cps.dal.dataobject.onboarding.CpsPlatformOnboardingDraftDO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
import com.qiji.cps.module.cps.dal.dataobject.rebate.CpsRebateConfigDO;
import com.qiji.cps.module.cps.dal.dataobject.vendor.CpsApiVendorDO;
import com.qiji.cps.module.cps.dal.mysql.adzone.CpsAdzoneMapper;
import com.qiji.cps.module.cps.dal.mysql.onboarding.CpsPlatformOnboardingDraftMapper;
import com.qiji.cps.module.cps.dal.mysql.platform.CpsPlatformMapper;
import com.qiji.cps.module.cps.dal.mysql.rebate.CpsRebateConfigMapper;
import com.qiji.cps.module.cps.dal.mysql.vendor.CpsApiVendorMapper;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingModeEnum;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingStatusEnum;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_CONFIG_INVALID;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_NOT_EXISTS;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_DRAFT_VERSION_CONFLICT;

@Service
@Validated
@RequiredArgsConstructor
public class CpsPlatformOnboardingDraftServiceImpl implements CpsPlatformOnboardingDraftService {

    private final CpsPlatformOnboardingDraftMapper draftMapper;
    private final CpsPlatformMapper platformMapper;
    private final CpsApiVendorMapper vendorMapper;
    private final CpsAdzoneMapper adzoneMapper;
    private final CpsRebateConfigMapper rebateMapper;
    private final ObjectMapper objectMapper;
    private final CpsPlatformOnboardingFingerprint fingerprint;

    @Override
    public CpsPlatformOnboardingDetailRespVO getDetail(String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode(normalizedCode);
        if (draft != null) {
            return toDetail(draft, readPayload(draft.getPayloadCiphertext()));
        }
        CpsPlatformDO runtimePlatform = platformMapper.selectByPlatformCode(normalizedCode);
        if (runtimePlatform == null) {
            return newTransientDetail(normalizedCode, CpsPlatformOnboardingModeEnum.CREATE.getCode(),
                    newEmptyPayload(normalizedCode));
        }
        return newTransientDetail(normalizedCode, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                buildRuntimePayload(runtimePlatform));
    }

    @Override
    public CpsPlatformOnboardingDetailRespVO getRuntimeDetail(String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode(normalizedCode);
        CpsPlatformDO runtimePlatform = platformMapper.selectByPlatformCode(normalizedCode);
        if (runtimePlatform == null) {
            return draft == null
                    ? newTransientDetail(normalizedCode, CpsPlatformOnboardingModeEnum.CREATE.getCode(),
                    newEmptyPayload(normalizedCode))
                    : toDetail(draft, newEmptyPayload(normalizedCode));
        }
        if (draft == null) {
            return newTransientDetail(normalizedCode, CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode(),
                    buildRuntimePayload(runtimePlatform));
        }
        return toDetail(draft, buildRuntimePayload(runtimePlatform));
    }

    @Override
    public CpsPlatformOnboardingDetailRespVO saveDraft(CpsPlatformOnboardingDraftSaveReqVO request) {
        if (request == null || request.getPayload() == null) {
            throw exception(ONBOARDING_CONFIG_INVALID, "平台接入配置不能为空");
        }
        String platformCode = normalizePlatformCode(request.getPlatformCode());
        CpsPlatformOnboardingPayload incoming = copyPayload(request.getPayload());
        validateAndNormalizePayloadPlatform(platformCode, incoming);

        CpsPlatformOnboardingDraftDO existing = draftMapper.selectByPlatformCode(platformCode);
        if (existing == null && request.getDraftVersion() != null) {
            throw exception(ONBOARDING_DRAFT_VERSION_CONFLICT);
        }
        CpsPlatformOnboardingPayload storedPayload =
                existing == null ? null : readPayload(existing.getPayloadCiphertext());
        CpsPlatformOnboardingPayload merged = mergeSecrets(platformCode, incoming, storedPayload);
        String payloadJson = writePayload(merged);
        String configFingerprint = fingerprint.calculate(merged);

        if (existing == null) {
            String mode = platformMapper.selectByPlatformCode(platformCode) == null
                    ? CpsPlatformOnboardingModeEnum.CREATE.getCode()
                    : CpsPlatformOnboardingModeEnum.RECONFIGURE.getCode();
            CpsPlatformOnboardingDraftDO created = CpsPlatformOnboardingDraftDO.builder()
                    .platformCode(platformCode)
                    .mode(mode)
                    .payloadCiphertext(payloadJson)
                    .draftVersion(1)
                    .configFingerprint(configFingerprint)
                    .status(CpsPlatformOnboardingStatusEnum.DRAFT.getCode())
                    .build();
            try {
                draftMapper.insert(created);
            } catch (DuplicateKeyException e) {
                if (draftMapper.selectByPlatformCode(platformCode) != null) {
                    throw exception(ONBOARDING_DRAFT_VERSION_CONFLICT);
                }
                throw e;
            }
            return toDetail(created, merged);
        }

        Integer expectedVersion = toVersion(request.getDraftVersion());
        int updated = draftMapper.updatePayload(existing.getId(), expectedVersion, payloadJson,
                configFingerprint, CpsPlatformOnboardingStatusEnum.DRAFT.getCode());
        if (updated == 0) {
            throw exception(ONBOARDING_DRAFT_VERSION_CONFLICT);
        }
        CpsPlatformOnboardingDraftDO updatedDraft = CpsPlatformOnboardingDraftDO.builder()
                .id(existing.getId())
                .platformCode(platformCode)
                .mode(existing.getMode())
                .payloadCiphertext(payloadJson)
                .draftVersion(expectedVersion + 1)
                .configFingerprint(configFingerprint)
                .status(CpsPlatformOnboardingStatusEnum.DRAFT.getCode())
                .build();
        return toDetail(updatedDraft, merged);
    }

    @Override
    public void deleteDraft(String platformCode, Long expectedVersion) {
        String normalizedCode = normalizePlatformCode(platformCode);
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode(normalizedCode);
        if (draft == null) {
            throw exception(ONBOARDING_DRAFT_NOT_EXISTS);
        }
        int deleted = draftMapper.deleteByIdAndVersion(draft.getId(), toVersion(expectedVersion));
        ensureVersionUpdated(deleted, draft.getId());
    }

    @Override
    public CpsPlatformOnboardingPayload getRequiredPayload(String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode(normalizedCode);
        if (draft == null) {
            throw exception(ONBOARDING_DRAFT_NOT_EXISTS);
        }
        return readPayload(draft.getPayloadCiphertext());
    }

    @Override
    public DraftSnapshot getRequiredSnapshot(String platformCode, Long expectedVersion) {
        DraftSnapshot snapshot = getRequiredSnapshot(platformCode);
        Integer requiredVersion = toVersion(expectedVersion);
        if (!requiredVersion.equals(snapshot.version().intValue())) {
            throw exception(ONBOARDING_DRAFT_VERSION_CONFLICT);
        }
        return snapshot;
    }

    @Override
    public DraftSnapshot getRequiredSnapshot(String platformCode) {
        String normalizedCode = normalizePlatformCode(platformCode);
        CpsPlatformOnboardingDraftDO draft = draftMapper.selectByPlatformCode(normalizedCode);
        if (draft == null) {
            throw exception(ONBOARDING_DRAFT_NOT_EXISTS);
        }
        return new DraftSnapshot(draft.getId(), draft.getDraftVersion().longValue(),
                draft.getConfigFingerprint(), draft.getValidatedFingerprint(), draft.getStatus(),
                draft.getPublishedAt(), readPayload(draft.getPayloadCiphertext()));
    }

    @Override
    public void markValidating(Long draftId, Long expectedVersion) {
        int updated = draftMapper.markValidating(
                draftId, toVersion(expectedVersion), CpsPlatformOnboardingStatusEnum.VALIDATING.getCode());
        ensureVersionUpdated(updated, draftId);
    }

    @Override
    public void markChecked(Long draftId, Long expectedVersion, String status,
                            String validatedFingerprint, String checkSummary, LocalDateTime validatedAt) {
        int updated = draftMapper.markChecked(draftId, toVersion(expectedVersion), status,
                validatedFingerprint, checkSummary, validatedAt);
        ensureVersionUpdated(updated, draftId);
    }

    @Override
    public void markPublished(Long draftId, Long expectedVersion, String expectedFingerprint,
                              LocalDateTime publishedAt) {
        int updated = draftMapper.markPublished(draftId, toVersion(expectedVersion),
                expectedFingerprint, CpsPlatformOnboardingStatusEnum.READY.getCode(),
                CpsPlatformOnboardingStatusEnum.PUBLISHED.getCode(), publishedAt);
        if (updated == 0) {
            throw exception(com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.ONBOARDING_PUBLISH_CONFLICT);
        }
    }

    private CpsPlatformOnboardingPayload buildRuntimePayload(CpsPlatformDO platform) {
        String platformCode = platform.getPlatformCode();
        List<CpsOnboardingVendor> vendors = safeList(vendorMapper.selectAllByPlatformCode(platformCode))
                .stream().map(this::toVendor).toList();
        List<CpsOnboardingAdzone> adzones = safeList(adzoneMapper.selectAllByPlatformCode(platformCode))
                .stream().map(this::toAdzone).toList();
        List<CpsOnboardingRebateRule> rules =
                safeList(rebateMapper.selectManagedRulesByPlatformCode(platformCode))
                        .stream().map(this::toRebateRule).toList();
        return CpsPlatformOnboardingPayload.builder()
                .platform(BeanUtils.toBean(platform, CpsPlatformSaveReqVO.class))
                .primaryVendorCode(platform.getActiveVendorCode())
                .runtimeDefaultAdzoneId(platform.getDefaultAdzoneId())
                .vendors(new ArrayList<>(vendors))
                .adzones(new ArrayList<>(adzones))
                .rebateRules(new ArrayList<>(rules))
                .build();
    }

    private CpsPlatformOnboardingPayload newEmptyPayload(String platformCode) {
        CpsPlatformSaveReqVO platform = new CpsPlatformSaveReqVO();
        platform.setPlatformCode(platformCode);
        platform.setStatus(0);
        return CpsPlatformOnboardingPayload.builder().platform(platform).build();
    }

    private CpsPlatformOnboardingPayload mergeSecrets(
            String platformCode,
            CpsPlatformOnboardingPayload incoming,
            CpsPlatformOnboardingPayload storedPayload) {
        Map<String, CpsOnboardingVendor> storedByCode =
                vendorsByCode(storedPayload == null ? null : storedPayload.getVendors());
        mergePlatformExtraConfig(platformCode, incoming, storedPayload);
        Map<String, CpsOnboardingVendor> runtimeByCode = needsSecretFallback(incoming.getVendors())
                ? runtimeVendorsByCode(platformCode) : Map.of();
        List<CpsOnboardingVendor> mergedVendors = safeList(incoming.getVendors()).stream()
                .map(vendor -> {
                    String vendorCode = normalizeOptionalCode(vendor == null ? null : vendor.getVendorCode());
                    CpsOnboardingVendor merged = fingerprint.mergeSecrets(vendor, storedByCode.get(vendorCode));
                    return fingerprint.mergeSecrets(merged, runtimeByCode.get(vendorCode));
                })
                .toList();
        incoming.setVendors(new ArrayList<>(mergedVendors));
        return incoming;
    }

    private Map<String, CpsOnboardingVendor> runtimeVendorsByCode(String platformCode) {
        List<CpsOnboardingVendor> runtime = safeList(vendorMapper.selectAllByPlatformCode(platformCode))
                .stream().map(this::toVendor).toList();
        return vendorsByCode(runtime);
    }

    private Map<String, CpsOnboardingVendor> vendorsByCode(List<CpsOnboardingVendor> vendors) {
        Map<String, CpsOnboardingVendor> byCode = new LinkedHashMap<>();
        for (CpsOnboardingVendor vendor : safeList(vendors)) {
            if (vendor != null) {
                byCode.putIfAbsent(normalizeOptionalCode(vendor.getVendorCode()), vendor);
            }
        }
        return byCode;
    }

    private boolean needsSecretFallback(List<CpsOnboardingVendor> vendors) {
        return safeList(vendors).stream().anyMatch(vendor -> vendor != null
                && (!hasText(vendor.getAppKey())
                || !hasText(vendor.getAppSecret())
                || !hasText(vendor.getAuthToken())
                || !hasText(vendor.getExtraConfig())));
    }

    private void mergePlatformExtraConfig(
            String platformCode,
            CpsPlatformOnboardingPayload incoming,
            CpsPlatformOnboardingPayload storedPayload) {
        if (hasText(incoming.getPlatform().getExtraConfig())) {
            return;
        }
        String storedExtraConfig = storedPayload == null || storedPayload.getPlatform() == null
                ? null : storedPayload.getPlatform().getExtraConfig();
        if (hasText(storedExtraConfig)) {
            incoming.getPlatform().setExtraConfig(storedExtraConfig);
            return;
        }
        CpsPlatformDO runtime = platformMapper.selectByPlatformCode(platformCode);
        if (runtime != null && hasText(runtime.getExtraConfig())) {
            incoming.getPlatform().setExtraConfig(runtime.getExtraConfig());
        }
    }

    private CpsPlatformOnboardingDetailRespVO newTransientDetail(
            String platformCode, String mode, CpsPlatformOnboardingPayload payload) {
        CpsPlatformOnboardingDetailRespVO detail = new CpsPlatformOnboardingDetailRespVO();
        detail.setPlatformCode(platformCode);
        detail.setMode(mode);
        detail.setConfigFingerprint(fingerprint.calculate(payload));
        detail.setStatus(CpsPlatformOnboardingStatusEnum.DRAFT.getCode());
        detail.setPayload(toResponsePayload(payload));
        return detail;
    }

    private CpsPlatformOnboardingDetailRespVO toDetail(
            CpsPlatformOnboardingDraftDO draft, CpsPlatformOnboardingPayload payload) {
        CpsPlatformOnboardingDetailRespVO detail = new CpsPlatformOnboardingDetailRespVO();
        detail.setId(draft.getId());
        detail.setPlatformCode(draft.getPlatformCode());
        detail.setMode(draft.getMode());
        detail.setDraftVersion(draft.getDraftVersion() == null ? null : draft.getDraftVersion().longValue());
        detail.setConfigFingerprint(draft.getConfigFingerprint());
        detail.setValidatedFingerprint(draft.getValidatedFingerprint());
        detail.setStatus(draft.getStatus());
        detail.setCheckSummary(draft.getCheckSummary());
        detail.setValidatedAt(draft.getValidatedAt());
        detail.setPublishedAt(draft.getPublishedAt());
        detail.setPayload(toResponsePayload(payload));
        return detail;
    }

    private CpsPlatformOnboardingPayloadRespVO toResponsePayload(CpsPlatformOnboardingPayload source) {
        CpsPlatformOnboardingPayload copy = copyPayload(source);
        List<CpsOnboardingVendorRespVO> vendors = safeList(copy.getVendors()).stream()
                .map(this::toVendorResponse)
                .toList();
        return CpsPlatformOnboardingPayloadRespVO.builder()
                .platform(toPlatformResponse(copy.getPlatform()))
                .primaryVendorCode(copy.getPrimaryVendorCode())
                .runtimeDefaultAdzoneId(copy.getRuntimeDefaultAdzoneId())
                .vendors(new ArrayList<>(vendors))
                .adzones(new ArrayList<>(safeList(copy.getAdzones())))
                .rebateRules(new ArrayList<>(safeList(copy.getRebateRules())))
                .build();
    }

    private CpsOnboardingPlatformRespVO toPlatformResponse(CpsPlatformSaveReqVO platform) {
        return CpsOnboardingPlatformRespVO.builder()
                .id(platform.getId())
                .platformCode(platform.getPlatformCode())
                .platformName(platform.getPlatformName())
                .platformLogo(platform.getPlatformLogo())
                .defaultAdzoneId(platform.getDefaultAdzoneId())
                .platformServiceRate(platform.getPlatformServiceRate())
                .sort(platform.getSort())
                .status(platform.getStatus())
                .extraConfigConfigured(hasText(platform.getExtraConfig()))
                .remark(platform.getRemark())
                .activeVendorCode(platform.getActiveVendorCode())
                .build();
    }

    private CpsOnboardingVendorRespVO toVendorResponse(CpsOnboardingVendor vendor) {
        return CpsOnboardingVendorRespVO.builder()
                .vendorCode(vendor.getVendorCode())
                .vendorName(vendor.getVendorName())
                .vendorType(vendor.getVendorType())
                .platformCode(vendor.getPlatformCode())
                .apiBaseUrl(vendor.getApiBaseUrl())
                .appKeyConfigured(hasText(vendor.getAppKey()))
                .appSecretConfigured(hasText(vendor.getAppSecret()))
                .authTokenConfigured(hasText(vendor.getAuthToken()))
                .defaultAdzoneId(vendor.getDefaultAdzoneId())
                .extraConfigConfigured(hasText(vendor.getExtraConfig()))
                .priority(vendor.getPriority())
                .status(vendor.getStatus())
                .remark(vendor.getRemark())
                .build();
    }

    private CpsOnboardingVendor toVendor(CpsApiVendorDO source) {
        return CpsOnboardingVendor.builder()
                .vendorCode(source.getVendorCode())
                .vendorName(source.getVendorName())
                .vendorType(source.getVendorType())
                .platformCode(source.getPlatformCode())
                .appKey(source.getAppKey())
                .appSecret(source.getAppSecret())
                .apiBaseUrl(source.getApiBaseUrl())
                .authToken(source.getAuthToken())
                .defaultAdzoneId(source.getDefaultAdzoneId())
                .extraConfig(source.getExtraConfig())
                .priority(source.getPriority())
                .status(source.getStatus())
                .remark(source.getRemark())
                .build();
    }

    private CpsOnboardingAdzone toAdzone(CpsAdzoneDO source) {
        return CpsOnboardingAdzone.builder()
                .platformCode(source.getPlatformCode())
                .adzoneId(source.getAdzoneId())
                .adzoneName(source.getAdzoneName())
                .adzoneType(source.getAdzoneType())
                .relationType(source.getRelationType())
                .relationId(source.getRelationId())
                .externalRelationId(source.getExternalRelationId())
                .externalSpecialId(source.getExternalSpecialId())
                .isDefault(source.getIsDefault())
                .status(source.getStatus())
                .build();
    }

    private CpsOnboardingRebateRule toRebateRule(CpsRebateConfigDO source) {
        return CpsOnboardingRebateRule.builder()
                .memberId(source.getMemberId())
                .memberLevelId(source.getMemberLevelId())
                .platformCode(source.getPlatformCode())
                .rebateRate(source.getRebateRate())
                .minRebateAmount(source.getMinRebateAmount())
                .maxRebateAmount(source.getMaxRebateAmount())
                .status(source.getStatus())
                .priority(source.getPriority())
                .build();
    }

    private void validateAndNormalizePayloadPlatform(
            String platformCode, CpsPlatformOnboardingPayload payload) {
        if (payload.getPlatform() == null) {
            throw exception(ONBOARDING_CONFIG_INVALID, "平台配置不能为空");
        }
        String payloadPlatformCode = normalizePlatformCode(payload.getPlatform().getPlatformCode());
        if (!platformCode.equals(payloadPlatformCode)) {
            throw exception(ONBOARDING_CONFIG_INVALID, "请求平台编码与配置平台编码不一致");
        }
        payload.getPlatform().setPlatformCode(platformCode);
        payload.setVendors(normalizeVendors(platformCode, payload.getVendors()));
        payload.setAdzones(normalizeAdzones(platformCode, payload.getAdzones()));
        payload.setRebateRules(normalizeRebateRules(platformCode, payload.getRebateRules()));
    }

    private List<CpsOnboardingVendor> normalizeVendors(
            String platformCode, List<CpsOnboardingVendor> vendors) {
        List<CpsOnboardingVendor> normalized = new ArrayList<>();
        for (CpsOnboardingVendor vendor : safeList(vendors)) {
            if (vendor == null) {
                throw exception(ONBOARDING_CONFIG_INVALID, "供应商配置不能为空");
            }
            vendor.setPlatformCode(normalizeNestedPlatformCode(
                    platformCode, vendor.getPlatformCode(), "供应商"));
            normalized.add(vendor);
        }
        return normalized;
    }

    private List<CpsOnboardingAdzone> normalizeAdzones(
            String platformCode, List<CpsOnboardingAdzone> adzones) {
        List<CpsOnboardingAdzone> normalized = new ArrayList<>();
        for (CpsOnboardingAdzone adzone : safeList(adzones)) {
            if (adzone == null) {
                throw exception(ONBOARDING_CONFIG_INVALID, "推广位配置不能为空");
            }
            adzone.setPlatformCode(normalizeNestedPlatformCode(
                    platformCode, adzone.getPlatformCode(), "推广位"));
            normalized.add(adzone);
        }
        return normalized;
    }

    private List<CpsOnboardingRebateRule> normalizeRebateRules(
            String platformCode, List<CpsOnboardingRebateRule> rules) {
        List<CpsOnboardingRebateRule> normalized = new ArrayList<>();
        for (CpsOnboardingRebateRule rule : safeList(rules)) {
            if (rule == null) {
                throw exception(ONBOARDING_CONFIG_INVALID, "返利配置不能为空");
            }
            rule.setPlatformCode(normalizeNestedPlatformCode(
                    platformCode, rule.getPlatformCode(), "返利"));
            normalized.add(rule);
        }
        return normalized;
    }

    private String normalizeNestedPlatformCode(
            String platformCode, String nestedPlatformCode, String configName) {
        String normalized = normalizeOptionalCode(nestedPlatformCode);
        if (normalized == null) {
            throw exception(ONBOARDING_CONFIG_INVALID, configName + "平台编码不能为空");
        }
        if (!platformCode.equals(normalized)) {
            throw exception(ONBOARDING_CONFIG_INVALID, configName + "平台编码与请求平台编码不一致");
        }
        return normalized;
    }

    private String normalizePlatformCode(String platformCode) {
        String normalized = normalizeOptionalCode(platformCode);
        if (normalized == null) {
            throw exception(ONBOARDING_CONFIG_INVALID, "平台编码不能为空");
        }
        return normalized;
    }

    private String normalizeOptionalCode(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Integer toVersion(Long version) {
        if (version == null || version <= 0 || version > Integer.MAX_VALUE) {
            throw exception(ONBOARDING_DRAFT_VERSION_CONFLICT);
        }
        return version.intValue();
    }

    private void ensureVersionUpdated(int updated, Long draftId) {
        if (updated != 0) {
            return;
        }
        if (draftMapper.selectById(draftId) == null) {
            throw exception(ONBOARDING_DRAFT_NOT_EXISTS);
        }
        throw exception(ONBOARDING_DRAFT_VERSION_CONFLICT);
    }

    private CpsPlatformOnboardingPayload copyPayload(CpsPlatformOnboardingPayload payload) {
        return readPayload(writePayload(payload));
    }

    private CpsPlatformOnboardingPayload readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, CpsPlatformOnboardingPayload.class);
        } catch (JsonProcessingException e) {
            throw exception(ONBOARDING_CONFIG_INVALID, "草稿内容无法解析");
        }
    }

    private String writePayload(CpsPlatformOnboardingPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw exception(ONBOARDING_CONFIG_INVALID, "草稿内容无法序列化");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

}
