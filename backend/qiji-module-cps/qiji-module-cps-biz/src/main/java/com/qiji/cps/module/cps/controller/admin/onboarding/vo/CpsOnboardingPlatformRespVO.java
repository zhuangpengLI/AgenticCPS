package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 平台接入平台配置 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CpsOnboardingPlatformRespVO {

    private Long id;

    @ToString.Include
    private String platformCode;

    @ToString.Include
    private String platformName;

    private String platformLogo;

    private String defaultAdzoneId;

    private BigDecimal platformServiceRate;

    private Integer sort;

    @ToString.Include
    private Integer status;

    private Boolean extraConfigConfigured;

    private String remark;

    private String activeVendorCode;

}
