package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import com.qiji.cps.module.cps.controller.admin.platform.vo.CpsPlatformSaveReqVO;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingAdzone;
import com.qiji.cps.module.cps.service.onboarding.model.CpsOnboardingRebateRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 平台接入配置 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CpsPlatformOnboardingPayloadRespVO {

    private CpsPlatformSaveReqVO platform;

    @ToString.Include
    private String primaryVendorCode;

    @ToString.Include
    private String runtimeDefaultAdzoneId;

    @Builder.Default
    private List<CpsOnboardingVendorRespVO> vendors = new ArrayList<>();

    @Builder.Default
    private List<CpsOnboardingAdzone> adzones = new ArrayList<>();

    @Builder.Default
    private List<CpsOnboardingRebateRule> rebateRules = new ArrayList<>();

    @ToString.Include(name = "platformCode")
    private String platformCodeForToString() {
        return platform == null ? null : platform.getPlatformCode();
    }

}
