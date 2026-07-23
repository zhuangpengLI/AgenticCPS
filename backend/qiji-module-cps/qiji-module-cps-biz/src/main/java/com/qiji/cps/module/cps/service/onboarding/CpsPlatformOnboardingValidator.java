package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.CpsVendorCapability;
import com.qiji.cps.module.cps.client.CpsVendorConfigValidationResult;
import com.qiji.cps.module.cps.client.CpsVendorDescriptor;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingCheckRespVO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CpsPlatformOnboardingValidator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CpsPlatformClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    public CpsPlatformOnboardingCheckRespVO validate(CpsPlatformOnboardingPayload payload) {
        return validateNormalized(payload).response();
    }

    ValidationResult validateNormalized(CpsPlatformOnboardingPayload payload) {
        List<CpsPlatformOnboardingCheckRespVO.Item> errors = new ArrayList<>();
        CpsPlatformOnboardingPayload normalized = copy(payload);
        String platformCode = normalized == null || normalized.getPlatform() == null
                ? null : normalize(normalized.getPlatform().getPlatformCode());

        validatePlatform(platformCode, errors);
        validateVendors(normalized, platformCode, errors);
        validateAdzones(normalized, platformCode, errors);
        validateRebates(normalized, platformCode, errors);

        return new ValidationResult(
                CpsPlatformOnboardingCheckRespVO.of(errors.isEmpty(), errors),
                errors.isEmpty() ? normalized : null);
    }

    private void validatePlatform(String platformCode,
                                  List<CpsPlatformOnboardingCheckRespVO.Item> errors) {
        Set<String> registered = new LinkedHashSet<>();
        safeSet(clientFactory.getRegisteredPlatformCodes()).stream()
                .map(CpsPlatformOnboardingValidator::normalize)
                .filter(StringUtils::hasText)
                .forEach(registered::add);
        safeList(clientFactory.getRegisteredVendorDescriptors()).stream()
                .map(CpsVendorDescriptor::getPlatformCode)
                .map(CpsPlatformOnboardingValidator::normalize)
                .filter(StringUtils::hasText)
                .forEach(registered::add);
        if (!StringUtils.hasText(platformCode) || !registered.contains(platformCode)) {
            add(errors, "PLATFORM_NOT_REGISTERED", "platform.platformCode",
                    "平台编码未注册", "platform");
        }
    }

    private void validateVendors(CpsPlatformOnboardingPayload payload, String platformCode,
                                 List<CpsPlatformOnboardingCheckRespVO.Item> errors) {
        if (payload == null) {
            add(errors, "PRIMARY_VENDOR_REQUIRED", "primaryVendorCode",
                    "必须选择一个已启用的主供应商", "vendor");
            return;
        }
        List<CpsOnboardingVendor> vendors = safeList(payload.getVendors());
        String primaryCode = normalize(payload.getPrimaryVendorCode());
        Set<String> seen = new HashSet<>();
        boolean duplicate = false;
        for (CpsOnboardingVendor vendor : vendors) {
            String key = vendor == null ? null : normalize(vendor.getVendorCode());
            if (StringUtils.hasText(key) && !seen.add(key)) {
                duplicate = true;
            }
        }
        if (duplicate) {
            add(errors, "VENDOR_DUPLICATE", "vendors",
                    "供应商编码规范化后不能重复", "vendor");
        }

        long enabledPrimaryCount = vendors.stream()
                .filter(vendor -> vendor != null
                        && primaryCode != null
                        && primaryCode.equals(normalize(vendor.getVendorCode()))
                        && enabled(vendor.getStatus()))
                .count();
        if (!StringUtils.hasText(primaryCode) || enabledPrimaryCount != 1) {
            add(errors, "PRIMARY_VENDOR_REQUIRED", "primaryVendorCode",
                    "必须选择且仅选择一个已启用的主供应商", "vendor");
        }

        for (int index = 0; index < vendors.size(); index++) {
            CpsOnboardingVendor vendor = vendors.get(index);
            if (vendor == null || !enabled(vendor.getStatus())) {
                continue;
            }
            String vendorCode = normalize(vendor.getVendorCode());
            CpsVendorDescriptor descriptor = clientFactory.getVendorDescriptor(vendorCode, platformCode);
            if (descriptor == null || clientFactory.getVendorClient(vendorCode, platformCode) == null) {
                add(errors, "VENDOR_NOT_REGISTERED", "vendors[" + index + "].vendorCode",
                        "已启用供应商没有注册客户端", "vendor");
                continue;
            }
            CpsVendorConfig config = toVendorConfig(vendor, platformCode);
            try {
                config.setExtraConfig(parseExtraConfig(vendor.getExtraConfig()));
            } catch (Exception e) {
                add(errors, "VENDOR_CONFIG_INVALID",
                        "vendors[" + index + "].extraConfig",
                        "供应商扩展配置格式无效", "vendor");
                continue;
            }
            if (descriptor.getConfigSchema() != null) {
                CpsVendorConfigValidationResult validation =
                        descriptor.getConfigSchema().validate(config);
                if (!validation.isValid()) {
                    for (String schemaError : validation.getErrors()) {
                        String field = schemaField(schemaError);
                        add(errors, "VENDOR_CONFIG_INVALID",
                                "vendors[" + index + "]." + field,
                                "供应商必填配置缺失：" + field, "vendor");
                    }
                }
            }
            Set<CpsVendorCapability> capabilities = descriptor.getCapabilities();
            boolean hasBusinessCapability = capabilities != null && capabilities.stream()
                    .anyMatch(capability -> capability != CpsVendorCapability.CONNECTION_TEST);
            if (!hasBusinessCapability) {
                add(errors, "VENDOR_NOT_REGISTERED", "vendors[" + index + "].capabilities",
                        "供应商客户端至少需要一项真实业务能力", "vendor");
            }
        }
    }

    private void validateAdzones(CpsPlatformOnboardingPayload payload, String platformCode,
                                 List<CpsPlatformOnboardingCheckRespVO.Item> errors) {
        if (payload == null) {
            add(errors, "GENERAL_ADZONE_REQUIRED", "adzones",
                    "至少配置一个已启用的通用推广位", "adzone");
            add(errors, "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId",
                    "运行时默认推广位无效", "adzone");
            return;
        }
        List<CpsOnboardingAdzone> adzones = safeList(payload.getAdzones());
        Set<String> seen = new HashSet<>();
        boolean duplicate = false;
        for (CpsOnboardingAdzone adzone : adzones) {
            String key = adzone == null ? null : normalize(adzone.getAdzoneId());
            if (StringUtils.hasText(key) && !seen.add(key)) {
                duplicate = true;
            }
        }
        if (duplicate) {
            add(errors, "ADZONE_DUPLICATE", "adzones",
                    "推广位 ID 规范化后不能重复", "adzone");
        }
        List<CpsOnboardingAdzone> enabledGeneral = adzones.stream()
                .filter(adzone -> adzone != null
                        && enabled(adzone.getStatus())
                        && "general".equals(normalize(adzone.getAdzoneType()))
                        && platformCode != null
                        && platformCode.equals(normalize(adzone.getPlatformCode())))
                .toList();
        if (enabledGeneral.isEmpty()) {
            add(errors, "GENERAL_ADZONE_REQUIRED", "adzones",
                    "至少配置一个本平台已启用的通用推广位", "adzone");
        }
        String defaultAdzoneId = normalize(payload.getRuntimeDefaultAdzoneId());
        boolean defaultValid = StringUtils.hasText(defaultAdzoneId)
                && enabledGeneral.stream().anyMatch(adzone ->
                defaultAdzoneId.equals(normalize(adzone.getAdzoneId())));
        if (!defaultValid) {
            add(errors, "DEFAULT_ADZONE_INVALID", "runtimeDefaultAdzoneId",
                    "运行时默认推广位必须指向本平台已启用的通用推广位", "adzone");
        } else {
            String primary = normalize(payload.getPrimaryVendorCode());
            safeList(payload.getVendors()).stream()
                    .filter(vendor -> vendor != null
                            && primary != null
                            && primary.equals(normalize(vendor.getVendorCode())))
                    .findFirst()
                    .ifPresent(vendor -> vendor.setDefaultAdzoneId(defaultAdzoneId));
        }
    }

    private void validateRebates(CpsPlatformOnboardingPayload payload, String platformCode,
                                 List<CpsPlatformOnboardingCheckRespVO.Item> errors) {
        List<CpsOnboardingRebateRule> rules =
                payload == null ? List.of() : safeList(payload.getRebateRules());
        Set<String> scopes = new HashSet<>();
        boolean duplicate = false;
        boolean hasDefault = false;
        for (int index = 0; index < rules.size(); index++) {
            CpsOnboardingRebateRule rule = rules.get(index);
            String base = "rebateRules[" + index + "]";
            if (rule == null) {
                add(errors, "REBATE_RULE_INVALID", base, "返利规则不能为空", "rebate");
                continue;
            }
            String rulePlatform = normalize(rule.getPlatformCode());
            if (!StringUtils.hasText(rulePlatform)
                    || platformCode == null || !platformCode.equals(rulePlatform)) {
                add(errors, "REBATE_PLATFORM_INVALID", base + ".platformCode",
                        "返利规则平台必须与根平台一致", "rebate");
            }
            if (rule.getMemberId() != null) {
                add(errors, "REBATE_MEMBER_RULE_FORBIDDEN", base + ".memberId",
                        "平台接入工作台不允许配置个人返利规则", "rebate");
            }
            BigDecimal rate = rule.getRebateRate();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0
                    || rate.compareTo(ONE_HUNDRED) > 0) {
                add(errors, "REBATE_RATE_INVALID", base + ".rebateRate",
                        "返利比例必须在 0 到 100 之间", "rebate");
            }
            BigDecimal min = rule.getMinRebateAmount();
            BigDecimal max = rule.getMaxRebateAmount();
            if (negative(min) || negative(max)
                    || min != null && max != null && min.compareTo(max) > 0) {
                add(errors, "REBATE_AMOUNT_INVALID", base,
                        "返利金额上下限必须非负且最小值不能大于最大值", "rebate");
            }
            if (!validStatus(rule.getStatus())) {
                add(errors, "REBATE_STATUS_INVALID", base + ".status",
                        "返利规则状态只能为 0 或 1", "rebate");
            }
            if (rule.getPriority() == null || rule.getPriority() < 0) {
                add(errors, "REBATE_PRIORITY_INVALID", base + ".priority",
                        "返利规则优先级必须为非负整数", "rebate");
            }
            String scope = rule.getMemberLevelId() == null
                    ? "default:" + rulePlatform
                    : "level:" + rule.getMemberLevelId() + ":" + rulePlatform;
            if (!scopes.add(scope)) {
                duplicate = true;
            }
            if (rule.getMemberLevelId() == null && rule.getMemberId() == null
                    && platformCode != null && platformCode.equals(rulePlatform)) {
                hasDefault = true;
            }
        }
        if (duplicate) {
            add(errors, "REBATE_SCOPE_DUPLICATE", "rebateRules",
                    "规范化后的返利作用域不能重复", "rebate");
        }
        if (!hasDefault) {
            add(errors, "DEFAULT_REBATE_REQUIRED", "rebateRules",
                    "默认返利比例必填", "rebate");
        }
    }

    private CpsPlatformOnboardingPayload copy(CpsPlatformOnboardingPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(payload),
                    CpsPlatformOnboardingPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("平台接入配置无法规范化", e);
        }
    }

    static CpsVendorConfig toVendorConfig(CpsOnboardingVendor vendor, String platformCode) {
        return CpsVendorConfig.builder()
                .vendorCode(normalize(vendor.getVendorCode()))
                .vendorType(vendor.getVendorType())
                .platformCode(platformCode)
                .appKey(vendor.getAppKey())
                .appSecret(vendor.getAppSecret())
                .apiBaseUrl(vendor.getApiBaseUrl())
                .authToken(vendor.getAuthToken())
                .defaultAdzoneId(vendor.getDefaultAdzoneId())
                .build();
    }

    private Map<String, String> parseExtraConfig(String extraConfig) throws Exception {
        if (!StringUtils.hasText(extraConfig)) {
            return Map.of();
        }
        return objectMapper.readValue(extraConfig, new TypeReference<Map<String, String>>() { });
    }

    private static String schemaField(String error) {
        if (error == null) {
            return "config";
        }
        int separator = error.lastIndexOf(':');
        return separator < 0 ? "config" : error.substring(separator + 1).trim();
    }

    private static boolean enabled(Integer status) {
        return Integer.valueOf(1).equals(status);
    }

    private static boolean validStatus(Integer status) {
        return Integer.valueOf(0).equals(status) || Integer.valueOf(1).equals(status);
    }

    private static boolean negative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> Set<T> safeSet(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private static void add(List<CpsPlatformOnboardingCheckRespVO.Item> errors,
                            String code, String fieldPath, String message, String section) {
        errors.add(CpsPlatformOnboardingCheckRespVO.Item.builder()
                .code(code)
                .fieldPath(fieldPath)
                .message(message)
                .section(section)
                .build());
    }

    record ValidationResult(CpsPlatformOnboardingCheckRespVO response,
                            CpsPlatformOnboardingPayload normalizedPayload) {
    }

}
