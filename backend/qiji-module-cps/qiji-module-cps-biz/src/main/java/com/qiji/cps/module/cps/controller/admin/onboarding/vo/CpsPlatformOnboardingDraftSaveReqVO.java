package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Schema(description = "管理后台 - 平台接入草稿保存 Request VO")
@Data
@ToString(onlyExplicitlyIncluded = true)
public class CpsPlatformOnboardingDraftSaveReqVO {

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "taobao")
    @NotBlank(message = "平台编码不能为空")
    @ToString.Include
    private String platformCode;

    @Schema(description = "当前草稿版本；首次保存不传", example = "1")
    @ToString.Include
    private Long draftVersion;

    @Schema(description = "待保存的平台接入配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "平台接入配置不能为空")
    @Valid
    private CpsPlatformOnboardingPayload payload;

}
