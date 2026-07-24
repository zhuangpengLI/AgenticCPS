package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Schema(description = "管理后台 - 发布平台接入配置 Request VO")
@Data
@ToString(onlyExplicitlyIncluded = true)
public class CpsPlatformOnboardingPublishReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "平台编码不能为空")
    @ToString.Include
    private String platformCode;

    @Schema(description = "草稿版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "草稿版本不能为空")
    @ToString.Include
    private Long draftVersion;

    @Schema(description = "检测通过的配置指纹", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置指纹不能为空")
    private String configFingerprint;

    @Schema(description = "发布后是否启用平台", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发布后启用标记不能为空")
    @ToString.Include
    private Boolean enableAfterPublish;

}
