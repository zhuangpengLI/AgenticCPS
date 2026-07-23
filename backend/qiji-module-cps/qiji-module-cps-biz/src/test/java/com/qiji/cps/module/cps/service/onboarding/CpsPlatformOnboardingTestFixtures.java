package com.qiji.cps.module.cps.service.onboarding;

import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingVendor;
import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class CpsPlatformOnboardingTestFixtures {

    private CpsPlatformOnboardingTestFixtures() {
    }

    static CpsPlatformOnboardingPayload validPayload() {
        CpsPlatformSaveReqVO platform = new CpsPlatformSaveReqVO();
        platform.setPlatformCode("taobao");
        platform.setPlatformName("淘宝联盟");
        platform.setPlatformLogo("https://example.test/taobao.png");
        platform.setDefaultAdzoneId("platform-default");
        platform.setPlatformServiceRate(new BigDecimal("6.00"));
        platform.setSort(10);
        platform.setStatus(1);
        platform.setExtraConfig("{\"source\":\"onboarding\"}");
        platform.setRemark("onboarding platform");
        platform.setActiveVendorCode("dataoke");

        return CpsPlatformOnboardingPayload.builder()
                .platform(platform)
                .primaryVendorCode("dataoke")
                .runtimeDefaultAdzoneId("adzone-primary")
                .vendors(new ArrayList<>(List.of(vendor("dataoke"), vendor("official"))))
                .adzones(new ArrayList<>(List.of(adzone("adzone-primary"), adzone("adzone-backup"))))
                .rebateRules(new ArrayList<>(List.of(
                        rebateRule(10L, "taobao", 20),
                        rebateRule(20L, "taobao", 10))))
                .build();
    }

    static CpsOnboardingVendor vendor(String vendorCode) {
        return CpsOnboardingVendor.builder()
                .vendorCode(vendorCode)
                .vendorName(vendorCode + " vendor")
                .vendorType("aggregator")
                .platformCode("taobao")
                .appKey(vendorCode + "-key")
                .appSecret(vendorCode + "-secret")
                .apiBaseUrl("https://example.test/" + vendorCode)
                .authToken(vendorCode + "-token")
                .defaultAdzoneId("adzone-primary")
                .extraConfig("{\"vendor\":\"" + vendorCode + "\"}")
                .priority(100)
                .status(1)
                .remark(vendorCode + " remark")
                .build();
    }

    static CpsOnboardingAdzone adzone(String adzoneId) {
        return CpsOnboardingAdzone.builder()
                .platformCode("taobao")
                .adzoneId(adzoneId)
                .adzoneName(adzoneId + " name")
                .adzoneType("general")
                .isDefault("adzone-primary".equals(adzoneId) ? 1 : 0)
                .status(1)
                .build();
    }

    static CpsOnboardingRebateRule rebateRule(Long memberLevelId, String platformCode, Integer priority) {
        return CpsOnboardingRebateRule.builder()
                .memberLevelId(memberLevelId)
                .platformCode(platformCode)
                .rebateRate(new BigDecimal("70.00"))
                .minRebateAmount(new BigDecimal("0.01"))
                .maxRebateAmount(new BigDecimal("100.00"))
                .status(1)
                .priority(priority)
                .build();
    }

}
