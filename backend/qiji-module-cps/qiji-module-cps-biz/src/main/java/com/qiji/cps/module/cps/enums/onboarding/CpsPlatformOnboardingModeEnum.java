package com.qiji.cps.module.cps.enums.onboarding;

import com.qiji.cps.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * Platform onboarding mode.
 */
@Getter
@AllArgsConstructor
public enum CpsPlatformOnboardingModeEnum implements ArrayValuable<String> {

    CREATE("CREATE", "首次接入"),
    RECONFIGURE("RECONFIGURE", "重新配置"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(CpsPlatformOnboardingModeEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
