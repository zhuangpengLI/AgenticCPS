package com.qiji.cps.module.cps.service.onboarding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiji.cps.module.cps.client.CpsApiVendorClient;
import com.qiji.cps.module.cps.client.CpsPlatformClientFactory;
import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import com.qiji.cps.module.cps.controller.admin.onboarding.vo.CpsPlatformOnboardingCheckRespVO;
import com.qiji.cps.module.cps.enums.onboarding.CpsPlatformOnboardingStatusEnum;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CpsPlatformOnboardingConnectionTester {

    private final CpsPlatformOnboardingDraftService draftService;
    private final CpsPlatformOnboardingValidator validator;
    private final CpsPlatformClientFactory clientFactory;
    private final ObjectMapper objectMapper;
    private final CpsPlatformOnboardingFingerprint fingerprint;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CpsPlatformOnboardingCheckRespVO test(String platformCode, Long draftVersion) {
        CpsPlatformOnboardingDraftService.DraftSnapshot snapshot =
                draftService.getRequiredSnapshot(platformCode, draftVersion);
        draftService.markValidating(snapshot.id(), draftVersion);

        CpsPlatformOnboardingValidator.ValidationResult validation;
        try {
            validation = validator.validateNormalized(snapshot.payload());
        } catch (RuntimeException exception) {
            return markUnexpectedFailure(snapshot.id(), draftVersion);
        }
        if (!validation.response().isSuccess()) {
            draftService.markChecked(snapshot.id(), draftVersion,
                    CpsPlatformOnboardingStatusEnum.FAILED.getCode(),
                    null, summarize(validation.response()), null);
            return validation.response();
        }

        CpsPlatformOnboardingPayload payload = validation.normalizedPayload();
        String validatedFingerprint;
        try {
            validatedFingerprint = fingerprint.calculate(payload);
        } catch (RuntimeException exception) {
            return markUnexpectedFailure(snapshot.id(), draftVersion);
        }
        String normalizedPlatform = normalize(payload.getPlatform().getPlatformCode());
        String normalizedPrimaryVendor = normalize(payload.getPrimaryVendorCode());
        List<CpsPlatformOnboardingCheckRespVO.Item> items = new ArrayList<>();
        boolean allPassed = true;
        for (CpsOnboardingVendor vendor : safeList(payload.getVendors())) {
            if (vendor == null || !Integer.valueOf(1).equals(vendor.getStatus())) {
                continue;
            }
            String vendorCode = normalize(vendor.getVendorCode());
            boolean passed = testVendor(vendor, vendorCode, normalizedPlatform);
            allPassed &= passed;
            items.add(CpsPlatformOnboardingCheckRespVO.Item.builder()
                    .code(passed ? "VENDOR_CONNECTION_OK" : "VENDOR_CONNECTION_FAILED")
                    .fieldPath("vendors." + vendorCode)
                    .section(describeVendor(vendor, vendorCode, normalizedPrimaryVendor))
                    .message(passed
                            ? "供应商连接检测通过"
                            : "供应商连接检测失败，请检查凭证和网络配置")
                    .build());
        }

        CpsPlatformOnboardingCheckRespVO response =
                CpsPlatformOnboardingCheckRespVO.of(allPassed, items);
        if (allPassed) {
            draftService.markChecked(snapshot.id(), draftVersion,
                    CpsPlatformOnboardingStatusEnum.READY.getCode(),
                    validatedFingerprint, summarize(response), LocalDateTime.now());
        } else {
            draftService.markChecked(snapshot.id(), draftVersion,
                    CpsPlatformOnboardingStatusEnum.FAILED.getCode(),
                    null, summarize(response), null);
        }
        return response;
    }

    private String describeVendor(CpsOnboardingVendor vendor, String vendorCode,
                                  String primaryVendorCode) {
        String vendorName = StringUtils.hasText(vendor.getVendorName())
                ? vendor.getVendorName().trim() : vendorCode;
        String role = vendorCode != null && vendorCode.equals(primaryVendorCode)
                ? "主供应商" : "备用供应商";
        return vendorName + "（" + role + "，" + vendorCode + "）";
    }

    private CpsPlatformOnboardingCheckRespVO markUnexpectedFailure(Long draftId,
                                                                    Long draftVersion) {
        CpsPlatformOnboardingCheckRespVO response = CpsPlatformOnboardingCheckRespVO.failed(
                CpsPlatformOnboardingCheckRespVO.Item.builder()
                        .code("ONBOARDING_TEST_FAILED")
                        .fieldPath("platform")
                        .section("platform")
                        .message("平台连接检测异常，请稍后重试")
                        .build());
        draftService.markChecked(draftId, draftVersion,
                CpsPlatformOnboardingStatusEnum.FAILED.getCode(),
                null, summarize(response), null);
        return response;
    }

    private boolean testVendor(CpsOnboardingVendor vendor, String vendorCode,
                               String platformCode) {
        try {
            CpsApiVendorClient client = clientFactory.getVendorClient(vendorCode, platformCode);
            if (client == null) {
                return false;
            }
            return client.testConnection(toVendorConfig(vendor, platformCode));
        } catch (Exception ignored) {
            // Raw vendor exception messages may contain submitted credentials.
            return false;
        }
    }

    private CpsVendorConfig toVendorConfig(CpsOnboardingVendor vendor, String platformCode) {
        CpsVendorConfig config = CpsPlatformOnboardingValidator.toVendorConfig(vendor, platformCode);
        config.setExtraConfig(parseExtraConfig(vendor.getExtraConfig()));
        return config;
    }

    private Map<String, String> parseExtraConfig(String extraConfig) {
        if (!StringUtils.hasText(extraConfig)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(extraConfig, new TypeReference<Map<String, String>>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String summarize(CpsPlatformOnboardingCheckRespVO response) {
        return response.getItems().stream()
                .map(item -> item.getCode() + ":" + item.getMessage())
                .reduce((left, right) -> left + ";" + right)
                .orElse(response.isSuccess() ? "READY" : "FAILED");
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

}
