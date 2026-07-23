package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates the stable identity of draft business configuration.
 */
@Component
@RequiredArgsConstructor
public class CpsPlatformOnboardingFingerprint {

    private static final Comparator<String> NULL_SAFE_STRING = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<Integer> NULL_SAFE_INTEGER = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<Long> NULL_SAFE_LONG = Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<BigDecimal> NULL_SAFE_DECIMAL = Comparator.nullsFirst(Comparator.naturalOrder());

    private static final Comparator<CanonicalVendor> VENDOR_COMPARATOR =
            Comparator.comparing(CanonicalVendor::vendorCode, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::vendorName, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::vendorType, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::platformCode, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::appKey, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::appSecret, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::apiBaseUrl, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::authToken, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::defaultAdzoneId, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::extraConfig, NULL_SAFE_STRING)
                    .thenComparing(CanonicalVendor::priority, NULL_SAFE_INTEGER)
                    .thenComparing(CanonicalVendor::status, NULL_SAFE_INTEGER)
                    .thenComparing(CanonicalVendor::remark, NULL_SAFE_STRING);

    private static final Comparator<CanonicalAdzone> ADZONE_COMPARATOR =
            Comparator.comparing(CanonicalAdzone::adzoneId, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::platformCode, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::adzoneName, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::adzoneType, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::relationType, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::relationId, NULL_SAFE_LONG)
                    .thenComparing(CanonicalAdzone::externalRelationId, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::externalSpecialId, NULL_SAFE_STRING)
                    .thenComparing(CanonicalAdzone::isDefault, NULL_SAFE_INTEGER)
                    .thenComparing(CanonicalAdzone::status, NULL_SAFE_INTEGER);

    private static final Comparator<CanonicalRebateRule> REBATE_RULE_COMPARATOR =
            Comparator.comparing(CpsPlatformOnboardingFingerprint::rebateScopeKey)
                    .thenComparing(CanonicalRebateRule::priority, NULL_SAFE_INTEGER)
                    .thenComparing(CanonicalRebateRule::rebateRate, NULL_SAFE_DECIMAL)
                    .thenComparing(CanonicalRebateRule::minRebateAmount, NULL_SAFE_DECIMAL)
                    .thenComparing(CanonicalRebateRule::maxRebateAmount, NULL_SAFE_DECIMAL)
                    .thenComparing(CanonicalRebateRule::status, NULL_SAFE_INTEGER);

    private final ObjectMapper objectMapper;

