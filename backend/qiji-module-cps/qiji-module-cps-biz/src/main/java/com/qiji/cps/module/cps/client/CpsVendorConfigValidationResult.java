package com.qiji.cps.module.cps.client;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CpsVendorConfigValidationResult {

    boolean valid;
    List<String> errors;

    public static CpsVendorConfigValidationResult valid() {
        return CpsVendorConfigValidationResult.builder()
                .valid(true)
                .errors(List.of())
                .build();
    }

    public static CpsVendorConfigValidationResult invalid(List<String> errors) {
        return CpsVendorConfigValidationResult.builder()
                .valid(false)
                .errors(List.copyOf(errors))
                .build();
    }
}
