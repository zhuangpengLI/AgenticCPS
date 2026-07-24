package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 平台配置中心聚合分页 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpsPlatformOnboardingPageRespVO {

    private String platformCode;
    private String platformName;
    private String primaryVendorCode;
    private Integer backupVendorCount;
    private String runtimeDefaultAdzoneId;
    private BigDecimal defaultRebateRate;
    private Integer completionPercent;
    private List<String> missingItems;
    private String connectionStatus;
    private Integer runtimeStatus;
    private String draftStatus;
    private LocalDateTime updateTime;
}
