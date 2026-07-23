package com.qiji.cps.module.cps.controller.admin.onboarding.vo;

import com.qiji.cps.module.cps.service.onboarding.model.CpsPlatformOnboardingPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 平台接入草稿详情 Response VO")
@Data
@ToString(onlyExplicitlyIncluded = true)
public class CpsPlatformOnboardingDetailRespVO {

    @Schema(description = "草稿主键；未首次保存时为空")
    @ToString.Include
    private Long id;

    @Schema(description = "平台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ToString.Include
    private String platformCode;

    @Schema(description = "接入模式：CREATE/RECONFIGURE", requiredMode = Schema.RequiredMode.REQUIRED)
    @ToString.Include
    private String mode;

    @Schema(description = "草稿版本；未首次保存时为空")
    @ToString.Include
    private Long draftVersion;

    @Schema(description = "当前配置指纹")
    private String configFingerprint;

    @Schema(description = "最近通过检测的配置指纹")
    private String validatedFingerprint;

    @Schema(description = "草稿状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @ToString.Include
    private String status;

    @Schema(description = "检测摘要")
    private String checkSummary;

    @Schema(description = "最近检测时间")
    private LocalDateTime validatedAt;

    @Schema(description = "最近发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "平台接入配置；敏感凭证只返回 configured 标记")
    private CpsPlatformOnboardingPayload payload;

}
