package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "管理后台 - 平台接入供应商 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CpsOnboardingVendorRespVO {

    @ToString.Include
    private String vendorCode;

    @ToString.Include
    private String vendorName;

    @ToString.Include
    private String vendorType;

    @ToString.Include
    private String platformCode;

    private String appKey;

    private String apiBaseUrl;

    private Boolean appSecretConfigured;

    private Boolean authTokenConfigured;

    private String defaultAdzoneId;

    private String extraConfig;

    private Integer priority;

    @ToString.Include
    private Integer status;

    private String remark;

}
