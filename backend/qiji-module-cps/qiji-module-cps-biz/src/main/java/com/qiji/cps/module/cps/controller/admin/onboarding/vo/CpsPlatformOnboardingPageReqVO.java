package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import com.qiji.cps.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 平台配置中心分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CpsPlatformOnboardingPageReqVO extends PageParam {

    private String keyword;

    private String platformName;

    private String platformCode;

    @Schema(description = "ALL/INCOMPLETE/READY/ENABLED/FAILED")
    private String status;
}
