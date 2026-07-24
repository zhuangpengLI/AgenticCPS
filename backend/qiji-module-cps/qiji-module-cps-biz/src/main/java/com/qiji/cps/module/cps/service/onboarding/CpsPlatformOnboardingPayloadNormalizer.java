package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Produces the canonical onboarding payload used by validation, connection testing and hashing.
 */
final class CpsPlatformOnboardingPayloadNormalizer {

    private CpsPlatformOnboardingPayloadNormalizer() {
    }

    static CpsPlatformOnboardingPayload normalizeCopy(CpsPlatformOnboardingPayload payload,
                                                       ObjectMapper objectMapper) {
        if (payload == null) {
            return null;
        }
        CpsPlatformOnboardingPayload normalized = copy(payload, objectMapper);
        if (normalized.getPlatform() != null) {
            normalized.getPlatform().setPlatformCode(code(normalized.getPlatform().getPlatformCode()));
            normalized.getPlatform().setActiveVendorCode(
                    code(normalized.getPlatform().getActiveVendorCode()));
            normalized.getPlatform().setDefaultAdzoneId(
                    opaque(normalized.getPlatform().getDefaultAdzoneId()));
        }
        normalized.setPrimaryVendorCode(code(normalized.getPrimaryVendorCode()));
        normalized.setRuntimeDefaultAdzoneId(opaque(normalized.getRuntimeDefaultAdzoneId()));

        for (CpsOnboardingVendor vendor : safeList(normalized.getVendors())) {
            if (vendor == null) {
                continue;
            }
            vendor.setVendorCode(code(vendor.getVendorCode()));
            vendor.setVendorType(code(vendor.getVendorType()));
            vendor.setPlatformCode(code(vendor.getPlatformCode()));
            vendor.setDefaultAdzoneId(opaque(vendor.getDefaultAdzoneId()));
        }
        for (CpsOnboardingAdzone adzone : safeList(normalized.getAdzones())) {
            if (adzone == null) {
                continue;
            }
            adzone.setPlatformCode(code(adzone.getPlatformCode()));
            adzone.setAdzoneId(opaque(adzone.getAdzoneId()));
            adzone.setAdzoneType(code(adzone.getAdzoneType()));
            adzone.setRelationType(code(adzone.getRelationType()));
            adzone.setExternalRelationId(opaque(adzone.getExternalRelationId()));
            adzone.setExternalSpecialId(opaque(adzone.getExternalSpecialId()));
        }
        for (CpsOnboardingRebateRule rule : safeList(normalized.getRebateRules())) {
            if (rule != null) {
                rule.setPlatformCode(code(rule.getPlatformCode()));
            }
        }

        synchronizeRuntimeFields(normalized);
        return normalized;
    }

    private static void synchronizeRuntimeFields(CpsPlatformOnboardingPayload payload) {
        String primaryVendorCode = payload.getPrimaryVendorCode();
        String defaultAdzoneId = payload.getRuntimeDefaultAdzoneId();
        if (payload.getPlatform() != null) {
            payload.getPlatform().setActiveVendorCode(primaryVendorCode);
            payload.getPlatform().setDefaultAdzoneId(defaultAdzoneId);
        }
        safeList(payload.getVendors()).stream()
                .filter(vendor -> vendor != null
                        && primaryVendorCode != null
                        && primaryVendorCode.equals(vendor.getVendorCode()))
                .findFirst()
                .ifPresent(vendor -> vendor.setDefaultAdzoneId(defaultAdzoneId));
    }

    private static CpsPlatformOnboardingPayload copy(CpsPlatformOnboardingPayload payload,
                                                      ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(payload),
                    CpsPlatformOnboardingPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("平台接入配置无法规范化", e);
        }
    }

    static String code(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    static String opaque(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

}
