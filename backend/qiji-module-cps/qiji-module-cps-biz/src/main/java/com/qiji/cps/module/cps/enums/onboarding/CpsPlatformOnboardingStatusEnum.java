package com.qiji.cps.module.cps.enums.onboarding;

import com.qiji.cps.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * Platform onboarding draft status.
 */
@Getter
@AllArgsConstructor
public enum CpsPlatformOnboardingStatusEnum implements ArrayValuable<String> {

    DRAFT("DRAFT", "草稿"),
    VALIDATING("VALIDATING", "校验中"),
    READY("READY", "待发布"),
    FAILED("FAILED", "校验失败"),
    PUBLISHED("PUBLISHED", "已发布"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(CpsPlatformOnboardingStatusEnum::getCode)
            .toArray(String[]::new);

    private final String code;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
