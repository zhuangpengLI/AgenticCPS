package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CpsPlatformOnboardingDraftDeleteReqVO {

    @NotBlank(message = "平台编码不能为空")
    private String platformCode;

    private Long draftVersion;
}
