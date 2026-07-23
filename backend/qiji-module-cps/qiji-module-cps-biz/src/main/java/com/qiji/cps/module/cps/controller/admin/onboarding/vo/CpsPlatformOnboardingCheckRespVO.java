package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformOnboardingCheckRespVO {

    private boolean success;

    @Builder.Default
    private List<Item> items = List.of();

    public static CpsPlatformOnboardingCheckRespVO success() {
        return new CpsPlatformOnboardingCheckRespVO(true, List.of());
    }

    public static CpsPlatformOnboardingCheckRespVO failed(Item... items) {
        return new CpsPlatformOnboardingCheckRespVO(false, List.copyOf(Arrays.asList(items)));
    }

    public static CpsPlatformOnboardingCheckRespVO of(boolean success, List<Item> items) {
        return new CpsPlatformOnboardingCheckRespVO(success, List.copyOf(items));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        private String code;
        private String fieldPath;
        private String message;
        private String section;

    }

}