    /**
     * Returns a lowercase SHA-256 hash over canonical business configuration.
     */
    public String calculate(CpsPlatformOnboardingPayload payload) {
        try {
            byte[] canonicalJson = objectMapper.writeValueAsBytes(canonicalize(payload));
            return sha256Hex(canonicalJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize onboarding fingerprint payload failed", e);
        }
    }

    /**
     * Applies the edit contract for masked credentials without changing either input.
     */
    public CpsOnboardingVendor mergeSecrets(CpsOnboardingVendor incoming, CpsOnboardingVendor stored) {
        if (incoming == null) {
            return null;
        }
        return CpsOnboardingVendor.builder()
                .vendorCode(incoming.getVendorCode())
                .vendorName(incoming.getVendorName())
                .vendorType(incoming.getVendorType())
                .platformCode(incoming.getPlatformCode())
                .appKey(incoming.getAppKey())
                .appSecret(mergeSecret(incoming.getAppSecret(), stored == null ? null : stored.getAppSecret()))
                .apiBaseUrl(incoming.getApiBaseUrl())
                .authToken(mergeSecret(incoming.getAuthToken(), stored == null ? null : stored.getAuthToken()))
                .defaultAdzoneId(incoming.getDefaultAdzoneId())
                .extraConfig(incoming.getExtraConfig())
                .priority(incoming.getPriority())
                .status(incoming.getStatus())
                .remark(incoming.getRemark())
                .build();
    }

    private CanonicalPayload canonicalize(CpsPlatformOnboardingPayload payload) {
        if (payload == null) {
            return new CanonicalPayload(null, null, null, List.of(), List.of(), List.of());
        }
        List<CanonicalVendor> vendors = canonicalVendors(payload.getVendors());
        List<CanonicalAdzone> adzones = canonicalAdzones(payload.getAdzones());
        List<CanonicalRebateRule> rebateRules = canonicalRebateRules(payload.getRebateRules());
        return new CanonicalPayload(
                canonicalPlatform(payload.getPlatform()),
                payload.getPrimaryVendorCode(),
                payload.getRuntimeDefaultAdzoneId(),
                vendors,
                adzones,
                rebateRules);
    }

    private List<CanonicalVendor> canonicalVendors(List<CpsOnboardingVendor> vendors) {
        if (vendors == null) {
            return List.of();
        }
        return vendors.stream()
                .map(this::canonicalVendor)
                .sorted(VENDOR_COMPARATOR)
                .toList();
    }

    private List<CanonicalAdzone> canonicalAdzones(List<CpsOnboardingAdzone> adzones) {
        if (adzones == null) {
            return List.of();
        }
        return adzones.stream()
                .map(this::canonicalAdzone)
                .sorted(ADZONE_COMPARATOR)
                .toList();
    }

    private List<CanonicalRebateRule> canonicalRebateRules(List<CpsOnboardingRebateRule> rebateRules) {
        if (rebateRules == null) {
            return List.of();
        }
        return rebateRules.stream()
                .map(this::canonicalRebateRule)
                .sorted(REBATE_RULE_COMPARATOR)
                .toList();
    }

    private CanonicalPlatform canonicalPlatform(CpsPlatformSaveReqVO platform) {
        if (platform == null) {
            return null;
        }
        return new CanonicalPlatform(
                platform.getPlatformCode(),
                platform.getPlatformName(),
                platform.getPlatformLogo(),
                platform.getDefaultAdzoneId(),
                normalize(platform.getPlatformServiceRate()),
                platform.getSort(),
                platform.getStatus(),
                platform.getExtraConfig(),
                platform.getRemark(),
                platform.getActiveVendorCode());
    }

    private CanonicalVendor canonicalVendor(CpsOnboardingVendor vendor) {
        if (vendor == null) {
            return new CanonicalVendor(null, null, null, null, null, null, null,
                    null, null, null, null, null, null);
        }
        return new CanonicalVendor(
                vendor.getVendorCode(),
                vendor.getVendorName(),
                vendor.getVendorType(),
                vendor.getPlatformCode(),
                vendor.getAppKey(),
                vendor.getAppSecret(),
                vendor.getApiBaseUrl(),
                vendor.getAuthToken(),
                vendor.getDefaultAdzoneId(),
                vendor.getExtraConfig(),
                normalizePriority(vendor.getPriority()),
                vendor.getStatus(),
                vendor.getRemark());
    }

    private CanonicalAdzone canonicalAdzone(CpsOnboardingAdzone adzone) {
        if (adzone == null) {
            return new CanonicalAdzone(null, null, null, null, null, null, null, null, null, null);
        }
        return new CanonicalAdzone(
                adzone.getPlatformCode(),
                adzone.getAdzoneId(),
                adzone.getAdzoneName(),
                adzone.getAdzoneType(),
                adzone.getRelationType(),
                adzone.getRelationId(),
                adzone.getExternalRelationId(),
                adzone.getExternalSpecialId(),
                adzone.getIsDefault(),
                adzone.getStatus());
    }

    private CanonicalRebateRule canonicalRebateRule(CpsOnboardingRebateRule rebateRule) {
        if (rebateRule == null) {
            return new CanonicalRebateRule(null, null, null, null, null, null, null, null);
        }
        return new CanonicalRebateRule(
                rebateRule.getMemberId(),
                rebateRule.getMemberLevelId(),
                rebateRule.getPlatformCode(),
                normalize(rebateRule.getRebateRate()),
                normalize(rebateRule.getMinRebateAmount()),
                normalize(rebateRule.getMaxRebateAmount()),
                rebateRule.getStatus(),
                normalizePriority(rebateRule.getPriority()));
    }

    private static String rebateScopeKey(CanonicalRebateRule rule) {
        return keyPart(rule.memberId())
                + "|" + keyPart(rule.memberLevelId())
                + "|" + keyPart(rule.platformCode());
    }

    private static String keyPart(Object value) {
        return value == null ? "N" : "V" + value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private Integer normalizePriority(Integer value) {
        return value == null ? 0 : value;
    }

    private String mergeSecret(String incoming, String stored) {
        return incoming == null || incoming.isBlank() ? stored : incoming;
    }

    private String sha256Hex(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record CanonicalPayload(
            CanonicalPlatform platform,
            String primaryVendorCode,
            String runtimeDefaultAdzoneId,
            List<CanonicalVendor> vendors,
            List<CanonicalAdzone> adzones,
            List<CanonicalRebateRule> rebateRules) {
    }

    private record CanonicalPlatform(
            String platformCode,
            String platformName,
            String platformLogo,
            String defaultAdzoneId,
            BigDecimal platformServiceRate,
            Integer sort,
            Integer status,
            String extraConfig,
            String remark,
            String activeVendorCode) {
    }

    private record CanonicalVendor(
            String vendorCode,
            String vendorName,
            String vendorType,
            String platformCode,
            String appKey,
            String appSecret,
            String apiBaseUrl,
            String authToken,
            String defaultAdzoneId,
            String extraConfig,
            Integer priority,
            Integer status,
            String remark) {
    }

    private record CanonicalAdzone(
            String platformCode,
            String adzoneId,
            String adzoneName,
            String adzoneType,
            String relationType,
            Long relationId,
            String externalRelationId,
            String externalSpecialId,
            Integer isDefault,
            Integer status) {
    }

    private record CanonicalRebateRule(
            Long memberId,
            Long memberLevelId,
            String platformCode,
            BigDecimal rebateRate,
            BigDecimal minRebateAmount,
            BigDecimal maxRebateAmount,
            Integer status,
            Integer priority) {
    }

}
