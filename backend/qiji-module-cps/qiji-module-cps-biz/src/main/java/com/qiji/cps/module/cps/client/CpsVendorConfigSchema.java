package com.qiji.cps.module.cps.client;

import com.qiji.cps.module.cps.client.dto.CpsVendorConfig;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CpsVendorConfigSchema {

    private final List<CpsVendorConfigField> fields;

    public CpsVendorConfigSchema(List<CpsVendorConfigField> fields) {
        this.fields = List.copyOf(fields);
    }

    public static CpsVendorConfigSchema standard() {
        return new CpsVendorConfigSchema(List.of(
                CpsVendorConfigField.required("appKey", true),
                CpsVendorConfigField.required("appSecret", true),
                CpsVendorConfigField.required("apiBaseUrl", false),
                CpsVendorConfigField.optional("authToken", true),
                CpsVendorConfigField.optional("defaultAdzoneId", false),
                CpsVendorConfigField.optional("timeoutMs", false),
                CpsVendorConfigField.optional("rateLimitPerMinute", false),
                CpsVendorConfigField.optional("retryMaxAttempts", false)
        ));
    }

    public CpsVendorConfigValidationResult validate(CpsVendorConfig config) {
        List<String> errors = new ArrayList<>();
        for (CpsVendorConfigField field : fields) {
            if (field.isRequired() && !StringUtils.hasText(read(config, field.getName()))) {
                errors.add("missing required config field: " + field.getName());
            }
        }
        return errors.isEmpty() ? CpsVendorConfigValidationResult.valid()
                : CpsVendorConfigValidationResult.invalid(errors);
    }

    public Map<String, String> maskedSummary(CpsVendorConfig config) {
        Map<String, String> summary = new LinkedHashMap<>();
        for (CpsVendorConfigField field : fields) {
            String value = read(config, field.getName());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            summary.put(field.getName(), field.isSensitive() ? "***" : value);
        }
        return summary;
    }

    private String read(CpsVendorConfig config, String fieldName) {
        if (config == null) {
            return null;
        }
        return switch (fieldName) {
            case "appKey" -> config.getAppKey();
            case "appSecret" -> config.getAppSecret();
            case "apiBaseUrl" -> config.getApiBaseUrl();
            case "authToken" -> config.getAuthToken();
            case "defaultAdzoneId" -> config.getDefaultAdzoneId();
            default -> config.getExtraConfig() == null ? null : config.getExtraConfig().get(fieldName);
        };
    }
}
