package com.qiji.cps.module.cps.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CpsVendorConfigField {

    String name;
    boolean required;
    boolean sensitive;

    public static CpsVendorConfigField required(String name, boolean sensitive) {
        return CpsVendorConfigField.builder()
                .name(name)
                .required(true)
                .sensitive(sensitive)
                .build();
    }

    public static CpsVendorConfigField optional(String name, boolean sensitive) {
        return CpsVendorConfigField.builder()
                .name(name)
                .required(false)
                .sensitive(sensitive)
                .build();
    }
}
