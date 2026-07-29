package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.framework.common.pojo.PageResult;
import com.qiji.cps.module.cps.client.CpsPlatformClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.*;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformPageReqVO;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.dal.dataobject.platform.CpsPlatformDO;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.qiji.cps.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.qiji.cps.module.cps.enums.CpsErrorCodeConstants.*;

/**
 * Aggregates the four onboarding runtime tables and owns lifecycle guards.
 * Runtime writes remain in the existing publish service so that the atomic
 * publish contract from Task 5 is preserved.
 */
@Service
@RequiredArgsConstructor
public class CpsPlatformOnboardingLifecycleService {

    private final CpsPlatformOnboardingDraftService draftService;
    private final CpsPlatformOnboardingService onboardingService;
    private final CpsPlatformOnboardingValidator validator;
    private final CpsPlatformOnboardingConnectionTester connectionTester;
    private final CpsPlatformClientFactory clientFactory;
    private final CpsPlatformService platformService;
    private final CpsApiVendorService vendorService;
    private final CpsAdzoneService adzoneService;
    private final CpsRebateConfigService rebateConfigService;

    public PageResult<CpsPlatformOnboardingPageRespVO> getPage(
            CpsPlatformOnboardingPageReqVO request) {
        CpsPlatformOnboardingPageReqVO req =
                request == null ? new CpsPlatformOnboardingPageReqVO() : request;
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        addAllNormalized(codes, clientFactory.getRegisteredPlatformCodes());
        safeList(clientFactory.getRegisteredVendorDescriptors()).stream()
                .filter(Objects::nonNull)
                .map(CpsVendorDescriptor::getPlatformCode)
                .forEach(code -> addNormalized(codes, code));
        try {
            CpsPlatformPageReqVO allReq = new CpsPlatformPageReqVO();
            allReq.setPageNo(1);
            allReq.setPageSize(200);
            PageResult<CpsPlatformDO> configured = platformService.getPlatformPage(allReq);
            safeList(configured == null ? null : configured.getList()).stream()
                    .map(CpsPlatformDO::getPlatformCode)
                    .forEach(code -> addNormalized(codes, code));
        } catch (RuntimeException ignored) {
            // A capability-only list is still useful when the runtime table is unavailable.
        }
        List<CpsPlatformOnboardingPageRespVO> all = codes.stream()
                .map(this::toPageItem)
                .filter(item -> matches(item, req))
                .sorted(Comparator.comparing(CpsPlatformOnboardingPageRespVO::getPlatformCode,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
        int pageNo = req.getPageNo() == null || req.getPageNo() < 1 ? 1 : req.getPageNo();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : req.getPageSize();
        int from = Math.min((pageNo - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageResult<>(all.subList(from, to), (long) all.size());
    }

    public CpsPlatformOnboardingDetailRespVO getDetail(String platformCode) {
        String code = normalize(platformCode);
        CpsPlatformOnboardingDetailRespVO runtime = draftService.getRuntimeDetail(code);
        CpsPlatformOnboardingDetailRespVO draft = draftService.getDetail(code);
        if (runtime == null) {
            return draft;
        }
        runtime.setRuntimePayload(runtime.getPayload());
        runtime.setDraftPayload(draft == null ? null : draft.getPayload());
        if (runtime.getPayload() == null && draft != null) {
            runtime.setPayload(draft.getPayload());
        }
        return runtime;
    }

    public List<CpsPlatformCapabilityRespVO> getPlatformCapabilities(
            String platformCode) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        addAllNormalized(codes, clientFactory.getRegisteredPlatformCodes());
        safeList(clientFactory.getRegisteredVendorDescriptors()).stream()
                .filter(Objects::nonNull)
                .map(CpsVendorDescriptor::getPlatformCode)
                .forEach(code -> addNormalized(codes, code));
        if (StringUtils.hasText(platformCode)) {
            String wanted = normalize(platformCode);
            codes.removeIf(code -> !wanted.equals(code));
        }
        return codes.stream().map(this::toCapability).toList();
    }

    public List<CpsVendorDescriptorRespVO> getVendorDescriptors(
            String platformCode) {
        return safeList(clientFactory.getRegisteredVendorDescriptors()).stream()
                .filter(Objects::nonNull)
                .filter(descriptor -> !StringUtils.hasText(platformCode)
                        || normalize(platformCode).equals(normalize(descriptor.getPlatformCode())))
                .map(this::toDescriptor)
                .toList();
    }

    public CpsPlatformOnboardingCheckRespVO validate(CpsPlatformOnboardingValidateReqVO request) {
        return validator.validate(request == null ? null : request.getPayload());
    }

    public CpsPlatformOnboardingCheckRespVO test(CpsPlatformOnboardingTestReqVO request) {
        return connectionTester.test(request.getPlatformCode(), request.getDraftVersion());
    }

    public CpsPlatformOnboardingCheckRespVO testVendor(
            CpsPlatformOnboardingVendorTestReqVO request) {
        return connectionTester.testVendor(
                request.getPlatformCode(), request.getDraftVersion(), request.getVendorCode());
    }

    public CpsPlatformOnboardingDetailRespVO saveDraft(CpsPlatformOnboardingDraftSaveReqVO request) {
        return draftService.saveDraft(request);
    }

    public void deleteDraft(CpsPlatformOnboardingDraftDeleteReqVO request) {
        String platformCode = normalize(request.getPlatformCode());
        Long expectedVersion = request.getDraftVersion();
        if (expectedVersion == null) {
            expectedVersion = draftService.getRequiredSnapshot(platformCode).version();
        }
        draftService.deleteDraft(platformCode, expectedVersion);
    }

    public CpsPlatformOnboardingDetailRespVO publish(CpsPlatformOnboardingPublishReqVO request) {
        return onboardingService.publish(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enablePlatform(String platformCode) {
        String code = normalize(platformCode);
        CpsPlatformDO platform = requirePlatform(code);
        CpsPlatformOnboardingDraftService.DraftSnapshot draft =
                draftService.getRequiredSnapshot(code);
        if (!CpsPlatformOnboardingStatusEnum.PUBLISHED.getCode().equals(draft.status())
                || !StringUtils.hasText(draft.configFingerprint())
                || !Objects.equals(draft.configFingerprint(), draft.validatedFingerprint())) {
            throw exception(ONBOARDING_TEST_REQUIRED);
        }
        CpsPlatformSaveReqVO request = toSaveRequest(platform, 1);
        platformService.updatePlatform(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disablePlatform(String platformCode) {
        CpsPlatformDO platform = requirePlatform(normalize(platformCode));
        if (!Integer.valueOf(1).equals(platform.getStatus())) {
            return;
        }
        platformService.updatePlatform(toSaveRequest(platform, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePlatformBundle(String platformCode) {
        String code = normalize(platformCode);
        CpsPlatformDO platform = requirePlatform(code);
        if (Integer.valueOf(1).equals(platform.getStatus())) {
            throw exception(ONBOARDING_PLATFORM_ENABLED);
        }
        vendorService.deleteVendorsNotIn(code, Set.of());
        adzoneService.deleteAdzonesNotIn(code, Set.of());
        rebateConfigService.deleteManagedRebateRulesNotIn(code, Set.of());
        CpsPlatformOnboardingDetailRespVO draft = draftService.getDetail(code);
        if (draft != null && draft.getId() != null && draft.getDraftVersion() != null) {
            draftService.deleteDraft(code, draft.getDraftVersion());
        }
        platformService.deletePlatform(platform.getId());
    }

    private CpsPlatformOnboardingPageRespVO toPageItem(String code) {
        CpsPlatformOnboardingDetailRespVO runtime = safeRuntimeDetail(code);
        CpsPlatformOnboardingDetailRespVO draft = safeDraftDetail(code);
        CpsPlatformOnboardingPayload runtimePayload = payload(runtime);
        CpsPlatformOnboardingPayload draftPayload = payload(draft);
        CpsPlatformOnboardingPayload effectivePayload =
                draft != null && draft.getId() != null && draftPayload != null
                        ? draftPayload : runtimePayload;
        CpsPlatformDO platform = safePlatform(code);
        String name = effectivePayload == null || effectivePayload.getPlatform() == null
                ? code : effectivePayload.getPlatform().getPlatformName();
        if (platform != null && StringUtils.hasText(platform.getPlatformName())) {
            name = platform.getPlatformName();
        }
        List<String> missing = new ArrayList<>();
        if (effectivePayload == null || effectivePayload.getPlatform() == null) {
            missing.add("PLATFORM");
        }
        if (effectivePayload == null || !hasPrimary(effectivePayload, code)) {
            missing.add("PRIMARY_VENDOR");
        }
        if (effectivePayload == null || !hasDefaultAdzone(effectivePayload, code)) {
            missing.add("DEFAULT_ADZONE");
        }
        BigDecimal defaultRate = defaultRate(effectivePayload, code);
        if (defaultRate == null) {
            missing.add("DEFAULT_REBATE");
        }
        String draftStatus = draft == null ? null : draft.getStatus();
        boolean tested = draft != null && StringUtils.hasText(draft.getConfigFingerprint())
                && Objects.equals(draft.getConfigFingerprint(), draft.getValidatedFingerprint())
                && (CpsPlatformOnboardingStatusEnum.READY.getCode().equals(draftStatus)
                || CpsPlatformOnboardingStatusEnum.PUBLISHED.getCode().equals(draftStatus));
        if (!tested) {
            missing.add("CONNECTION_TEST");
        }
        return CpsPlatformOnboardingPageRespVO.builder()
                .platformCode(code)
                .platformName(name)
                .primaryVendorCode(effectivePayload == null ? null : effectivePayload.getPrimaryVendorCode())
                .backupVendorCount(effectivePayload == null ? 0 : backupCount(effectivePayload))
                .runtimeDefaultAdzoneId(effectivePayload == null ? null : effectivePayload.getRuntimeDefaultAdzoneId())
                .defaultRebateRate(defaultRate)
                .completionPercent((5 - missing.size()) * 20)
                .missingItems(List.copyOf(missing))
                .connectionStatus(connectionStatus(draftStatus, tested))
                .runtimeStatus(platform == null ? null : platform.getStatus())
                .draftStatus(draftStatus)
                .updateTime(updateTime(platform, draft))
                .build();
    }

    private CpsPlatformOnboardingDetailRespVO safeRuntimeDetail(String code) {
        if (draftService == null) {
            return null;
        }
        try {
            return draftService.getRuntimeDetail(code);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private CpsPlatformOnboardingDetailRespVO safeDraftDetail(String code) {
        if (draftService == null) {
            return null;
        }
        try {
            return draftService.getDetail(code);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private CpsPlatformDO safePlatform(String code) {
        if (platformService == null) {
            return null;
        }
        try {
            return platformService.getPlatformByCode(code);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private CpsPlatformCapabilityRespVO toCapability(String code) {
        CpsPlatformClient client = clientFactory.getClient(code);
        List<CpsVendorDescriptorRespVO> vendors =
                getVendorDescriptors(code);
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        if (client != null && client.getCapabilities() != null) {
            client.getCapabilities().stream().map(CpsVendorCapability::getCode)
                    .forEach(capabilities::add);
        }
        vendors.stream().flatMap(v -> safeList(v.getCapabilities()).stream())
                .forEach(capabilities::add);
        CpsPlatformDO platform = platformService.getPlatformByCode(code);
        return CpsPlatformCapabilityRespVO.builder()
                .platformCode(code)
                .platformName(platform == null ? code : platform.getPlatformName())
                .capabilities(List.copyOf(capabilities))
                .vendors(vendors)
                .build();
    }

    private CpsVendorDescriptorRespVO toDescriptor(CpsVendorDescriptor d) {
        return CpsVendorDescriptorRespVO.builder()
                .vendorCode(d.getVendorCode()).platformCode(d.getPlatformCode())
                .vendorType(d.getVendorType())
                .capabilities(safeSet(d.getCapabilities()).stream()
                        .map(CpsVendorCapability::getCode).toList())
                .configSchema(d.getConfigSchema()).governancePolicy(d.getGovernancePolicy())
                .sdkModule(d.getSdkModule()).version(d.getVersion()).build();
    }

    private CpsPlatformSaveReqVO toSaveRequest(CpsPlatformDO p, int status) {
        CpsPlatformSaveReqVO request = new CpsPlatformSaveReqVO();
        request.setId(p.getId()); request.setPlatformCode(p.getPlatformCode());
        request.setPlatformName(p.getPlatformName()); request.setPlatformLogo(p.getPlatformLogo());
        request.setDefaultAdzoneId(p.getDefaultAdzoneId());
        request.setPlatformServiceRate(p.getPlatformServiceRate()); request.setSort(p.getSort());
        request.setStatus(status); request.setExtraConfig(p.getExtraConfig()); request.setRemark(p.getRemark());
        request.setActiveVendorCode(p.getActiveVendorCode());
        return request;
    }

    private CpsPlatformDO requirePlatform(String code) {
        CpsPlatformDO platform = platformService.getPlatformByCode(code);
        if (platform == null) {
            throw exception(PLATFORM_NOT_EXISTS);
        }
        return platform;
    }

    private static CpsPlatformOnboardingPayload payload(CpsPlatformOnboardingDetailRespVO detail) {
        return detail == null || detail.getPayload() == null ? null : fromResponse(detail.getPayload());
    }

    private static CpsPlatformOnboardingPayload fromResponse(CpsPlatformOnboardingPayloadRespVO p) {
        if (p == null) return null;
        CpsPlatformOnboardingPayload payload = new CpsPlatformOnboardingPayload();
        if (p.getPlatform() != null) {
            com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO platform =
                    new com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO();
            platform.setPlatformCode(p.getPlatform().getPlatformCode());
            platform.setPlatformName(p.getPlatform().getPlatformName());
            platform.setPlatformLogo(p.getPlatform().getPlatformLogo());
            platform.setDefaultAdzoneId(p.getPlatform().getDefaultAdzoneId());
            platform.setPlatformServiceRate(p.getPlatform().getPlatformServiceRate());
            platform.setSort(p.getPlatform().getSort());
            platform.setStatus(p.getPlatform().getStatus());
            platform.setRemark(p.getPlatform().getRemark());
            platform.setActiveVendorCode(p.getPlatform().getActiveVendorCode());
            payload.setPlatform(platform);
        }
        payload.setPrimaryVendorCode(p.getPrimaryVendorCode());
        payload.setRuntimeDefaultAdzoneId(p.getRuntimeDefaultAdzoneId());
        payload.setVendors(p.getVendors() == null ? new ArrayList<>() : p.getVendors().stream()
                .map(v -> CpsOnboardingVendor.builder().vendorCode(v.getVendorCode())
                        .vendorName(v.getVendorName()).vendorType(v.getVendorType())
                        .platformCode(v.getPlatformCode()).apiBaseUrl(v.getApiBaseUrl())
                        .defaultAdzoneId(v.getDefaultAdzoneId())
                        .appKeyConfigured(v.getAppKeyConfigured())
                        .appSecretConfigured(v.getAppSecretConfigured())
                        .authTokenConfigured(v.getAuthTokenConfigured())
                        .extraConfigConfigured(v.getExtraConfigConfigured())
                        .configuredFields(v.getConfiguredFields())
                        .status(v.getStatus()).priority(v.getPriority()).build()).toList());
        payload.setAdzones(p.getAdzones() == null ? new ArrayList<>() : p.getAdzones());
        payload.setRebateRules(p.getRebateRules() == null ? new ArrayList<>() : p.getRebateRules());
        return payload;
    }

    private boolean hasPrimary(CpsPlatformOnboardingPayload p, String platformCode) {
        if (!StringUtils.hasText(p.getPrimaryVendorCode())) return false;
        return safeList(p.getVendors()).stream().anyMatch(v -> v != null
                && Integer.valueOf(1).equals(v.getStatus())
                && p.getPrimaryVendorCode().equalsIgnoreCase(v.getVendorCode())
                && (!StringUtils.hasText(v.getPlatformCode())
                || platformCode.equalsIgnoreCase(v.getPlatformCode()))
                && credentialsConfigured(v, platformCode));
    }

    private static boolean hasDefaultAdzone(CpsPlatformOnboardingPayload p, String platformCode) {
        return StringUtils.hasText(p.getRuntimeDefaultAdzoneId())
                && safeList(p.getAdzones()).stream().anyMatch(a -> a != null
                && Integer.valueOf(1).equals(a.getStatus())
                && (!StringUtils.hasText(a.getPlatformCode())
                || platformCode.equalsIgnoreCase(a.getPlatformCode()))
                && p.getRuntimeDefaultAdzoneId().equals(a.getAdzoneId()));
    }

    private static BigDecimal defaultRate(CpsPlatformOnboardingPayload p, String platformCode) {
        if (p == null) return null;
        return safeList(p.getRebateRules()).stream()
                .filter(r -> r != null && r.getMemberId() == null && r.getMemberLevelId() == null
                        && Integer.valueOf(1).equals(r.getStatus()))
                .filter(r -> !StringUtils.hasText(r.getPlatformCode())
                        || platformCode.equalsIgnoreCase(r.getPlatformCode()))
                .filter(r -> r.getRebateRate() != null)
                .max(Comparator
                        .comparingInt((CpsOnboardingRebateRule r) ->
                                platformCode.equalsIgnoreCase(r.getPlatformCode()) ? 1 : 0)
                        .thenComparing(CpsOnboardingRebateRule::getPriority,
                                Comparator.nullsFirst(Integer::compareTo)))
                .map(CpsOnboardingRebateRule::getRebateRate)
                .orElse(null);
    }

    private boolean credentialsConfigured(CpsOnboardingVendor vendor, String platformCode) {
        CpsVendorDescriptor descriptor = clientFactory.getVendorDescriptor(
                vendor.getVendorCode(), platformCode);
        if (descriptor == null || descriptor.getConfigSchema() == null
                || descriptor.getConfigSchema().getFields() == null) {
            return Boolean.TRUE.equals(vendor.getAppKeyConfigured())
                    && Boolean.TRUE.equals(vendor.getAppSecretConfigured());
        }
        return descriptor.getConfigSchema().getFields().stream()
                .filter(field -> field.isRequired())
                .allMatch(field -> configuredField(vendor, field.getName()));
    }

    private static boolean configuredField(CpsOnboardingVendor vendor, String fieldName) {
        return switch (fieldName) {
            case "appKey" -> Boolean.TRUE.equals(vendor.getAppKeyConfigured())
                    || StringUtils.hasText(vendor.getAppKey());
            case "appSecret" -> Boolean.TRUE.equals(vendor.getAppSecretConfigured())
                    || StringUtils.hasText(vendor.getAppSecret());
            case "authToken" -> Boolean.TRUE.equals(vendor.getAuthTokenConfigured())
                    || StringUtils.hasText(vendor.getAuthToken());
            case "apiBaseUrl" -> StringUtils.hasText(vendor.getApiBaseUrl());
            case "defaultAdzoneId" -> StringUtils.hasText(vendor.getDefaultAdzoneId());
            default -> safeList(vendor.getConfiguredFields()).contains(fieldName);
        };
    }

    private static int backupCount(CpsPlatformOnboardingPayload p) {
        return (int) safeList(p.getVendors()).stream()
                .filter(v -> v != null && Integer.valueOf(1).equals(v.getStatus())
                        && !Objects.equals(v.getVendorCode(), p.getPrimaryVendorCode())).count();
    }

    private static String connectionStatus(String status, boolean tested) {
        if (CpsPlatformOnboardingStatusEnum.FAILED.getCode().equals(status)) return "FAILED";
        if (tested) return "PASSED";
        return "NOT_TESTED";
    }

    private static LocalDateTime updateTime(CpsPlatformDO p, CpsPlatformOnboardingDetailRespVO d) {
        return p != null && p.getUpdateTime() != null ? p.getUpdateTime()
                : d == null ? null : d.getValidatedAt() != null ? d.getValidatedAt() : d.getPublishedAt();
    }

    private static boolean matches(CpsPlatformOnboardingPageRespVO item,
                                   CpsPlatformOnboardingPageReqVO req) {
        if (StringUtils.hasText(req.getPlatformCode())
                && !req.getPlatformCode().equalsIgnoreCase(item.getPlatformCode())) return false;
        if (StringUtils.hasText(req.getPlatformName())
                && !contains(item.getPlatformName(), req.getPlatformName())) return false;
        if (StringUtils.hasText(req.getKeyword())
                && !(contains(item.getPlatformCode(), req.getKeyword())
                || contains(item.getPlatformName(), req.getKeyword()))) return false;
        String status = normalizeStatus(req.getStatus());
        return status == null || "ALL".equals(status)
                || "INCOMPLETE".equals(status) && item.getCompletionPercent() < 100
                || "READY".equals(status) && item.getCompletionPercent() == 100
                || "ENABLED".equals(status) && Integer.valueOf(1).equals(item.getRuntimeStatus())
                || "FAILED".equals(status) && "FAILED".equals(item.getConnectionStatus());
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(query.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static String normalizeStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static void addNormalized(Set<String> target, String value) {
        String normalized = normalize(value);
        if (normalized != null) target.add(normalized);
    }

    private static void addAllNormalized(Set<String> target, Set<String> values) {
        if (values != null) values.forEach(value -> addNormalized(target, value));
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> Set<T> safeSet(Set<T> values) {
        return values == null ? Set.of() : values;
    }
}
